import { useAllRecords, useCan, useI18n, useSendTestMessage, useSession, type RecordRow } from '@mysms/core';
import { Banner, Button, Card, SegmentedControl, SelectField, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';

type Part = 'rules' | 'channels' | 'templates';
const PARTS: readonly Part[] = ['rules', 'channels', 'templates'];

/** The rules of one notification type: which roles get it, with their e-mail and SMS wording. */
function Rules() {
  const { t } = useI18n();
  const mayTypes = useCan('comms.notification_type.read');
  const types = useAllRecords('comms.NotificationType', {}, mayTypes);
  const [typeId, setTypeId] = useState('');
  const options = (types.data?.items ?? []).map((row) => ({
    value: row.id,
    label: `${String(row.code ?? '')} — ${String(row.description ?? '')}`,
  }));
  if (!mayTypes) return <Banner intent="info">{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Card title={t('comms.setup.type')}>
        {types.isError ? <Banner intent="danger">{problemText(t, types.error)}</Banner> : null}
        <SelectField
          label={t('comms.setup.type')}
          value={typeId}
          options={[{ value: '', label: t('comms.setup.chooseType') }, ...options]}
          onChange={(e) => setTypeId(e.target.value)}
        />
      </Card>
      {typeId ? (
        <RelatedRecords
          resource="comms.NotificationRule"
          title={t('comms.setup.rules')}
          intro={t('comms.setup.rulesIntro')}
          parentId={typeId}
          columns={['roleId', 'isEnabled', 'emailSubjectTemplate']}
        />
      ) : null}
    </>
  );
}

/** Sends the fixed test text through a channel to the caller's own address (comms.SendTestMessage). */
function SendTest({ channel }: { channel: RecordRow }) {
  const { t } = useI18n();
  const own = useSession((s) => s.me?.user?.email ?? '');
  const send = useSendTestMessage();
  const [to, setTo] = useState(own);
  return (
    <div className="chip-row">
      <TextInput label={t('comms.setup.testTo')} value={to} onChange={(e) => setTo(e.target.value)} />
      <Button
        variant="secondary"
        disabled={!to.trim()}
        loading={send.isPending}
        onClick={() => send.mutate({ notificationChannelId: channel.id, to: to.trim() })}
      >
        {t('comms.setup.sendTest')}
      </Button>
      {send.isSuccess ? <Banner intent="success">{t('comms.setup.testSent')}</Banner> : null}
      {send.error ? <Banner intent="danger">{problemText(t, send.error)}</Banner> : null}
    </div>
  );
}

/** The tenant's channels; one opens in place with its settings (secrets are written, never shown back). */
function Channels() {
  const { t } = useI18n();
  const mayTest = useCan('comms.notification_channel.send_test_message');
  const [search, setSearch] = useSearchParams();
  const channelId = search.get('channel') ?? '';
  return (
    <>
      <RelatedRecords
        resource="comms.NotificationChannel"
        title={t('comms.setup.channels')}
        columns={['code', 'description', 'isEnabled']}
        rowTool={(row) => (
          <Button
            variant="secondary"
            onClick={() =>
              setSearch((previous) => {
                const next = new URLSearchParams(previous);
                next.set('channel', row.id);
                return next;
              })
            }
          >
            {t('comms.setup.settings')}
          </Button>
        )}
      />
      {channelId ? (
        <>
          <RelatedRecords
            resource="comms.NotificationChannelProperty"
            title={t('comms.setup.channelSettings')}
            intro={t('comms.setup.secretsIntro')}
            parentId={channelId}
            columns={['propertyKey', 'propertyValue', 'secretValueMasked', 'isMandatory']}
          />
          {mayTest ? (
            <Card title={t('comms.setup.sendTest')}>
              <p className="hint">{t('comms.setup.testHint')}</p>
              <SendTest channel={{ id: channelId }} />
            </Card>
          ) : null}
        </>
      ) : null}
    </>
  );
}

/**
 * COM-13 Notification setup (tenant): the tenant administrator's rules (which roles get each notification type and
 * its wording), delivery channels with their settings — secrets written once and shown only masked — and a test
 * message to the administrator's own address, and the message templates. Each part shows only to a role that may
 * read it; tenant-level settings are for tenant administrators only (the server refuses everyone else).
 */
export function NotificationSetupScreen() {
  const { t } = useI18n();
  const [search, setSearch] = useSearchParams();
  const asked = search.get('part') as Part | null;
  const part: Part = asked && PARTS.includes(asked) ? asked : 'rules';
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.COM13.title')}</h1>
        <p className="page__subtitle">{t('comms.setup.intro')}</p>
      </header>
      <SegmentedControl
        label={t('comms.setup.parts')}
        options={PARTS.map((value) => ({ value, label: t(`comms.setup.part.${value}`) }))}
        value={part}
        onChange={(value) => setSearch({ part: value })}
      />
      {part === 'rules' ? <Rules /> : null}
      {part === 'channels' ? <Channels /> : null}
      {part === 'templates' ? (
        <RelatedRecords
          resource="comms.MessageTemplate"
          title={t('comms.setup.templates')}
          columns={['name', 'channel', 'code', 'isActive']}
        />
      ) : null}
    </div>
  );
}
