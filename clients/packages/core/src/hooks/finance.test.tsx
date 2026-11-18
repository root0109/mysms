import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import {
  FEE_FAMILY,
  FEE_OFFICE,
  fakeApi,
  fakeFinance,
  financeFixture,
  json,
  schoolAdmin,
} from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  feeFilter,
  isPayable,
  useFamilyStudents,
  useFeeDues,
  useFeeHeads,
  useFeePayments,
  useFeePlans,
  useFeeRefunds,
  useFeeStatement,
  useFeeStructures,
  useGenerateFeeDues,
  useInvoice,
  useInvoiceFromDues,
  useInvoiceLines,
  useInvoiceStep,
  useInvoices,
  usePublishFeeStructure,
  useQuoteCheckout,
  useRecordFeePayment,
  useRefundFeePayment,
  useSendFeeReminders,
  useStartCheckout,
} from './finance';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const f = financeFixture;

async function setup(permissions: readonly string[] = FEE_OFFICE) {
  const me = { ...schoolAdmin, permissions: [...permissions] };
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: me.user }),
    'GET /v1/iam/me': () => json(me),
    ...fakeFinance(),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

const sent = (api: Awaited<ReturnType<typeof setup>>['api'], path: string) =>
  api.calls.filter((c) => c.method !== 'GET' && c.path === path);
const bodyOf = (api: Awaited<ReturnType<typeof setup>>['api'], path: string) =>
  JSON.parse(sent(api, path).at(-1)?.body ?? '{}') as Record<string, unknown>;

describe('finance helpers', () => {
  it('builds equality filters and tells payable invoices', () => {
    expect(feeFilter({ studentId: 's1', status: '', feeHeadId: undefined })).toBe('studentId = "s1"');
    expect(feeFilter({ a: 'x', b: 'y' })).toBe('a = "x" AND b = "y"');
    expect(feeFilter({ a: '' })).toBeUndefined();
    expect(isPayable({ status: 'posted' })).toBe(true);
    expect(isPayable({ status: 'partially_paid' })).toBe(true);
    expect(isPayable({ status: 'draft' })).toBe(false);
  });
});

describe('finance hooks', () => {
  it('bills dues on an invoice, posts it, takes a counter payment and refunds part of it', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        dues: useFeeDues(feeFilter({ studentId: f.studentId })),
        invoices: useInvoices(undefined),
        payments: useFeePayments(undefined),
        refunds: useFeeRefunds(undefined),
        heads: useFeeHeads(),
        plans: useFeePlans(),
        structures: useFeeStructures(),
        fromDues: useInvoiceFromDues(),
        step: useInvoiceStep(),
        record: useRecordFeePayment(),
        refund: useRefundFeePayment(),
        remind: useSendFeeReminders(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.dues.data?.items).toHaveLength(2));
    expect(result.current.dues.data?.items.map((due) => due.netAmount)).toEqual(['10260.00', '20520.00']);
    await waitFor(() => expect(result.current.heads.data?.items[0]?.name).toBe('Tuition'));
    await waitFor(() => expect(result.current.plans.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.structures.data?.items[0]?.status).toBe('draft'));

    let invoice = await act(() =>
      result.current.fromDues.mutateAsync({
        studentId: f.studentId,
        dueIds: [f.firstDueId, f.secondDueId],
        billToName: '  Mr Rao ',
      }),
    );
    expect(bodyOf(api, '/v1/finance/invoices:fromDues')).toEqual({
      studentId: f.studentId,
      dueIds: [f.firstDueId, f.secondDueId],
      billToName: 'Mr Rao',
    });
    expect(sent(api, '/v1/finance/invoices:fromDues')[0]?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(invoice.total).toBe('30780.00');
    invoice = await act(() =>
      result.current.step.mutateAsync({ step: 'post', id: invoice.id ?? '', etag: invoice.etag ?? '' }),
    );
    expect(invoice.invoiceNumber).toBe('INV-000001');
    await waitFor(() => expect(result.current.invoices.data?.items[0]?.status).toBe('posted'));

    const payment = await act(() =>
      result.current.record.mutateAsync({
        payerName: ' Mr Rao ',
        billingName: 'Mr Rao',
        amount: '10000.00 ',
        currencyId: f.currencyId,
        paymentMethod: 'cheque',
        instrumentReference: ' 004512 ',
        instrumentDate: '2026-04-02',
        bankName: 'State Bank',
        allocations: [{ invoiceId: invoice.id ?? '', amount: ' 10000.00' }],
      }),
    );
    expect(bodyOf(api, '/v1/finance/payments:record')).toEqual({
      payerName: 'Mr Rao',
      billingName: 'Mr Rao',
      amount: '10000.00',
      currencyId: f.currencyId,
      paymentMethod: 'cheque',
      instrumentReference: '004512',
      instrumentDate: '2026-04-02',
      bankName: 'State Bank',
      allocations: [{ invoiceId: invoice.id, amount: '10000.00' }],
    });
    expect(payment.receiptNumber).toBe('RCT-000001');
    await waitFor(() => expect(result.current.invoices.data?.items[0]?.status).toBe('partially_paid'));
    await waitFor(() => expect(result.current.payments.data?.items).toHaveLength(1));

    await act(() =>
      result.current.refund.mutateAsync({
        paymentId: payment.id ?? '',
        etag: payment.etag ?? '',
        amount: ' 4000.00 ',
        refundMethod: 'cheque',
        reason: ' Overpaid ',
      }),
    );
    expect(bodyOf(api, `/v1/finance/payments/${payment.id}:refund`)).toEqual({
      etag: payment.etag,
      amount: '4000.00',
      refundMethod: 'cheque',
      reason: 'Overpaid',
    });
    await waitFor(() => expect(result.current.refunds.data?.items[0]?.amount).toBe('4000.00'));

    const reminded = await act(() =>
      result.current.remind.mutateAsync({ institutionId: f.institutionId, invoiceIds: [], channel: 'sms' }),
    );
    expect(reminded).toMatchObject({ queued: 1 });
  });

  it('reads one invoice with its lines and cancels a draft, releasing its dues', async () => {
    const { api, wrapper } = await setup();
    const make = renderHook(() => useInvoiceFromDues(), { wrapper });
    const draft = await act(() =>
      make.result.current.mutateAsync({ studentId: f.studentId, dueIds: [f.firstDueId] }),
    );
    expect(bodyOf(api, '/v1/finance/invoices:fromDues')).not.toHaveProperty('billToName');
    const { result } = renderHook(
      () => ({
        invoice: useInvoice(draft.id ?? null),
        lines: useInvoiceLines(draft.id ?? null),
        none: useInvoice(null),
        step: useInvoiceStep(),
        dues: useFeeDues(feeFilter({ status: 'draft' })),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.invoice.data?.status).toBe('draft'));
    await waitFor(() =>
      expect(result.current.lines.data?.items[0]?.description).toBe('Tuition · installment 1'),
    );
    expect(result.current.none.fetchStatus).toBe('idle');
    await waitFor(() => expect(result.current.dues.data?.items).toHaveLength(1));
    await act(() =>
      result.current.step.mutateAsync({
        step: 'cancel',
        id: draft.id ?? '',
        etag: draft.etag ?? '',
        reason: ' Raised by mistake ',
      }),
    );
    expect(bodyOf(api, `/v1/finance/invoices/${draft.id}:cancel`)).toEqual({
      etag: draft.etag,
      reason: 'Raised by mistake',
    });
    await waitFor(() => expect(result.current.dues.data?.items).toHaveLength(2));
  });

  it('runs fee dues and publishes a structure as operations', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => ({ run: useGenerateFeeDues(), publish: usePublishFeeStructure() }), {
      wrapper,
    });
    const run = await act(() =>
      result.current.run.mutateAsync({
        feePlanId: f.planId,
        academicYearId: f.yearId,
        startDate: '2026-04-01',
        studentIds: [f.studentId],
        classSectionId: 'class-5a',
        totalAmount: ' 36000.00 ',
      }),
    );
    expect(bodyOf(api, '/v1/finance/student-fee-dues:generate')).toEqual({
      feePlanId: f.planId,
      academicYearId: f.yearId,
      startDate: '2026-04-01',
      studentIds: [f.studentId],
      programEnrollmentIds: [],
      classSectionId: 'class-5a',
      totalAmount: '36000.00',
    });
    expect(run.status).toBe('succeeded');
    await act(() =>
      result.current.run.mutateAsync({
        feePlanId: f.planId,
        academicYearId: f.yearId,
        startDate: '2026-04-01',
        studentIds: [f.studentId],
      }),
    );
    expect(bodyOf(api, '/v1/finance/student-fee-dues:generate')).not.toHaveProperty('totalAmount');
    const published = await act(() => result.current.publish.mutateAsync(f.structureId));
    expect(published.result).toMatchObject({ students: 30, dues: 60 });
    expect(
      sent(api, `/v1/finance/fee-structures/${f.structureId}:publish`)[0]?.headers.get('Idempotency-Key'),
    ).toBeTruthy();
  });

  it("gives a guardian their child's statement, a quote and a checkout under the attempt's key", async () => {
    const { api, wrapper } = await setup(FEE_FAMILY);
    // the fee office bills and posts first (the fake does not check who calls)
    const make = renderHook(() => ({ fromDues: useInvoiceFromDues(), step: useInvoiceStep() }), { wrapper });
    const draft = await act(() =>
      make.result.current.fromDues.mutateAsync({
        studentId: f.studentId,
        dueIds: [f.firstDueId, f.secondDueId],
      }),
    );
    const posted = await act(() =>
      make.result.current.step.mutateAsync({ step: 'post', id: draft.id ?? '', etag: draft.etag ?? '' }),
    );

    const { result } = renderHook(
      () => ({
        family: useFamilyStudents(),
        statement: useFeeStatement(f.studentId),
        quote: useQuoteCheckout(posted.id ?? null),
        start: useStartCheckout(),
      }),
      { wrapper },
    );
    await waitFor(() =>
      expect(result.current.family.students).toEqual([{ id: f.studentId, name: f.studentName, self: false }]),
    );
    expect(result.current.family.isPending).toBe(false);
    await waitFor(() => expect(result.current.statement.data?.balance).toBe('30780.00'));
    await waitFor(() => expect(result.current.quote.data?.totalAmount).toBe('30780.00'));
    const session = await act(() =>
      result.current.start.mutateAsync({
        invoiceId: posted.id ?? '',
        billingName: ' Mr Rao ',
        attemptKey: 'attempt-1',
      }),
    );
    expect(session.gatewayOrderId).toBe('order_TEST1');
    const started = sent(api, '/v1/finance/checkout:start')[0];
    expect(started?.headers.get('Idempotency-Key')).toBe('attempt-1');
    expect(JSON.parse(started?.body ?? '{}')).toEqual({
      purpose: 'invoice',
      targetId: posted.id,
      billingName: 'Mr Rao',
      payerConsent: true,
    });
    await waitFor(() =>
      expect(result.current.statement.data?.payments?.map((payment) => payment.status)).toEqual([
        'initiated',
      ]),
    );
  });
});
