"""Post-processing of generated Java: drop unused imports and wrap lines to the 120-column house style."""
import re

LIMIT = 120


def drop_unused_imports(text):
    lines = text.split('\n')
    body = '\n'.join(l for l in lines if not l.startswith('import '))
    body_nostr = re.sub(r'"(?:\\.|[^"\\])*"', '""', body)
    out = []
    for l in lines:
        m = re.match(r'import ([\w.]+)\.(\w+);', l)
        if m and not re.search(r'\b%s\b' % m.group(2), body_nostr):
            continue
        out.append(l)
    return '\n'.join(out)


def depth_map(line):
    """Paren/brace depth before each character, ignoring string contents; plus in-string flags."""
    depth = 0
    depths = []
    in_str = []
    s = False
    esc = False
    for ch in line:
        depths.append(depth)
        in_str.append(s)
        if s:
            if esc:
                esc = False
            elif ch == '\\':
                esc = True
            elif ch == '"':
                s = False
            continue
        if ch == '"':
            s = True
        elif ch in '([{':
            depth += 1
        elif ch in ')]}':
            depth -= 1
    return depths, in_str


def wrap_comment(line):
    m = re.match(r'^(\s*\*\s?)(.*)$', line)
    prefix, rest = m.group(1), m.group(2)
    words = rest.split(' ')
    out = []
    cur = prefix
    for w in words:
        if len(cur) + len(w) + 1 > LIMIT and cur.strip() not in ('*', ''):
            out.append(cur.rstrip())
            cur = prefix + w
        else:
            cur = cur + ('' if cur.endswith(' ') or cur == prefix else ' ') + w
    out.append(cur.rstrip())
    return out


def split_string(line, indent):
    """Split the longest string literal that crosses the limit into a concatenation."""
    depths, in_str = depth_map(line)
    for m in re.finditer(r'"(?:\\.|[^"\\])*"', line):
        start, end = m.span()
        if in_str[start]:
            continue
        if end <= LIMIT - 2 and start < LIMIT:
            continue
        lit = m.group(0)[1:-1]
        room = LIMIT - start - 4
        if room < 20:
            cut_at = None
        else:
            cut_at = lit.rfind(' ', 0, room)
        if not cut_at or cut_at < 10:
            # put the whole literal on its own line instead
            return None
        first = '"' + lit[:cut_at + 1] + '"'
        second = '"' + lit[cut_at + 1:] + '"'
        return [line[:start] + first, ' ' * (indent + 8) + '+ ' + second + line[end:]]
    return None


def break_code(line):
    indent = len(line) - len(line.lstrip())
    cont = ' ' * (indent + 8)
    depths, in_str = depth_map(line)
    candidates = []
    # break after ", " at the shallowest depth > 0, the latest position that fits
    for i in range(len(line) - 1):
        if in_str[i]:
            continue
        if line[i] == ',' and line[i + 1] == ' ' and i < LIMIT:
            candidates.append((depths[i], 0, i + 1, 'comma'))
        if line[i] == '.' and i > indent + 4 and line[i - 1] == ')' and i < LIMIT:
            candidates.append((depths[i], 1, i, 'dot'))
        if line[i:i + 3] == ' + ' and i < LIMIT:
            candidates.append((depths[i], 2, i, 'plus'))
        if line[i:i + 3] in (' ? ', ' : ') and i < LIMIT:
            candidates.append((depths[i], 3, i, 'tern'))
        if line[i:i + 4] in (' && ', ' || ') and i < LIMIT:
            candidates.append((depths[i], 2, i, 'bool'))
        if line[i] == '(' and i < LIMIT - 1 and line[i + 1] != ')':
            candidates.append((depths[i] + 1, 5, i + 1, 'paren'))
        if line[i:i + 3] == ' = ' and i < LIMIT:
            candidates.append((depths[i], 6, i + 3, 'assign'))
        if line[i:i + 4] == ' -> ' and i < LIMIT:
            candidates.append((depths[i] + 1, 4, i + 3, 'lambda'))
    candidates = [c for c in candidates if c[2] > indent + 10]
    if not candidates:
        return None
    min_depth = min(c[0] for c in candidates)
    best = [c for c in candidates if c[0] == min_depth]
    pos = max(best, key=lambda c: c[2])[2]
    head = line[:pos].rstrip()
    tail = line[pos:].lstrip()
    return [head, cont + tail]


def wrap_line(line, in_text_block):
    if len(line) <= LIMIT:
        return [line]
    stripped = line.lstrip()
    indent = len(line) - len(stripped)
    if in_text_block:
        words = stripped.split(' ')
        out = []
        cur = ' ' * indent
        for w in words:
            if len(cur) + len(w) + 1 > LIMIT and cur.strip():
                out.append(cur.rstrip())
                cur = ' ' * (indent + 4) + w
            else:
                cur = cur + ('' if not cur.strip() else ' ') + w
        out.append(cur.rstrip())
        return out
    if stripped.startswith('/**') and stripped.endswith('*/'):
        pad = ' ' * indent
        inner = stripped[3:-2].strip()
        return [pad + '/**'] + wrap_comment(pad + ' * ' + inner) + [pad + ' */']
    if stripped.startswith('*'):
        return wrap_comment(line)
    if stripped.startswith('//'):
        return [line]
    parts = break_code(line)
    if parts is None or len(parts[0]) > LIMIT and len(parts[0]) >= len(line) - 2:
        s = split_string(line, indent)
        if s is None:
            return [line]
        parts = s
    out = []
    for p in parts:
        if len(p) > LIMIT and p != line:
            out.extend(wrap_line(p, False))
        else:
            out.append(p)
    return out


def format_java(text):
    text = drop_unused_imports(text)
    out = []
    in_block = False
    for line in text.split('\n'):
        quotes = line.count('"""')
        if in_block:
            if quotes:
                in_block = False
                out.append(line)
                continue
            out.extend(wrap_line(line, True))
            continue
        if quotes == 1:
            in_block = True
            out.append(line)
            continue
        out.extend(wrap_line(line, False))
    return '\n'.join(out)
