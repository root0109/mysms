"""Reads the generated contract sources: Api interface method signatures and model getters/enums."""
import os
import re

GEN = os.path.normpath(os.path.dirname(os.path.abspath(__file__)) + '/../../../backend/contracts/target/generated-sources/openapi')
GEN += '/{m}/src/main/java/com/mysms/contracts/{m}/'


def _strip_annotations(params):
    out = []
    depth = 0
    cur = ''
    for ch in params:
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur)
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    result = []
    for p in out:
        p = re.sub(r'@\w+(\([^()]*(\([^()]*\)[^()]*)*\))?', '', p).strip()
        p = re.sub(r'\s+', ' ', p)
        typ, name = p.rsplit(' ', 1)
        result.append((typ, name))
    return result


def api_methods(module, tag):
    """{methodName: (returnType, [(type, name)], pathConstantValue)} of a generated Api interface."""
    path = GEN.format(m=module) + 'api/' + tag + 'Api.java'
    if not os.path.exists(path):
        return None
    src = open(path).read()
    consts = dict(re.findall(r'String (PATH_\w+) = "([^"]+)";', src))
    methods = {}
    for m in re.finditer(r'value = \w+Api\.(PATH_\w+),.*?\)\s*\n\s*ResponseEntity<([\w<>, .]+)> (\w+)\(\s*(.*?)\);', src,
                         re.S):
        const, ret, name, params = m.groups()
        methods[name] = (ret, _strip_annotations(params) if params.strip() else [], consts[const])
    return methods


def model(module, cls):
    """{'fields': {name: type}, 'enums': {EnumName: [values]}} of a generated model class."""
    path = GEN.format(m=module) + 'model/' + cls + '.java'
    if not os.path.exists(path):
        return None
    src = open(path).read()
    fields = {}
    for typ, name in re.findall(r'^  private (?:@Nullable )?([\w<>, .]+?) (\w+)(?: = [^;]+)?;', src, re.M):
        fields[name] = typ
    enums = {}
    for m in re.finditer(r'public enum (\w+) \{(.*?)\n\n', src, re.S):
        enums[m.group(1)] = re.findall(r'\w+\("([^"]*)"\)', m.group(2))
    top_enum = re.search(r'^public enum (\w+)', src, re.M)
    return dict(fields=fields, enums=enums, is_enum=bool(top_enum),
                enum_values=re.findall(r'^  \w+\("([^"]*)"\)', src, re.M) if top_enum else None)
