/**
 * Minimal URL helpers that behave the same in browsers, Node and React Native. React Native's built-in
 * URL / URLSearchParams do not implement `pathname`, `get` or `has`, so shared code must not rely on them.
 */

/** A query lookup: URLSearchParams satisfies it on the web; `parseQuery` provides it everywhere. */
export interface QueryLookup {
  has(key: string): boolean;
  get(key: string): string | null;
}

const decode = (s: string) => {
  try {
    return decodeURIComponent(s.replace(/\+/g, ' '));
  } catch {
    return s;
  }
};

/** `a=1&b&c=x%20y` → lookup (first value wins, like URLSearchParams.get). */
export function parseQuery(search: string): QueryLookup {
  const map = new Map<string, string>();
  for (const part of search.replace(/^\?/, '').split('&')) {
    if (!part) continue;
    const eq = part.indexOf('=');
    const key = decode(eq < 0 ? part : part.slice(0, eq));
    if (!map.has(key)) map.set(key, eq < 0 ? '' : decode(part.slice(eq + 1)));
  }
  return { has: (k) => map.has(k), get: (k) => map.get(k) ?? null };
}

/** The path of an absolute or relative URL, without query or fragment. */
export function pathOf(url: string): string {
  const withoutScheme = url.replace(/^[a-z][a-z0-9+.-]*:\/\/[^/?#]*/i, '');
  const path = withoutScheme.split(/[?#]/)[0] ?? '';
  return path === '' ? '/' : path;
}

/** The query string of a URL, without `?` and fragment. */
export const searchOf = (url: string): string => /\?([^#]*)/.exec(url)?.[1] ?? '';
