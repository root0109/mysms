import { describe, expect, it } from 'vitest';
import { SCREENS } from '../generated/screens';
import {
  buildPath,
  compileRoutes,
  matchRoute,
  resolveLink,
  routerPaths,
  screenById,
  toRoutePath,
} from './resolve';

describe('route table', () => {
  it('is generated from the full ui/02 inventory', () => {
    expect(SCREENS).toHaveLength(186);
    expect(screenById('ACC-01')?.routes).toContain('/auth/login');
    expect(screenById('PLT-07')?.clients).toEqual(['C']);
    expect(screenById('ACC-06')?.placement).toBe('sheet');
    expect(screenById('NOPE-01')).toBeUndefined();
  });
});

describe('toRoutePath', () => {
  it('normalises every link form', () => {
    expect(toRoutePath('mysms://sis/students/42')).toBe('/sis/students/42');
    expect(toRoutePath('https://demo.example.com/app/sis/students/42?tab=fees#x')).toBe('/sis/students/42');
    expect(toRoutePath('/app/org/institutions/')).toBe('/org/institutions');
    expect(toRoutePath('/org/institutions')).toBe('/org/institutions');
    expect(toRoutePath('/')).toBe('/');
  });

  it('rejects unsafe paths', () => {
    expect(toRoutePath('/sis/../iam/users')).toBeNull();
    expect(toRoutePath('/sis/./students')).toBeNull();
    expect(toRoutePath('/sis/students%2F1')).toBeNull();
    expect(toRoutePath('/sis/<script>')).toBeNull();
    expect(toRoutePath('javascript:alert(1)')).toBeNull();
  });
});

describe('matchRoute / resolveLink', () => {
  it('extracts and decodes path params', () => {
    const m = matchRoute('/org/institutions/abc%20d');
    expect(m?.screen.id).toBe('ADM-03');
    expect(m?.params).toEqual({ institutionId: 'abc d' });
  });

  it('prefers literal segments over parameters', () => {
    const compiled = compileRoutes([
      { id: 'X-01', title: 'item', routes: ['/x/{id}'], clients: ['W'] },
      { id: 'X-02', title: 'new', routes: ['/x/new'], clients: ['W'] },
    ]);
    expect(matchRoute('/x/new', undefined, compiled)?.screen.id).toBe('X-02');
    expect(matchRoute('/x/7', undefined, compiled)?.params).toEqual({ id: '7' });
    expect(matchRoute('/y', undefined, compiled)).toBeNull();
  });

  it('opens the native screen, falls back to the web, or rejects', () => {
    expect(resolveLink('mysms://org/institutions/i1', 'W', 'https://w')).toMatchObject({ kind: 'screen' });
    expect(resolveLink('mysms://org/institutions/i1', 'S', 'https://w')).toEqual({
      kind: 'web-fallback',
      webUrl: 'https://w/app/org/institutions/i1',
    });
    expect(resolveLink('mysms://does/not/exist', 'S', 'https://w')).toEqual({ kind: 'invalid' });
    expect(resolveLink('mysms://../etc', 'W', 'https://w')).toEqual({ kind: 'invalid' });
    expect(resolveLink('/master/countries', 'C', 'https://w')).toMatchObject({ kind: 'screen' });
    expect(resolveLink('/master/countries', 'W', 'https://w')).toEqual({ kind: 'invalid' });
  });

  it('matches query variants before their base route', () => {
    expect(matchRoute('/timetable/sessions', 'W')?.screen.id).toBe('TT-02');
    expect(
      matchRoute('/timetable/sessions', 'W', undefined, new URLSearchParams('view=room'))?.screen.id,
    ).toBe('TT-04');
    expect(
      matchRoute('/timetable/sessions', 'W', undefined, new URLSearchParams('view=staff'))?.screen.id,
    ).toBe('TT-02');
    const mine = resolveLink('mysms://library/loans?mine', 'S', 'https://w');
    expect(mine.kind).toBe('screen');
    expect(resolveLink('https://h/app/timetable/sessions?view=room', 'W', 'https://w')).toMatchObject({
      kind: 'screen',
      match: { route: '/timetable/sessions?view=room' },
    });
    expect(resolveLink('mysms://timetable/sessions?view=room', 'S', 'https://w')).toEqual({
      kind: 'web-fallback',
      webUrl: 'https://w/app/timetable/sessions?view=room',
    });
    const paths = routerPaths('W');
    expect(paths).toContain('/timetable/sessions');
    expect(paths.some((p) => p.includes('?'))).toBe(false);
    expect(new Set(paths).size).toBe(paths.length);
  });

  it('builds concrete paths', () => {
    expect(buildPath('/sis/students/{studentId}', { studentId: 'a/b' })).toBe('/sis/students/a%2Fb');
    expect(() => buildPath('/sis/students/{studentId}', {})).toThrow('missing route param studentId');
  });
});
