import { describe, expect, it } from 'vitest';
import { can, canAll, canAny } from './can';

describe('permissions', () => {
  it('checks arrays, sets and missing grants', () => {
    expect(can(['master.country.read'], 'master.country.read')).toBe(true);
    expect(can(new Set(['master.country.read']), 'master.country.read')).toBe(true);
    expect(can(['master.country.read'], 'master.country.create')).toBe(false);
    expect(can(undefined, 'master.country.read')).toBe(false);
    expect(can(null, 'master.country.read')).toBe(false);
    expect(canAny(['master.country.read'], ['master.country.create', 'master.country.read'])).toBe(true);
    expect(canAll(['master.country.read'], ['master.country.create', 'master.country.read'])).toBe(false);
  });
});
