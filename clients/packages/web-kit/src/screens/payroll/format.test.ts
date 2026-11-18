import { describe, expect, it } from 'vitest';
import { revisionOf } from './format';

describe('the row a payroll write names', () => {
  it('is a new row without an id, the id with its etag for a stored row, and nothing without the etag', () => {
    expect(revisionOf(null)).toEqual({});
    expect(revisionOf({})).toEqual({});
    expect(revisionOf({ id: 'a', etag: '"3"' })).toEqual({ id: 'a', etag: '"3"' });
    // the server refuses an update or delete without If-Match (428): the action is not offered
    expect(revisionOf({ id: 'a' })).toBeNull();
  });
});
