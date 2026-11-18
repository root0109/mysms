import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { createFormatters } from '@mysms/i18n';
import { fakeApi, fakePayroll, json, payrollFixture, schoolAdmin } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  centsText,
  grossOf,
  isAmount,
  moneyText,
  monthPeriod,
  salaryInForce,
  sumAmounts,
  toCents,
  useCreateStaffSalary,
  useCurrencyCode,
  useDeleteDisbursementAccount,
  useDeleteSalaryTemplate,
  useDeleteStaffSalary,
  useDeleteTemplateComponent,
  useDisbursementAccounts,
  useGeneratePayslips,
  usePayCategories,
  usePayComponents,
  usePaymentTypes,
  usePayrollPayments,
  usePayslipLines,
  usePayslipStep,
  usePayslips,
  useRecordPayrollPayment,
  useSalaryTemplates,
  useSaveDisbursementAccount,
  useSaveSalaryTemplate,
  useSaveStaffSalaryComponent,
  useSaveTemplateComponent,
  useAllStaff,
  useStaffSalaries,
  useStaffSalaryComponents,
  useTemplateComponents,
  type StaffSalary,
} from './payroll';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const f = payrollFixture;

async function setup(options: Parameters<typeof fakePayroll>[0] = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...fakePayroll(options),
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

const body = (api: Awaited<ReturnType<typeof setup>>['api'], method: string, path: string) =>
  JSON.parse(api.calls.find((c) => c.method === method && c.path === path)?.body ?? '{}') as Record<
    string,
    unknown
  >;

describe('exact amounts', () => {
  it('reads amounts as whole cents and writes them back with two decimals', () => {
    expect(toCents('42000.50')).toBe(4_200_050);
    expect(toCents(' 0.5 ')).toBe(50);
    expect(toCents('7')).toBe(700);
    expect(toCents('999999999999.99')).toBe(99_999_999_999_999);
    expect(toCents('1.005')).toBeNull();
    expect(toCents('-1.00')).toBeNull();
    expect(toCents('1e3')).toBeNull();
    expect(toCents('1234567890123')).toBeNull();
    expect(centsText(4_200_050)).toBe('42000.50');
    expect(centsText(5)).toBe('0.05');
    expect(centsText(-15_050)).toBe('-150.50');
    expect(isAmount('12.3')).toBe(true);
    expect(isAmount('abc')).toBe(false);
  });

  it('adds amounts without floating-point error', () => {
    // 0.1 + 0.2 is 0.30000000000000004 in floating point
    expect(sumAmounts(['0.10', '0.20'])).toBe('0.30');
    expect(sumAmounts(['30000.00', '12000.50', '0.01'])).toBe('42000.51');
    expect(sumAmounts([])).toBe('0.00');
    expect(sumAmounts(['1.00', 'x'])).toBeNull();
  });

  it('works out the gross of a new salary from its earning amounts only', () => {
    const templateComponents = [
      { id: 'b', payComponentId: f.basicId, payCategoryId: f.fixedId },
      { id: 'h', payComponentId: f.hraId, payCategoryId: f.percentId },
      { id: 'p', payComponentId: f.pfId, payCategoryId: f.percentId },
    ];
    const payComponents = [
      { id: f.basicId, code: 'BASIC', name: 'Basic', componentType: 'earning' as const },
      { id: f.hraId, code: 'HRA', name: 'HRA', componentType: 'earning' as const },
      { id: f.pfId, code: 'PF', name: 'PF', componentType: 'deduction' as const },
    ];
    // 30000.00 + 12000.50; the 3600.00 deduction is not part of the gross
    expect(grossOf({ b: '30000.00', h: '12000.50', p: '3600.00' }, templateComponents, payComponents)).toBe(
      '42000.50',
    );
    expect(grossOf({ b: '30000.00' }, templateComponents, payComponents)).toBe('30000.00');
    expect(grossOf({ b: 'lots' }, templateComponents, payComponents)).toBeNull();
  });

  it('shows money in the currency when it is known and as a plain decimal when it is not', () => {
    const format = createFormatters('en-IN');
    expect(moneyText(format, 'en-IN', '4200050.5', 'INR')).toBe('₹42,00,050.50');
    expect(moneyText(format, 'en-IN', '4200050.5', null)).toBe('42,00,050.50');
    expect(moneyText(format, 'en-IN', undefined, 'INR')).toBe('');
  });
});

describe('periods and salaries in force', () => {
  it('turns a month into its pay period, leap years included', () => {
    expect(monthPeriod('2026-01')).toEqual({ periodStart: '2026-01-01', periodEnd: '2026-01-31' });
    expect(monthPeriod('2026-02')).toEqual({ periodStart: '2026-02-01', periodEnd: '2026-02-28' });
    expect(monthPeriod('2028-02')).toEqual({ periodStart: '2028-02-01', periodEnd: '2028-02-29' });
    expect(monthPeriod('2026-04')?.periodEnd).toBe('2026-04-30');
    expect(monthPeriod('2026-13')).toBeNull();
    expect(monthPeriod('2026-1')).toBeNull();
  });

  it('finds the latest salary that has taken effect by the day', () => {
    const salary = (id: string, staffId: string, effectiveDate: string): StaffSalary => ({
      id,
      staffId,
      salaryTemplateId: 't',
      grossSalary: '1.00',
      effectiveDate,
    });
    const salaries = [
      salary('old', 'a', '2025-04-01'),
      salary('new', 'a', '2026-01-16'),
      salary('future', 'a', '2099-01-01'),
      salary('other', 'b', '2026-01-20'),
    ];
    expect(salaryInForce(salaries, 'a', '2026-01-15')?.id).toBe('old');
    expect(salaryInForce(salaries, 'a', '2026-01-16')?.id).toBe('new');
    expect(salaryInForce(salaries, 'a', '2025-03-31')).toBeUndefined();
    expect(salaryInForce(salaries, 'b', '2026-01-19')).toBeUndefined();
  });
});

describe('payroll reads', () => {
  it('reads the catalogues, templates, salaries, staff, accounts, payments and the currency code', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        components: usePayComponents(),
        categories: usePayCategories(),
        types: usePaymentTypes(),
        templates: useSalaryTemplates(),
        templateComponents: useTemplateComponents(f.templateId),
        salaries: useStaffSalaries(),
        salaryComponents: useStaffSalaryComponents(f.ashaSalaryId),
        staff: useAllStaff(),
        accounts: useDisbursementAccounts(),
        payments: usePayrollPayments(),
        currency: useCurrencyCode(f.currencyId),
        noCurrency: useCurrencyCode(null),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.currency).toBe('INR'));
    await waitFor(() => expect(result.current.payments.isSuccess).toBe(true));
    expect(result.current.components.data?.items.map((c) => c.code)).toEqual(['BASIC', 'HRA', 'PF', 'EPF']);
    expect(result.current.categories.data?.items).toHaveLength(2);
    expect(result.current.types.data?.items.map((t) => t.code)).toEqual(['SALARY', 'ADVANCE']);
    expect(result.current.templates.data?.items[0]?.name).toBe('Teaching staff');
    expect(result.current.templateComponents.data?.items).toHaveLength(3);
    expect(result.current.salaries.data?.items).toHaveLength(3);
    expect(result.current.salaryComponents.data?.items.map((c) => c.amount)).toEqual([
      '30000.00',
      '12000.00',
      '3600.00',
    ]);
    expect(result.current.staff.data?.items).toHaveLength(2);
    // the account number is never answered, only its masked form
    expect(result.current.accounts.data?.items[0]).not.toHaveProperty('accountNumber');
    expect(result.current.accounts.data?.items[0]?.accountNumberMasked).toBe('••••4321');
    expect(result.current.noCurrency).toBeNull();
  });

  it('reads one staff member’s payslips, a run’s payslips and a payslip’s lines', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        mine: usePayslips({ staffIds: [f.ashaStaffId] }),
        nobody: usePayslips({ staffIds: [] }),
        run: usePayslips({
          institutionId: f.institutionId,
          periodStart: '2025-12-01',
          periodEnd: '2025-12-31',
        }),
        lines: usePayslipLines(f.ashaDecemberId),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.lines.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.run.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.mine.isSuccess).toBe(true));
    expect(result.current.mine.data?.items.map((p) => p.id)).toEqual([f.ashaDecemberId]);
    expect(result.current.nobody.fetchStatus).toBe('idle');
    expect(result.current.run.data?.items).toHaveLength(2);
    // 30000.00 + 12000.00 - 3600.00
    expect(result.current.mine.data?.items[0]?.netAmount).toBe('38400.00');
    expect(result.current.lines.data?.items.map((l) => `${l.componentCode} ${l.amount}`)).toEqual([
      'BASIC 30000.00',
      'HRA 12000.00',
      'PF 3600.00',
    ]);
    const filters = api.calls
      .filter((c) => c.path === '/v1/payroll/payslips')
      .map((c) => new URLSearchParams(c.search).get('filter'));
    expect(filters).toContain(`(staffId = "${f.ashaStaffId}")`);
    expect(filters).toContain(
      `institutionId = "${f.institutionId}" AND periodStart = "2025-12-01" AND periodEnd = "2025-12-31"`,
    );
  });
});

describe('payroll writes', () => {
  it('creates, renames and deletes a template and its components under If-Match', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        save: useSaveSalaryTemplate(),
        remove: useDeleteSalaryTemplate(),
        saveComponent: useSaveTemplateComponent(),
        removeComponent: useDeleteTemplateComponent(),
      }),
      { wrapper },
    );
    const created = await act(() =>
      result.current.save.mutateAsync({
        institutionId: f.institutionId,
        name: ' Office staff ',
        isActive: true,
      }),
    );
    expect(created.name).toBe('Office staff');
    const renamed = await act(() =>
      result.current.save.mutateAsync({
        id: created.id ?? '',
        etag: created.etag ?? '',
        institutionId: f.institutionId,
        name: 'Office',
        isActive: false,
      }),
    );
    expect(renamed).toMatchObject({ name: 'Office', isActive: false, etag: '"2"' });
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(new URLSearchParams(patch?.search).get('updateMask')).toBe('name,isActive');

    const component = await act(() =>
      result.current.saveComponent.mutateAsync({
        salaryTemplateId: created.id ?? '',
        payComponentId: f.basicId,
        payCategoryId: f.fixedId,
        value: '',
      }),
    );
    expect(component).not.toHaveProperty('value');
    const changed = await act(() =>
      result.current.saveComponent.mutateAsync({
        salaryTemplateId: created.id ?? '',
        id: component.id ?? '',
        etag: component.etag ?? '',
        payComponentId: f.basicId,
        payCategoryId: f.percentId,
        value: ' 50 ',
      }),
    );
    expect(changed).toMatchObject({ payCategoryId: f.percentId, value: '50' });
    await act(() =>
      result.current.removeComponent.mutateAsync({
        salaryTemplateId: created.id ?? '',
        id: component.id ?? '',
        etag: changed.etag ?? '',
      }),
    );
    await act(() => result.current.remove.mutateAsync({ id: created.id ?? '', etag: renamed.etag ?? '' }));
    expect(api.calls.filter((c) => c.method === 'DELETE')).toHaveLength(2);

    // a template a salary uses stays
    await act(async () => {
      await expect(result.current.remove.mutateAsync({ id: f.templateId, etag: '"1"' })).rejects.toSatisfy(
        (error: unknown) => isApiError(error) && error.status === 409,
      );
    });
  });

  it('creates a salary with its component amounts, changes and adds an amount, and deletes an unused salary', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        create: useCreateStaffSalary(),
        saveComponent: useSaveStaffSalaryComponent(),
        remove: useDeleteStaffSalary(),
      }),
      { wrapper },
    );
    const salary = await act(() =>
      result.current.create.mutateAsync({
        staffId: f.vikramStaffId,
        salaryTemplateId: f.templateId,
        effectiveDate: '2026-04-01',
        grossSalary: '20000.00',
        remarks: ' Annual raise ',
        amounts: { [f.templateBasicId]: '20000.00', [f.templatePfId]: ' 2400.00 ' },
      }),
    );
    expect(body(api, 'POST', '/v1/payroll/staff-salaries')).toEqual({
      staffId: f.vikramStaffId,
      salaryTemplateId: f.templateId,
      effectiveDate: '2026-04-01',
      grossSalary: '20000.00',
      remarks: 'Annual raise',
    });
    const posted = api.calls
      .filter((c) => c.path === `/v1/payroll/staff-salaries/${salary.id}/components`)
      .map((c) => JSON.parse(c.body) as unknown);
    expect(posted).toEqual([
      { salaryTemplateComponentId: f.templateBasicId, amount: '20000.00' },
      { salaryTemplateComponentId: f.templatePfId, amount: '2400.00' },
    ]);
    const added = await act(() =>
      result.current.saveComponent.mutateAsync({
        staffSalaryId: salary.id ?? '',
        salaryTemplateComponentId: f.templateHraId,
        amount: '8000.00',
      }),
    );
    const changed = await act(() =>
      result.current.saveComponent.mutateAsync({
        staffSalaryId: salary.id ?? '',
        id: added.id ?? '',
        etag: added.etag ?? '',
        salaryTemplateComponentId: f.templateHraId,
        amount: '8000.50',
      }),
    );
    expect(changed.amount).toBe('8000.50');
    await act(() => result.current.remove.mutateAsync({ id: salary.id ?? '', etag: salary.etag ?? '' }));
    // a salary a payslip was generated from stays
    await act(async () => {
      await expect(result.current.remove.mutateAsync({ id: f.ashaSalaryId, etag: '"1"' })).rejects.toSatisfy(
        (error: unknown) => isApiError(error) && error.status === 409,
      );
    });
  });

  it('generates payslips with an Idempotency-Key, pays one and cancels another under its etag', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => ({ generate: useGeneratePayslips(), step: usePayslipStep() }), {
      wrapper,
    });
    const operation = await act(() =>
      result.current.generate.mutateAsync({
        institutionId: f.institutionId,
        periodStart: '2026-01-01',
        periodEnd: '2026-01-31',
      }),
    );
    expect(operation).toMatchObject({ module: 'payroll', status: 'running', operationId: f.operationId });
    const generate = api.calls.find((c) => c.path === '/v1/payroll/payslips:generate');
    expect(generate?.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);

    const { result: run } = renderHook(
      () =>
        usePayslips({ institutionId: f.institutionId, periodStart: '2026-01-01', periodEnd: '2026-01-31' }),
      { wrapper },
    );
    await waitFor(() => expect(run.current.data?.items).toHaveLength(2));
    const [first, second] = run.current.data?.items ?? [];
    const paid = await act(() =>
      result.current.step.mutateAsync({
        step: 'markPaid',
        payslipId: first?.id ?? '',
        etag: first?.etag ?? '',
        payment: {
          disbursementAccountId: f.accountId,
          paymentMethod: 'bank_transfer',
          transactionDate: '2026-02-01',
          referenceNumber: ' UTR-1 ',
        },
      }),
    );
    expect(paid.status).toBe('paid');
    expect(body(api, 'POST', `/v1/payroll/payslips/${first?.id}:markPaid`)).toEqual({
      etag: '"1"',
      disbursementAccountId: f.accountId,
      paymentMethod: 'bank_transfer',
      transactionDate: '2026-02-01',
      referenceNumber: 'UTR-1',
    });
    const cancelled = await act(() =>
      result.current.step.mutateAsync({
        step: 'cancel',
        payslipId: second?.id ?? '',
        reason: ' wrong month ',
      }),
    );
    expect(cancelled.status).toBe('cancelled');
    expect(body(api, 'POST', `/v1/payroll/payslips/${second?.id}:cancel`)).toEqual({ reason: 'wrong month' });
    // a paid payslip no longer changes
    await act(async () => {
      await expect(
        result.current.step.mutateAsync({ step: 'cancel', payslipId: first?.id ?? '' }),
      ).rejects.toSatisfy((error: unknown) => isApiError(error) && error.status === 409);
    });
  });

  it('saves a disbursement account with a write-only number and records a payment', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        save: useSaveDisbursementAccount(),
        remove: useDeleteDisbursementAccount(),
        pay: useRecordPayrollPayment(),
      }),
      { wrapper },
    );
    const draft = {
      institutionId: f.institutionId,
      managedByUserId: schoolAdmin.user?.id ?? '',
      name: ' Petty cash ',
      accountKey: 'PC',
      accountNumber: '000123456789',
      bankRoutingCode: 'HDFC0000001',
      branchName: '',
    };
    const created = await act(() => result.current.save.mutateAsync(draft));
    expect(created.accountNumberMasked).toBe('••••6789');
    expect(created).not.toHaveProperty('accountNumber');
    await act(() =>
      result.current.save.mutateAsync({
        ...draft,
        id: created.id ?? '',
        etag: created.etag ?? '',
        name: 'Petty cash box',
        accountNumber: '',
      }),
    );
    const patch = api.calls.find((c) => c.method === 'PATCH');
    // an empty number keeps the stored one: it is not sent at all
    expect(new URLSearchParams(patch?.search).get('updateMask')).toBe(
      'institutionId,managedByUserId,name,accountKey,bankRoutingCode',
    );
    expect(JSON.parse(patch?.body ?? '{}')).not.toHaveProperty('accountNumber');
    // the account is at its second version after the edit
    await act(() => result.current.remove.mutateAsync({ id: created.id ?? '', etag: '"2"' }));

    const payment = await act(() =>
      result.current.pay.mutateAsync({
        institutionId: f.institutionId,
        staffId: f.vikramStaffId,
        transactionNumber: ' ADV-1 ',
        transactionDate: '2026-02-10',
        amount: '5000.00',
        paymentMethod: 'cash',
        paymentTypeId: f.salaryTypeId,
        remarks: '  ',
      }),
    );
    expect(payment.transactionNumber).toBe('ADV-1');
    expect(body(api, 'POST', '/v1/payroll/payroll-payments')).toEqual({
      institutionId: f.institutionId,
      staffId: f.vikramStaffId,
      transactionNumber: 'ADV-1',
      transactionDate: '2026-02-10',
      amount: '5000.00',
      paymentMethod: 'cash',
      paymentTypeId: f.salaryTypeId,
    });
  });
});
