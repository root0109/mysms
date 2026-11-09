"""Builds the CRUD model: OpenAPI standard operations (List/Get/Create/Update/Delete) joined with the live catalog."""
import collections
import glob
import json
import os
import re

import yaml

TOOLS = os.path.dirname(os.path.abspath(__file__)) + '/'
OA = os.path.normpath(TOOLS + '../../../api/openapi') + '/'
CATALOG = json.load(open(TOOLS + 'target/catalog.json'))
COMMON = yaml.safe_load(open(OA + 'common.yaml'))
VERBS = ('List', 'Get', 'Create', 'Update', 'Delete')
AUDIT = {'id', 'tenant_id', 'created_at', 'created_by', 'updated_at', 'updated_by', 'deleted_at', 'row_version'}
# path parameters whose column is named after the referenced table's role rather than the parameter
PARENT_COLUMNS = {'userAccountId': 'user_id', 'audienceGroupId': 'group_id', 'feedbackThreadId': 'thread_id',
                  'notificationPreferenceSetId': 'preference_set_id', 'programmingExerciseId': 'exercise_id',
                  'attendanceSheetId': 'sheet_id'}
# contract resources stored in a table of another name (the contract disambiguates names the schema does not need to)
TABLE_ALIASES = {('lms', 'LmsCourse'): 'lms.course', ('lms', 'LmsAssignment'): 'lms.assignment',
                 ('lms', 'LmsAssignmentSubmission'): 'lms.assignment_submission'}
OUT_ONLY = {'id': 'id', 'createTime': 'created_at', 'updateTime': 'updated_at', 'createdBy': 'created_by',
            'updatedBy': 'updated_by'}


def snake(s):
    return re.sub(r'(?<=[a-z0-9])(?=[A-Z])', '_', s).lower()


def camel(s):
    p = s.split('_')
    return p[0] + ''.join(x[:1].upper() + x[1:] for x in p[1:])


def pascal(s):
    c = camel(s)
    return c[:1].upper() + c[1:]


def ref_name(r):
    return r.split('/')[-1]


class Module:
    def __init__(self, name):
        self.name = name
        self.doc = yaml.safe_load(open(OA + name + '.yaml'))
        self.schemas = self.doc['components']['schemas']

    def resolve(self, s):
        while s and '$ref' in s:
            r = s['$ref']
            if r.startswith('./common.yaml'):
                s = COMMON['components']['schemas'][ref_name(r)]
            else:
                s = self.schemas[ref_name(r)]
        return s


def java_type(mod, prop):
    """(kind, java type, detail) of one contract property, as OpenAPI Generator renders it."""
    if '$ref' in prop:
        target = mod.resolve(prop)
        name = ref_name(prop['$ref'])
        if target.get('enum'):
            return ('enumref', name, target)
        if target.get('type') == 'object' or 'properties' in target:
            return ('object', name, target)
        return java_type(mod, target)
    if 'allOf' in prop and len(prop['allOf']) == 1:
        merged = dict(prop)
        merged.pop('allOf')
        merged.update(prop['allOf'][0])
        return java_type(mod, merged)
    t = prop.get('type')
    f = prop.get('format')
    if t == 'string':
        if prop.get('enum'):
            return ('enum', None, prop)
        return {'uuid': ('scalar', 'UUID', None), 'date': ('scalar', 'LocalDate', None),
                'date-time': ('scalar', 'OffsetDateTime', None), 'byte': ('scalar', 'byte[]', None),
                'binary': ('unsupported', 'binary', None)}.get(f, ('scalar', 'String', None))
    if t == 'integer':
        return ('scalar', 'Long' if f == 'int64' else 'Integer', None)
    if t == 'number':
        return ('scalar', 'BigDecimal' if f not in ('float', 'double') else ('Float' if f == 'float' else 'Double'),
                None)
    if t == 'boolean':
        return ('scalar', 'Boolean', None)
    if t == 'array':
        item = java_type(mod, prop.get('items', {}))
        return ('array', item, prop)
    if t == 'object' or t is None:
        if 'properties' in prop:
            return ('inlineobject', None, prop)
        return ('map', 'Object', prop)
    return ('unsupported', str(t), None)


def build():
    resources = []
    skipped = []
    for path in sorted(glob.glob(OA + '*.yaml')):
        name = os.path.basename(path)[:-5]
        if name in ('common', 'mysms-api', 'redocly'):
            continue
        mod = Module(name)
        by_resource = collections.defaultdict(dict)
        for p, item in mod.doc['paths'].items():
            for verb, op in item.items():
                if not isinstance(op, dict) or 'operationId' not in op:
                    continue
                oid = op['operationId'].split('.', 1)[1]
                m = re.match(r'^(List|Get|Create|Update|Delete)([A-Z]\w+)$', oid)
                if not m:
                    continue
                kind = m.group(1)
                res = resource_of(mod, kind, op, p)
                if res is None:
                    continue
                if kind in by_resource[res]:
                    skipped.append((name, res, 'duplicate ' + kind + ' ' + oid))
                    continue
                by_resource[res][kind] = dict(path=p, verb=verb, op=op, oid=oid, tag=op['tags'][0])
        for res, ops in sorted(by_resource.items()):
            r = describe(mod, res, ops)
            if isinstance(r, str):
                skipped.append((name, res, r))
            else:
                resources.append(r)
    return resources, skipped


def resource_of(mod, kind, op, path):
    """The resource schema name an operation acts on."""
    if kind == 'List':
        sch = op['responses'].get('200', {}).get('content', {}).get('application/json', {}).get('schema', {})
        if '$ref' not in sch:
            return None
        lst = mod.resolve(sch)
        items = lst.get('properties', {}).get('items', {}).get('items', {})
        return ref_name(items['$ref']) if '$ref' in items else None
    if kind in ('Get', 'Update'):
        sch = op['responses'].get('200', {}).get('content', {}).get('application/json', {}).get('schema', {})
        return ref_name(sch['$ref']) if '$ref' in sch else None
    if kind == 'Create':
        sch = op.get('requestBody', {}).get('content', {}).get('application/json', {}).get('schema', {})
        resp = op['responses'].get('201', {}).get('content', {}).get('application/json', {}).get('schema', {})
        if '$ref' not in sch or '$ref' not in resp or ref_name(sch['$ref']) != ref_name(resp['$ref']):
            return None
        return ref_name(sch['$ref'])
    if kind == 'Delete':
        if '204' not in op['responses']:
            return None
        params = re.findall(r'\{(\w+)\}', path)
        last = params[-1] if params else None
        if not last or not last.endswith('Id') or not path.endswith('{' + last + '}'):
            return None
        return last[:-2][:1].upper() + last[:-2][1:]
    return None


def path_params(mod, op, path):
    out = []
    for prm in op.get('parameters', []):
        p = prm
        if '$ref' in prm:
            if prm['$ref'].startswith('./common.yaml'):
                p = COMMON['components']['parameters'][ref_name(prm['$ref'])]
            else:
                p = mod.doc['components']['parameters'][ref_name(prm['$ref'])]
        if p.get('in') == 'path':
            out.append(p['name'])
    return out


def describe(mod, res, ops):
    table = TABLE_ALIASES.get((mod.name, res), mod.name + '.' + snake(res))
    if table not in CATALOG:
        return 'no table ' + table
    if res not in mod.schemas:
        return 'no schema'
    t = CATALOG[table]
    cols = {c['name']: c for c in t['cols']}
    sch = mod.schemas[res]
    if sch.get('allOf') or sch.get('oneOf'):
        return 'composed schema'
    props = sch.get('properties', {})
    required = set(sch.get('required', []))
    upd = mod.schemas.get(res + 'Update', {}).get('properties', {}) if 'Update' in ops else {}
    fields = []
    problems = []
    for pn, prop in props.items():
        if pn == 'etag':
            continue
        if pn in OUT_ONLY:
            col = OUT_ONLY[pn]
            if col not in cols:
                problems.append('no column for ' + pn)
            fields.append(dict(name=pn, col=col, audit=True, prop=prop, jt=java_type(mod, prop)))
            continue
        col = snake(pn)
        # a secret: write-only plaintext in the contract, an application-encrypted `<name>_enc` column
        if prop.get('writeOnly') and col + '_enc' in cols:
            fields.append(dict(name=pn, col=col + '_enc', audit=False, prop=prop, jt=java_type(mod, prop), ro=False,
                               required=pn in required, writable=pn in upd, colinfo=cols[col + '_enc'], secret=True))
            continue
        # its display form: a stored `<name>_masked` column, or computed from the secret on read
        if pn.endswith('Masked') and prop.get('readOnly') and props.get(pn[:-6], {}).get('writeOnly') \
                and snake(pn[:-6]) + '_enc' in cols:
            if col in cols:
                fields.append(dict(name=pn, col=col, audit=False, prop=prop, jt=java_type(mod, prop), ro=True,
                                   required=pn in required, writable=False, colinfo=cols[col], masked_of=pn[:-6]))
            else:
                fields.append(dict(name=pn, col=None, audit=False, prop=prop, jt=java_type(mod, prop), ro=True,
                                   required=False, writable=False, colinfo=None, masked_of=pn[:-6], virtual=True))
            continue
        if col not in cols and col.startswith('is_') is False and ('is_' + col) in cols:
            col = 'is_' + col
        if col not in cols:
            problems.append('no column for ' + pn)
            continue
        jt = java_type(mod, prop)
        if jt[0] in ('unsupported', 'inlineobject'):
            problems.append('type %s of %s' % (jt[0], pn))
        if col.endswith('_enc') or col in AUDIT:
            problems.append('special column ' + col)
        fields.append(dict(name=pn, col=col, audit=False, prop=prop, jt=jt, ro=bool(prop.get('readOnly')),
                           required=pn in required, writable=pn in upd, colinfo=cols[col]))
    if 'etag' in props and 'row_version' not in cols:
        problems.append('etag without row_version')
    mapped_cols = {f['col'] for f in fields if f['col']}
    # NOT NULL columns without default that the API cannot supply
    for c in t['cols']:
        if c['name'] in mapped_cols or c['name'] in AUDIT:
            continue
        if c['nn'] and c['default'] is None and not c['generated'] and not c['identity']:
            problems.append('unsupplied not-null ' + c['name'])
    if problems:
        return '; '.join(problems)
    parents = set()
    for k, o in ops.items():
        pp = path_params(mod, o['op'], o['path'])
        own = 1 if k in ('Get', 'Update', 'Delete') else 0
        if len(pp) - own > 1:
            return 'deeply nested ' + k
        if len(pp) - own == 1:
            parents.add(pp[0])
        else:
            parents.add(None)
    if len(parents) > 1:
        return 'mixed nesting ' + str(sorted(map(str, parents)))
    parent = parents.pop()
    pcol = None
    if parent:
        pcol = PARENT_COLUMNS.get(parent, snake(parent))
        pf = next((f for f in fields if f['col'] == pcol), None)
        if pf is None:
            return 'parent %s not a field' % parent
    return dict(module=mod.name, name=res, table=table, t=t, cols=cols, fields=fields, ops=ops, parent=parent,
                parent_col=pcol,
                tenant='tenant_id' in cols, soft='deleted_at' in cols, versioned='row_version' in cols,
                has_updated='updated_at' in cols, has_created='created_at' in cols, mod=mod,
                update_props=upd)


if __name__ == '__main__':
    rs, sk = build()
    print('resources', len(rs), 'ops', sum(len(r['ops']) for r in rs))
    c = collections.Counter()
    for r in rs:
        c['tenant' if r['tenant'] else 'global'] += 1
        c['soft' if r['soft'] else 'hard'] += 1
        c['versioned' if r['versioned'] else 'unversioned'] += 1
        c['nested' if r['parent'] else 'top'] += 1
        c['inst' if 'institution_id' in r['cols'] else 'noinst'] += 1
        for f in r['fields']:
            c['jt:' + f['jt'][0]] += 1
            if f['jt'][0] == 'array':
                c['arr:' + str(f['jt'][1][0]) + ':' + f['colinfo']['type']] += 1
            if f['jt'][0] in ('map', 'object'):
                c['obj:' + f['colinfo']['type']] += 1
    for k, v in sorted(c.items()):
        print(k, v)
    print('skipped', len(sk))
    reasons = collections.Counter(re.sub(r' [\w.]+$', '', s[2].split(';')[0]) for s in sk)
    for k, v in reasons.most_common():
        print(' ', v, k)
    json.dump([list(s) for s in sk], open(TOOLS + 'target/skipped.json', 'w'), indent=0)
