"""Generates crud-spec.json per module (sample payloads, fixtures) and checks every sample against the real schema."""
import collections
import hashlib
import json
import os
import re
import subprocess
import uuid

import gen
import model as M

INSTITUTION = '0196f000-0000-7000-8000-00000000a001'
NS = uuid.UUID('0196f000-0000-7000-8000-000000000000')
# the migrated scratch database of catalog.sh (connection from the standard PG* environment variables)
PSQL = ['psql', '-d', os.environ.get('MYSMS_MODEL_DB', 'mysms_model'), '-v', 'ON_ERROR_STOP=1', '-X', '-q', '-At']
END_WORDS = ('end', 'to', 'until', 'expir', 'due', 'finish', 'close', 'last', 'max', 'upper', 'stop', 'returned',
             'period_end', 'valid_to', 'ends')
# per-table sample adjustments the generic rules cannot infer (check constraints across columns)
OVERRIDES = json.load(open(os.path.dirname(os.path.abspath(__file__)) + '/overrides.json'))


def uid(*parts):
    return str(uuid.uuid5(NS, '/'.join(parts)))


def chain_head(table):
    """The first column of the table's path to an institution (so fixture rows belong to the test institution)."""
    cols = {c['name']: c for c in M.CATALOG[table]['cols']}
    scope = gen.scoping({'table': table}, cols)
    return scope[1][0][0] if scope[0] == 'chain' else None


def unique_cols(table):
    out = set()
    for c in M.CATALOG[table]['cons'] or []:
        if c['type'] == 'u':
            out |= set(c['cols'])
    for i in M.CATALOG[table]['uidx'] or []:
        out |= {x for x in i['cols'] if x}
    return out - {'id', 'tenant_id'}


def fixture_id(table):
    """The one fixture row of a table; the institution fixture is the institution the contract test scopes to."""
    return INSTITUTION if table == 'org.institution' else uid('fixture', table)


def check_enums(table):
    out = {}
    for c in M.CATALOG[table]['cons'] or []:
        if c['type'] != 'c':
            continue
        m = re.match(r"CHECK \(\(\((\w+)\)::text = ANY \(\(ARRAY\[(.*?)\]\)::text\[\]\)\)\)$", c['def'])
        if m:
            out[m.group(1)] = re.findall(r"'([^']*)'::character varying", m.group(2))
            continue
        m = re.match(r"CHECK \(\(\((\w+)\)::text = '([^']*)'::text\)\)$", c['def'])
        if m:
            out[m.group(1)] = [m.group(2)]
    return out


def is_end(col):
    c = col.lower()
    return any(w in c.split('_') or c.startswith(w) or c.endswith('_' + w) for w in END_WORDS) or c.endswith('_to') \
        or c in ('ends_at', 'end_at', 'to_at', 'date_to', 'period_end')


def sample(table, col, kind, salt):
    """(sql literal, json value) for one column."""
    info = next(c for c in M.CATALOG[table]['cols'] if c['name'] == col)
    base = info['base']
    typ = info['type']
    enums = check_enums(table)
    if col in enums:
        v = enums[col][0]
        return "'%s'" % v, v
    if base == 'uuid':
        if col == 'institution_id':
            return "'%s'" % INSTITUTION, INSTITUTION
        v = uid(table, col, salt)
        return "'%s'" % v, v
    if base in ('varchar', 'text', 'bpchar'):
        m = re.search(r'\((\d+)\)', typ)
        n = int(m.group(1)) if m else 60
        if base == 'bpchar':
            v = ('ABCDEFGH' + hashlib.md5((table + col + salt).encode()).hexdigest().upper())[:n]
            v = re.sub(r'[^A-Z]', 'X', v)
        else:
            h = hashlib.md5((table + col + salt).encode()).hexdigest()[:8]
            v = ('t-' + col.replace('_', '-') + '-' + h)
            if 'email' in col:
                v = 'u' + h + '@example.org'
            elif 'url' in col or col.endswith('_uri') or col == 'website':
                v = 'https://example.org/' + h
            elif 'phone' in col or 'mobile' in col:
                v = '+9198' + str(int(h, 16) % 10 ** 8).zfill(8)
            elif 'color' in col or 'colour' in col:
                v = '#336699'
            elif col in ('code', ) or col.endswith('_code'):
                v = ('C' + h.upper())
            v = v[:n]
        return "'%s'" % v, v
    if base in ('int2', 'int4', 'int8'):
        v = 2 if is_end(col) else 1
        if col in unique_cols(table):
            v += {'fixture': 0, 'api': 2}.get(salt, 4)
        return str(v), (str(v) if base == 'int8' else v)
    if base == 'numeric':
        v = '2' if is_end(col) else '1'
        return v, v
    if base == 'bool':
        return 'false', False
    if base == 'date':
        v = '2026-01-20' if is_end(col) else '2026-01-10'
        return "'%s'" % v, v
    if base == 'timestamptz':
        v = '2026-01-10T10:00:00Z' if is_end(col) else '2026-01-10T09:00:00Z'
        return "'%s'" % v, v
    if base == 'time':
        v = '10:00' if is_end(col) else '09:00'
        return "'%s'" % v, v
    if base in ('_varchar', '_text'):
        return "ARRAY['a']", ['a']
    if base == 'inet':
        return "'10.0.0.1'", '10.0.0.1'
    if base == 'jsonb':
        return "'{}'::jsonb", {}
    raise ValueError('no sample for %s.%s %s' % (table, col, base))


class Fixtures:
    """Minimal parent rows (one per table) for in-schema foreign keys, inserted before the sample rows."""

    def __init__(self, module):
        self.module = module
        self.rows = collections.OrderedDict()

    def row_id(self, table):
        if table not in self.rows:
            self.build(table)
        return fixture_id(table)

    def alt_id(self, table):
        key = table + '#alt'
        if key not in self.rows:
            self.build(table, salt='alt')
        return uid('fixture-alt', table)

    def build(self, table, depth=0, salt='fixture'):
        if depth > 6:
            raise ValueError('fixture recursion at ' + table)
        values = self.values(table, salt, set(), depth)
        values['id'] = "'%s'" % (fixture_id(table) if salt == 'fixture' else uid('fixture-alt', table))
        if salt != 'fixture':
            table_key = table + '#alt'
        else:
            table_key = table
        cols = list(values)
        sql = 'INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (id) DO NOTHING' % (
            table, ', '.join(cols), ', '.join(values[c] for c in cols))
        self.rows[table_key] = sql

    def values(self, table, salt, given, depth):
        info = M.CATALOG[table]
        fks = {fk['col']: fk['ref'] for fk in gen.fk_cons(table)}
        ov = OVERRIDES.get(table, {})
        values = collections.OrderedDict()
        for c in info['cols']:
            name = c['name']
            if name in ('id', 'created_at', 'updated_at', 'created_by', 'updated_by', 'deleted_at'):
                continue
            if name == 'tenant_id':
                values[name] = ':tenant'
                continue
            if name == 'row_version':
                values[name] = '1'
                continue
            if name in given:
                continue
            if name in ov.get('fixture', {}):
                lit = ov['fixture'][name]
                if lit is None:
                    continue
                if isinstance(lit, str) and lit.startswith('@fk:'):
                    values[name] = "'%s'" % self.row_id(lit[4:])
                else:
                    values[name] = lit
                continue
            need = c['nn'] and c['default'] is None and not c['generated'] and not c['identity']
            if not need and name != 'institution_id' and name != chain_head(table):
                continue
            if name in fks and fks[name].split('.')[0] == self.module:
                ref = fks[name]
                if ref == table:
                    continue
                if salt != 'fixture' and name in unique_cols(table):
                    values[name] = "'%s'" % self.alt_id(ref)
                    continue
                if ref not in self.rows:
                    self.build(ref, depth + 1)
                values[name] = "'%s'" % fixture_id(ref)
            else:
                values[name] = sample(table, name, 'fixture', salt)[0]
        return values


def body_for(a, fx):
    """The JSON create body and the SQL row it should produce (for the schema check)."""
    table = a['table']
    ov = OVERRIDES.get(table, {})
    body = collections.OrderedDict()
    fks = {fk['col']: fk['ref'] for fk in gen.fk_cons(table)}
    for f in a['fields']:
        if f['ro'] and not f['from_path']:
            continue
        col = f['col']
        if col in ov.get('api', {}):
            v = ov['api'][col]
            if v is None:
                continue
            if isinstance(v, str) and v.startswith('@fk:'):
                v = fx.row_id(v[4:])
            elif isinstance(v, str) and v.startswith('@fkalt:'):
                v = fx.alt_id(v[7:])
            body[f['name']] = v
            continue
        if f['conv'] == 'secret':
            # plaintext in, never out: long enough that the mask shows only its last four characters
            if 'Create' in a['methods']:
                h = hashlib.md5((table + col).encode()).hexdigest()[:6]
                body[f['name']] = ('Secret-' + h + '-00001234')[-int(f['prop'].get('maxLength', 60)):]
            continue
        required = f['name'] in a['dto'].get('required', ()) or (f['nn'] and not f['has_default']) or f['from_path'] \
            or bool(f['prop'].get('x-sample'))
        chain_col = a['scope'][0] == 'chain' and col == a['scope'][1][0][0]
        required = required or f['name'] in required_props(a) or col == 'institution_id' or chain_col
        if not required:
            continue
        if col in fks and fks[col].split('.')[0] == a['module']:
            ref = fks[col]
            if ref == table:
                continue
            # a reference inside a unique key points at a second parent row, so the sample never collides with a
            # fixture row of the same table that another resource's test inserted
            in_unique = any(col in u['cols'] for u in a['uniques']) and not f['from_path'] and not chain_col \
                and ref != 'org.institution'
            body[f['name']] = fx.alt_id(ref) if in_unique else fx.row_id(ref)
            continue
        lit, val = sample(table, col, 'api', 'api')
        if f['conv'] == 'enum' or f['conv'] == 'enumlist':
            enum_vals = enum_values(a, f)
            if f['conv'] == 'enum':
                db = check_enums(table).get(col)
                pick = next((x for x in enum_vals if not db or x in db), enum_vals[0])
                val = pick
            else:
                val = [enum_vals[0]]
        if f['conv'] == 'duration':
            val = '%ds' % (int(val) * (60 if col.endswith('_minutes') else 1))
        if f['prop'].get('maxLength') and isinstance(val, str):
            val = val[:f['prop']['maxLength']]
        if f['prop'].get('minimum') is not None and isinstance(val, int):
            val = max(val, int(f['prop']['minimum']))
        if f['prop'].get('pattern') == '^\\d+(\\.\\d+)?$' or f['prop'].get('pattern') == '^-?\\d+(\\.\\d+)?$':
            val = str(val)
        body[f['name']] = val
    return body


def required_props(a):
    sch = a['r']['mod'].schemas[a['name']]
    return set(sch.get('required', []))


def enum_values(a, f):
    t = f['dto_type']
    inner = re.match(r'List<(.+)>', t).group(1) if t.startswith('List<') else t
    if inner.endswith('Enum') and '.' not in inner:
        return a['dto']['enums'][inner]
    import javasrc
    m = javasrc.model(a['module'], inner)
    return m['enum_values']


def to_sql_literal(a, f, v):
    if v is None:
        return 'NULL'
    if isinstance(v, bool):
        return 'true' if v else 'false'
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, list):
        return 'ARRAY[%s]::text[]' % ', '.join("'%s'" % x for x in v)
    if isinstance(v, dict):
        return "'%s'::jsonb" % json.dumps(v)
    return "'%s'" % str(v).replace("'", "''")


def update_for(a, body):
    """One writable field and a new value for it (not a key, reference or scope column)."""
    ov = OVERRIDES.get(a['table'], {})
    if 'update' in ov:
        return ov['update']
    uniq = {c for u in a['uniques'] for c in u['cols']}
    fkcols = {fk['col'] for fk in gen.fk_cons(a['table'])}
    best = None
    for f in a['fields']:
        if not f['writable'] or f['ro'] or f['col'] in uniq or f['col'] in fkcols or f['col'] == 'institution_id' \
                or f['conv'] == 'secret':
            continue
        if f['base'] == 'uuid' or f['conv'] in ('enum', 'enumlist') or f['col'] in check_cols(a['table']):
            continue
        if f['base'] in ('varchar', 'text') and not f['prop'].get('pattern') and not f['prop'].get('format'):
            n = f['prop'].get('maxLength', 60)
            return {f['name']: ('Updated ' + f['col'])[:n]}
        if f['base'] == 'bool' and best is None:
            best = {f['name']: True}
    return best or {}


def check_cols(table):
    cols = set()
    for c in M.CATALOG[table]['cons'] or []:
        if c['type'] == 'c':
            cols |= set(re.findall(r'\b([a-z_][a-z0-9_]*)\b', c['def']))
    return cols


def transition_specs(a, parent_id, sample_id):
    """The transitions of the resource for the contract test, and the SQL that proves each against the schema:
    the sample row moved into the guarded state (prepare), then the transition's own UPDATE."""
    specs = []
    proofs = []
    for t in a['transitions']:
        path = t['path']
        names = re.findall(r'\{(\w+)\}', path)
        if t['nested']:
            path = path.replace('{%s}' % names[0], parent_id)
        path = path.replace('{%s}' % names[-1], '{id}')
        body = collections.OrderedDict()
        if t['reason']:
            body['reason'] = 'Contract test reason'
        params = {}
        for col, v in t['set'].items():
            if isinstance(v, str) and v.startswith('$param:'):
                name = v[7:]
                typ = t['req_fields'][name]
                body[name] = '2026-02-01T09:00:00Z' if typ == 'OffsetDateTime' else 'Contract test note'
                params[name] = body[name]
        prepare = []
        if t['guard']:
            sets = ', '.join('%s = %s' % (c, to_sql_literal(a, None, vs[0])) for c, vs in t['guard'].items())
            prepare.append("UPDATE %s SET %s WHERE id = '{id}'" % (a['table'], sets))
        sets = []
        for c, v in t['set'].items():
            lit = gen.sql_value(c, v)
            lit = {':now': 'now()', ':today': 'current_date', ':actor': "'%s'" % uid('actor')}.get(lit, lit)
            if lit.startswith(':p'):
                lit = to_sql_literal(a, None, params[lit[2].lower() + lit[3:]])
            sets.append('%s = %s' % (c, lit))
        proofs += [x.replace('{id}', sample_id) for x in prepare]
        proofs.append("UPDATE %s SET %s WHERE id = '%s'" % (a['table'], ', '.join(sets), sample_id))
        literal = collections.OrderedDict()
        stamped = []
        for c, v in t['set'].items():
            if c not in a['by_col']:
                continue
            name = a['by_col'][c]['name']
            if isinstance(v, str) and v.startswith('$'):
                stamped.append(name)
            else:
                literal[name] = v
        specs.append(collections.OrderedDict(
            operation=t['op'], path=path, permission=t['permission'],
            guard={a['by_col'][c]['name']: vs for c, vs in t['guard'].items()}, prepare=prepare, body=body,
            sets=literal, stamps=stamped, etag=t['has_etag'], ifMatch=t['if_match']))
    return specs, proofs


def verify(statements):
    """Runs the statements in one rolled-back transaction as superuser against the migrated model database."""
    tenant = uid('tenant')
    script = 'BEGIN;\n' + '\n'.join(s.replace(':tenant', "'%s'" % tenant) + ';' for s in statements) + '\nROLLBACK;\n'
    p = subprocess.run(PSQL, input=script, capture_output=True, text=True)
    return p.returncode == 0, p.stderr.strip()


def main():
    resources, _ = M.build()
    analysed = []
    for r in resources:
        try:
            analysed.append(gen.analyse(r))
        except gen.Skip:
            pass
    by_module = collections.defaultdict(list)
    failures = []
    for a in analysed:
        fx = Fixtures(a['module'])
        if a['module'] == 'platform':
            fx.rows['platform.tenant'] = ("INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, "
                                          "'t' || substr(md5(CAST(:tenant AS text)), 1, 12), 'Test tenant') "
                                          "ON CONFLICT (id) DO NOTHING")
        if a['parent']:
            # the parent row comes from a fixture of the parent's table
            ref = next((fk['ref'] for fk in gen.fk_cons(a['table']) if fk['col'] == a['parent']['col']), None)
            if ref is None:
                failures.append((a['table'], 'parent without foreign key'))
                continue
            parent_id = fx.row_id(ref)
        body = body_for(a, fx)
        if a['scope'][0] == 'chain':
            pass
        meths = a['methods']
        coll = meths.get('List', meths.get('Create', {})).get('path')
        item = meths.get('Get', meths.get('Update', meths.get('Delete', {}))).get('path')
        if a['parent']:
            pname = a['r']['parent']
            coll = coll and coll.replace('{%s}' % pname, parent_id)
            item = item and item.replace('{%s}' % pname, parent_id)
        if item:
            item = re.sub(r'\{\w+\}$', '{id}', item)
        if coll is None:
            failures.append((a['table'], 'no collection path'))
            continue
        # the row the API create (or the seed) produces, checked against the schema
        cols = collections.OrderedDict()
        for f in a['fields']:
            if f['name'] in body:
                v = body[f['name']]
                if f['conv'] == 'duration':
                    v = int(v[:-1]) // (60 if f['col'].endswith('_minutes') else 1)
                cols[f['col']] = to_sql_literal(a, f, v)
        for f in a['fields']:
            if f.get('copied'):  # read from the parent row, as the generated INSERT does
                cols[f['col']] = "(SELECT p.%s FROM %s p WHERE p.id = '%s')" % (f['copied']['col'],
                                                                                f['copied']['table'], parent_id)
            elif f['from_path'] and f['col'] not in cols:
                cols[f['col']] = "'%s'" % parent_id
        if not a['glob']:
            cols['tenant_id'] = ':tenant'
        cols['row_version'] = '1'
        sample_id = uid('sample', a['table'])
        cols['id'] = "'%s'" % sample_id
        insert = 'INSERT INTO %s (%s) VALUES (%s)' % (a['table'], ', '.join(cols), ', '.join(cols.values()))
        update = update_for(a, body)
        statements = list(fx.rows.values()) + [insert]
        if update:
            f = next(x for x in a['fields'] if x['name'] == list(update)[0])
            statements.append("UPDATE %s SET %s = %s WHERE id = '%s'" % (a['table'], f['col'],
                                                                         to_sql_literal(a, f, list(update.values())[0]),
                                                                         sample_id))
        transitions, proofs = transition_specs(a, parent_id if a['parent'] else None, sample_id)
        statements += proofs
        ok, err = verify(statements)
        if not ok:
            failures.append((a['table'], err.split('\n')[0][:300]))
            continue
        dup = any(all(a['by_col'][c]['name'] in body or a['by_col'][c]['from_path'] for c in u['cols'])
                  and not u['nnd'] for u in a['uniques'])
        spec = collections.OrderedDict(
            name=a['name'], table=a['table'], ops=sorted(meths), collection=coll, item=item, scope='global' if a['glob'] else a['scope'][0],
            fixtures=list(fx.rows.values()), create=body, update=update, duplicateConflicts=dup)
        create_op = a['r']['ops'].get('Create')
        if create_op and {} in (create_op['op'].get('security') or []):
            spec['publicCreate'] = True  # anyone may create (a public form): no permission check on Create
        if a['partition']:
            spec['partition'] = a['partition']['field']
        if gen.rules_access(a):
            spec['ownerOnly'] = parent_id  # the parent user; see the Rules' beforeAccess
        if transitions:
            spec['transitions'] = transitions
        secrets = []
        for f in a['fields']:
            if f['conv'] != 'secret' or f['name'] not in body:
                continue
            masked = next((m['name'] for m in a['masked'] if m['of'] == f['name']), None) or next(
                (x['name'] for x in a['fields'] if x['masked_of'] == f['name']), None)
            plain = body[f['name']]
            rotated = 'Rotated-secret-5678' if f['writable'] and 'Update' in a['methods'] else None
            secrets.append(collections.OrderedDict(
                field=f['name'], masked=masked, mask='••••' + plain[-4:], rotated=rotated,
                # the stored value must be an envelope, never the plaintext (run after create)
                check="DO $$ BEGIN IF EXISTS (SELECT 1 FROM %s WHERE id = '{id}' AND (%s IS NULL OR %s NOT LIKE 'v1.%%' "
                      "OR strpos(%s, '{plain}') > 0)) THEN RAISE EXCEPTION 'secret stored in clear'; END IF; END $$"
                      % (a['table'], f['col'], f['col'], f['col'])))
        if secrets:
            spec['secrets'] = secrets
        rows = [insert.replace(sample_id, uid('module-run', a['table']))] + [
            x.replace(sample_id, uid('module-run', a['table'])) for x in statements[len(fx.rows) + 1:]]
        rows.append("UPDATE %s SET deleted_at = now() WHERE id = '%s'" % (a['table'], uid('module-run', a['table'])))
        spec['_rows'] = rows
        if 'Create' not in meths:
            spec['seedId'] = sample_id
            spec['seed'] = [insert]
        by_module[a['module']].append(spec)
    # the whole module as its contract test runs it: one shared database, resources one after the other, each sample
    # row soft-deleted at the end of its turn
    for mod, specs in sorted(by_module.items()):
        statements = []
        for spec in specs:
            statements += spec['fixtures'] + spec.pop('_rows')
        ok, err = verify(statements)
        if not ok:
            failures.append((mod + ' (whole module)', err.split('\n')[0][:300]))
    # reference cleanup (gen_events.py): one dependent row per action, referencing an owner that is then deleted
    import gen_events
    cleanup_actions, _ = gen_events.actions()
    cleanup = collections.defaultdict(list)
    for act in cleanup_actions:
        table = act['module'] + '.' + act['table']
        fx = Fixtures(act['module'])
        if act['module'] == 'platform':
            fx.rows['platform.tenant'] = ("INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, "
                                          "'t' || substr(md5(CAST(:tenant AS text)), 1, 12), 'Test tenant') "
                                          "ON CONFLICT (id) DO NOTHING")
        owner = uid('cleanup-owner', table, act['col'])
        rows = {}
        for which in ('row', 'other'):
            values = fx.values(table, 'cleanup-' + which + '-' + act['col'], {act['col']}, 0)
            # values a check constraint ties to the column ("*": every cleanup row of the table)
            tied = OVERRIDES.get(table, {}).get('cleanup', {})
            for name, lit in list(tied.get('*', {}).items()) + list(tied.get(act['col'], {}).items()):
                values[name] = "'%s'" % fx.row_id(lit[4:]) if lit.startswith('@fk:') else lit
            # the other tenant's row names the same owner (a deletion in one tenant must not touch it), unless the
            # column is unique across tenants
            if which == 'other' and act['col'] in unique_cols(table):
                values[act['col']] = "'%s'" % uid('cleanup-other-owner', table, act['col'])
            else:
                values[act['col']] = "'%s'" % owner
            values['id'] = "'%s'" % uid('cleanup-' + which, table, act['col'])
            cols = list(values)
            rows[which] = 'INSERT INTO %s (%s) VALUES (%s)' % (table, ', '.join(cols), ', '.join(values[c] for c in cols))
        # the other tenant gets parent rows of its own (in-module foreign keys may include the tenant)
        renamed = {INSTITUTION: uid('cleanup-other-institution')}  # unique keys within an institution stay apart
        for key in fx.rows:
            base_table = key.split('#')[0]
            if base_table not in M.CATALOG or not any(c['name'] == 'tenant_id' for c in M.CATALOG[base_table]['cols']):
                continue  # a global table's row is shared by every tenant
            original = uid('fixture-alt', base_table) if key.endswith('#alt') else fixture_id(base_table)
            renamed[original] = uid('cleanup-other-parent', original)
        keep = {uid('cleanup-other', table, act['col'])} | ({owner} if act['col'] not in unique_cols(table) else set())
        uuid_re = r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

        def is_global(sql):
            target = re.match(r'INSERT INTO (\S+)', sql).group(1)
            return target in M.CATALOG and not any(c['name'] == 'tenant_id' for c in M.CATALOG[target]['cols'])
        # rows of global tables are shared by every tenant: their ids (the last value: id comes last) stay
        global_ids = {re.findall(uuid_re, x)[-1] for x in fx.rows.values() if is_global(x) and re.findall(uuid_re, x)}

        def other_tenant(sql):
            # every id of the other tenant's rows is its own (unique keys over logical references may span
            # tenants), except the deleted owner it shares and the rows of global tables
            def swap(match):
                found = match.group(0)
                if found in keep or found in global_ids:
                    return found
                return renamed.get(found) or uid('cleanup-other-uuid', found)
            return sql if is_global(sql) else re.sub(uuid_re, swap, sql)
        entry = collections.OrderedDict(
            event=act['owner'] + '.deleted', table=table, column=act['col'], action=act['action'],
            ownerId=owner, rowId=uid('cleanup-row', table, act['col']), otherRowId=uid('cleanup-other', table, act['col']),
            fixtures=list(fx.rows.values()), row=rows['row'],
            otherFixtures=[other_tenant(x) for x in fx.rows.values()], other=other_tenant(rows['other']),
            softDeletable=any(c['name'] == 'deleted_at' for c in M.CATALOG[table]['cols']))
        ok, err = verify(entry['fixtures'] + [entry['row']])
        if not ok:
            failures.append((table + '.' + act['col'] + ' (cleanup)', err.split('\n')[0][:300]))
        cleanup[act['module']].append(entry)
    for mod, entries in sorted(cleanup.items()):
        path = gen.ROOT + mod + '/src/component-test/resources/reference-cleanup.json'
        json.dump({'module': mod, 'cleanup': entries}, open(path, 'w'), indent=1)
    for mod, specs in by_module.items():
        path = gen.ROOT + mod + '/src/component-test/resources/crud-spec.json'
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)
        json.dump({'module': mod, 'resources': specs}, open(path, 'w'), indent=1)
    print('specs', sum(len(v) for v in by_module.values()), 'failures', len(failures))
    for t, e in failures:
        print(' ', t, '|', e)
    json.dump(failures, open(gen.WORK + 'spec_failures.json', 'w'), indent=1)


if __name__ == '__main__':
    main()
