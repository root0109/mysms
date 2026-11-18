/**
 * Razorpay Checkout in the browser: the gateway's own payment window, opened for an order MySMS created
 * (finance.StartCheckout). The window's answer is not proof of payment — the money is recorded only when the
 * gateway's signed notification reaches finance — so the screen only learns that the window closed and then follows
 * the statement until the payment shows as received.
 */
const SCRIPT = 'https://checkout.razorpay.com/v1/checkout.js';

interface RazorpayOptions {
  key: string;
  order_id: string;
  name: string;
  description: string;
  prefill: { name: string };
  handler: () => void;
  modal: { ondismiss: () => void };
}

type RazorpayConstructor = new (options: RazorpayOptions) => { open: () => void };

let loading: Promise<RazorpayConstructor> | null = null;

/** The Checkout script, loaded once per page; a failed load can be tried again. */
function load(): Promise<RazorpayConstructor> {
  const existing = (window as unknown as { Razorpay?: RazorpayConstructor }).Razorpay;
  if (existing) return Promise.resolve(existing);
  loading ??= new Promise<RazorpayConstructor>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = SCRIPT;
    script.async = true;
    script.onload = () => {
      const loaded = (window as unknown as { Razorpay?: RazorpayConstructor }).Razorpay;
      if (loaded) resolve(loaded);
      else reject(new Error('checkout unavailable'));
    };
    script.onerror = () => {
      loading = null;
      reject(new Error('checkout unavailable'));
    };
    document.head.appendChild(script);
  });
  return loading;
}

/** Opens the gateway's window for the order; resolves when the payer finishes or closes it. */
export async function openCheckout(order: {
  key: string;
  orderId: string;
  title: string;
  description: string;
  payerName: string;
}): Promise<void> {
  const Razorpay = await load();
  await new Promise<void>((resolve) => {
    new Razorpay({
      key: order.key,
      order_id: order.orderId,
      name: order.title,
      description: order.description,
      prefill: { name: order.payerName },
      handler: () => resolve(),
      modal: { ondismiss: () => resolve() },
    }).open();
  });
}
