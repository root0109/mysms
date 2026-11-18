import type { CheckoutProof, SubscriptionCheckout } from '@mysms/core';

/** The gateway's own checkout script: the card, UPI and bank pages are the gateway's, never MySMS's. */
const CHECKOUT_SCRIPT = 'https://checkout.razorpay.com/v1/checkout.js';

interface RazorpayResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

interface RazorpayOptions {
  key: string;
  order_id: string;
  name: string;
  description: string;
  handler: (response: RazorpayResponse) => void;
  modal: { ondismiss: () => void };
}

type RazorpayConstructor = new (options: RazorpayOptions) => { open(): void };

let loading: Promise<RazorpayConstructor> | null = null;

/** Loads the checkout script once per page. */
function loadCheckout(): Promise<RazorpayConstructor> {
  const existing = (window as { Razorpay?: RazorpayConstructor }).Razorpay;
  if (existing) return Promise.resolve(existing);
  loading ??= new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = CHECKOUT_SCRIPT;
    script.async = true;
    script.onload = () => {
      const loaded = (window as { Razorpay?: RazorpayConstructor }).Razorpay;
      if (loaded) resolve(loaded);
      else reject(new Error('the checkout script did not load'));
    };
    script.onerror = () => {
      loading = null;
      reject(new Error('the checkout script did not load'));
    };
    document.head.appendChild(script);
  });
  return loading;
}

/**
 * Opens the gateway's checkout for the order the platform opened. Resolves with the gateway's proof of payment (which
 * the server verifies — the client never decides that a payment happened), or null when the payer closes the window.
 */
export async function payWithGateway(
  checkout: SubscriptionCheckout,
  labels: { name: string; description: string },
): Promise<CheckoutProof | null> {
  if (!checkout.gatewayOrderId || !checkout.gatewayPublicKey) throw new Error('no gateway order to pay');
  const Razorpay = await loadCheckout();
  return new Promise((resolve) => {
    new Razorpay({
      key: checkout.gatewayPublicKey as string,
      order_id: checkout.gatewayOrderId as string,
      name: labels.name,
      description: labels.description,
      handler: (response) =>
        resolve({
          gatewayOrderId: response.razorpay_order_id,
          gatewayPaymentId: response.razorpay_payment_id,
          gatewaySignature: response.razorpay_signature,
        }),
      modal: { ondismiss: () => resolve(null) },
    }).open();
  });
}
