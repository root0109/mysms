import {
  localDay,
  shiftDay,
  useAllRecords,
  useBirthdays,
  useCan,
  useI18n,
  useInstitutions,
  useSendGreetings,
  weekOf,
  type BirthdayKind,
  type GreetingChannel,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, SegmentedControl, SelectField, Skeleton } from '@mysms/ui-web';
import { useState } from 'react';
import { problemText } from '../recordText';

type Span = 'today' | 'week' | 'month';
const SPANS: readonly Span[] = ['today', 'week', 'month'];
const KINDS: readonly BirthdayKind[] = ['birthday', 'work_anniversary'];
const CHANNELS: readonly GreetingChannel[] = ['email', 'sms', 'push'];

/** The days (both included) a span covers from today: today, this Monday-to-Sunday week, or the next 30 days. */
function spanDays(span: Span, today: string): { from: string; to: string } {
  if (span === 'today') return { from: today, to: today };
  if (span === 'week') return weekOf(today);
  return { from: today, to: shiftDay(today, 30) };
}

/**
 * SIS-12 Birthdays: the birthdays and work anniversaries at one of the caller's institutions today, this week or in
 * the next 30 days (people.ListBirthdays), birthdays and anniversaries chosen separately; the people ticked are sent
 * a greeting from one of the school's message templates by e-mail, SMS or push (people.SendGreetings), and the
 * screen says how many were queued and how many skipped.
 */
export function BirthdaysScreen() {
  const { t, format, timeZone } = useI18n();
  const mayRead = useCan('people.person.list_birthdays');
  const maySend = useCan('people.person.send_greetings');
  const mayTemplates = useCan('comms.message_template.read');
  const institutions = useInstitutions(mayRead);
  const [institutionId, setInstitutionId] = useState('');
  const chosenInstitution = institutionId || institutions.data?.items[0]?.id || '';
  const [span, setSpan] = useState<Span>('week');
  const [kinds, setKinds] = useState<readonly BirthdayKind[]>(KINDS);
  const today = localDay(new Date(), timeZone);
  const days = spanDays(span, today);
  const list = useBirthdays(
    mayRead && chosenInstitution && kinds.length > 0
      ? { institutionId: chosenInstitution, fromDate: days.from, toDate: days.to, kinds }
      : null,
  );
  const templates = useAllRecords(
    'comms.MessageTemplate',
    { filter: 'isActive = true' },
    maySend && mayTemplates,
  );
  const send = useSendGreetings();
  const [picked, setPicked] = useState<ReadonlySet<string>>(new Set());
  const [templateId, setTemplateId] = useState('');
  const [channel, setChannel] = useState<GreetingChannel>('email');
  const rows = list.data?.items ?? [];
  const toggle = (personId: string) =>
    setPicked((old) => {
      const next = new Set(old);
      if (next.has(personId)) next.delete(personId);
      else next.add(personId);
      return next;
    });
  if (!mayRead)
    return (
      <div className="page">
        <h1 className="page__title">{t('screen.SIS12.title')}</h1>
        <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
      </div>
    );
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.SIS12.title')}</h1>
        <p className="page__subtitle">{t('people.birthdays.intro')}</p>
      </header>
      {(institutions.data?.items.length ?? 0) > 1 ? (
        <SelectField
          label={t('people.birthdays.institution')}
          value={chosenInstitution}
          options={(institutions.data?.items ?? []).map((row) => ({ value: row.id ?? '', label: row.name }))}
          onChange={(e) => setInstitutionId(e.target.value)}
        />
      ) : null}
      <SegmentedControl
        label={t('people.birthdays.span')}
        options={SPANS.map((value) => ({ value, label: t(`people.birthdays.span.${value}`) }))}
        value={span}
        onChange={setSpan}
      />
      <fieldset className="chip-row">
        <legend>{t('people.birthdays.kinds')}</legend>
        {KINDS.map((kind) => (
          <label key={kind}>
            <input
              type="checkbox"
              checked={kinds.includes(kind)}
              onChange={() =>
                setKinds(kinds.includes(kind) ? kinds.filter((k) => k !== kind) : [...kinds, kind])
              }
            />{' '}
            {enumLabel(t, 'birthdayKind', kind)}
          </label>
        ))}
      </fieldset>
      {list.isPending && kinds.length > 0 ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {list.isError ? <Banner intent="danger">{problemText(t, list.error)}</Banner> : null}
      {list.isSuccess && rows.length === 0 ? <p className="hint">{t('people.birthdays.none')}</p> : null}
      {rows.length > 0 ? (
        <Card title={t('people.birthdays.list', { from: format.day(days.from), to: format.day(days.to) })}>
          <ul className="leave-list">
            {rows.map((row) => (
              <li key={`${row.personId}-${row.kind}`} className="leave-list__item">
                {maySend ? (
                  <input
                    type="checkbox"
                    aria-label={t('people.birthdays.pick', { name: row.displayName })}
                    checked={picked.has(row.personId)}
                    onChange={() => toggle(row.personId)}
                  />
                ) : null}
                <div className="leave-list__what">
                  <span>{row.displayName}</span>
                  <span className="hint">
                    {[
                      format.day(row.date),
                      enumLabel(t, 'birthdayKind', row.kind),
                      enumLabel(t, 'birthdayRole', row.role),
                      row.years ? t('people.birthdays.years', { years: row.years }) : '',
                    ]
                      .filter(Boolean)
                      .join(' · ')}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      {maySend && rows.length > 0 ? (
        <Card title={t('people.birthdays.greet')}>
          {!mayTemplates ? <Banner intent="info">{t('people.birthdays.noTemplates')}</Banner> : null}
          {send.data ? (
            <Banner intent="success">
              {t('people.birthdays.sent', { queued: send.data.queued, skipped: send.data.skipped })}
            </Banner>
          ) : null}
          {send.error ? <Banner intent="danger">{problemText(t, send.error)}</Banner> : null}
          <div className="form">
            <SelectField
              label={t('people.birthdays.template')}
              value={templateId}
              options={[
                { value: '', label: t('people.birthdays.chooseTemplate') },
                ...(templates.data?.items ?? []).map((row) => ({
                  value: row.id,
                  label: String(row.name ?? ''),
                })),
              ]}
              onChange={(e) => setTemplateId(e.target.value)}
            />
            <SelectField
              label={t('people.birthdays.channel')}
              value={channel}
              options={CHANNELS.map((value) => ({ value, label: enumLabel(t, 'greetingChannel', value) }))}
              onChange={(e) => setChannel(e.target.value as GreetingChannel)}
            />
            <div className="form__actions">
              <Button
                disabled={picked.size === 0 || !templateId}
                loading={send.isPending}
                onClick={() => send.mutate({ personIds: [...picked], templateId, channel })}
              >
                {t('people.birthdays.send', { count: picked.size })}
              </Button>
            </div>
          </div>
        </Card>
      ) : null}
    </div>
  );
}
