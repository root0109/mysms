import {
  REMINDER_CHANNELS,
  feeFilter,
  useCan,
  useI18n,
  useInstitutions,
  useInvoices,
  useSendFeeReminders,
  useStudentNames,
  type Invoice,
  type ReminderChannel,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, SelectField, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { failureText, useFeeMoney } from './fees';
import { RetryBanner } from './shared';

const CHANNEL: Readonly<Record<ReminderChannel, MessageKey>> = {
  sms: 'fin.reminders.channel.sms',
  email: 'fin.reminders.channel.email',
  push: 'fin.reminders.channel.push',
};

/**
 * FIN-10 Reminders: the audience — the institution's open invoices (posted, not fully paid), all or the ones ticked —
 * and the channel; finance.SendFeeReminders records one reminder per invoice and publishes it for comms to deliver.
 * Delivery itself is comms' work and is not built in this release: the screen says so.
 */
export function RemindersScreen() {
  const { t } = useI18n();
  const maySend = useCan('finance.invoice.send_fee_reminders');
  const mayRead = useCan('finance.invoice.read');
  const mayInstitutions = useCan('org.institution.read');
  const institutions = useInstitutions(maySend && mayInstitutions);
  const [institutionChoice, setInstitution] = useState('');
  const institutionId = institutionChoice || institutions.data?.items[0]?.id || '';
  const open = useInvoices(
    institutionId
      ? `${feeFilter({ institutionId })} AND (status = "posted" OR status = "partially_paid")`
      : undefined,
    maySend && mayRead && Boolean(institutionId),
  );
  const rows = open.data?.items ?? [];
  const names = useStudentNames(rows.flatMap((row) => (row.studentId ? [row.studentId] : [])));
  const money = useFeeMoney(rows[0]?.currencyId);
  const send = useSendFeeReminders();
  const [channel, setChannel] = useState<ReminderChannel>('sms');
  const [ticked, setTicked] = useState<readonly string[]>([]);
  const title = t('screen.FIN10.title');
  if (!maySend)
    return (
      <Card title={title}>
        <p className="hint">{t('fin.common.noAccess')}</p>
      </Card>
    );
  const columns: Column<Invoice>[] = [
    {
      key: 'pick',
      header: t('fin.ledger.pick'),
      render: (row) => (
        <input
          type="checkbox"
          aria-label={t('fin.reminders.pickOne', { number: row.invoiceNumber })}
          checked={ticked.includes(row.id ?? '')}
          onChange={() =>
            setTicked((current) =>
              current.includes(row.id ?? '')
                ? current.filter((id) => id !== row.id)
                : [...current, row.id ?? ''],
            )
          }
        />
      ),
    },
    { key: 'number', header: t('fin.invoice.number'), render: (row) => row.invoiceNumber },
    {
      key: 'billed',
      header: t('fin.invoice.billTo'),
      render: (row) => `${row.billToName}${row.studentId ? ` · ${names.get(row.studentId) ?? ''}` : ''}`,
    },
    { key: 'total', header: t('fin.invoice.total'), align: 'end', render: (row) => money(row.total) },
    { key: 'paid', header: t('fin.invoice.paid'), align: 'end', render: (row) => money(row.amountPaid) },
  ];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('fin.reminders.intro')}</p>
      </header>
      <Banner intent="warning">{t('fin.reminders.deliveryLimit')}</Banner>
      {send.error ? <Banner intent="danger">{failureText(t, send.error)}</Banner> : null}
      {send.data ? (
        <Banner intent="success">
          {t('fin.reminders.done', { queued: send.data.queued, skipped: send.data.skipped })}
        </Banner>
      ) : null}
      <Card title={t('fin.reminders.audience')}>
        {mayInstitutions ? (
          <SelectField
            label={t('fin.common.institution')}
            options={(institutions.data?.items ?? []).map((item) => ({
              value: item.id ?? '',
              label: item.name,
            }))}
            value={institutionId}
            onChange={(event) => {
              setInstitution(event.target.value);
              setTicked([]);
            }}
          />
        ) : (
          <p className="hint">{t('fin.reminders.noInstitutions')}</p>
        )}
        {!mayRead ? (
          <p className="hint">{t('fin.reminders.noPreview')}</p>
        ) : open.isError ? (
          <RetryBanner onRetry={() => void open.refetch()} />
        ) : (
          <DataTable
            caption={t('fin.reminders.audience')}
            columns={columns}
            rows={rows}
            rowKey={(row) => row.id ?? row.invoiceNumber}
            loading={Boolean(institutionId) && open.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('fin.reminders.none')}
            density="compact"
          />
        )}
        <SelectField
          label={t('fin.reminders.channel')}
          options={REMINDER_CHANNELS.map((value) => ({ value, label: t(CHANNEL[value]) }))}
          value={channel}
          onChange={(event) => setChannel(event.target.value as ReminderChannel)}
        />
        <div className="page__actions">
          <Button
            loading={send.isPending}
            disabled={!institutionId}
            onClick={() => send.mutate({ institutionId, invoiceIds: [...ticked], channel })}
          >
            {ticked.length > 0
              ? t('fin.reminders.sendTicked', { count: ticked.length })
              : t('fin.reminders.sendAll')}
          </Button>
        </div>
      </Card>
    </div>
  );
}
