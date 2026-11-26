import type { payroll } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * One school paying two staff members from one salary template: Asha Menon (the signed-in teacher) and Vikram Das.
 * Asha's salary changed on 1 April 2025 and a raise is already entered for 2099; both were paid for December 2025.
 */
export const payrollFixture = {
  institutionId: attendanceFixture.institutionId,
  currencyId: id('e01'),
  templateId: id('e10'),
  basicId: id('e21'),
  hraId: id('e22'),
  pfId: id('e23'),
  epfId: id('e24'),
  fixedId: id('e31'),
  percentId: id('e32'),
  templateBasicId: id('e41'),
  templateHraId: id('e42'),
  templatePfId: id('e43'),
  ashaStaffId: id('e51'),
  vikramStaffId: id('e52'),
  ashaSalaryId: id('e61'),
  ashaRaiseId: id('e62'),
  vikramSalaryId: id('e63'),
  ashaDecemberId: id('e71'),
  vikramDecemberId: id('e72'),
  accountId: id('e81'),
  salaryTypeId: id('e91'),
  advanceTypeId: id('e92'),
  operationId: id('ea1'),
} as const;

/** What a payroll officer's role holds. */
export const PAYROLL_OFFICER = [
  'payroll.pay_component.read',
  'payroll.pay_category.read',
  'payroll.payment_type.read',
  'payroll.salary_template.read',
  'payroll.salary_template.create',
  'payroll.salary_template.update',
  'payroll.salary_template.delete',
  'payroll.salary_template_component.read',
  'payroll.salary_template_component.create',
  'payroll.salary_template_component.update',
  'payroll.salary_template_component.delete',
  'payroll.staff_salary.read',
  'payroll.staff_salary.create',
  'payroll.staff_salary.delete',
  'payroll.staff_salary_component.read',
  'payroll.staff_salary_component.create',
  'payroll.staff_salary_component.update',
  'payroll.payslip.read',
  'payroll.payslip_line.read',
  'payroll.payslip.generate_payslips',
  'payroll.payslip.mark_payslip_paid',
  'payroll.payslip.cancel_payslip',
  'payroll.operation.read',
  'payroll.disbursement_account.read',
  'payroll.disbursement_account.create',
  'payroll.disbursement_account.update',
  'payroll.disbursement_account.delete',
  'payroll.payroll_payment.read',
  'payroll.payroll_payment.create',
  'org.institution.read',
  'hr.staff.read',
  'people.person.read',
] as const;

/** What a staff member needs to read payslips (the contract has no own-payslips permission: see HR-08). */
export const PAYSLIP_READER = ['payroll.payslip.read', 'payroll.payslip_line.read', 'hr.staff.read'] as const;

type Stored<T> = { -readonly [K in keyof T]: T[K] } & { id: string; version: number };

/** A fake collection: rows with a version, served with an etag; writes check If-Match (412) like the server. */
function collection<T extends object>(rows: readonly (T & { id: string })[]) {
  const map = new Map<string, Stored<T>>(rows.map((row) => [row.id, { ...row, version: 1 } as Stored<T>]));
  const out = (row: Stored<T>) => {
    const { version, ...rest } = row;
    return { ...rest, etag: `"${version}"` };
  };
  return { map, out };
}

/** The server's If-Match rule for an update or delete: 428 without one, 412 when it is not the row's version. */
const precondition = (req: Request, row: { version: number }): Response | null => {
  const ifMatch = req.headers.get('If-Match');
  if (ifMatch === null) return problem(428, 'PRECONDITION_REQUIRED');
  return ifMatch === `"${row.version}"` ? null : problem(412, 'ETAG_MISMATCH');
};

let sequence = 0;
const nextId = () => id(`ef${String(++sequence).padStart(4, '0')}`);

/**
 * The payroll module as the screens use it, in memory, with the server's rules for that slice: payslips generated
 * once per staff member and period from the salary in force on the period's last day (whole months only here — the
 * server's day-by-day proration is proven by its own tests), an operation that runs once and then has succeeded,
 * paying and cancelling only unpaid payslips (409) under the body etag (412), a payment recorded for a paid net,
 * write-only account numbers kept masked, and deletes refused while rows are in use (409).
 */
export function fakePayroll(options: { generateFails?: boolean } = {}): Record<string, Handler> {
  const f = payrollFixture;
  const inst = f.institutionId;
  const components = [
    { id: f.basicId, code: 'BASIC', name: 'Basic pay', componentType: 'earning' as const },
    { id: f.hraId, code: 'HRA', name: 'House rent allowance', componentType: 'earning' as const },
    { id: f.pfId, code: 'PF', name: 'Provident fund', componentType: 'deduction' as const },
    { id: f.epfId, code: 'EPF', name: 'Employer PF share', componentType: 'defined' as const },
  ];
  const categories = [
    { id: f.fixedId, code: 'FIXED', name: 'Fixed amount' },
    { id: f.percentId, code: 'PCT_BASIC', name: '% of basic' },
  ];
  const templates = collection<payroll.SalaryTemplate>([
    { id: f.templateId, institutionId: inst, name: 'Teaching staff', isActive: true },
  ]);
  const templateComponents = collection<payroll.SalaryTemplateComponent>([
    {
      id: f.templateBasicId,
      salaryTemplateId: f.templateId,
      payComponentId: f.basicId,
      payCategoryId: f.fixedId,
    },
    {
      id: f.templateHraId,
      salaryTemplateId: f.templateId,
      payComponentId: f.hraId,
      payCategoryId: f.percentId,
      value: '40.0000',
    },
    {
      id: f.templatePfId,
      salaryTemplateId: f.templateId,
      payComponentId: f.pfId,
      payCategoryId: f.percentId,
      value: '12.0000',
    },
  ]);
  const salaries = collection<payroll.StaffSalary>([
    {
      id: f.ashaSalaryId,
      staffId: f.ashaStaffId,
      salaryTemplateId: f.templateId,
      grossSalary: '42000.00',
      effectiveDate: '2025-04-01',
    },
    {
      id: f.ashaRaiseId,
      staffId: f.ashaStaffId,
      salaryTemplateId: f.templateId,
      grossSalary: '50400.00',
      effectiveDate: '2099-04-01',
    },
    {
      id: f.vikramSalaryId,
      staffId: f.vikramStaffId,
      salaryTemplateId: f.templateId,
      grossSalary: '18000.00',
      effectiveDate: '2025-06-01',
    },
  ]);
  const salaryComponent = (staffSalaryId: string, salaryTemplateComponentId: string, amount: string) => ({
    id: nextId(),
    staffSalaryId,
    salaryTemplateComponentId,
    amount,
  });
  const salaryComponents = collection<payroll.StaffSalaryComponent>([
    salaryComponent(f.ashaSalaryId, f.templateBasicId, '30000.00'),
    salaryComponent(f.ashaSalaryId, f.templateHraId, '12000.00'),
    salaryComponent(f.ashaSalaryId, f.templatePfId, '3600.00'),
    salaryComponent(f.ashaRaiseId, f.templateBasicId, '36000.00'),
    salaryComponent(f.ashaRaiseId, f.templateHraId, '14400.00'),
    salaryComponent(f.ashaRaiseId, f.templatePfId, '4320.00'),
    salaryComponent(f.vikramSalaryId, f.templateBasicId, '18000.00'),
    salaryComponent(f.vikramSalaryId, f.templatePfId, '2160.00'),
  ]);
  const payslips = collection<payroll.Payslip>([]);
  const lines = new Map<string, payroll.PayslipLine[]>();
  const payments = collection<payroll.PayrollPayment>([]);
  // the number is write-only: the fake keeps only its masked form, as the answers do
  const accounts = collection<Omit<payroll.DisbursementAccount, 'accountNumber'>>([
    {
      id: f.accountId,
      institutionId: inst,
      managedByUserId: id('e99'),
      name: 'Salary account',
      accountKey: 'SAL',
      accountNumberMasked: '••••4321',
      bankRoutingCode: 'SBIN0001234',
    },
  ]);
  let payslipNumber = 0;
  let paymentNumber = 0;
  const cents = (amount: string) => Math.round(Number(amount) * 100);
  const text = (value: number) => (value / 100).toFixed(2);

  /** A payslip as the server writes it: the in-force salary's components are its lines. */
  const writePayslip = (
    staffId: string,
    periodStart: string,
    periodEnd: string,
    status: 'unpaid' | 'paid',
    payslipId = nextId(),
  ) => {
    const salary = [...salaries.map.values()]
      .filter((row) => row.staffId === staffId && row.effectiveDate <= periodEnd)
      .sort((a, b) => b.effectiveDate.localeCompare(a.effectiveDate))[0];
    if (!salary) return null;
    const own = [...salaryComponents.map.values()].filter((row) => row.staffSalaryId === salary.id);
    const rows = own.map((row) => {
      const template = templateComponents.map.get(row.salaryTemplateComponentId);
      const component = components.find((c) => c.id === template?.payComponentId);
      return {
        id: nextId(),
        payslipId,
        staffSalaryComponentId: row.id,
        amount: row.amount,
        componentCode: component?.code,
        componentName: component?.name,
        componentType: component?.componentType,
        etag: '"1"',
      };
    });
    const sum = (type: string) =>
      rows.filter((row) => row.componentType === type).reduce((total, row) => total + cents(row.amount), 0);
    const gross = sum('earning');
    const deductions = sum('deduction');
    const payslip: Stored<payroll.Payslip> = {
      id: payslipId,
      institutionId: inst,
      payslipNumber: `PS-${String(++payslipNumber).padStart(6, '0')}`,
      staffId,
      staffSalaryId: salary.id,
      periodStart,
      periodEnd,
      payPercentage: '100.00',
      leaveDays: '0.0',
      leaveDeduction: '0.00',
      status,
      grossAmount: text(gross),
      deductionTotal: text(deductions),
      netAmount: text(gross - deductions),
      version: 1,
    };
    payslips.map.set(payslipId, payslip);
    lines.set(payslipId, rows);
    return payslip;
  };
  writePayslip(f.ashaStaffId, '2025-12-01', '2025-12-31', 'paid', f.ashaDecemberId);
  writePayslip(f.vikramStaffId, '2025-12-01', '2025-12-31', 'paid', f.vikramDecemberId);

  let operation: payroll.Operation | null = null;

  const filterOf = (req: Request) => new URL(req.url).searchParams.get('filter') ?? '';
  const matches = (filter: string, field: string, value: string | undefined) =>
    !new RegExp(`${field} = "`).test(filter) || filter.includes(`${field} = "${value}"`);
  const list = <T extends object>(
    req: Request,
    c: ReturnType<typeof collection<T>>,
    keep = (_: Stored<T>) => true,
  ) => onePage(req, [...c.map.values()].filter(keep).map(c.out));

  const update =
    <T extends object>(c: ReturnType<typeof collection<T>>, param: string): Handler =>
    async (req, params) => {
      const row = c.map.get(params[param] ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const refused = precondition(req, row);
      if (refused) return refused;
      const body = (await req.json()) as Record<string, unknown>;
      // a PATCH sets the fields its body names; null clears one (JAVA-CODING-RULES §7.2)
      for (const [field, value] of Object.entries(body)) {
        if (value === null) delete (row as Record<string, unknown>)[field];
        else (row as Record<string, unknown>)[field] = value;
      }
      row.version += 1;
      return json(c.out(row));
    };
  const remove =
    <T extends object>(
      c: ReturnType<typeof collection<T>>,
      param: string,
      inUse: (rowId: string) => boolean,
    ): Handler =>
    (req, params) => {
      const row = c.map.get(params[param] ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const refused = precondition(req, row);
      if (refused) return refused;
      if (inUse(row.id))
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The record is still in use.',
        });
      c.map.delete(row.id);
      return new Response(null, { status: 204 });
    };

  const payslipStep =
    (to: 'paid' | 'cancelled'): Handler =>
    async (req, params) => {
      const row = payslips.map.get(params.payslipId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as payroll.MarkPayslipPaidRequest & { reason?: string };
      if (body.etag !== undefined && body.etag !== `"${row.version}"`) return problem(412, 'ETAG_MISMATCH');
      if (row.status !== 'unpaid')
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The payslip cannot be changed this way in its current state.',
        });
      if (to === 'paid') {
        const account = accounts.map.get(body.disbursementAccountId);
        if (!account || account.institutionId !== row.institutionId)
          return problem(422, 'VALIDATION_FAILED', 'Validation failed', {
            errors: [{ field: 'disbursementAccountId', message: 'disbursement account does not exist' }],
          });
        if (cents(row.netAmount ?? '0') > 0) {
          const paymentId = nextId();
          payments.map.set(paymentId, {
            id: paymentId,
            institutionId: row.institutionId,
            transactionNumber: `PP-${String(++paymentNumber).padStart(6, '0')}`,
            staffId: row.staffId,
            payslipId: row.id,
            disbursementAccountId: body.disbursementAccountId,
            transactionDate: body.transactionDate,
            amount: row.netAmount ?? '0.00',
            paymentMethod: body.paymentMethod,
            ...(body.referenceNumber ? { referenceNumber: body.referenceNumber } : {}),
            version: 1,
          });
        }
      }
      row.status = to;
      row.version += 1;
      return json(payslips.out(row));
    };

  return {
    'GET /v1/org/institutions': (req) =>
      onePage(req, [{ id: inst, code: 'GVS', name: 'Green Valley School', currencyId: f.currencyId }]),
    'GET /v1/master/currencies/{currencyId}': (_req, params) =>
      params.currencyId === f.currencyId
        ? json({ id: f.currencyId, code: 'INR', name: 'Indian rupee', minorUnits: 2 })
        : problem(404, 'NOT_FOUND'),
    'GET /v1/hr/staff': (req) => {
      const filter = filterOf(req);
      const staff = [
        { id: f.ashaStaffId, personId: id('e5a'), institutionId: inst },
        { id: f.vikramStaffId, personId: id('e5b'), institutionId: inst },
      ];
      return onePage(
        req,
        staff.filter((member) => !filter || filter.includes(member.id)),
      );
    },
    'GET /v1/people/persons': (req) => {
      const filter = filterOf(req);
      return onePage(
        req,
        [
          { id: id('e5a'), displayName: 'Asha Menon' },
          { id: id('e5b'), displayName: 'Vikram Das' },
        ].filter((person) => filter.includes(person.id)),
      );
    },
    'GET /v1/payroll/pay-components': (req) => onePage(req, components),
    'GET /v1/payroll/pay-categories': (req) => onePage(req, categories),
    'GET /v1/payroll/payment-types': (req) =>
      onePage(req, [
        { id: f.salaryTypeId, code: 'SALARY', name: 'Salary' },
        { id: f.advanceTypeId, code: 'ADVANCE', name: 'Advance' },
      ]),

    'GET /v1/payroll/salary-templates': (req) => list(req, templates),
    'POST /v1/payroll/salary-templates': async (req) => {
      const body = (await req.json()) as payroll.SalaryTemplate;
      if (!body.name || !body.institutionId) return problem(422, 'VALIDATION_FAILED');
      const row = { ...body, id: nextId(), version: 1 };
      templates.map.set(row.id, row);
      return json(templates.out(row), { status: 201 });
    },
    'PATCH /v1/payroll/salary-templates/{salaryTemplateId}': update(templates, 'salaryTemplateId'),
    'DELETE /v1/payroll/salary-templates/{salaryTemplateId}': remove(templates, 'salaryTemplateId', (rowId) =>
      [...salaries.map.values()].some((salary) => salary.salaryTemplateId === rowId),
    ),
    'GET /v1/payroll/salary-templates/{salaryTemplateId}/components': (req, params) =>
      list(req, templateComponents, (row) => row.salaryTemplateId === params.salaryTemplateId),
    'POST /v1/payroll/salary-templates/{salaryTemplateId}/components': async (req, params) => {
      const body = (await req.json()) as payroll.SalaryTemplateComponent;
      const taken = [...templateComponents.map.values()].some(
        (row) =>
          row.salaryTemplateId === params.salaryTemplateId && row.payComponentId === body.payComponentId,
      );
      if (taken)
        return problem(409, 'DUPLICATE', 'Conflict', { detail: 'The template already has this component.' });
      const row = { ...body, id: nextId(), salaryTemplateId: params.salaryTemplateId, version: 1 };
      templateComponents.map.set(row.id, row);
      return json(templateComponents.out(row), { status: 201 });
    },
    'PATCH /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}': update(
      templateComponents,
      'salaryTemplateComponentId',
    ),
    'DELETE /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}': remove(
      templateComponents,
      'salaryTemplateComponentId',
      (rowId) => [...salaryComponents.map.values()].some((row) => row.salaryTemplateComponentId === rowId),
    ),

    'GET /v1/payroll/staff-salaries': (req) => list(req, salaries),
    'POST /v1/payroll/staff-salaries': async (req) => {
      const body = (await req.json()) as payroll.StaffSalary;
      if (
        [...salaries.map.values()].some(
          (r) => r.staffId === body.staffId && r.effectiveDate === body.effectiveDate,
        )
      )
        return problem(409, 'DUPLICATE', 'Conflict', { detail: 'A salary from this date already exists.' });
      const row = { ...body, id: nextId(), version: 1 };
      salaries.map.set(row.id, row);
      return json(salaries.out(row), { status: 201 });
    },
    'DELETE /v1/payroll/staff-salaries/{staffSalaryId}': remove(salaries, 'staffSalaryId', (rowId) =>
      [...payslips.map.values()].some((row) => row.staffSalaryId === rowId),
    ),
    'GET /v1/payroll/staff-salaries/{staffSalaryId}/components': (req, params) =>
      list(req, salaryComponents, (row) => row.staffSalaryId === params.staffSalaryId),
    'POST /v1/payroll/staff-salaries/{staffSalaryId}/components': async (req, params) => {
      const body = (await req.json()) as payroll.StaffSalaryComponent;
      const row = { ...body, id: nextId(), staffSalaryId: params.staffSalaryId, version: 1 };
      salaryComponents.map.set(row.id, row);
      return json(salaryComponents.out(row), { status: 201 });
    },
    'PATCH /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}': update(
      salaryComponents,
      'staffSalaryComponentId',
    ),

    'POST /v1/payroll/payslips:generate': async (req) => {
      if (!req.headers.get('Idempotency-Key')) return problem(400, 'INVALID_ARGUMENT');
      const body = (await req.json()) as payroll.GeneratePayslipsRequest;
      if (body.periodEnd < body.periodStart)
        return problem(422, 'VALIDATION_FAILED', 'Validation failed', {
          errors: [{ field: 'periodEnd', message: 'must not be before periodStart' }],
        });
      let generated = 0;
      let kept = 0;
      for (const staffId of body.staffIds?.length ? body.staffIds : [f.ashaStaffId, f.vikramStaffId]) {
        const existing = [...payslips.map.values()].some(
          (row) =>
            row.staffId === staffId &&
            row.periodStart === body.periodStart &&
            row.periodEnd === body.periodEnd &&
            row.status !== 'cancelled',
        );
        if (existing) kept++;
        else if (writePayslip(staffId, body.periodStart, body.periodEnd, 'unpaid')) generated++;
      }
      operation = {
        operationId: f.operationId,
        module: 'payroll',
        kind: 'payroll.GeneratePayslips',
        status: options.generateFails ? 'failed' : 'succeeded',
        progressPercent: 100,
        createTime: '2026-02-01T08:00:00Z',
        doneTime: '2026-02-01T08:00:02Z',
        ...(options.generateFails
          ? { errorMessage: 'Payslip generation stopped before it finished.' }
          : { result: { generated, kept, skippedCount: 0, skipped: [] } }),
      };
      // answered while it still runs, so the screen polls it once
      return json(
        { ...operation, status: 'running', progressPercent: 50, doneTime: undefined },
        { status: 202 },
      );
    },
    'GET /v1/{module}/operations/{operationId}': (_req, params) =>
      operation && params.module === 'payroll' && params.operationId === operation.operationId
        ? json(operation)
        : problem(404, 'NOT_FOUND'),
    'GET /v1/payroll/payslips': (req) => {
      const filter = filterOf(req);
      return list(
        req,
        payslips,
        (row) =>
          matches(filter, 'institutionId', row.institutionId) &&
          matches(filter, 'periodStart', row.periodStart) &&
          matches(filter, 'periodEnd', row.periodEnd) &&
          (!filter.includes('staffId = "') || filter.includes(`staffId = "${row.staffId}"`)),
      );
    },
    'GET /v1/payroll/payslips/{payslipId}/lines': (req, params) =>
      onePage(req, lines.get(params.payslipId ?? '') ?? []),
    'POST /v1/payroll/payslips/{payslipId}:markPaid': payslipStep('paid'),
    'POST /v1/payroll/payslips/{payslipId}:cancel': payslipStep('cancelled'),

    'GET /v1/payroll/disbursement-accounts': (req) => list(req, accounts),
    'POST /v1/payroll/disbursement-accounts': async (req) => {
      const { accountNumber, ...body } = (await req.json()) as payroll.DisbursementAccount;
      if (!accountNumber) return problem(422, 'VALIDATION_FAILED');
      const row = {
        ...body,
        id: nextId(),
        accountNumberMasked: `••••${accountNumber.slice(-4)}`,
        version: 1,
      };
      accounts.map.set(row.id, row);
      return json(accounts.out(row), { status: 201 });
    },
    'PATCH /v1/payroll/disbursement-accounts/{disbursementAccountId}': async (req, params) => {
      const row = accounts.map.get(params.disbursementAccountId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const refused = precondition(req, row);
      if (refused) return refused;
      const { accountNumber, ...changes } = (await req.json()) as payroll.DisbursementAccountUpdate;
      Object.assign(
        row,
        changes,
        accountNumber ? { accountNumberMasked: `••••${accountNumber.slice(-4)}` } : {},
      );
      row.version += 1;
      return json(accounts.out(row));
    },
    'DELETE /v1/payroll/disbursement-accounts/{disbursementAccountId}': remove(
      accounts,
      'disbursementAccountId',
      (rowId) => [...payments.map.values()].some((row) => row.disbursementAccountId === rowId),
    ),
    'GET /v1/payroll/payroll-payments': (req) => list(req, payments),
    'POST /v1/payroll/payroll-payments': async (req) => {
      const body = (await req.json()) as payroll.PayrollPayment;
      if (!body.transactionNumber || !body.amount) return problem(422, 'VALIDATION_FAILED');
      const row = { ...body, id: nextId(), version: 1 };
      payments.map.set(row.id, row);
      return json(payments.out(row), { status: 201 });
    },
  };
}
