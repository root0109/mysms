import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { finance } from '@mysms/sdk';
import { FeeHeadApi } from '@mysms/sdk/finance/apis/FeeHeadApi';
import { FeePlanApi } from '@mysms/sdk/finance/apis/FeePlanApi';
import { FeeStructureApi } from '@mysms/sdk/finance/apis/FeeStructureApi';
import { InvoiceApi } from '@mysms/sdk/finance/apis/InvoiceApi';
import { PaymentApi } from '@mysms/sdk/finance/apis/PaymentApi';
import { RefundApi } from '@mysms/sdk/finance/apis/RefundApi';
import { StudentFeeDueApi } from '@mysms/sdk/finance/apis/StudentFeeDueApi';
import { newIdempotencyKey } from '../api/ids';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { useCan, useSession } from './session';
import { useMyChildren } from './sis';

export type FeeDue = finance.StudentFeeDue;
export type Invoice = finance.Invoice;
export type InvoiceLine = finance.InvoiceLine;
export type FeePayment = finance.Payment;
export type FeeRefund = finance.Refund;
export type FeeStatement = finance.FeeStatement;
export type FeeHead = finance.FeeHead;
export type FeePlan = finance.FeePlan;
export type FeeStructure = finance.FeeStructure;
export type FinanceOperation = finance.Operation;
export type OrderQuote = finance.OrderQuote;
export type CheckoutSession = finance.CheckoutSession;
export type CounterMethod = finance.RecordPaymentRequest['paymentMethod'];
export type RefundMethod = NonNullable<finance.RefundPaymentRequest['refundMethod']>;
export type ReminderChannel = finance.SendFeeRemindersRequest['channel'];
export type InvoiceStep = 'post' | 'cancel';

export const COUNTER_METHODS: readonly CounterMethod[] = [
  'cash',
  'cheque',
  'dd',
  'bank_transfer',
  'card_pos',
  'adjustment',
];
export const REFUND_METHODS: readonly RefundMethod[] = [
  'gateway',
  'cash',
  'cheque',
  'bank_transfer',
  'adjustment',
];
export const REMINDER_CHANNELS: readonly ReminderChannel[] = ['sms', 'email', 'push'];

/** An invoice the family can pay online: posted and not fully paid. */
export const isPayable = (invoice: Pick<Invoice, 'status'>): boolean =>
  invoice.status === 'posted' || invoice.status === 'partially_paid';

const KEY = (resource: string, params?: unknown) => queryKey('finance', resource, params);

/** After a write: the finance lists of `resources` and the standard screens of their record types are stale. */
async function refresh(qc: QueryClient, resources: readonly string[], records: readonly string[]) {
  for (const record of records) {
    void qc.invalidateQueries({ queryKey: queryKey('records', `finance.${record}`) });
    void qc.invalidateQueries({ queryKey: queryKey('records', `finance.${record}#item`) });
  }
  await Promise.all(resources.map((resource) => qc.invalidateQueries({ queryKey: KEY(resource) })));
}

/** An AIP-160 filter of the given equalities (empty values left out); undefined when nothing is filtered. */
export function feeFilter(fields: Readonly<Record<string, string | undefined>>): string | undefined {
  const terms = Object.entries(fields)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([field, value]) => `${field} = "${value}"`);
  return terms.length ? terms.join(' AND ') : undefined;
}

// ---- reads ----

/** finance.ListStudentFeeDues: the dues the caller may read (the fee office's, or the family's own), by due date. */
export function useFeeDues(filter: string | undefined, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('dues', filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StudentFeeDueApi).listStudentFeeDues({
            size: 500,
            ...(filter ? { filter } : {}),
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** finance.ListInvoices, newest first. */
export function useInvoices(filter: string | undefined, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('invoices', filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(InvoiceApi).listInvoices({
            size: 500,
            ...(filter ? { filter } : {}),
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** finance.GetInvoice: one invoice with its etag. */
export function useInvoice(invoiceId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: invoiceId !== null,
    queryKey: KEY('invoice', invoiceId),
    queryFn: () => call(api.get(InvoiceApi).getInvoice({ invoiceId: invoiceId as string })),
    staleTime: STALE_TIME.detail,
  });
}

/** finance.ListInvoiceLines of one invoice. */
export function useInvoiceLines(invoiceId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: invoiceId !== null,
    queryKey: KEY('invoice-lines', invoiceId),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(InvoiceApi)
            .listInvoiceLines({ invoiceId: invoiceId as string, size: 500, ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** finance.ListPayments, newest first (the fee office's, or the caller's own). */
export function useFeePayments(filter: string | undefined, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('payments', filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(PaymentApi).listPayments({
            size: 500,
            ...(filter ? { filter } : {}),
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** finance.ListRefunds, newest first. */
export function useFeeRefunds(filter: string | undefined, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('refunds', filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(RefundApi).listRefunds({
            size: 500,
            ...(filter ? { filter } : {}),
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * finance.GetFeeStatement: a student's dues, invoices and payments with the totals the server worked out (the screen
 * never recomputes an official amount). Refetched every few seconds while `polling` (a checkout waits for the
 * gateway's notification).
 */
export function useFeeStatement(studentId: string | null, academicYearId?: string, polling = false) {
  const { api } = useRuntime();
  return useQuery({
    enabled: studentId !== null,
    queryKey: KEY('statement', { studentId, academicYearId: academicYearId ?? null }),
    queryFn: () =>
      call(
        api.get(InvoiceApi).getFeeStatement({
          studentId: studentId as string,
          ...(academicYearId ? { academicYearId } : {}),
        }),
      ),
    staleTime: STALE_TIME.detail,
    refetchInterval: polling ? 4_000 : false,
  });
}

/** finance.ListFeeHeads: the chargeable items, by name. */
export function useFeeHeads(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('fee-heads'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(FeeHeadApi).listFeeHeads({ size: 500, sort: ['name'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.reference,
  });
}

/** finance.ListFeePlans: the payment terms, by name. */
export function useFeePlans(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('fee-plans'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(FeePlanApi).listFeePlans({ size: 500, sort: ['name'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** finance.ListFeeStructures: what each class or program costs per year, newest first. */
export function useFeeStructures(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('fee-structures'),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(FeeStructureApi)
            .listFeeStructures({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

// ---- writes ----

/** What a fee run charges: a plan for a year, from a start date, to named students and/or a class section. */
export interface FeeRunDraft {
  feePlanId: string;
  academicYearId: string;
  startDate: string;
  studentIds: string[];
  classSectionId?: string;
  totalAmount?: string;
}

/** finance.GenerateFeeDues: answers the finished operation (with its counts); one Idempotency-Key per attempt. */
export function useGenerateFeeDues() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: FeeRunDraft) =>
      call(
        api.get(StudentFeeDueApi).generateFeeDues({
          idempotencyKey: newIdempotencyKey(),
          generateFeeDuesRequest: {
            feePlanId: draft.feePlanId,
            academicYearId: draft.academicYearId,
            startDate: draft.startDate,
            studentIds: draft.studentIds,
            programEnrollmentIds: [],
            ...(draft.classSectionId ? { classSectionId: draft.classSectionId } : {}),
            ...(draft.totalAmount?.trim() ? { totalAmount: draft.totalAmount.trim() } : {}),
          },
        }),
      ),
    onSettled: () => refresh(qc, ['dues', 'statement'], ['StudentFeeDue']),
  });
}

/** finance.PublishFeeStructure: generates the structure's dues, then publishes it; answers the fee run's operation. */
export function usePublishFeeStructure() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (feeStructureId: string) =>
      call(
        api.get(FeeStructureApi).publishFeeStructure({ feeStructureId, idempotencyKey: newIdempotencyKey() }),
      ),
    onSettled: () => refresh(qc, ['fee-structures', 'dues', 'statement'], ['FeeStructure', 'StudentFeeDue']),
  });
}

/** finance.CreateInvoiceFromDues: a draft invoice billing the student's chosen dues. */
export function useInvoiceFromDues() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: { studentId: string; dueIds: string[]; billToName?: string }) =>
      call(
        api.get(InvoiceApi).createInvoiceFromDues({
          idempotencyKey: newIdempotencyKey(),
          createInvoiceFromDuesRequest: {
            studentId: request.studentId,
            dueIds: request.dueIds,
            ...(request.billToName?.trim() ? { billToName: request.billToName.trim() } : {}),
          },
        }),
      ),
    onSettled: () => refresh(qc, ['dues', 'invoices', 'statement'], ['StudentFeeDue', 'Invoice']),
  });
}

/**
 * finance.PostInvoice (numbered, never changed again) and finance.CancelInvoice (a draft only; its dues are
 * released), under the etag the screen last read (412 when the invoice changed meanwhile, 409 in another state).
 */
export function useInvoiceStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      step,
      id,
      etag,
      reason,
    }: {
      step: InvoiceStep;
      id: string;
      etag: string;
      reason?: string;
    }) => {
      const invoices = api.get(InvoiceApi);
      return step === 'post'
        ? call(invoices.postInvoice({ invoiceId: id, postInvoiceRequest: { etag } }))
        : call(
            invoices.cancelInvoice({
              invoiceId: id,
              cancelInvoiceRequest: { etag, ...(reason?.trim() ? { reason: reason.trim() } : {}) },
            }),
          );
    },
    onSettled: () => refresh(qc, ['invoices', 'invoice', 'dues', 'statement'], ['Invoice', 'StudentFeeDue']),
  });
}

/** One slice of a counter payment: exactly one due or one invoice, and its amount. */
export interface AllocationDraft {
  invoiceId?: string;
  studentFeeDueId?: string;
  amount: string;
}

/** A counter payment as recorded at the fee office. */
export interface CounterPaymentDraft {
  payerName: string;
  billingName: string;
  amount: string;
  currencyId: string;
  paymentMethod: CounterMethod;
  instrumentReference?: string;
  instrumentDate?: string;
  bankName?: string;
  allocations: AllocationDraft[];
}

/** finance.RecordPayment: the server numbers the receipt; one Idempotency-Key per attempt. */
export function useRecordFeePayment() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: CounterPaymentDraft) =>
      call(
        api.get(PaymentApi).recordPayment({
          idempotencyKey: newIdempotencyKey(),
          recordPaymentRequest: {
            payerName: draft.payerName.trim(),
            billingName: draft.billingName.trim(),
            amount: draft.amount.trim(),
            currencyId: draft.currencyId,
            paymentMethod: draft.paymentMethod,
            ...(draft.instrumentReference?.trim()
              ? { instrumentReference: draft.instrumentReference.trim() }
              : {}),
            ...(draft.instrumentDate ? { instrumentDate: draft.instrumentDate } : {}),
            ...(draft.bankName?.trim() ? { bankName: draft.bankName.trim() } : {}),
            allocations: draft.allocations.map((slice) => ({ ...slice, amount: slice.amount.trim() })),
          },
        }),
      ),
    onSettled: () => refresh(qc, ['payments', 'invoices', 'invoice', 'dues', 'statement'], ['Payment']),
  });
}

/** finance.RefundPayment: part (an amount) or the rest of a payment; one Idempotency-Key per attempt. */
export function useRefundFeePayment() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: {
      paymentId: string;
      etag: string;
      amount?: string;
      refundMethod?: RefundMethod;
      reason?: string;
    }) =>
      call(
        api.get(PaymentApi).refundPayment({
          paymentId: request.paymentId,
          idempotencyKey: newIdempotencyKey(),
          refundPaymentRequest: {
            etag: request.etag,
            ...(request.amount?.trim() ? { amount: request.amount.trim() } : {}),
            ...(request.refundMethod ? { refundMethod: request.refundMethod } : {}),
            ...(request.reason?.trim() ? { reason: request.reason.trim() } : {}),
          },
        }),
      ),
    onSettled: () => refresh(qc, ['payments', 'refunds', 'invoices', 'invoice', 'statement'], ['Payment']),
  });
}

/** finance.SendFeeReminders: one reminder per open invoice of the institution (only the named ones when given). */
export function useSendFeeReminders() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (request: { institutionId: string; invoiceIds: string[]; channel: ReminderChannel }) =>
      call(api.get(InvoiceApi).sendFeeReminders({ sendFeeRemindersRequest: request })),
  });
}

/** finance.QuoteCheckout: what paying the invoice online costs now. */
export function useQuoteCheckout(invoiceId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: invoiceId !== null,
    queryKey: KEY('quote', invoiceId),
    queryFn: () =>
      call(
        api
          .get(PaymentApi)
          .quoteCheckout({ quoteCheckoutRequest: { purpose: 'invoice', targetId: invoiceId as string } }),
      ),
    staleTime: 0,
    retry: false,
  });
}

/**
 * finance.StartCheckout: opens the gateway order for the invoice and an initiated payment. The key is the attempt's
 * (`attemptKey`): a double tap replays the first checkout instead of opening a second order.
 */
export function useStartCheckout() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: { invoiceId: string; billingName: string; attemptKey: string }) =>
      call(
        api.get(PaymentApi).startCheckout({
          idempotencyKey: request.attemptKey,
          startCheckoutRequest: {
            purpose: 'invoice',
            targetId: request.invoiceId,
            billingName: request.billingName.trim(),
            payerConsent: true,
          },
        }),
      ),
    onSettled: () => refresh(qc, ['statement', 'payments'], ['Payment']),
  });
}

/** A student whose fees the signed-in family member follows: their own student record, or a child. */
export interface FamilyStudent {
  id: string;
  /** The child's display name (sis); none for the caller's own record. */
  name?: string;
  self: boolean;
}

const NO_STUDENTS: readonly string[] = [];

/**
 * The students whose fees the signed-in student or guardian may see: their own student records (iam.GetMe) and their
 * children (sis.ListMyChildren, when the role may list them). The server checks every read again; this only chooses.
 */
export function useFamilyStudents() {
  const mayChildren = useCan('sis.guardian.list_my_children');
  const own = useSession((state) => state.me?.studentIds ?? NO_STUDENTS);
  const children = useMyChildren(mayChildren);
  const students: FamilyStudent[] = [
    ...own.map((id) => ({ id, self: true })),
    ...(children.data?.items ?? [])
      .filter((card) => card.student.id && !own.includes(card.student.id))
      .map((card) => ({ id: card.student.id ?? '', name: card.person.displayName, self: false })),
  ];
  return {
    students,
    isPending: mayChildren && children.isPending,
    isError: children.isError,
    refetch: () => void children.refetch(),
  };
}
