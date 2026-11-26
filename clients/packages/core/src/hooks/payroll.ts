import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { hr, payroll } from '@mysms/sdk';
import { StaffApi } from '@mysms/sdk/hr/apis/StaffApi';
import { CurrencyApi } from '@mysms/sdk/master/apis/CurrencyApi';
import { DisbursementAccountApi } from '@mysms/sdk/payroll/apis/DisbursementAccountApi';
import { PayCategoryApi } from '@mysms/sdk/payroll/apis/PayCategoryApi';
import { PayComponentApi } from '@mysms/sdk/payroll/apis/PayComponentApi';
import { PaymentTypeApi } from '@mysms/sdk/payroll/apis/PaymentTypeApi';
import { PayrollPaymentApi } from '@mysms/sdk/payroll/apis/PayrollPaymentApi';
import { PayslipApi } from '@mysms/sdk/payroll/apis/PayslipApi';
import { SalaryTemplateApi } from '@mysms/sdk/payroll/apis/SalaryTemplateApi';
import { StaffSalaryApi } from '@mysms/sdk/payroll/apis/StaffSalaryApi';
import type { Formatters } from '@mysms/i18n';
import { newIdempotencyKey } from '../api/ids';
import { allPages, pageQuery } from '../api/pages';
import { patchOf } from '../api/patch';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type PayComponent = payroll.PayComponent;
export type PayCategory = payroll.PayCategory;
export type PaymentType = payroll.PaymentType;
export type SalaryTemplate = payroll.SalaryTemplate;
export type SalaryTemplateComponent = payroll.SalaryTemplateComponent;
export type StaffSalary = payroll.StaffSalary;
export type StaffSalaryComponent = payroll.StaffSalaryComponent;
export type Payslip = payroll.Payslip;
export type PayslipLine = payroll.PayslipLine;
export type PayrollPayment = payroll.PayrollPayment;
export type DisbursementAccount = payroll.DisbursementAccount;
export type PayrollOperation = payroll.Operation;
export type PaymentMethod = payroll.PayrollPayment['paymentMethod'];
export type StaffMember = hr.Staff;
export type PayslipStep = 'markPaid' | 'cancel';

export const PAYMENT_METHODS: readonly PaymentMethod[] = ['bank_transfer', 'cash', 'cheque', 'other'];

// ---- exact amounts: the contract carries money as decimal strings, never as floating point ----

/** An amount the payroll tables can hold (numeric(14,2)): up to twelve whole digits and two decimals, not negative. */
const AMOUNT = /^\d{1,12}(\.\d{1,2})?$/;

export const isAmount = (value: string): boolean => AMOUNT.test(value.trim());

/** An amount in whole cents (exact: at most 14 digits, well inside a safe integer), or null when it is not one. */
export function toCents(amount: string): number | null {
  const value = amount.trim();
  if (!AMOUNT.test(value)) return null;
  const [whole = '0', fraction = ''] = value.split('.');
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
}

/** Whole cents as the contract writes an amount, with two decimals: 4200050 → `42000.50`. */
export function centsText(cents: number): string {
  const sign = cents < 0 ? '-' : '';
  const absolute = Math.abs(cents);
  return `${sign}${Math.floor(absolute / 100)}.${String(absolute % 100).padStart(2, '0')}`;
}

/** The exact sum of amounts; null when one of them is not an amount. */
export function sumAmounts(amounts: readonly string[]): string | null {
  let total = 0;
  for (const amount of amounts) {
    const cents = toCents(amount);
    if (cents === null) return null;
    total += cents;
  }
  return centsText(total);
}

/**
 * An amount for reading: in the institution's currency through the shared money formatting when it is known, else
 * as a plain number with two decimals (never a guessed currency).
 */
export function moneyText(
  format: Formatters,
  formatLocale: string,
  amount: string | undefined,
  currencyCode: string | null,
): string {
  if (amount === undefined) return '';
  if (currencyCode) return format.money(amount, currencyCode);
  return new Intl.NumberFormat(formatLocale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(amount),
  );
}

// ---- periods and salaries in force ----

const MONTH = /^(\d{4})-(\d{2})$/;

/** The pay period of a calendar month (`YYYY-MM`): its first and last day; null when it is not a month. */
export function monthPeriod(month: string): { periodStart: string; periodEnd: string } | null {
  const match = MONTH.exec(month);
  if (!match) return null;
  const year = Number(match[1]);
  const monthNumber = Number(match[2]);
  if (monthNumber < 1 || monthNumber > 12) return null;
  const lastDay = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate();
  return { periodStart: `${month}-01`, periodEnd: `${month}-${String(lastDay).padStart(2, '0')}` };
}

/**
 * The salary of a staff member in force on a day (the latest that has taken effect by then; the server pays a
 * period the same way). Undefined when none has taken effect yet.
 */
export function salaryInForce(
  salaries: readonly StaffSalary[],
  staffId: string,
  day: string,
): StaffSalary | undefined {
  return salaries
    .filter((salary) => salary.staffId === staffId && salary.effectiveDate <= day)
    .reduce<StaffSalary | undefined>(
      (latest, salary) => (!latest || salary.effectiveDate > latest.effectiveDate ? salary : latest),
      undefined,
    );
}

// ---- reads ----

const KEY = (resource: string, params?: unknown) => queryKey('payroll', resource, params);

/** After a write: the payroll lists of `resources` and the standard screens of their record types are stale. */
async function refresh(qc: QueryClient, resources: readonly string[], records: readonly string[]) {
  for (const record of records) {
    void qc.invalidateQueries({ queryKey: queryKey('records', `payroll.${record}`) });
    void qc.invalidateQueries({ queryKey: queryKey('records', `payroll.${record}#item`) });
  }
  await Promise.all(resources.map((resource) => qc.invalidateQueries({ queryKey: KEY(resource) })));
}

/** payroll.ListPayComponents: the earning, deduction and employer-defined components of the tenant. */
export function usePayComponents(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('pay-components'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(PayComponentApi).listPayComponents({ size: 500, sort: ['name'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.reference,
  });
}

/** payroll.ListPayCategories: the calculation bases of template components (fixed amount, % of basic…). */
export function usePayCategories(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('pay-categories'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(PayCategoryApi).listPayCategories({ size: 500, sort: ['name'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.reference,
  });
}

/** payroll.ListPaymentTypes: salary, advance, bonus… the kinds of a payroll payment. */
export function usePaymentTypes(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('payment-types'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(PaymentTypeApi).listPaymentTypes({ size: 500, sort: ['name'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.reference,
  });
}

/** payroll.ListSalaryTemplates: every template the caller may see (their institutions'), by name. */
export function useSalaryTemplates(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('salary-templates'),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(SalaryTemplateApi).listSalaryTemplates({ size: 500, sort: ['name'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** payroll.ListSalaryTemplateComponents of one template, in the order they were added. */
export function useTemplateComponents(salaryTemplateId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: salaryTemplateId !== null,
    queryKey: KEY('salary-template-components', salaryTemplateId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(SalaryTemplateApi).listSalaryTemplateComponents({
            salaryTemplateId: salaryTemplateId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * payroll.ListStaffSalaries: the effective-dated salaries the caller may see — every one, or one staff member's —
 * newest first.
 */
export function useStaffSalaries({ staffId, enabled = true }: { staffId?: string; enabled?: boolean } = {}) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('staff-salaries', staffId ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StaffSalaryApi).listStaffSalaries({
            size: 500,
            ...(staffId ? { filter: `staffId = "${staffId}"` } : {}),
            sort: ['effectiveDate,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** payroll.ListStaffSalaryComponents: the resolved amounts of one salary. */
export function useStaffSalaryComponents(staffSalaryId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: staffSalaryId !== null,
    queryKey: KEY('staff-salary-components', staffSalaryId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StaffSalaryApi).listStaffSalaryComponents({
            staffSalaryId: staffSalaryId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** hr.ListStaff: every staff record the caller may see, to choose whose salary is set or who is paid. */
export function useAllStaff(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('hr', 'staff', { all: true }),
    queryFn: () => allPages((page) => call(api.get(StaffApi).listStaff({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
}

/** Which payslips a screen shows: one institution's for one period (the run), or one staff member's own. */
export type PayslipQuery =
  { institutionId: string; periodStart: string; periodEnd: string } | { staffIds: readonly string[] };

const payslipFilter = (query: PayslipQuery): string =>
  'staffIds' in query
    ? `(${query.staffIds.map((id) => `staffId = "${id}"`).join(' OR ')})`
    : `institutionId = "${query.institutionId}" AND periodStart = "${query.periodStart}" AND periodEnd = "${query.periodEnd}"`;

/** payroll.ListPayslips of a run or of one staff member, newest period first. */
export function usePayslips(query: PayslipQuery | null) {
  const { api } = useRuntime();
  const usable = query !== null && (!('staffIds' in query) || query.staffIds.length > 0);
  return useQuery({
    enabled: usable,
    queryKey: KEY('payslips', query),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(PayslipApi).listPayslips({
            size: 500,
            filter: payslipFilter(query as PayslipQuery),
            sort: ['periodStart,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** payroll.ListPayslipLines: the lines of one payslip in the order they were computed. */
export function usePayslipLines(payslipId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: payslipId !== null,
    queryKey: KEY('payslip-lines', payslipId),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(PayslipApi)
            .listPayslipLines({ payslipId: payslipId as string, size: 500, ...pageQuery(page) }),
        ),
      ),
    // a payslip's lines never change once written
    staleTime: STALE_TIME.reference,
  });
}

/** payroll.ListDisbursementAccounts: the institution bank accounts salaries are paid from (numbers masked). */
export function useDisbursementAccounts(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('disbursement-accounts'),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(DisbursementAccountApi)
            .listDisbursementAccounts({ size: 500, sort: ['name'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** payroll.ListPayrollPayments: what was paid to staff, newest first. */
export function usePayrollPayments(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('payroll-payments'),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(PayrollPaymentApi)
            .listPayrollPayments({ size: 500, sort: ['transactionDate,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** master.GetCurrency: the ISO code of an institution's currency, for the shared money formatting. */
export function useCurrencyCode(currencyId: string | null | undefined): string | null {
  const { api } = useRuntime();
  const read = useQuery({
    enabled: Boolean(currencyId),
    queryKey: queryKey('master', 'currency', currencyId),
    queryFn: () => call(api.get(CurrencyApi).getCurrency({ currencyId: currencyId as string })),
    staleTime: STALE_TIME.reference,
    retry: false,
  });
  return read.data?.code ?? null;
}

// ---- writes ----

/**
 * Which row a save writes: a new one has no id; a stored one carries its id and the etag it was read at, sent as
 * If-Match (the server answers 428 without one and 412 when it is stale), so an edit never overwrites blindly.
 */
export type Revision = { id?: undefined; etag?: undefined } | { id: string; etag: string };

/** A salary template as saved. */
export type SalaryTemplateDraft = Revision & { institutionId: string; name: string; isActive: boolean };

/** payroll.CreateSalaryTemplate / UpdateSalaryTemplate (If-Match: a stale etag is a 412). */
export function useSaveSalaryTemplate() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: SalaryTemplateDraft) => {
      const templates = api.get(SalaryTemplateApi);
      const salaryTemplate = {
        institutionId: draft.institutionId,
        name: draft.name.trim(),
        isActive: draft.isActive,
      };
      if (draft.id === undefined) return call(templates.createSalaryTemplate({ salaryTemplate }));
      return call(
        templates.updateSalaryTemplate({
          salaryTemplateId: draft.id,
          ifMatch: draft.etag,
          salaryTemplateUpdate: { name: salaryTemplate.name, isActive: salaryTemplate.isActive },
        }),
      );
    },
    onSettled: () => refresh(qc, ['salary-templates'], ['SalaryTemplate']),
  });
}

/** payroll.DeleteSalaryTemplate: refused (409) while a staff salary uses it. */
export function useDeleteSalaryTemplate() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(api.get(SalaryTemplateApi).deleteSalaryTemplate({ salaryTemplateId: id, ifMatch: etag })),
    onSettled: () => refresh(qc, ['salary-templates'], ['SalaryTemplate']),
  });
}

/** A template component as saved. */
export type TemplateComponentDraft = Revision & {
  salaryTemplateId: string;
  payComponentId: string;
  payCategoryId: string;
  /** Amount or rate for the calculation basis; empty when the template leaves it to each salary. */
  value: string;
};

/** payroll.CreateSalaryTemplateComponent / UpdateSalaryTemplateComponent. */
export function useSaveTemplateComponent() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: TemplateComponentDraft) => {
      const { salaryTemplateId, payComponentId, payCategoryId } = draft;
      const templates = api.get(SalaryTemplateApi);
      const trimmed = draft.value.trim();
      if (draft.id === undefined)
        return call(
          templates.createSalaryTemplateComponent({
            salaryTemplateId,
            salaryTemplateComponent: {
              payComponentId,
              payCategoryId,
              ...(trimmed ? { value: trimmed } : {}),
            },
          }),
        );
      return call(
        templates.updateSalaryTemplateComponent({
          salaryTemplateId,
          salaryTemplateComponentId: draft.id,
          ifMatch: draft.etag,
          // an emptied value is sent as null, which clears it
          salaryTemplateComponentUpdate: { payComponentId, payCategoryId, value: trimmed || null },
        }),
      );
    },
    onSettled: () => refresh(qc, ['salary-template-components'], ['SalaryTemplateComponent']),
  });
}

/** payroll.DeleteSalaryTemplateComponent: refused (409) while a staff salary has an amount for it. */
export function useDeleteTemplateComponent() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ salaryTemplateId, id, etag }: { salaryTemplateId: string; id: string; etag: string }) =>
      call(
        api.get(SalaryTemplateApi).deleteSalaryTemplateComponent({
          salaryTemplateId,
          salaryTemplateComponentId: id,
          ifMatch: etag,
        }),
      ),
    onSettled: () => refresh(qc, ['salary-template-components'], ['SalaryTemplateComponent']),
  });
}

/** A new salary: from a template and a date, with an amount for each of the template's components. */
export interface StaffSalaryDraft {
  staffId: string;
  salaryTemplateId: string;
  effectiveDate: string;
  remarks?: string;
  /** Amount per template component id. */
  amounts: Readonly<Record<string, string>>;
}

/** What a new salary's gross is: the exact sum of its earning amounts. */
export function grossOf(
  amounts: Readonly<Record<string, string>>,
  templateComponents: readonly SalaryTemplateComponent[],
  payComponents: readonly PayComponent[],
): string | null {
  const typeOf = new Map(payComponents.map((component) => [component.id, component.componentType]));
  return sumAmounts(
    templateComponents
      .filter((component) => typeOf.get(component.payComponentId) === 'earning')
      .map((component) => amounts[component.id ?? ''] ?? '0'),
  );
}

/**
 * payroll.CreateStaffSalary, then payroll.CreateStaffSalaryComponent for each amount. The contract writes a salary
 * and its components one by one, so a failure part way leaves the salary with the components written so far: the
 * screen lists the salary with what is missing, and the rest is added there.
 */
export function useCreateStaffSalary() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ amounts, remarks, ...draft }: StaffSalaryDraft & { grossSalary: string }) => {
      const salaries = api.get(StaffSalaryApi);
      const salary = await call(
        salaries.createStaffSalary({
          staffSalary: { ...draft, ...(remarks?.trim() ? { remarks: remarks.trim() } : {}) },
        }),
      );
      for (const [salaryTemplateComponentId, amount] of Object.entries(amounts))
        await call(
          salaries.createStaffSalaryComponent({
            staffSalaryId: salary.id ?? '',
            staffSalaryComponent: { salaryTemplateComponentId, amount: amount.trim() },
          }),
        );
      return salary;
    },
    onSettled: () =>
      refresh(qc, ['staff-salaries', 'staff-salary-components'], ['StaffSalary', 'StaffSalaryComponent']),
  });
}

/** payroll.UpdateStaffSalaryComponent (a changed amount) or CreateStaffSalaryComponent (a missing one). */
export function useSaveStaffSalaryComponent() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (
      draft: Revision & { staffSalaryId: string; salaryTemplateComponentId: string; amount: string },
    ) => {
      const salaries = api.get(StaffSalaryApi);
      const amount = draft.amount.trim();
      if (draft.id === undefined)
        return call(
          salaries.createStaffSalaryComponent({
            staffSalaryId: draft.staffSalaryId,
            staffSalaryComponent: { salaryTemplateComponentId: draft.salaryTemplateComponentId, amount },
          }),
        );
      return call(
        salaries.updateStaffSalaryComponent({
          staffSalaryId: draft.staffSalaryId,
          staffSalaryComponentId: draft.id,
          ifMatch: draft.etag,
          staffSalaryComponentUpdate: { amount },
        }),
      );
    },
    onSettled: () => refresh(qc, ['staff-salary-components'], ['StaffSalaryComponent']),
  });
}

/** payroll.DeleteStaffSalary: refused (409) once a payslip was generated from it. */
export function useDeleteStaffSalary() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(api.get(StaffSalaryApi).deleteStaffSalary({ staffSalaryId: id, ifMatch: etag })),
    onSettled: () => refresh(qc, ['staff-salaries'], ['StaffSalary']),
  });
}

/**
 * payroll.GeneratePayslips for an institution and period (the named staff only, when given): answers the
 * operation, which the screen polls at the payroll module (`useOperation`). The Idempotency-Key is one per attempt.
 */
export function useGeneratePayslips() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: {
      institutionId: string;
      periodStart: string;
      periodEnd: string;
      staffIds?: string[];
    }) =>
      call(
        api.get(PayslipApi).generatePayslips({
          idempotencyKey: newIdempotencyKey(),
          generatePayslipsRequest: request,
        }),
      ),
    onSettled: () => refresh(qc, ['payslips'], ['Payslip']),
  });
}

/** How a payslip is paid: the account, the method and day, and the bank or cheque reference. */
export interface PayslipPayment {
  disbursementAccountId: string;
  paymentMethod: PaymentMethod;
  transactionDate: string;
  referenceNumber?: string;
}

/**
 * payroll.MarkPayslipPaid (records the payment of the net amount) and payroll.CancelPayslip, under the etag the
 * screen last read (412 when the payslip changed meanwhile, 409 once it is no longer unpaid).
 */
export function usePayslipStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (
      input:
        | { step: 'markPaid'; payslipId: string; etag?: string; payment: PayslipPayment }
        | { step: 'cancel'; payslipId: string; etag?: string; reason?: string },
    ) => {
      const payslips = api.get(PayslipApi);
      const etag = input.etag ? { etag: input.etag } : {};
      if (input.step === 'cancel')
        return call(
          payslips.cancelPayslip({
            payslipId: input.payslipId,
            cancelPayslipRequest: {
              ...etag,
              ...(input.reason?.trim() ? { reason: input.reason.trim() } : {}),
            },
          }),
        );
      const { referenceNumber, ...payment } = input.payment;
      return call(
        payslips.markPayslipPaid({
          payslipId: input.payslipId,
          markPayslipPaidRequest: {
            ...etag,
            ...payment,
            ...(referenceNumber?.trim() ? { referenceNumber: referenceNumber.trim() } : {}),
          },
        }),
      );
    },
    onSettled: () => refresh(qc, ['payslips', 'payroll-payments'], ['Payslip', 'PayrollPayment']),
  });
}

/** A disbursement account as saved: the account number is write-only and sent only when typed. */
export type DisbursementAccountDraft = Revision & {
  institutionId: string;
  managedByUserId: string;
  name: string;
  accountKey: string;
  /** The full number; empty on an edit keeps the stored one. */
  accountNumber: string;
  bankRoutingCode: string;
  branchName: string;
};

/** payroll.CreateDisbursementAccount / UpdateDisbursementAccount. */
export function useSaveDisbursementAccount() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: DisbursementAccountDraft) => {
      const accounts = api.get(DisbursementAccountApi);
      const number = draft.accountNumber.trim();
      const branch = draft.branchName.trim();
      const body = {
        institutionId: draft.institutionId,
        managedByUserId: draft.managedByUserId,
        name: draft.name.trim(),
        accountKey: draft.accountKey.trim(),
        bankRoutingCode: draft.bankRoutingCode.trim(),
        ...(branch ? { branchName: branch } : {}),
      };
      if (draft.id === undefined)
        return call(
          accounts.createDisbursementAccount({ disbursementAccount: { ...body, accountNumber: number } }),
        );
      // an emptied branch is named without a value, which patchOf sends as null: it is cleared; an empty number
      // keeps the stored one (it is never returned), so it is left out
      const changes = {
        ...body,
        branchName: branch || undefined,
        ...(number ? { accountNumber: number } : {}),
      };
      return call(
        accounts.updateDisbursementAccount({
          disbursementAccountId: draft.id,
          ifMatch: draft.etag,
          disbursementAccountUpdate: patchOf(changes),
        }),
      );
    },
    onSettled: () => refresh(qc, ['disbursement-accounts'], ['DisbursementAccount']),
  });
}

/** payroll.DeleteDisbursementAccount: refused (409) once a payment was made from it. */
export function useDeleteDisbursementAccount() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(
        api
          .get(DisbursementAccountApi)
          .deleteDisbursementAccount({ disbursementAccountId: id, ifMatch: etag }),
      ),
    onSettled: () => refresh(qc, ['disbursement-accounts'], ['DisbursementAccount']),
  });
}

/** A payment that settles no payslip (an advance, a bonus, a reimbursement): payroll.CreatePayrollPayment. */
export interface PayrollPaymentDraft {
  institutionId: string;
  staffId: string;
  transactionNumber: string;
  transactionDate: string;
  amount: string;
  paymentMethod: PaymentMethod;
  paymentTypeId?: string;
  disbursementAccountId?: string;
  referenceNumber?: string;
  remarks?: string;
}

export function useRecordPayrollPayment() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: PayrollPaymentDraft) => {
      const optional = Object.fromEntries(
        (['paymentTypeId', 'disbursementAccountId', 'referenceNumber', 'remarks'] as const).flatMap((key) => {
          const value = draft[key]?.trim();
          return value ? [[key, value]] : [];
        }),
      );
      return call(
        api.get(PayrollPaymentApi).createPayrollPayment({
          payrollPayment: {
            institutionId: draft.institutionId,
            staffId: draft.staffId,
            transactionNumber: draft.transactionNumber.trim(),
            transactionDate: draft.transactionDate,
            amount: draft.amount.trim(),
            paymentMethod: draft.paymentMethod,
            ...optional,
          },
        }),
      );
    },
    onSettled: () => refresh(qc, ['payroll-payments'], ['PayrollPayment']),
  });
}
