/** Pages one read may follow: 40 pages (of up to 500 rows) bound any list a screen needs whole. */
export const MAX_PAGES = 40;

/** The navigation part of every list response (api/README "Pagination"). */
export interface Paging {
  page?: number;
  next?: string;
}

/** Which page to read: its one-based number and, when it continues a previous page, that page's cursor. */
export interface PageParam {
  page: number;
  cursor?: string;
}

/** The first page of a list. */
export const FIRST_PAGE: PageParam = { page: 1 };

/**
 * The page after the one a response holds, taken from its `next` link: the next page number and the cursor the link
 * carries (reading by cursor is a keyset seek on the server, however deep the page). Undefined on the last page.
 */
export function nextPage(paging: Paging | undefined): PageParam | undefined {
  const next = paging?.next;
  if (!next) return undefined;
  // read by hand: React Native's URLSearchParams cannot get a parameter
  const query = new Map(
    (next.includes('?') ? next.slice(next.indexOf('?') + 1) : '')
      .split('&')
      .map((pair) => pair.split('=') as [string, string | undefined])
      .map(([name, value]) => [decodeURIComponent(name), decodeURIComponent(value ?? '')] as const),
  );
  const page = Number(query.get('page'));
  const cursor = query.get('cursor') || undefined;
  return {
    page: Number.isInteger(page) && page > 0 ? page : (paging?.page ?? 0) + 1,
    ...(cursor ? { cursor } : {}),
  };
}

/** The request parameters of a page, ready to spread into a list call. */
export function pageQuery(param: PageParam): { page: number; cursor?: string } {
  return param.cursor ? { page: param.page, cursor: param.cursor } : { page: param.page };
}

/**
 * Reads every page of a list operation (follows each response's `next` link until there is none). For lists that are
 * small by nature — a class roster, a sheet's marks, a pick-list — where the screen needs the whole set. A list that
 * does not end within {@link MAX_PAGES} pages is a fault (a link that repeats, or a list that is not small) and is
 * refused rather than followed forever.
 */
export async function allPages<T>(
  fetchPage: (page: PageParam) => Promise<{ data?: T[]; paging?: Paging }>,
): Promise<{ items: T[] }> {
  const items: T[] = [];
  let param: PageParam | undefined = FIRST_PAGE;
  for (let read = 0; read < MAX_PAGES && param; read++) {
    const result = await fetchPage(param);
    items.push(...(result.data ?? []));
    param = nextPage(result.paging);
    if (!param) return { items };
  }
  throw new Error(`a list did not end within ${MAX_PAGES} pages`);
}
