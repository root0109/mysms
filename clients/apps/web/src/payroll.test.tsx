import {
  PAYROLL_OFFICER,
  PAYSLIP_READER,
  SELF_SERVICE,
  fakePayroll,
  payrollFixture,
  schoolAdmin,
} from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const f = payrollFixture;
const officer = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...PAYROLL_OFFICER] };
// a teacher reads payslips but not the list of schools: amounts show as plain decimals
const teacher = {
  ...schoolAdmin,
  staffIds: [f.ashaStaffId],
  permissions: [...SELF_SERVICE, ...PAYSLIP_READER],
};
const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;
const cells = (row: HTMLElement) =>
  within(row)
    .getAllByRole('cell')
    .map((cell) => cell.textContent);
const bodyOf = (api: Awaited<ReturnType<typeof signedIn>>['api'], path: string) =>
  JSON.parse(api.calls.find((c) => c.method !== 'GET' && c.path === path)?.body ?? '{}') as unknown;

describe('HR-07 Payslip run', () => {
  it('generates a month, follows the run, reviews a payslip and pays one and cancels another', async () => {
    const { api } = await signedIn('/app/payroll/payslips', officer, fakePayroll());
    expect(await screen.findByRole('heading', { name: 'Payslip run' })).toBeInTheDocument();
    fireEvent.change(await screen.findByLabelText('Month'), { target: { value: '2026-01' } });
    expect(screen.getByText(/runs from 2026-01-01 to 2026-01-31/)).toBeInTheDocument();
    expect(await screen.findByText(/No payslips for this school and month yet/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Generate payslips' }));
    expect(
      await screen.findByText('2 payslips generated, 0 already there and kept, 0 staff members skipped.'),
    ).toBeInTheDocument();
    expect(
      JSON.parse(api.calls.find((c) => c.path === '/v1/payroll/payslips:generate')?.body ?? '{}'),
    ).toEqual({
      institutionId: f.institutionId,
      periodStart: '2026-01-01',
      periodEnd: '2026-01-31',
    });
    // the run was followed at the payroll module
    expect(api.calls.some((c) => c.path === `/v1/payroll/operations/${f.operationId}`)).toBe(true);

    const table = await screen.findByRole('table', { name: 'Payslips of the month' });
    const asha = (await within(table).findByText('Asha Menon')).closest('tr') as HTMLElement;
    // gross 30000.00 + 12000.00, deductions 3600.00, net 38400.00, in the school's currency
    expect(cells(asha).slice(2)).toEqual(['Unpaid', '₹42,000.00', '₹3,600.00', '₹38,400.00']);
    await userEvent.click(asha);
    const detail = card('Payslip of Asha Menon');
    const lines = await within(detail).findByRole('table', { name: /Lines of payslip/ });
    await waitFor(() => expect(within(lines).getAllByRole('row')).toHaveLength(4));
    expect(cells(within(lines).getAllByRole('row')[2] as HTMLElement)).toEqual([
      'House rent allowance',
      'Earning',
      '₹12,000.00',
    ]);

    fireEvent.change(within(detail).getByLabelText('Payment date'), { target: { value: '2026-02-01' } });
    await userEvent.type(within(detail).getByLabelText('Bank or cheque reference (optional)'), 'UTR-5501');
    await userEvent.click(within(detail).getByRole('button', { name: 'Mark paid (₹38,400.00)' }));
    expect(await screen.findByText('The payslip is paid and its payment is recorded.')).toBeInTheDocument();
    await waitFor(() => expect(cells(asha)[2]).toBe('Paid'));
    const paid = api.calls.find((c) => c.path.endsWith(':markPaid'));
    expect(JSON.parse(paid?.body ?? '{}')).toEqual({
      etag: '"1"',
      disbursementAccountId: f.accountId,
      paymentMethod: 'bank_transfer',
      transactionDate: '2026-02-01',
      referenceNumber: 'UTR-5501',
    });
    expect(
      await within(card('Payslip of Asha Menon')).findByText(/does not change any more/),
    ).toBeInTheDocument();

    const vikram = within(table).getByText('Vikram Das').closest('tr') as HTMLElement;
    await userEvent.click(vikram);
    const other = card('Payslip of Vikram Das');
    await userEvent.type(within(other).getByLabelText('Reason (optional)'), 'Joined late');
    await userEvent.click(within(other).getByRole('button', { name: 'Cancel payslip' }));
    expect(await screen.findByText(/The payslip is cancelled/)).toBeInTheDocument();
    await waitFor(() => expect(cells(vikram)[2]).toBe('Cancelled'));
    expect(JSON.parse(api.calls.find((c) => c.path.endsWith(':cancel'))?.body ?? '{}')).toEqual({
      etag: '"1"',
      reason: 'Joined late',
    });
  });

  it('says so when the run fails, and shows nothing it may not use', async () => {
    await signedIn('/app/payroll/payslips', officer, fakePayroll({ generateFails: true }));
    fireEvent.change(await screen.findByLabelText('Month'), { target: { value: '2026-01' } });
    await userEvent.click(screen.getByRole('button', { name: 'Generate payslips' }));
    expect(await screen.findByText('Payslip generation stopped before it finished.')).toBeInTheDocument();
  });

  it('tells a role without payslip permissions that it may not open the run', async () => {
    await signedIn(
      '/app/payroll/payslips',
      { ...schoolAdmin, permissions: [...SELF_SERVICE] },
      fakePayroll(),
    );
    expect(await screen.findByText('Your role may not open payslip runs.')).toBeInTheDocument();
  });
});

describe('HR-08 My payslips', () => {
  it('shows only the staff member’s own payslips with their lines, and says the PDF is not served', async () => {
    const { api } = await signedIn('/app/payroll/payslips?mine', teacher, fakePayroll());
    expect(await screen.findByRole('heading', { name: 'My payslips', level: 1 })).toBeInTheDocument();
    const list = await screen.findByRole('table', { name: 'My payslips' });
    await waitFor(() => expect(within(list).getAllByRole('row')).toHaveLength(2));
    expect(cells(within(list).getAllByRole('row')[1] as HTMLElement)).toEqual([
      '1 Dec 2025 – 31 Dec 2025',
      'PS-000001',
      'Paid',
      '38,400.00',
    ]);
    const filter = new URLSearchParams(
      api.calls.find((c) => c.path === '/v1/payroll/payslips')?.search ?? '',
    ).get('filter');
    expect(filter).toBe(`(staffId = "${f.ashaStaffId}")`);
    const lines = await screen.findByRole('table', { name: 'Lines of payslip PS-000001' });
    await waitFor(() => expect(within(lines).getAllByRole('row')).toHaveLength(4));
    expect(screen.getByText(/A PDF of the payslip is not available yet/)).toBeInTheDocument();
    expect(screen.queryByText('Vikram Das')).not.toBeInTheDocument();
  });

  it('explains an account without a staff record and a role that may not read payslips', async () => {
    const { unmount } = await signedIn(
      '/app/payroll/payslips?mine',
      { ...teacher, staffIds: [] },
      fakePayroll(),
    );
    expect(await screen.findByText(/not linked to a staff record/)).toBeInTheDocument();
    unmount();
    await signedIn(
      '/app/payroll/payslips?mine',
      { ...teacher, permissions: [...SELF_SERVICE] },
      fakePayroll(),
    );
    expect(await screen.findByText(/Your role may not read payslips/)).toBeInTheDocument();
  });
});

describe('HR-05 Salary templates', () => {
  it('creates a template and adds, changes and removes its components', async () => {
    const { api } = await signedIn('/app/payroll/salary-templates', officer, fakePayroll());
    const list = await screen.findByRole('table', { name: 'Templates' });
    await userEvent.click(await within(list).findByText('Teaching staff'));
    const components = await screen.findByRole('table', { name: 'Components of Teaching staff' });
    await waitFor(() => expect(within(components).getAllByRole('row')).toHaveLength(4));
    expect(cells(within(components).getAllByRole('row')[2] as HTMLElement).slice(0, 4)).toEqual([
      'House rent allowance',
      'Earning',
      '% of basic',
      '40.0000',
    ]);

    // the one component not in the template yet is offered
    const detail = card('Template: Teaching staff');
    expect(within(detail).getByLabelText('Component')).toHaveDisplayValue(
      'Employer PF share (Employer share)',
    );
    await userEvent.type(within(detail).getByLabelText('Amount or rate (optional)'), '12.5.0');
    expect(within(detail).getByText('Type a number with at most four decimals.')).toBeInTheDocument();
    await userEvent.clear(within(detail).getByLabelText('Amount or rate (optional)'));
    await userEvent.type(within(detail).getByLabelText('Amount or rate (optional)'), '1800');
    await userEvent.click(within(detail).getByRole('button', { name: 'Add component' }));
    expect(await screen.findByText('The component is added.')).toBeInTheDocument();
    expect(bodyOf(api, `/v1/payroll/salary-templates/${f.templateId}/components`)).toEqual({
      payComponentId: f.epfId,
      payCategoryId: f.fixedId,
      value: '1800',
    });
    expect(
      await within(detail).findByText('Every pay component is already in this template.'),
    ).toBeInTheDocument();

    await userEvent.click(within(detail).getByRole('button', { name: 'Edit House rent allowance' }));
    const value = within(detail).getByLabelText('Amount or rate (optional)');
    await userEvent.clear(value);
    await userEvent.type(value, '50');
    await userEvent.click(within(detail).getByRole('button', { name: 'Save component' }));
    expect(await screen.findByText('The component is saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');

    // a component a salary uses stays
    await userEvent.click(within(detail).getByRole('button', { name: 'Remove Basic pay' }));
    expect(await screen.findByText('The record is still in use.')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Name of a new template'), 'Office staff');
    await userEvent.click(screen.getByRole('button', { name: 'Create template' }));
    expect(await screen.findByText('The template is created.')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Template: Office staff' })).toBeInTheDocument();
  });
});

describe('HR-06 Staff salary', () => {
  it('shows what is in force today, changes an amount and enters a new salary with its exact gross', async () => {
    const { api } = await signedIn('/app/payroll/staff-salaries', officer, fakePayroll());
    const list = await screen.findByRole('table', { name: 'Salaries by staff member' });
    const asha = (await within(list).findByText('Asha Menon')).closest('tr') as HTMLElement;
    expect(cells(asha)).toEqual(['Asha Menon', '₹42,000.00 from 1 Apr 2025', '₹50,400.00 from 1 Apr 2099']);
    await userEvent.click(asha);
    const detail = card('Salaries of Asha Menon');
    const history = within(detail).getByRole('table', { name: 'Salary history' });
    expect(within(history).getByText('In force')).toBeInTheDocument();
    expect(within(history).getByText('Upcoming')).toBeInTheDocument();
    const basic = await within(detail).findByLabelText('Amount of Basic pay');
    expect(basic).toHaveValue('30000.00');
    await userEvent.clear(basic);
    await userEvent.type(basic, '31000.5');
    const row = basic.closest('li') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('The amount of Basic pay is saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(JSON.parse(patch?.body ?? '{}')).toEqual({ amount: '31000.5' });

    const form = card('New salary');
    await userEvent.selectOptions(within(form).getByLabelText('Staff member'), 'Vikram Das');
    fireEvent.change(within(form).getByLabelText('In force from'), { target: { value: '2026-04-01' } });
    await userEvent.type(await within(form).findByLabelText('Amount of Basic pay'), '20000.10');
    await userEvent.type(within(form).getByLabelText('Amount of House rent allowance'), '8000.20');
    await userEvent.type(within(form).getByLabelText('Amount of Provident fund'), '2400');
    // 20000.10 + 8000.20 exactly; the deduction is not part of the gross
    expect(
      within(form).getByText('Gross per month: ₹28,000.30 (the sum of the earnings).'),
    ).toBeInTheDocument();
    await userEvent.click(within(form).getByRole('button', { name: 'Save salary' }));
    expect(await screen.findByText('The salary is saved.')).toBeInTheDocument();
    expect(bodyOf(api, '/v1/payroll/staff-salaries')).toEqual({
      staffId: f.vikramStaffId,
      salaryTemplateId: f.templateId,
      effectiveDate: '2026-04-01',
      grossSalary: '28000.30',
    });
    expect(api.calls.filter((c) => c.method === 'POST' && c.path.endsWith('/components'))).toHaveLength(3);
  });
});

describe('HR-09 Payroll payments', () => {
  it('shows accounts masked, adds one without ever showing its number, and records a payment', async () => {
    const { api } = await signedIn('/app/payroll/payroll-payments', officer, fakePayroll());
    const accounts = await screen.findByRole('table', { name: 'Accounts salaries are paid from' });
    expect(await within(accounts).findByText('••••4321')).toBeInTheDocument();

    const add = card('Accounts salaries are paid from');
    await userEvent.type(within(add).getByLabelText('Account label'), 'Petty cash');
    await userEvent.type(within(add).getByLabelText('Short key'), 'PC');
    await userEvent.type(within(add).getByLabelText('Account number'), '000123456789');
    await userEvent.type(within(add).getByLabelText('Bank routing code'), 'HDFC0000001');
    await userEvent.click(within(add).getByRole('button', { name: 'Add account' }));
    expect(await screen.findByText('The account is added.')).toBeInTheDocument();
    expect(await within(accounts).findByText('••••6789')).toBeInTheDocument();
    expect(screen.queryByText(/000123456789/)).not.toBeInTheDocument();
    expect(within(add).getByLabelText('Account number')).toHaveValue('');

    const record = card('Record a payment');
    await userEvent.selectOptions(within(record).getByLabelText('Kind of payment'), 'Advance');
    await userEvent.type(within(record).getByLabelText('Transaction number'), 'ADV-7');
    fireEvent.change(within(record).getByLabelText('Payment date'), { target: { value: '2026-02-10' } });
    await userEvent.type(within(record).getByLabelText('Amount'), '0');
    expect(within(record).getByRole('button', { name: 'Record payment' })).toBeDisabled();
    await userEvent.clear(within(record).getByLabelText('Amount'));
    await userEvent.type(within(record).getByLabelText('Amount'), '5000');
    await userEvent.click(within(record).getByRole('button', { name: 'Record payment' }));
    expect(await screen.findByText('The payment is recorded.')).toBeInTheDocument();
    const payments = screen.getByRole('table', { name: 'Payments' });
    const row = (await within(payments).findByText('ADV-7')).closest('tr') as HTMLElement;
    expect(cells(row)).toEqual([
      '10 Feb 2026',
      'ADV-7',
      'Asha Menon',
      'Advance',
      'Bank transfer',
      '',
      '₹5,000.00',
    ]);
    expect(bodyOf(api, '/v1/payroll/payroll-payments')).toEqual({
      institutionId: f.institutionId,
      staffId: f.ashaStaffId,
      transactionNumber: 'ADV-7',
      transactionDate: '2026-02-10',
      amount: '5000',
      paymentMethod: 'bank_transfer',
      paymentTypeId: f.advanceTypeId,
    });
  });
});
