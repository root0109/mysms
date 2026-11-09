import { describe, expect, it } from 'vitest';
import { OPERATION_MODULES } from '../generated/operations';
import { fakeApi, json } from '@mysms/fixtures';
import { createApiClient } from '../api/client';
import { cancelOperation, getOperation, isDone, isOperationModule, pollInterval } from './operations';

const op = (module: string, status = 'running') => ({
  operationId: 'op1',
  module,
  kind: 'fees.generate',
  status,
  createTime: '2026-11-06T00:00:00Z',
});

describe('operations', () => {
  it('knows the operation-owning modules from the contract', () => {
    expect(OPERATION_MODULES).toEqual([
      'admission',
      'assessment',
      'comms',
      'finance',
      'iam',
      'lms',
      'payroll',
      'platform',
      'sis',
      'timetable',
    ]);
    expect(isOperationModule('finance')).toBe(true);
    expect(isOperationModule('library')).toBe(false);
  });

  it('polls and cancels at the owning module', async () => {
    const api = fakeApi({
      'GET /v1/{module}/operations/{operationId}': (_r, p) => json(op(p.module ?? '', 'succeeded')),
      'POST /v1/{module}/operations/{operationId}:cancel': (_r, p) => json(op(p.module ?? '', 'cancelled')),
    });
    const client = createApiClient({ baseUrl: 'https://h', clientId: 'test', fetch: api.fetch });
    await expect(getOperation(client, op('finance'))).resolves.toMatchObject({
      module: 'finance',
      status: 'succeeded',
    });
    await expect(cancelOperation(client, op('payroll'))).resolves.toMatchObject({ status: 'cancelled' });
    expect(api.calls.map((c) => `${c.method} ${c.path}`)).toEqual([
      'GET /v1/finance/operations/op1',
      'POST /v1/payroll/operations/op1:cancel',
    ]);
    expect(() => getOperation(client, op('library'))).toThrow('does not own operations');
  });

  it('stops at terminal states and backs off', () => {
    expect(isDone({ status: 'running' })).toBe(false);
    expect(isDone({ status: 'failed' })).toBe(true);
    expect(pollInterval(0)).toBe(1000);
    expect(pollInterval(10)).toBe(5000);
  });
});
