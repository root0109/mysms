#!/usr/bin/env python3
"""Writes database/migrations/iam/V1_030_004__iam_permission_catalogue.sql: every permission code a tenant user can be
granted, taken from the contracts' x-permission (operations reachable with bearerAuth). Ids are derived from the code
(md5 -> uuid) so every environment has the same ids. Self-service permissions apply to every role and cannot be
removed from it. The catalogue migration is applied and immutable: later changes (for example
V1_030_005, which makes a user's own MFA factors and sessions self-service) are new migrations, not edits here."""
import glob, os, yaml
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
API = os.path.join(ROOT, 'api', 'openapi') + os.sep
OUT = os.path.join(ROOT, 'database', 'migrations', 'iam', 'V1_030_004__iam_permission_catalogue.sql')
SELF = {
    'iam.user_account.get_me', 'iam.user_account.logout', 'iam.user_account.change_password',
    'iam.user_account.accept_terms', 'iam.user_account.switch_role',
    'iam.setting.read_mine', 'iam.setting.update_mine', 'iam.setting_definition.read',
    'people.person.read_mine', 'people.person.update_mine',
}
perms = {}
for f in sorted(glob.glob(API + '*.yaml')):
    m = os.path.basename(f)[:-5]
    if m in ('common', 'mysms-api', 'redocly'):
        continue
    d = yaml.safe_load(open(f))
    for p, item in d['paths'].items():
        for verb, op in item.items():
            if verb not in ('get', 'post', 'put', 'patch', 'delete'):
                continue
            schemes = {k for s in (op.get('security') or []) for k in s}
            if 'bearerAuth' not in schemes:
                continue
            code = op['x-permission']
            perms.setdefault(code, (m, op.get('summary') or op['operationId']))
missing = SELF - set(perms)
assert not missing, missing
def q(s):
    return "'" + s.replace("'", "''") + "'"
rows = []
for code in sorted(perms):
    m, summary = perms[code]
    self_service = code in SELF
    rows.append(f"    (md5('mysms.permission:' || {q(code)})::uuid, {q(code)}, {q(summary[:150])}, {q(m)}, "
                f"{'true' if self_service else 'false'}, {'false' if self_service else 'true'})")
sql = f"""-- MySMS iam: the permission catalogue — every permission code a tenant user can be granted (the x-permission of
-- each operation callable with a tenant user's token), generated from api/openapi by database/tools/gen_permission_catalogue.py.
-- Ids are derived from the code, so they are identical in every environment. Self-service permissions (the caller's
-- own account, settings and person record) apply to every role and cannot be removed. Later catalogue changes are
-- new migrations. {len(rows)} permissions.
INSERT INTO iam.permission (id, code, display_name, module_code, applies_to_all_roles, is_modifiable)
VALUES
""" + ",\n".join(rows) + ";\n"
open(OUT, 'w').write(sql)
print(len(rows), 'permissions,', len(SELF), 'self-service')
