import { describe, expect, it } from 'vitest';
import { patchOf } from './patch';

describe('patchOf', () => {
  it('sends the named fields, an unset one as null, and leaves the rest out', () => {
    const changes: { title?: string; note?: string | null; days?: number } = { title: 'T', note: undefined };
    expect(JSON.stringify(patchOf(changes))).toBe('{"title":"T","note":null}');
  });
});
