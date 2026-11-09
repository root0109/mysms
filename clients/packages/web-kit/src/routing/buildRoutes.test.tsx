import { SCREENS, matchRoute, type ClientKind } from '@mysms/core';
import { matchRoutes } from 'react-router';
import { describe, expect, it } from 'vitest';
import { buildAppRoutes } from './buildRoutes';
import { toRouterPath } from './paths';
import { safeNext } from './safeNext';

const Dummy = () => null;
const options = (client: ClientKind) => ({
  client,
  Shell: Dummy,
  Home: Dummy,
  built: { 'ACC-01': <p>sign in</p> },
});

describe('buildAppRoutes', () => {
  for (const client of ['W', 'S', 'P', 'T', 'B', 'C'] as ClientKind[]) {
    it(`mounts every ${client} screen of the inventory, query variants included`, () => {
      const routes = buildAppRoutes(options(client));
      const screens = SCREENS.filter((s) => s.clients.includes(client) && s.routes.length > 0);
      expect(screens.length).toBeGreaterThan(5);
      for (const s of screens) {
        for (const route of s.routes) {
          const [path = route, query] = route.split('?');
          // only segment-initial parameters are placeholders; a literal `:verb` stays literal
          const concrete = toRouterPath(path).replace(/\/:([A-Za-z0-9]+)/g, '/x-$1');
          const matched = matchRoutes(routes, concrete);
          expect(matched?.[matched.length - 1]?.route.path, `${s.id} ${route}`).toBe(toRouterPath(path));
          expect(
            matchRoute(concrete.replace(/^\/app/, ''), client, undefined, new URLSearchParams(query ?? ''))
              ?.screen.id,
            `${s.id} ${route}`,
          ).toBe(s.id);
        }
      }
    });
  }

  it('puts sign-in, reset and sign-up outside the session guard', () => {
    const routes = buildAppRoutes(options('W'));
    const top = routes.map((r) => r.path);
    expect(top).toEqual(expect.arrayContaining(['/app/auth/login', '/app/auth/reset', '/app/auth/signup']));
    const app = routes.find((r) => r.path === '/app');
    expect(app?.children?.some((c) => c.path === '/app/auth/login')).toBe(false);
    expect(app?.children?.some((c) => c.path === '/app/auth/terms')).toBe(true);
  });

  it('refuses a configuration without the sign-in screen', () => {
    expect(() => buildAppRoutes({ client: 'W', Shell: Dummy, Home: Dummy, built: {} })).toThrow('ACC-01');
  });
});

describe('safeNext', () => {
  it('accepts only same-app destinations after sign-in', () => {
    expect(safeNext('/app/sis/students/1')).toBe('/app/sis/students/1');
    expect(safeNext(null)).toBe('/app');
    expect(safeNext('https://evil.example')).toBe('/app');
    expect(safeNext('//evil.example')).toBe('/app');
    expect(safeNext('/app\\..\\x')).toBe('/app');
    expect(safeNext('/app/auth/login')).toBe('/app');
  });
});
