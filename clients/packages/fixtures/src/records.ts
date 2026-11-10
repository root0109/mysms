import { json, problem, type Handler } from './fakeApi';
import { pageOf } from './paging';

/** A record type of the generic record screens, as the fake backend serves it. */
export interface FakeRecordType {
  /** Collection path, e.g. `/v1/sis/student-categories`. */
  collection: string;
  /** Item path with the id placeholder, e.g. `/v1/sis/student-categories/{studentCategoryId}`. */
  item: string;
  /** Required body fields (422 when absent). */
  required: string[];
  rows?: Record<string, unknown>[];
}

/**
 * Standard operations over an in-memory table, with the server's rules for the slice the screens use: ETag and
 * If-Match (428/412), required fields (422), soft delete, `name : "x"` contains-filters and keyset-like paging.
 */
export function fakeRecords(type: FakeRecordType): Record<string, Handler> {
  const rows = new Map<string, { row: Record<string, unknown>; version: number }>();
  let sequence = 0;
  const newId = () => `01960000-0000-7000-8000-${String(++sequence).padStart(12, '0')}`;
  for (const row of type.rows ?? []) {
    const id = (row.id as string | undefined) ?? newId();
    rows.set(id, { row: { ...row, id }, version: 1 });
  }
  const out = (id: string) => {
    const entry = rows.get(id);
    if (!entry) return null;
    return { ...entry.row, etag: `"${entry.version}"` };
  };
  const idParam = /\{(\w+)\}$/.exec(type.item)?.[1] ?? 'id';
  const ifMatch = (req: Request, id: string): Response | null => {
    const header = req.headers.get('If-Match');
    if (!header) return problem(428, 'PRECONDITION_REQUIRED');
    if (header !== `"${rows.get(id)?.version}"`) return problem(412, 'PRECONDITION_FAILED');
    return null;
  };
  return {
    [`GET ${type.collection}`]: (req) => {
      const url = new URL(req.url);
      const filter = url.searchParams.get('filter') ?? '';
      const match = /^(\w+) : "(.*)"$/.exec(filter);
      let all = [...rows.keys()].map((id) => out(id) as Record<string, unknown>);
      if (match) {
        const [, field, text] = match as unknown as [string, string, string];
        all = all.filter((r) =>
          String(r[field] ?? '')
            .toLowerCase()
            .includes(text.toLowerCase()),
        );
      }
      return pageOf(req, all);
    },
    [`POST ${type.collection}`]: async (req) => {
      const body = (await req.json()) as Record<string, unknown>;
      const missing = type.required.filter((f) => body[f] === undefined || body[f] === '');
      if (missing.length > 0)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: missing.map((field) => ({ field, message: 'must not be null', code: 'NotNull' })),
        });
      const id = newId();
      rows.set(id, { row: { ...body, id }, version: 1 });
      return json(out(id), { status: 201, headers: { ETag: '"1"' } });
    },
    [`GET ${type.item}`]: (_req, p) => {
      const found = out(p[idParam] ?? '');
      return found ? json(found, { headers: { ETag: found.etag as string } }) : problem(404, 'NOT_FOUND');
    },
    [`PATCH ${type.item}`]: async (req, p) => {
      const id = p[idParam] ?? '';
      const entry = rows.get(id);
      if (!entry) return problem(404, 'NOT_FOUND');
      const refused = ifMatch(req, id);
      if (refused) return refused;
      const body = (await req.json()) as Record<string, unknown>;
      entry.row = { ...entry.row, ...body, id };
      entry.version += 1;
      const updated = out(id) as Record<string, unknown>;
      return json(updated, { headers: { ETag: updated.etag as string } });
    },
    [`DELETE ${type.item}`]: (req, p) => {
      const id = p[idParam] ?? '';
      if (!rows.has(id)) return problem(404, 'NOT_FOUND');
      const refused = ifMatch(req, id);
      if (refused) return refused;
      rows.delete(id);
      return new Response(null, { status: 204 });
    },
  };
}
