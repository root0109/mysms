import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeRecords, json } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { RESOURCES } from '../generated/resources';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  actionAllowed,
  createRecord,
  deleteRecord,
  getRecord,
  listRecords,
  runAction,
  toPatch,
  toPayload,
  updateRecord,
} from './api';
import { useCreateRecord, useDeleteRecord, useInfiniteRecords, useRecord, useUpdateRecord } from './hooks';
import type { ActionDef, ResourceDef } from './types';

/** An area's URL prefix: an underscore in the area name is a hyphen in the URL (`time_attendance` → `/v1/time-attendance/`). */
const urlPrefix = (area: string) => `/v1/${area.replaceAll('_', '-')}/`;
const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const PARENT = '01960000-0000-7000-8000-0000000000aa';

/** A nested resource shaped like the generated ones. */
const NOTES: ResourceDef = {
  key: 'demo.Note',
  module: 'demo',
  label: 'Note',
  parent: 'personId',
  scope: 'chain',
  ops: {
    List: { path: '/v1/demo/persons/{personId}/notes', permission: 'demo.note.read' },
    Create: { path: '/v1/demo/persons/{personId}/notes', permission: 'demo.note.create' },
    Get: { path: '/v1/demo/persons/{personId}/notes/{noteId}', permission: 'demo.note.read' },
    Update: { path: '/v1/demo/persons/{personId}/notes/{noteId}', permission: 'demo.note.update' },
    Delete: { path: '/v1/demo/persons/{personId}/notes/{noteId}', permission: 'demo.note.delete' },
  },
  fields: [
    { name: 'personId', label: 'Person', kind: 'id', required: true },
    { name: 'title', label: 'Title', kind: 'text', required: true, writable: true },
    { name: 'pages', label: 'Pages', kind: 'integer', writable: true },
    { name: 'pinned', label: 'Pinned', kind: 'boolean', writable: true },
    { name: 'status', label: 'Status', kind: 'enum', values: ['open'], readOnly: true },
    { name: 'pin', label: 'Pin', kind: 'text', writable: true, writeOnly: true },
  ],
};

const SNOOZE: ActionDef = {
  operation: 'demo.SnoozeNote',
  label: 'Snooze',
  path: '/v1/demo/persons/{personId}/notes/{noteId}:snooze',
  permission: 'demo.note.snooze',
  guard: { status: ['open', 'held'], pinned: [false] },
  etag: true,
  ifMatch: true,
  reason: true,
  params: [{ name: 'pages', label: 'Pages', kind: 'integer', required: true, writable: true }],
};

function setup() {
  const api = fakeApi({
    'POST /v1/demo/persons/{personId}/notes/{noteId}:snooze': async (request, params) =>
      json({ id: params.noteId, status: 'snoozed', etag: '"2"', sent: JSON.parse(await request.text()) }),
    'POST /v1/iam/auth:login': () => json({ tokens, user: { id: 'u1' } }),
    'GET /v1/iam/me': () => json({ user: { id: 'u1' }, permissions: ['*'] }),
    ...fakeRecords({
      collection: '/v1/demo/persons/{personId}/notes',
      item: '/v1/demo/persons/{personId}/notes/{noteId}',
      required: ['title'],
      rows: [{ id: 'n1', personId: PARENT, title: 'First', pages: 3 }],
    }),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com/',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, runtime, wrapper };
}

describe('generic record operations', () => {
  it('describes every served resource with its operations and fields', () => {
    expect(RESOURCES.length).toBeGreaterThan(200);
    for (const r of RESOURCES) {
      expect(r.key).toBe(`${r.module}.${r.key.split('.')[1]}`);
      for (const op of Object.values(r.ops)) expect(op?.path.startsWith(urlPrefix(r.module))).toBe(true);
      if (r.parent) expect(r.ops.List?.path ?? r.ops.Create?.path).toContain(`{${r.parent}}`);
    }
    const categories = RESOURCES.find((r) => r.key === 'sis.StudentCategory');
    expect(categories?.fields.find((f) => f.name === 'name')).toMatchObject({ kind: 'text', required: true });
  });

  it('sends only what a create may carry, and a patch only the fields passed', () => {
    const values = { personId: PARENT, title: 'T', pages: '7', pinned: 'true', status: 'open', extra: 1 };
    expect(toPayload(NOTES, values)).toEqual({
      personId: PARENT,
      title: 'T',
      pages: 7,
      pinned: true,
    });
    expect(toPatch(NOTES, { ...values, pages: '', pinned: false })).toEqual({
      title: 'T',
      pages: null,
      pinned: false,
    });
    expect(toPayload(NOTES, { title: 'T', pages: '7.5' })).toEqual({ title: 'T', pages: '7.5' });
  });

  it('offers a state transition only in the states its guard allows', () => {
    expect(actionAllowed(SNOOZE, { id: 'n1', status: 'open' })).toBe(true);
    expect(actionAllowed(SNOOZE, { id: 'n1', status: 'held', pinned: false })).toBe(true);
    expect(actionAllowed(SNOOZE, { id: 'n1', status: 'open', pinned: true })).toBe(false);
    expect(actionAllowed(SNOOZE, { id: 'n1', status: 'closed' })).toBe(false);
    expect(actionAllowed({ ...SNOOZE, guard: {} }, { id: 'n1' })).toBe(true);
    const notice = RESOURCES.find((r) => r.key === 'comms.Notice');
    expect(notice?.actions?.find((a) => a.operation === 'comms.PublishNotice')).toMatchObject({
      path: '/v1/comms/notices/{noticeId}:publish',
      guard: { isPublished: [false], isClosed: [false] },
      etag: true,
      reason: false,
    });
    for (const r of RESOURCES)
      for (const a of r.actions ?? []) expect(a.path.startsWith(urlPrefix(r.module))).toBe(true);
  });

  it('runs a state transition with the etag, a trimmed reason and typed fields', async () => {
    const { api, runtime } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
    const changed = await runAction(
      runtime.api,
      SNOOZE,
      'n1',
      '"1"',
      { reason: '  later ', params: { pages: '3' } },
      PARENT,
    );
    expect(changed).toMatchObject({ status: 'snoozed', sent: { etag: '"1"', reason: 'later', pages: 3 } });
    const call = api.calls.find((c) => c.path.endsWith(':snooze'));
    expect(call?.path).toBe(`/v1/demo/persons/${PARENT}/notes/n1:snooze`);
    expect(call?.headers.get('If-Match')).toBe('"1"');
    const bare = await runAction(
      runtime.api,
      { ...SNOOZE, etag: false },
      'n1',
      '"1"',
      { reason: ' ' },
      PARENT,
    );
    expect(bare).toMatchObject({ sent: {} });
  });

  it('lists, reads, creates, updates and deletes over the authenticated transport', async () => {
    const { api, runtime } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
    const client = runtime.api;
    const page = await listRecords(client, NOTES, {
      parentId: PARENT,
      size: 10,
      filter: 'title : "Fi"',
      sort: ['title'],
    });
    expect(page.data.map((r) => r.id)).toEqual(['n1']);
    expect(page.paging).toMatchObject({ page: 1, size: 1 });
    const listed = api.calls.find((c) => c.method === 'GET' && c.path.endsWith('/notes'));
    expect(listed?.headers.get('Authorization')).toBe('Bearer a1');
    const one = await getRecord(client, NOTES, 'n1', PARENT);
    expect(one).toMatchObject({ data: { title: 'First' }, etag: '"1"' });
    const created = await createRecord(client, NOTES, { title: 'Second' }, PARENT);
    expect(created).toMatchObject({ title: 'Second', personId: PARENT });
    const updated = await updateRecord(client, NOTES, 'n1', '"1"', { title: 'Renamed', pages: 4 }, PARENT);
    expect(updated).toMatchObject({ title: 'Renamed', pages: 4, etag: '"2"' });
    // only the fields passed are sent: a value sets, an empty one clears (null), a left-out one is kept; an
    // untouched secret is left out (empty means keep), a typed one replaces it
    const patches = () =>
      api.calls.filter((c) => c.method === 'PATCH').map((c) => ({ search: c.search, body: c.body }));
    expect(patches()[0]?.search).toBe('');
    await updateRecord(client, NOTES, 'n1', '"2"', { title: 'Renamed', pages: '', pin: '' }, PARENT);
    expect(JSON.parse(String(patches()[1]?.body))).toEqual({ title: 'Renamed', pages: null });
    await updateRecord(client, NOTES, 'n1', '"3"', { title: 'Renamed', pin: '4321' }, PARENT);
    expect(JSON.parse(String(patches()[2]?.body))).toEqual({ title: 'Renamed', pin: '4321' });
    await expect(updateRecord(client, NOTES, 'n1', '"1"', { title: 'x' }, PARENT)).rejects.toSatisfy(
      (e: unknown) => isApiError(e) && e.status === 412,
    );
    await expect(createRecord(client, NOTES, {}, PARENT)).rejects.toSatisfy(
      (e: unknown) => isApiError(e) && e.status === 422 && e.errors[0]?.field === 'title',
    );
    await deleteRecord(client, NOTES, 'n1', '"4"', PARENT);
    await expect(getRecord(client, NOTES, 'n1', PARENT)).rejects.toSatisfy(
      (e: unknown) => isApiError(e) && e.status === 404,
    );
    await expect(listRecords(client, NOTES)).rejects.toThrow('no value for {personId}');
    await expect(listRecords(client, { ...NOTES, ops: {} })).rejects.toThrow('has no List operation');
  });

  it('serves the screens through query hooks that refresh after each change', async () => {
    const { runtime, wrapper } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
    const { result } = renderHook(
      () => ({
        list: useInfiniteRecords(NOTES, { parentId: PARENT }),
        idle: useInfiniteRecords(NOTES, {}),
        one: useRecord(NOTES, 'n1', PARENT),
        create: useCreateRecord(NOTES),
        update: useUpdateRecord(NOTES),
        remove: useDeleteRecord(NOTES),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.list.data?.pages[0]?.data).toHaveLength(1));
    expect(result.current.idle.fetchStatus).toBe('idle');
    await waitFor(() => expect(result.current.one.data?.etag).toBe('"1"'));
    await act(() => result.current.create.mutateAsync({ values: { title: 'New' }, parentId: PARENT }));
    await waitFor(() => expect(result.current.list.data?.pages[0]?.data).toHaveLength(2));
    await act(() =>
      result.current.update.mutateAsync({
        id: 'n1',
        etag: '"1"',
        values: { title: 'Changed' },
        parentId: PARENT,
      }),
    );
    await waitFor(() => expect(result.current.one.data?.etag).toBe('"2"'));
    await act(() => result.current.remove.mutateAsync({ id: 'n1', etag: '"2"', parentId: PARENT }));
    await waitFor(() => expect(result.current.list.data?.pages[0]?.data).toHaveLength(1));
  });
});
