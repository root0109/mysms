import { SCREENS } from '../generated/screens';
import type { ClientKind, ScreenDef } from './types';
import { parseQuery, pathOf, searchOf, type QueryLookup } from './url';

export interface RouteMatch {
  screen: ScreenDef;
  route: string;
  params: Record<string, string>;
}

export type LinkResolution =
  { kind: 'screen'; match: RouteMatch } | { kind: 'web-fallback'; webUrl: string } | { kind: 'invalid' };

export interface Compiled {
  screen: ScreenDef;
  route: string;
  /** Path part of the route (a route may add query conditions: `/sis/students?mine`, `?view=room`). */
  path: string;
  regex: RegExp;
  names: string[];
  /** Query conditions: [key, required value or null for "key present"]. */
  conditions: [string, string | null][];
}

const escapeRegex = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

export function compileRoutes(screens: readonly ScreenDef[]): Compiled[] {
  const out: Compiled[] = [];
  for (const screen of screens) {
    for (const route of screen.routes) {
      const [path = route, query = ''] = route.split('?');
      const conditions: [string, string | null][] = query
        ? query.split('&').map((pair) => {
            const [k = '', v] = pair.split('=');
            return [k, v ?? null];
          })
        : [];
      const names: string[] = [];
      const pattern = path
        .split(/(\{[A-Za-z][A-Za-z0-9]*\})/)
        .map((part) => {
          const m = /^\{([A-Za-z][A-Za-z0-9]*)\}$/.exec(part);
          if (m?.[1]) {
            names.push(m[1]);
            return '([^/]+)';
          }
          return escapeRegex(part);
        })
        .join('');
      out.push({ screen, route, path, regex: new RegExp(`^${pattern}/?$`), names, conditions });
    }
  }
  // query variants beat their base route; literal segments beat parameters (`/x/new` before `/x/{id}`)
  return out.sort((a, b) => b.conditions.length - a.conditions.length || a.names.length - b.names.length);
}

const COMPILED = compileRoutes(SCREENS);

/** Safe paths only: no traversal, no encoded slashes, no control characters. */
const SAFE_PATH = /^\/[A-Za-z0-9\-._~/%]*$/;

/** Normalises any link form to a route path: `mysms://x/y`, `https://host/app/x/y`, `/app/x/y` or `/x/y`. */
export function toRoutePath(link: string): string | null {
  let path: string;
  if (link.startsWith('mysms://')) path = '/' + link.slice('mysms://'.length);
  else if (/^https?:\/\//.test(link)) path = pathOf(link);
  else path = link;
  path = path.split(/[?#]/)[0] ?? '';
  if (path.startsWith('/app/')) path = path.slice(4);
  if (!SAFE_PATH.test(path) || path.split('/').some((s) => s === '..' || s === '.') || /%2f/i.test(path))
    return null;
  return path.length > 1 ? path.replace(/\/$/, '') : path;
}

const satisfies = (c: Compiled, query: QueryLookup) =>
  c.conditions.every(([k, v]) => (v === null ? query.has(k) : query.get(k) === v));

/** Finds the screen for a route path (and query), optionally restricted to screens a client carries. */
export function matchRoute(
  path: string,
  client?: ClientKind,
  compiled: Compiled[] = COMPILED,
  query: QueryLookup = parseQuery(''),
): RouteMatch | null {
  for (const c of compiled) {
    if (client && !c.screen.clients.includes(client)) continue;
    if (!satisfies(c, query)) continue;
    const m = c.regex.exec(path);
    if (!m) continue;
    const params: Record<string, string> = {};
    c.names.forEach((n, i) => {
      params[n] = decodeURIComponent(m[i + 1] ?? '');
    });
    return { screen: c.screen, route: c.route, params };
  }
  return null;
}

/**
 * Deep-link resolution (ARCHITECTURE §4, ui/05 §2): the native screen when this client has it, otherwise the
 * web URL when the web app has it, otherwise invalid (polite dead end).
 */
export function resolveLink(link: string, client: ClientKind, webOrigin: string): LinkResolution {
  const path = toRoutePath(link);
  if (!path) return { kind: 'invalid' };
  const search = searchOf(link);
  const query = parseQuery(search);
  const own = matchRoute(path, client, COMPILED, query);
  if (own) return { kind: 'screen', match: own };
  if (client !== 'W' && matchRoute(path, 'W', COMPILED, query)) {
    return { kind: 'web-fallback', webUrl: `${webOrigin}/app${path}${search ? `?${search}` : ''}` };
  }
  return { kind: 'invalid' };
}

/** Distinct path patterns a client must mount (query variants share their base path). */
export function routerPaths(client: ClientKind, compiled: Compiled[] = COMPILED): string[] {
  return [...new Set(compiled.filter((c) => c.screen.clients.includes(client)).map((c) => c.path))];
}

/** Builds a concrete route from a pattern: `buildPath('/sis/students/{studentId}', { studentId })`. */
export function buildPath(route: string, params: Record<string, string>): string {
  return route.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, (_, name: string) => {
    const value = params[name];
    if (value === undefined) throw new Error(`missing route param ${name} for ${route}`);
    return encodeURIComponent(value);
  });
}

export const screenById = (id: string): ScreenDef | undefined => SCREENS.find((s) => s.id === id);
