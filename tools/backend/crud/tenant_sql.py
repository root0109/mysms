"""Adds the explicit tenant predicate to SQL (JAVA-CODING-RULES §1.6, review check R1).

Every SELECT, UPDATE and DELETE over a tenant table states `tenant_id = :tenantId` itself: after the WHERE of the
statement (or subquery) that reads the table, and in the ON of each joined tenant table. core's TenantJdbcClient binds
:tenantId from the call context, so row-level security stays the backstop instead of the mechanism. Used by gen.py
for generated repositories and by apply_tenant_sql.py for hand-written ones; it never changes a statement's meaning:
a WHERE with a top-level OR is parenthesised before the predicate is added.
"""
import json
import os
import re

TOKEN = re.compile(r"'(?:[^']|'')*'|\$\$.*?\$\$|--[^\n]*|[A-Za-z_][\w.]*|:\w+|::|\S", re.S)
STOP = {'WHERE', 'SET', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'FULL', 'CROSS', 'ON', 'ORDER', 'GROUP', 'LIMIT', 'OFFSET',
        'FOR', 'AS', 'USING', 'RETURNING', 'UNION', 'WINDOW', 'HAVING', 'EXCEPT', 'INTERSECT', 'VALUES', 'SELECT',
        'NATURAL', 'LATERAL', 'TABLESAMPLE', 'AND', 'OR', 'NOT'}
CLAUSE_END = {'ORDER', 'GROUP', 'LIMIT', 'OFFSET', 'FOR', 'RETURNING', 'HAVING', 'UNION', 'WINDOW', 'EXCEPT',
              'INTERSECT'}


class Unsupported(Exception):
    pass


def tenant_tables():
    """Every table with a tenant column, from the schema catalogue the generator reads (target/catalog.json)."""
    catalog = json.load(open(os.path.dirname(os.path.abspath(__file__)) + '/target/catalog.json'))
    return {t for t, info in catalog.items() if any(c['name'] == 'tenant_id' for c in info['cols'])}


def rewrite_java(src, tables):
    """Java source with the predicate added to every text block that holds SQL."""
    def fix(m):
        body = m.group(2)
        if not re.search(r'\b(SELECT|UPDATE|DELETE)\b', body):
            return m.group(0)
        try:
            return m.group(1) + add_tenant(body, tables) + m.group(3)
        except Unsupported as e:
            print('  left as is (%s):' % e, ' '.join(body.split())[:100])
            return m.group(0)
    return re.sub(r'(""")(.*?)(""")', fix, src, flags=re.S)


def _tokens(sql):
    return [(m.start(), m.end(), m.group(0)) for m in TOKEN.finditer(sql)]


def add_tenant(sql, tenant_tables):
    """The SQL with the tenant predicate added wherever a tenant table is read or changed (idempotent)."""
    toks = _tokens(sql)
    inserts = []  # (position, text)
    frames = [dict(base=None, joins=0, where=False, start=0)]
    i = 0
    n = len(toks)

    def word(k):
        return toks[k][2].upper() if k < n else ''

    def table_at(k):
        """(table, alias, next index) when token k names a tenant table."""
        if k >= n or toks[k][2].lower() not in tenant_tables:
            return None
        alias = None
        j = k + 1
        if word(j) == 'AS':
            j += 1
        if j < n and re.match(r'^[A-Za-z_]\w*$', toks[j][2]) and toks[j][2].upper() not in STOP:
            alias = toks[j][2]
            j += 1
        return toks[k][2].lower(), alias, j

    def clause_has_top_or(k):
        """Whether the clause starting after token k has an OR at its own depth (until it ends)."""
        depth = 0
        for j in range(k, n):
            t = toks[j][2].upper()
            if t == '(':
                depth += 1
            elif t == ')':
                if depth == 0:
                    return False, j
                depth -= 1
            elif depth == 0 and t in CLAUSE_END | {'JOIN', 'WHERE', 'LEFT', 'INNER'} and j > k:
                return False, j
            elif depth == 0 and t == 'OR':
                end = j
                d2 = 0
                for e in range(j, n):
                    u = toks[e][2].upper()
                    if u == '(':
                        d2 += 1
                    elif u == ')':
                        if d2 == 0:
                            return True, e
                        d2 -= 1
                    elif d2 == 0 and u in CLAUSE_END | {'JOIN', 'WHERE', 'LEFT', 'INNER'}:
                        return True, e
                return True, n
        return False, n

    def own_predicate(k, end, ref):
        """Whether tokens k..end hold `[ref.]tenant_id = :tenantId` at their own depth and outside any OR (a predicate
        inside a sub-select or an OR branch does not filter this table)."""
        depth = 0
        level = []
        for j in range(k, min(end, n)):
            w = toks[j][2]
            if w == '(':
                depth += 1
            elif w == ')':
                depth -= 1
            elif depth == 0:
                if w.upper() == 'OR':
                    return False
                level.append(w)
        text = ' '.join(level)
        name = re.escape(ref.rstrip('.')) if ref else None
        pattern = (r'(^|\s)%s\s*\.\s*tenant_id\s*=\s*:\s*tenantId' % name) if name else r'(^|\s)tenant_id\s*=\s*:\s*tenantId'
        return re.search(pattern, text) is not None

    while i < n:
        t = toks[i][2]
        u = t.upper()
        f = frames[-1]
        if u in ('UNION', 'INTERSECT', 'EXCEPT'):
            raise Unsupported('a compound statement (%s); add the tenant predicate to each part by hand' % u)
        if u == 'USING' and f['base'] is not None and not f['base']['done'] and f.get('verb') == 'DELETE':
            raise Unsupported('DELETE ... USING; add the tenant predicate for every table by hand')
        if u == 'FROM' and f['base'] is not None and f.get('verb') == 'UPDATE' and not f['base']['done']:
            raise Unsupported('UPDATE ... FROM; add the tenant predicate for every table by hand')
        if u in ('DELETE', 'UPDATE', 'SELECT'):
            f.setdefault('verb', u)
        if t == '(':
            frames.append(dict(base=None, joins=0, where=False, start=i))
        elif t == ')':
            if len(frames) == 1:
                raise Unsupported('unbalanced ")" in a SQL fragment; add the tenant predicate by hand')
            _close(frames.pop(), toks, i, inserts)
        elif u in ('FROM', 'UPDATE') and f['base'] is None and not (u == 'FROM' and word(i - 1) in ('DISTINCT',)):
            found = table_at(i + 1)
            if found:
                f['base'] = dict(table=found[0], alias=found[1], done=False)
                i = found[2]
                if i < n and toks[i][2] == ',':
                    raise Unsupported('a comma join after %s; write it as JOIN ... ON' % found[0])
                continue
        elif u == 'JOIN':
            found = table_at(i + 1)
            if found:
                j = found[2]
                if word(j) == 'ON':
                    ref = (found[1] or found[0].split('.')[1])
                    has_or, end = clause_has_top_or(j + 1)
                    if not own_predicate(j + 1, end, ref + '.'):
                        inserts.append((toks[j][1], ' %s.tenant_id = :tenantId AND' % ref))
                        if has_or:
                            inserts.append((toks[j + 1][0], '('))
                            inserts.append((toks[end - 1][1], ')'))
                f['joins'] += 1
                i = j
                continue
        elif u == 'WHERE' and f['base'] and not f['base']['done']:
            f['where'] = True
            f['base']['done'] = True
            has_or, end = clause_has_top_or(i + 1)
            ref = _ref(f)
            if not own_predicate(i + 1, end, ref):
                _where_insert(sql, toks, i, end, ref, has_or, inserts)
        elif u in CLAUSE_END and f['base'] and not f['base']['done']:
            _close(f, toks, i, inserts, before=True)
        i += 1
    while frames:
        _close(frames.pop(), toks, n, inserts)
    out = sql
    for pos, text in sorted(inserts, key=lambda x: x[0], reverse=True):
        out = out[:pos] + text + out[pos:]
    return out


def _ref(frame):
    base = frame['base']
    if base['alias']:
        return base['alias'] + '.'
    return base['table'].split('.')[1] + '.' if frame['joins'] else ''


def _where_insert(sql, toks, i, end, ref, has_or, inserts):
    pos = toks[i][1]
    line_start = sql.rfind('\n', 0, toks[i][0]) + 1
    line_end = sql.find('\n', pos)
    line_end = len(sql) if line_end < 0 else line_end
    before = sql[line_start:toks[i][0]]
    pred = '%stenant_id = :tenantId' % ref
    lead = before[:len(before) - len(before.lstrip())]
    if before.strip() == '':
        # WHERE opens its line: the predicate takes the line, the old condition follows aligned on AND
        inserts.append((pos, ' ' + pred + '\n' + before + '  AND'))
    elif (line_end - line_start) + len(pred) + 6 > 118:
        inserts.append((pos, ' ' + pred + '\n' + lead + '    AND'))
    else:
        inserts.append((pos, ' ' + pred + ' AND'))
    if has_or:
        inserts.append((toks[i + 1][0], '('))
        close_at = toks[end - 1][1] if end - 1 < len(toks) else len(sql.rstrip())
        inserts.append((close_at, ')'))


def _close(frame, toks, i, inserts, before=False):
    base = frame.get('base')
    if base and not base['done']:
        base['done'] = True
        raise Unsupported('a statement over %s has no WHERE in this text; add the tenant predicate by hand'
                          % base['table'])
