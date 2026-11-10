import { describe, expect, it } from 'vitest';
import { MAX_PAGES, allPages, nextPage, pageQuery } from './pages';

describe('allPages', () => {
  it('follows the next links to the end and keeps the order', async () => {
    const asked: string[] = [];
    const pages: Record<string, { data?: number[]; paging?: { page: number; next?: string } }> = {
      '1': { data: [1, 2], paging: { page: 1, next: '/v1/x?page=2&size=2&cursor=c2' } },
      '2:c2': { paging: { page: 2, next: '/v1/x?page=3&size=2&cursor=c3' } },
      '3:c3': { data: [3], paging: { page: 3 } },
    };
    const all = await allPages((param) => {
      const key = param.cursor ? `${param.page}:${param.cursor}` : String(param.page);
      asked.push(key);
      return Promise.resolve(pages[key] ?? {});
    });
    expect(all.items).toEqual([1, 2, 3]);
    expect(asked).toEqual(['1', '2:c2', '3:c3']);
  });

  it('refuses a list that never ends instead of following it forever', async () => {
    let reads = 0;
    await expect(
      allPages(() => {
        reads += 1;
        return Promise.resolve({ data: [reads], paging: { page: reads, next: '/v1/x?page=2&cursor=again' } });
      }),
    ).rejects.toThrow(/did not end/);
    expect(reads).toBe(MAX_PAGES);
  });
});

describe('nextPage', () => {
  it('reads the page number and the cursor of the next link', () => {
    expect(nextPage({ page: 3, next: '/v1/hr/staff?page=4&size=50&sort=name,desc&cursor=eyJ2-_' })).toEqual({
      page: 4,
      cursor: 'eyJ2-_',
    });
    expect(nextPage({ page: 1 })).toBeUndefined();
    expect(nextPage(undefined)).toBeUndefined();
    expect(nextPage({ page: 2, next: '/v1/x?size=5' })).toEqual({ page: 3 });
  });

  it('turns a page into request parameters', () => {
    expect(pageQuery({ page: 2, cursor: 'c' })).toEqual({ page: 2, cursor: 'c' });
    expect(pageQuery({ page: 1 })).toEqual({ page: 1 });
  });
});
