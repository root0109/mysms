import { json } from './fakeApi';

/**
 * The fake side of the list protocol (api/README "Pagination"), as the server answers it: `page` (one-based) and
 * `size` select the page, a `cursor` from a `next` link continues it, and the response is `{ data, paging }` with
 * relative `first` / `previous` / `next` links that keep the request's other parameters. The fake cursor is the
 * offset of the next row; like the server's, it is bound to the page it opens (a mismatch answers 400).
 */
export function pageOf<T>(
  req: Request,
  all: readonly T[],
  options: { defaultSize?: number; maxSize?: number } = {},
): Response {
  const url = new URL(req.url);
  const query = url.searchParams;
  const page = Number(query.get('page') ?? 1);
  const asked = Number(query.get('size') ?? options.defaultSize ?? 50);
  if (!Number.isInteger(page) || page < 1 || !Number.isInteger(asked) || asked < 1 || asked > 500) {
    return json({ status: 400, code: 'INVALID_ARGUMENT', title: 'Bad paging' }, { status: 400 });
  }
  // a fake may serve smaller pages than asked for, to exercise paging
  const size = Math.min(asked, options.maxSize ?? asked);
  const cursor = query.get('cursor');
  if (cursor !== null && cursor !== `${page}:${(page - 1) * size}`) {
    return json(
      { status: 400, code: 'INVALID_ARGUMENT', title: 'cursor was issued for another page' },
      { status: 400 },
    );
  }
  const start = (page - 1) * size;
  const data = all.slice(start, start + size);
  const link = (number: number, withCursor: boolean) => {
    const kept = new URLSearchParams(query);
    kept.delete('page');
    kept.delete('size');
    kept.delete('cursor');
    const rest = kept.toString();
    return (
      `${url.pathname}?page=${number}&size=${size}${rest ? `&${rest}` : ''}` +
      (withCursor ? `&cursor=${number}:${(number - 1) * size}` : '')
    );
  };
  return json({
    data,
    paging: {
      page,
      size: data.length,
      first: link(1, false),
      ...(page > 1 ? { previous: link(page - 1, false) } : {}),
      ...(start + size < all.length ? { next: link(page + 1, true) } : {}),
    },
  });
}

/** A whole list as one page when the request does not limit it, for fakes whose lists are always small. */
export function onePage<T>(req: Request, all: readonly T[]): Response {
  return pageOf(req, all, { defaultSize: Math.max(all.length, 1) });
}
