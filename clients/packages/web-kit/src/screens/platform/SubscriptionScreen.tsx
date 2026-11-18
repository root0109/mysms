import {
  useCan,
  useCancelSubscription,
  useConfirmSubscriptionCheckout,
  useI18n,
  useSaasPlans,
  useStartSubscriptionCheckout,
  useTenantSubscriptions,
  type SaasPlan,
  type TenantSubscription,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  StatusChip,
  TextArea,
  type Column,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useState } from 'react';
import { payWithGateway } from './razorpay';
import { problemText } from './problems';
import { RetryBanner } from './shared';

const STATUS: Readonly<Record<string, { label: MessageKey; intent: StatusChipProps['intent'] }>> = {
  pending: { label: 'plt.subscription.status.pending', intent: 'warning' },
  active: { label: 'plt.subscription.status.active', intent: 'success' },
  cancelled: { label: 'plt.subscription.status.cancelled', intent: 'neutral' },
  expired: { label: 'plt.subscription.status.expired', intent: 'neutral' },
};

/**
 * ADM-14 Subscription & billing: the tenant's subscriptions (platform.ListTenantSubscriptions), the plans on sale
 * (platform.ListSaasPlans) and buying one — platform.StartSubscriptionCheckout opens a gateway order, the gateway's
 * own checkout takes the payment, and platform.ConfirmSubscriptionCheckout hands the gateway's signature to the
 * server, which alone decides that the subscription is paid (the gateway's webhook would activate it too). A pending
 * or active subscription can be cancelled with a reason; an active one stays in force until its end date.
 */
export function SubscriptionScreen() {
  const { t, format } = useI18n();
  const mayRead = useCan('platform.tenant_subscription.read');
  const mayBuy = useCan('platform.tenant_subscription.start_subscription_checkout');
  const mayCancel = useCan('platform.tenant_subscription.cancel_subscription');
  const subscriptions = useTenantSubscriptions(mayRead);
  const plans = useSaasPlans(mayBuy);
  const start = useStartSubscriptionCheckout();
  const confirm = useConfirmSubscriptionCheckout();
  const cancel = useCancelSubscription();
  const [notice, setNotice] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<unknown>(null);
  const [cancelling, setCancelling] = useState<TenantSubscription | null>(null);
  const [reason, setReason] = useState('');

  const buy = (plan: SaasPlan) => {
    setNotice(null);
    setPayError(null);
    start.mutate(
      { planId: plan.id as string },
      {
        onSuccess: (checkout) => {
          setPaying(true);
          payWithGateway(checkout, { name: t('app.brand.name'), description: plan.displayName })
            .then((proof) => {
              if (!proof) {
                setNotice(t('plt.subscription.dismissed'));
                return undefined;
              }
              return confirm.mutateAsync({ subscriptionId: checkout.paymentId, proof }).then(() => {
                setNotice(t('plt.subscription.paid', { plan: plan.displayName }));
              });
            })
            .catch((error: unknown) => setPayError(error))
            .finally(() => setPaying(false));
        },
      },
    );
  };

  const columns: Column<TenantSubscription>[] = [
    {
      key: 'status',
      header: t('plt.subscription.status'),
      render: (s) => {
        const state = STATUS[s.status ?? ''];
        return <StatusChip intent={state?.intent} label={state ? t(state.label) : (s.status ?? '')} />;
      },
    },
    {
      key: 'period',
      header: t('plt.subscription.period'),
      render: (s) =>
        s.startDate && s.endDate
          ? t('plt.subscription.periodRange', { from: format.day(s.startDate), to: format.day(s.endDate) })
          : '',
    },
    { key: 'price', header: t('plt.subscription.price'), align: 'end', render: (s) => s.price ?? '' },
    {
      key: 'actions',
      header: t('plt.subscription.actions'),
      render: (s) =>
        mayCancel && (s.status === 'pending' || s.status === 'active') ? (
          <Button
            variant="ghost"
            onClick={() => {
              setReason('');
              setCancelling(s);
            }}
          >
            {t('plt.subscription.cancel')}
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ADM14.title')}</h1>
        <p className="page__subtitle">{t('plt.subscription.intro')}</p>
      </header>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {!mayRead ? (
        <Banner intent="info">{t('plt.subscription.noAccess')}</Banner>
      ) : subscriptions.isError ? (
        <RetryBanner onRetry={() => void subscriptions.refetch()} />
      ) : (
        <Card title={t('plt.subscription.list')}>
          <DataTable
            caption={t('plt.subscription.list')}
            columns={columns}
            rows={subscriptions.data?.items}
            rowKey={(s) => s.id ?? s.requestedByUserId}
            loading={subscriptions.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('plt.subscription.none')}
          />
        </Card>
      )}
      {mayBuy ? (
        <Card title={t('plt.subscription.plans')}>
          {start.isError ? <Banner intent="danger">{problemText(t, start.error)}</Banner> : null}
          {payError ? <Banner intent="danger">{problemText(t, payError)}</Banner> : null}
          {plans.isError ? (
            <RetryBanner onRetry={() => void plans.refetch()} />
          ) : plans.isPending ? (
            <p className="hint">{t('common.state.loading')}</p>
          ) : plans.data.items.length === 0 ? (
            <p className="hint">{t('plt.subscription.noPlans')}</p>
          ) : (
            <ul className="result-list">
              {plans.data.items.map((plan) => (
                <li key={plan.id}>
                  <strong>{plan.displayName}</strong>{' '}
                  <span className="hint">
                    {t('plt.subscription.planTerms', {
                      price: plan.price ?? '0',
                      months: plan.billingCycleMonths,
                    })}
                  </span>{' '}
                  <Button
                    loading={(start.isPending || paying) && start.variables?.planId === plan.id}
                    onClick={() => buy(plan)}
                  >
                    {t('plt.subscription.buy')}
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </Card>
      ) : null}
      <Dialog
        open={cancelling !== null}
        title={t('plt.subscription.cancel')}
        onClose={() => setCancelling(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setCancelling(null)}>
              {t('common.action.close')}
            </Button>
            <Button
              variant="danger"
              loading={cancel.isPending}
              onClick={() => {
                if (!cancelling?.id) return;
                cancel.mutate(
                  { id: cancelling.id, etag: cancelling.etag, reason },
                  {
                    onSuccess: () => {
                      setCancelling(null);
                      setNotice(t('plt.subscription.cancelled'));
                    },
                  },
                );
              }}
            >
              {t('plt.subscription.cancel')}
            </Button>
          </>
        }
      >
        <p>{t('plt.subscription.cancelConfirm')}</p>
        <TextArea
          label={t('plt.subscription.reason')}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
        {cancel.isError ? <Banner intent="danger">{problemText(t, cancel.error)}</Banner> : null}
      </Dialog>
    </div>
  );
}
