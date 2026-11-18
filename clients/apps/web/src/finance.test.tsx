import {
  FEE_FAMILY,
  FEE_OFFICE,
  SELF_SERVICE,
  fakeFinance,
  financeFixture,
  schoolAdmin,
} from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const f = financeFixture;
const office = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...FEE_OFFICE] };
const guardian = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...FEE_FAMILY] };
const card = async (name: string | RegExp) =>
  (await screen.findByRole('heading', { name })).closest('section') as HTMLElement;
const post = (body: unknown) =>
  new Request('https://school.example.com/', { method: 'POST', body: JSON.stringify(body) });

/** A fake whose dues of Asha Rao are already billed on a posted invoice, INV-000001 (30780.00). */
async function withPostedInvoice() {
  const fake = fakeFinance();
  const created = (await (
    await fake['POST /v1/finance/invoices:fromDues']!(
      post({ studentId: f.studentId, dueIds: [f.firstDueId, f.secondDueId] }),
      {},
    )
  ).json()) as { id: string; etag: string };
  await fake['POST /v1/finance/invoices/{invoiceId}:post']!(post({ etag: created.etag }), {
    invoiceId: created.id,
  });
  return fake;
}

describe('FIN-03 Dues ledger and FIN-05 Invoice', () => {
  it('raises an invoice from the draft dues of one student and posts it', async () => {
    const { api, router } = await signedIn('/app/finance/student-fee-dues', office, fakeFinance());
    expect(await screen.findByRole('heading', { name: 'Dues ledger' })).toBeInTheDocument();
    const table = await screen.findByRole('table', { name: 'Dues' });
    await waitFor(() => expect(within(table).getAllByText('₹10,260.00')).toHaveLength(1));
    // 10800.00 less 5 % is 10260.00; 21600.00 less 5 % is 20520.00
    expect(within(table).getByText('₹20,520.00')).toBeInTheDocument();

    for (const box of screen.getAllByRole('checkbox', { name: 'Select Tuition' })) await userEvent.click(box);
    await userEvent.type(screen.getByLabelText('Name on the invoice'), 'Mr Rao');
    await userEvent.click(screen.getByRole('button', { name: 'Raise invoice for 2 dues' }));
    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/app\/finance\/invoices\/.+/));
    expect(
      JSON.parse(api.calls.find((c) => c.path === '/v1/finance/invoices:fromDues')?.body ?? '{}'),
    ).toEqual({
      studentId: f.studentId,
      dueIds: [f.firstDueId, f.secondDueId],
      billToName: 'Mr Rao',
    });

    const facts = await card('Invoice');
    expect(await within(facts).findByText('₹30,780.00')).toBeInTheDocument();
    expect(within(facts).getByText('₹1,620.00')).toBeInTheDocument();
    const lines = await screen.findByRole('table', { name: 'Lines' });
    expect(await within(lines).findByText('Tuition · installment 2')).toBeInTheDocument();

    await userEvent.click(within(facts).getByRole('button', { name: 'Post' }));
    expect(await screen.findByRole('heading', { name: 'Invoice INV-000001' })).toBeInTheDocument();
    expect(within(await card('Invoice')).queryByRole('button', { name: 'Post' })).not.toBeInTheDocument();
    expect(JSON.parse(api.calls.find((c) => c.path.endsWith(':post'))?.body ?? '{}')).toEqual({
      etag: '"1"',
    });
  });
});

describe('FIN-06 Payments & receipts', () => {
  it('records a counter payment against the invoice and refunds part of it', async () => {
    const { api } = await signedIn('/app/finance/payments', office, await withPostedInvoice());
    expect(await screen.findByRole('heading', { name: 'Payments & receipts' })).toBeInTheDocument();
    const counter = await card('Counter payment');
    await userEvent.type(within(counter).getByLabelText('Student id'), f.studentId);
    const slice = await within(counter).findByLabelText('Invoice INV-000001 · outstanding ₹30,780.00');
    await userEvent.type(slice, '10000.00');
    await userEvent.type(within(counter).getByLabelText('Payer'), 'Mr Rao');
    expect(within(counter).getByText('Payment: ₹10,000.00')).toBeInTheDocument();
    await userEvent.click(within(counter).getByRole('button', { name: 'Record payment' }));
    expect(await screen.findByText('Payment recorded: receipt RCT-000001.')).toBeInTheDocument();
    const recorded = JSON.parse(
      api.calls.find((c) => c.path === '/v1/finance/payments:record')?.body ?? '{}',
    ) as {
      amount: string;
      allocations: unknown[];
    };
    expect(recorded.amount).toBe('10000.00');
    expect(recorded.allocations).toEqual([{ invoiceId: expect.any(String), amount: '10000.00' }]);
    expect(
      api.calls.find((c) => c.path === '/v1/finance/payments:record')?.headers.get('Idempotency-Key'),
    ).toBeTruthy();

    await userEvent.click(await screen.findByRole('button', { name: 'Refund RCT-000001' }));
    const refund = await card('Refund RCT-000001');
    await userEvent.type(within(refund).getByLabelText(/Amount to refund/), '12000.00');
    await userEvent.type(within(refund).getByLabelText('Reason'), 'Overpaid');
    await userEvent.click(within(refund).getByRole('button', { name: 'Refund' }));
    // more than was paid: the server's reason shows
    expect(await screen.findByText(/at most what is left to refund \(10000.00\)/)).toBeInTheDocument();
    await userEvent.clear(within(refund).getByLabelText(/Amount to refund/));
    await userEvent.type(within(refund).getByLabelText(/Amount to refund/), '4000.00');
    await userEvent.click(within(refund).getByRole('button', { name: 'Refund' }));
    expect(await screen.findByText('The refund is done.')).toBeInTheDocument();
    const refunds = await screen.findByRole('table', { name: 'Refunds' });
    expect(await within(refunds).findByText('₹4,000.00')).toBeInTheDocument();
  });
});

describe('FIN-08 Fee statement and FIN-07 Pay fees', () => {
  it("shows a guardian their child's statement and opens a checkout for the open invoice", async () => {
    const { api } = await signedIn('/app/finance/pay', guardian, await withPostedInvoice());
    expect(await screen.findByRole('heading', { name: 'Pay fees' })).toBeInTheDocument();
    const summary = await card('Summary');
    expect(await within(summary).findAllByText('₹30,780.00')).toHaveLength(2);
    expect(within(summary).getByText('₹0.00')).toBeInTheDocument();

    await userEvent.click(within(await card('Open invoices')).getByRole('button', { name: 'Pay' }));
    const checkout = await card('Pay invoice INV-000001');
    expect(await within(checkout).findByText('₹30,780.00')).toBeInTheDocument();
    // the gateway's window, as its script would give it: the payer closes it; closing proves nothing
    const opened: { order_id: string; prefill: { name: string } }[] = [];
    Object.assign(window, {
      Razorpay: class {
        constructor(
          private readonly options: {
            order_id: string;
            prefill: { name: string };
            modal: { ondismiss: () => void };
          },
        ) {
          opened.push(options);
        }
        open() {
          this.options.modal.ondismiss();
        }
      },
    });
    await userEvent.click(within(checkout).getByRole('button', { name: 'Pay ₹30,780.00' }));
    expect(
      await screen.findByText('Waiting for the payment gateway to confirm the payment…'),
    ).toBeInTheDocument();
    expect(opened).toEqual([
      expect.objectContaining({ order_id: 'order_TEST1', prefill: { name: 'Asha Rao' } }),
    ]);
    // nothing is paid until the gateway's signed notification arrives
    expect(within(await card('Summary')).getByText('₹0.00')).toBeInTheDocument();
    Reflect.deleteProperty(window, 'Razorpay');
    const started = api.calls.find((c) => c.path === '/v1/finance/checkout:start');
    expect(started?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(started?.body ?? '{}')).toMatchObject({ purpose: 'invoice', billingName: 'Asha Rao' });
  });

  it('lists the dues, invoices and payments of the statement', async () => {
    await signedIn(`/app/finance/statement/${f.studentId}`, guardian, fakeFinance());
    expect(await screen.findByRole('heading', { name: 'Fee statement' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: f.studentName })).toBeInTheDocument();
    const summary = await card('Summary');
    expect(await within(summary).findAllByText('₹30,780.00')).toHaveLength(2);
    expect(within(await card('Dues')).getAllByText('Draft')).toHaveLength(2);
  });
});

describe('FIN-10 Fee reminders', () => {
  it('says that reminders are recorded but not yet delivered', async () => {
    await signedIn('/app/finance/reminders', office, await withPostedInvoice());
    expect(await screen.findByRole('heading', { name: 'Fee reminders' })).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Remind for every open invoice' }));
    expect(await screen.findByText('1 reminders recorded; 0 invoices were not open.')).toBeInTheDocument();
  });
});
