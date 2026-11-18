import type { finance } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * One school's fees: Asha Rao (a student of attendanceFixture) is charged Tuition in two installments of an annual
 * plan (5 % off): 10260.00 due on 1 April and 20520.00 on 30 June; a draft fee structure waits to be published.
 */
export const financeFixture = {
  institutionId: attendanceFixture.institutionId,
  studentId: attendanceFixture.students[0]?.id ?? '',
  studentName: attendanceFixture.students[0]?.name ?? '',
  yearId: id('fa1'),
  currencyId: id('fc1'),
  tuitionId: id('fd1'),
  planId: id('fe1'),
  structureId: id('ff1'),
  firstDueId: id('f01'),
  secondDueId: id('f02'),
  operationId: id('f11'),
} as const;

/** What the fee office's role holds. */
export const FEE_OFFICE = [
  'finance.fee_head.read',
  'finance.fee_plan.read',
  'finance.fee_structure.read',
  'finance.fee_structure.publish',
  'finance.student_fee_due.read',
  'finance.student_fee_due.generate_fee_dues',
  'finance.invoice.read',
  'finance.invoice_line.read',
  'finance.invoice.create_invoice_from_dues',
  'finance.invoice.post_invoice',
  'finance.invoice.cancel_invoice',
  'finance.invoice.send_fee_reminders',
  'finance.invoice.get_fee_statement',
  'finance.payment.read',
  'finance.payment.record',
  'finance.payment.refund_payment',
  'finance.refund.read',
  'org.institution.read',
] as const;

/** What a guardian needs to follow and pay their children's fees. */
export const FEE_FAMILY = [
  'finance.invoice.get_fee_statement',
  'finance.payment.quote_checkout',
  'finance.payment.start_checkout',
  'finance.payment.read',
  'sis.guardian.list_my_children',
] as const;

type Stored<T> = { -readonly [K in keyof T]: T[K] } & { id: string; version: number };

const out = <T extends object>(row: Stored<T>) => {
  const { version, ...rest } = row;
  return { ...rest, etag: `"${version}"` };
};
const cents = (amount: string | undefined) => Math.round(Number(amount ?? '0') * 100);
const text = (value: number) => (value / 100).toFixed(2);
let sequence = 0;
const nextId = () => id(`f9${String(++sequence).padStart(4, '0')}`);

/**
 * The finance module as the screens use it, in memory, with the server's rules for that slice: invoices raised from
 * draft dues of one student, posted and cancelled only as drafts under the body etag (412/409), counter payments
 * whose allocations add up and stay within what is outstanding (422), refunds of at most what is left (422), the
 * statement's totals, a checkout opened for an open invoice, and reminders for the open invoices.
 */
export function fakeFinance(): Record<string, Handler> {
  const f = financeFixture;
  const due = (
    dueId: string,
    dueDate: string,
    amount: string,
    discount: string,
  ): Stored<finance.StudentFeeDue> => ({
    id: dueId,
    version: 1,
    institutionId: f.institutionId,
    studentId: f.studentId,
    academicYearId: f.yearId,
    installmentId: id('fb1'),
    feeHeadId: f.tuitionId,
    dueDate,
    amount,
    currencyId: f.currencyId,
    discountPercent: '5.00',
    discountAmount: discount,
    netAmount: text(cents(amount) - cents(discount)),
    status: 'draft',
  });
  const dues = new Map<string, Stored<finance.StudentFeeDue>>([
    [f.firstDueId, due(f.firstDueId, '2026-04-01', '10800.00', '540.00')],
    [f.secondDueId, due(f.secondDueId, '2026-06-30', '21600.00', '1080.00')],
  ]);
  const invoices = new Map<string, Stored<finance.Invoice>>();
  const lines = new Map<string, finance.InvoiceLine[]>();
  const payments = new Map<string, Stored<finance.Payment>>();
  const refunds: finance.Refund[] = [];
  const structures = new Map<string, Stored<finance.FeeStructure>>([
    [
      f.structureId,
      {
        id: f.structureId,
        version: 1,
        institutionId: f.institutionId,
        academicYearId: f.yearId,
        feePlanId: f.planId,
        name: 'Class 5 fees',
        status: 'draft',
      },
    ],
  ]);
  let invoiceNumber = 0;
  let receiptNumber = 0;
  const filterOf = (req: Request) => new URL(req.url).searchParams.get('filter') ?? '';
  const keeps = (filter: string, field: string, value: string | undefined) =>
    !new RegExp(`${field} = "`).test(filter) || filter.includes(`${field} = "${value}"`);
  const operation = (result: Record<string, unknown>) =>
    json(
      {
        operationId: f.operationId,
        module: 'finance',
        kind: 'finance.GenerateFeeDues',
        status: 'succeeded',
        result,
      },
      { status: 202 },
    );
  const paidTo = (targetId: string) =>
    [...payments.values()]
      .filter((payment) => payment.status === 'received')
      .flatMap((payment) => payment.allocations ?? [])
      .filter((slice) => slice.invoiceId === targetId || slice.studentFeeDueId === targetId)
      .reduce((total, slice) => total + cents(slice.amount), 0);
  const settle = (invoice: Stored<finance.Invoice>) => {
    const paid = paidTo(invoice.id);
    invoice.amountPaid = text(paid);
    invoice.status = paid >= cents(invoice.total) ? 'paid' : paid > 0 ? 'partially_paid' : 'posted';
    invoice.version += 1;
  };
  const invoiceStep =
    (to: 'posted' | 'cancelled'): Handler =>
    async (req, params) => {
      const row = invoices.get(params.invoiceId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string };
      if (body.etag !== undefined && body.etag !== `"${row.version}"`) return problem(412, 'ETAG_MISMATCH');
      if (row.status !== 'draft')
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'Only a draft invoice changes.',
        });
      row.status = to;
      row.version += 1;
      if (to === 'posted') row.invoiceNumber = `INV-${String(++invoiceNumber).padStart(6, '0')}`;
      else
        for (const billed of dues.values())
          if (billed.invoiceId === row.id) {
            billed.status = 'draft';
            delete billed.invoiceId;
          }
      return json(out(row));
    };

  return {
    'GET /v1/master/currencies/{currencyId}': () =>
      json({ id: f.currencyId, code: 'INR', name: 'Indian rupee', minorUnits: 2, etag: '"1"' }),
    'GET /v1/org/institutions': (req) =>
      onePage(req, [
        { id: f.institutionId, name: 'Green Valley School', currencyId: f.currencyId, etag: '"1"' },
      ]),
    'GET /v1/sis/me/children': (req) =>
      onePage(req, [
        {
          student: { id: f.studentId, personId: id('601'), status: 'active' },
          person: { id: id('601'), firstName: 'Asha', lastName: 'Rao', displayName: f.studentName },
        },
      ]),
    'GET /v1/finance/fee-heads': (req) =>
      onePage(req, [{ id: f.tuitionId, code: 'TUI', name: 'Tuition', isActive: true, etag: '"1"' }]),
    'GET /v1/finance/fee-plans': (req) =>
      onePage(req, [
        { id: f.planId, institutionId: f.institutionId, code: 'ANNUAL', name: 'Annual plan', etag: '"1"' },
      ]),
    'GET /v1/finance/fee-structures': (req) => onePage(req, [...structures.values()].map(out)),
    'POST /v1/finance/fee-structures/{feeStructureId}:publish': (_req, params) => {
      const row = structures.get(params.feeStructureId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      row.status = 'published';
      row.version += 1;
      return operation({ students: 30, dues: 60, kept: 0, skippedCount: 0, skipped: [] });
    },
    'POST /v1/finance/student-fee-dues:generate': async (req) => {
      const body = (await req.json()) as finance.GenerateFeeDuesRequest;
      return operation({
        students: body.studentIds?.length ?? 0,
        dues: 2 * (body.studentIds?.length ?? 0),
        kept: 0,
      });
    },
    'GET /v1/finance/student-fee-dues': (req) => {
      const filter = filterOf(req);
      return onePage(
        req,
        [...dues.values()]
          .filter((row) => keeps(filter, 'studentId', row.studentId) && keeps(filter, 'status', row.status))
          .map(out),
      );
    },
    'POST /v1/finance/invoices:fromDues': async (req) => {
      const body = (await req.json()) as finance.CreateInvoiceFromDuesRequest;
      const billed = body.dueIds.map((dueId) => dues.get(dueId));
      if (billed.some((row) => !row || row.status !== 'draft'))
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', { detail: 'A due is already billed.' });
      const invoiceId = nextId();
      const subtotal = billed.reduce((sum, row) => sum + cents(row?.amount), 0);
      const discount = billed.reduce((sum, row) => sum + cents(row?.discountAmount), 0);
      const invoice: Stored<finance.Invoice> = {
        id: invoiceId,
        version: 1,
        institutionId: f.institutionId,
        invoiceNumber: `DRAFT-${invoiceId.replace(/-/g, '')}`,
        studentId: body.studentId,
        billToName: body.billToName ?? f.studentName,
        invoiceDate: '2026-04-01',
        currencyId: f.currencyId,
        subtotal: text(subtotal),
        discountTotal: text(discount),
        taxTotal: '0.00',
        total: text(subtotal - discount),
        amountPaid: '0.00',
        status: 'draft',
      };
      invoices.set(invoiceId, invoice);
      lines.set(
        invoiceId,
        billed.map((row, index) => ({
          id: nextId(),
          invoiceId,
          description: `Tuition · installment ${index + 1}`,
          unitPrice: row?.amount ?? '0',
          discountAmount: row?.discountAmount ?? '0',
          lineTotal: row?.netAmount ?? '0',
          etag: '"1"',
        })),
      );
      for (const row of billed) {
        if (!row) continue;
        row.status = 'invoiced';
        row.invoiceId = invoiceId;
        row.version += 1;
      }
      return json(out(invoice));
    },
    'GET /v1/finance/invoices': (req) => {
      const filter = filterOf(req);
      return onePage(
        req,
        [...invoices.values()]
          .filter((row) => keeps(filter, 'studentId', row.studentId) && keeps(filter, 'status', row.status))
          .filter(
            (row) =>
              !filter.includes('status = "posted" OR') ||
              row.status === 'posted' ||
              row.status === 'partially_paid',
          )
          .map(out),
      );
    },
    'GET /v1/finance/invoices/{invoiceId}': (_req, params) => {
      const row = invoices.get(params.invoiceId ?? '');
      return row ? json(out(row), { headers: { ETag: `"${row.version}"` } }) : problem(404, 'NOT_FOUND');
    },
    'GET /v1/finance/invoices/{invoiceId}/lines': (req, params) =>
      onePage(req, lines.get(params.invoiceId ?? '') ?? []),
    'POST /v1/finance/invoices/{invoiceId}:post': invoiceStep('posted'),
    'POST /v1/finance/invoices/{invoiceId}:cancel': invoiceStep('cancelled'),
    'POST /v1/finance/invoices:sendReminders': async (req) => {
      const body = (await req.json()) as finance.SendFeeRemindersRequest;
      const open = [...invoices.values()].filter(
        (row) => row.status === 'posted' || row.status === 'partially_paid',
      );
      const chosen = body.invoiceIds?.length ? open.filter((row) => body.invoiceIds?.includes(row.id)) : open;
      return json({
        queued: chosen.length,
        skipped: (body.invoiceIds?.length ?? 0) ? body.invoiceIds!.length - chosen.length : 0,
      });
    },
    'GET /v1/finance/payments': (req) => onePage(req, [...payments.values()].map(out)),
    'POST /v1/finance/payments:record': async (req) => {
      const body = (await req.json()) as finance.RecordPaymentRequest;
      const allocations = body.allocations ?? [];
      const total = allocations.reduce((sum, slice) => sum + cents(slice.amount), 0);
      if (total !== cents(body.amount))
        return problem(422, 'VALIDATION_FAILED', 'Validation failed', {
          errors: [{ field: 'allocations', message: 'the allocations must add up to the amount' }],
        });
      for (const slice of allocations) {
        const target = slice.invoiceId
          ? invoices.get(slice.invoiceId)
          : dues.get(slice.studentFeeDueId ?? '');
        const owed = slice.invoiceId
          ? cents((target as finance.Invoice | undefined)?.total)
          : cents((target as finance.StudentFeeDue | undefined)?.netAmount);
        if (!target || cents(slice.amount) > owed - paidTo(target.id ?? ''))
          return problem(422, 'VALIDATION_FAILED', 'Validation failed', {
            errors: [{ field: 'allocations[0].amount', message: 'exceeds what is outstanding' }],
          });
      }
      const paymentId = nextId();
      const payment: Stored<finance.Payment> = {
        id: paymentId,
        version: 1,
        institutionId: f.institutionId,
        payerUserId: '',
        payerName: body.payerName,
        purpose: 'fees',
        amount: body.amount,
        currencyId: body.currencyId,
        billingName: body.billingName,
        status: 'received',
        paymentMethod: body.paymentMethod,
        receiptNumber: `RCT-${String(++receiptNumber).padStart(6, '0')}`,
        receivedAt: '2026-04-02T05:30:00Z',
        allocations: allocations.map((slice) => ({
          ...slice,
          id: nextId(),
          currencyId: body.currencyId,
        })),
      };
      payments.set(paymentId, payment);
      for (const slice of allocations) {
        const invoice = slice.invoiceId ? invoices.get(slice.invoiceId) : undefined;
        if (invoice) settle(invoice);
      }
      return json(out(payment), { status: 201 });
    },
    'POST /v1/finance/payments/{paymentId}:refund': async (req, params) => {
      const payment = payments.get(params.paymentId ?? '');
      if (!payment) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as finance.RefundPaymentRequest;
      const left =
        cents(payment.amount) -
        refunds
          .filter((row) => row.paymentId === payment.id)
          .reduce((sum, row) => sum + cents(row.amount), 0);
      const amount = body.amount ? cents(body.amount) : left;
      if (amount > left)
        return problem(422, 'VALIDATION_FAILED', 'Validation failed', {
          errors: [{ field: 'amount', message: `at most what is left to refund (${text(left)})` }],
        });
      const refund: finance.Refund = {
        id: nextId(),
        paymentId: payment.id,
        amount: text(amount),
        status: 'processed',
        refundMethod: body.refundMethod ?? 'cash',
        refundedAt: '2026-04-03T05:30:00Z',
        etag: '"1"',
      };
      refunds.push(refund);
      if (amount === left) payment.status = 'refunded';
      payment.version += 1;
      return json(refund);
    },
    'GET /v1/finance/refunds': (req) => onePage(req, refunds),
    'GET /v1/finance/students/{studentId}/fee-statement': (_req, params) => {
      if (params.studentId !== f.studentId) return problem(404, 'NOT_FOUND');
      const owned = [...dues.values()];
      const billed = [...invoices.values()].filter(
        (row) => row.status !== 'draft' && row.status !== 'cancelled',
      );
      const due =
        owned
          .filter((row) => !row.invoiceId || invoices.get(row.invoiceId)?.status === 'draft')
          .reduce((sum, row) => sum + cents(row.netAmount), 0) +
        billed.reduce((sum, row) => sum + cents(row.total), 0);
      const paid = [...owned.map((row) => row.id), ...billed.map((row) => row.id)].reduce(
        (sum, targetId) => sum + paidTo(targetId),
        0,
      );
      return json({
        dues: owned.map(out),
        invoices: [...invoices.values()].map(out),
        payments: [...payments.values()].map(out),
        totalDue: text(due),
        totalPaid: text(paid),
        balance: text(due - paid),
        currencyId: f.currencyId,
        currencyCode: 'INR',
      });
    },
    'POST /v1/finance/checkout:quote': async (req) => {
      const body = (await req.json()) as finance.QuoteCheckoutRequest;
      const invoice = invoices.get(body.targetId);
      if (!invoice) return problem(404, 'NOT_FOUND');
      const left = text(cents(invoice.total) - cents(invoice.amountPaid));
      return json({
        originalAmount: left,
        discountAmount: '0.00',
        taxAmount: '0.00',
        totalAmount: left,
        currencyCode: 'INR',
        couponApplied: false,
      });
    },
    'POST /v1/finance/checkout:start': async (req) => {
      const body = (await req.json()) as finance.StartCheckoutRequest;
      const invoice = invoices.get(body.targetId);
      if (!invoice) return problem(404, 'NOT_FOUND');
      const paymentId = nextId();
      const payment: Stored<finance.Payment> = {
        id: paymentId,
        version: 1,
        institutionId: f.institutionId,
        payerUserId: id('99'),
        purpose: 'invoice',
        invoiceId: invoice.id,
        amount: text(cents(invoice.total) - cents(invoice.amountPaid)),
        currencyId: f.currencyId,
        billingName: body.billingName,
        status: 'initiated',
        paymentMethod: 'gateway',
        gatewayOrderId: 'order_TEST1',
      };
      payments.set(paymentId, payment);
      return json({ payment: out(payment), gatewayOrderId: 'order_TEST1', gatewayPublicKey: 'rzp_test_key' });
    },
  };
}
