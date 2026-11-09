import { describe, expect, it } from 'vitest';
import { parseQuery, pathOf, searchOf } from './url';

describe('portable URL helpers', () => {
  it('extracts paths from absolute, custom-scheme and relative URLs', () => {
    expect(pathOf('https://h.example:8443/app/sis/students/1?x=1#y')).toBe('/app/sis/students/1');
    expect(pathOf('https://h.example')).toBe('/');
    expect(pathOf('/v1/iam/auth:login?a')).toBe('/v1/iam/auth:login');
    expect(pathOf('')).toBe('/');
  });

  it('parses queries like URLSearchParams', () => {
    const q = parseQuery('?view=room&mine&name=a%20b&plus=a+b&view=other&bad=%E0%A4%A');
    expect(q.get('view')).toBe('room');
    expect(q.has('mine')).toBe(true);
    expect(q.get('mine')).toBe('');
    expect(q.get('name')).toBe('a b');
    expect(q.get('plus')).toBe('a b');
    expect(q.get('bad')).toBe('%E0%A4%A');
    expect(q.has('missing')).toBe(false);
    expect(q.get('missing')).toBeNull();
    expect(parseQuery('').has('x')).toBe(false);
    expect(searchOf('mysms://a/b?mine#frag')).toBe('mine');
    expect(searchOf('/a/b')).toBe('');
  });
});
