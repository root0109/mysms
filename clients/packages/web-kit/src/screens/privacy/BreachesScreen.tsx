import {
  boardClock,
  breachChanges,
  breachOpen,
  breachSteps,
  governanceFailureKey,
  isApiError,
  newIdempotencyKey,
  useBreaches,
  useBreachStep,
  useCan,
  useI18n,
  useReportBreach,
  useUpdateBreach,
  type Breach,
  type BreachStep,
  type BreachView,
  type NotificationAudience,
  type NotificationKind,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SegmentedControl,
  SelectField,
  StatusChip,
  TextArea,
  TextInput,
  type Column,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useEffect, useState } from 'react';

const CLOCK_INTENT: Readonly<Record<ReturnType<typeof boardClock>, StatusChipProps['intent']>> = {
  reported: 'success',
  notABreach: 'neutral',
  overdue: 'danger',
  dueSoon: 'warning',
  running: 'info',
};
const AUDIENCES: readonly NotificationAudience[] = ['board', 'principals', 'processor', 'tenant'];
const KINDS: readonly NotificationKind[] = ['initial', 'detailed', 'update'];

/** The server's sentence for one field of a refused change (422), already in the user's language. */
const fieldError = (error: unknown, field: string) =>
  isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;

/** A comma-separated list typed by the officer, as the codes it names. */
const codes = (text: string) =>
  text
    .split(',')
    .map((c) => c.trim())
    .filter(Boolean);

/** A local date and time typed in a datetime-local field, as the instant it names; undefined when empty. */
const instant = (local: string) => (local ? new Date(local).toISOString() : undefined);

/**
 * ADM-20 Breach register (ui/02; design PRIVACY-AND-CONSENT-CENTRE §11): the school's personal-data breaches, the
 * Board's nearest deadline first, each with the Board's 72-hour clock from the moment the school became aware —
 * met, running (hours left), due within a day or overdue. A breach opens with what is known of it, editable while it
 * is open, the notices recorded so far and the steps its status allows: triage, contain, record a notice sent to the
 * Board, the people affected, a processor or the school, close, or find it was not a breach. Notices are sent outside
 * MySMS and recorded here. Every step needs its own permission.
 */
export function BreachesScreen() {
  const { t, format } = useI18n();
  const [view, setView] = useState<BreachView>('open');
  const breaches = useBreaches(view);
  const mayReport = useCan('privacy.breach.report');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [changed, setChanged] = useState<Breach | null>(null);
  // the list's copy when it still shows the breach, else the one last answered (a closed breach leaves the open view)
  const listed = breaches.data?.items.find((b) => b.id === selectedId);
  const open = selectedId ? (listed ?? (changed?.id === selectedId ? changed : null)) : null;

  const columns: Column<Breach>[] = [
    { key: 'number', header: t('privacy.breaches.column.number'), render: (b) => b.number ?? '' },
    {
      key: 'status',
      header: t('privacy.breaches.column.status'),
      render: (b) => enumLabel(t, 'breachStatus', b.status),
    },
    {
      key: 'aware',
      header: t('privacy.breaches.column.aware'),
      render: (b) => format.dateTime(b.becameAwareAt),
    },
    {
      key: 'clock',
      header: t('privacy.breaches.column.clock'),
      render: (b) => <BoardClock breach={b} />,
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.breaches.title')}</h1>
        <p className="page__subtitle">{t('privacy.breaches.intro')}</p>
      </header>
      {mayReport ? <ReportBreach /> : null}
      <SegmentedControl
        label={t('privacy.breaches.view')}
        options={[
          { value: 'open', label: t('privacy.breaches.view.open') },
          { value: 'overdue', label: t('privacy.breaches.view.overdue') },
          { value: 'all', label: t('privacy.breaches.view.all') },
        ]}
        value={view}
        onChange={(value) => setView(value as BreachView)}
      />
      {breaches.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void breaches.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : (
        <DataTable
          caption={t('privacy.breaches.caption')}
          columns={columns}
          rows={breaches.data?.items}
          rowKey={(b) => b.id ?? ''}
          loading={breaches.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('privacy.breaches.empty')}
          onRowClick={(b) => setSelectedId(b.id ?? null)}
        />
      )}
      {open ? (
        <BreachDetail breach={open} onChanged={setChanged} onClose={() => setSelectedId(null)} />
      ) : null}
    </div>
  );
}

/** The Board's clock of a breach as a chip: met, not a breach, overdue, due within a day, or the hours left. */
function BoardClock({ breach }: { breach: Breach }) {
  const { t, format } = useI18n();
  const clock = boardClock(breach);
  const hours = Math.abs(breach.hoursRemaining ?? 0);
  const label =
    clock === 'reported'
      ? t('privacy.breaches.clock.reported')
      : clock === 'notABreach'
        ? t('privacy.breaches.clock.notABreach')
        : clock === 'overdue'
          ? t('privacy.breaches.clock.overdue', { hours: format.number(hours) })
          : t('privacy.breaches.clock.left', {
              hours: format.number(hours),
              date: breach.boardReportDueAt ? format.dateTime(breach.boardReportDueAt) : '',
            });
  return <StatusChip intent={CLOCK_INTENT[clock]} label={label} />;
}

/** The report form: what happened, when it was detected and when the school became aware (the clock's start). */
function ReportBreach() {
  const { t } = useI18n();
  const report = useReportBreach();
  const [key, setKey] = useState(newIdempotencyKey);
  const [description, setDescription] = useState('');
  const [detectedAt, setDetectedAt] = useState('');
  const [becameAwareAt, setBecameAwareAt] = useState('');
  const [estimated, setEstimated] = useState('');
  const [categories, setCategories] = useState('');
  const [modules, setModules] = useState('');
  const [sent, setSent] = useState<string | null>(null);
  const ready = description.trim() && detectedAt && becameAwareAt;
  const submit = () => {
    const detected = instant(detectedAt);
    const aware = instant(becameAwareAt);
    if (!detected || !aware) return;
    const breach: Breach = {
      description: description.trim(),
      detectedAt: detected,
      becameAwareAt: aware,
      ...(estimated ? { estimatedPrincipals: Number(estimated) } : {}),
      ...(codes(categories).length ? { categories: codes(categories) } : {}),
      ...(codes(modules).length ? { modules: codes(modules) } : {}),
    };
    setSent(null);
    report.mutate(
      { breach, idempotencyKey: key },
      {
        onSuccess: (created) => {
          setSent(created.number ?? '');
          setKey(newIdempotencyKey());
          setDescription('');
          setDetectedAt('');
          setBecameAwareAt('');
          setEstimated('');
          setCategories('');
          setModules('');
        },
      },
    );
  };
  return (
    <Card title={t('privacy.breaches.report')}>
      <div className="consent-card">
        <TextArea
          label={t('privacy.breaches.description')}
          value={description}
          maxLength={4000}
          error={fieldError(report.error, 'description')}
          onChange={(event) => setDescription(event.currentTarget.value)}
        />
        <TextInput
          type="datetime-local"
          label={t('privacy.breaches.detectedAt')}
          value={detectedAt}
          error={fieldError(report.error, 'detectedAt')}
          onChange={(event) => setDetectedAt(event.currentTarget.value)}
        />
        <TextInput
          type="datetime-local"
          label={t('privacy.breaches.becameAwareAt')}
          hint={t('privacy.breaches.becameAwareAtHint')}
          value={becameAwareAt}
          error={fieldError(report.error, 'becameAwareAt')}
          onChange={(event) => setBecameAwareAt(event.currentTarget.value)}
        />
        <TextInput
          type="number"
          min={0}
          label={t('privacy.breaches.estimatedPrincipals')}
          value={estimated}
          error={fieldError(report.error, 'estimatedPrincipals')}
          onChange={(event) => setEstimated(event.currentTarget.value)}
        />
        <TextInput
          label={t('privacy.breaches.categories')}
          hint={t('privacy.breaches.codesHint')}
          value={categories}
          error={fieldError(report.error, 'categories')}
          onChange={(event) => setCategories(event.currentTarget.value)}
        />
        <TextInput
          label={t('privacy.breaches.modules')}
          hint={t('privacy.breaches.codesHint')}
          value={modules}
          error={fieldError(report.error, 'modules')}
          onChange={(event) => setModules(event.currentTarget.value)}
        />
        {sent !== null ? (
          <Banner intent="success">{t('privacy.breaches.reported', { number: sent })}</Banner>
        ) : null}
        {report.error && !isApiError(report.error) ? (
          <Banner intent="danger">{t('common.error.generic')}</Banner>
        ) : null}
        <div className="consent-card__actions">
          <Button variant="primary" loading={report.isPending} disabled={!ready} onClick={submit}>
            {t('privacy.breaches.report')}
          </Button>
        </div>
      </div>
    </Card>
  );
}

type StepName = BreachStep['step'];

function BreachDetail({
  breach,
  onChanged,
  onClose,
}: {
  breach: Breach;
  onChanged: (changed: Breach) => void;
  onClose: () => void;
}) {
  const { t, format } = useI18n();
  const run = useBreachStep();
  const allowed: Readonly<Record<StepName, boolean>> = {
    triage: useCan('privacy.breach.triage'),
    contain: useCan('privacy.breach.contain'),
    recordNotification: useCan('privacy.breach.record_notification'),
    close: useCan('privacy.breach.close'),
    markNotABreach: useCan('privacy.breach.mark_not_a_breach'),
  };
  const mayUpdate = useCan('privacy.breach.update');
  const steps = breachSteps(breach).filter((name) => allowed[name]);
  const [step, setStep] = useState<StepName | null>(null);
  const [done, setDone] = useState(false);
  const perform = (action: BreachStep) =>
    run.mutate(
      { breach, action },
      {
        onSuccess: (changed) => {
          setStep(null);
          setDone(true);
          onChanged(changed);
        },
      },
    );
  return (
    <Dialog
      open
      title={t('privacy.breaches.detail', { number: breach.number ?? '' })}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          {t('common.action.close')}
        </Button>
      }
    >
      <p>
        {enumLabel(t, 'breachStatus', breach.status)} · <BoardClock breach={breach} />
      </p>
      <p>{breach.description}</p>
      <p className="hint">
        {t('privacy.breaches.timeline', {
          detected: format.dateTime(breach.detectedAt),
          aware: format.dateTime(breach.becameAwareAt),
        })}
      </p>
      {breach.notABreachReason ? <p>{breach.notABreachReason}</p> : null}
      <h3>{t('privacy.breaches.notifications')}</h3>
      {breach.notifications?.length ? (
        <ul>
          {breach.notifications.map((n) => (
            <li key={n.id ?? `${n.audience}-${n.kind}-${n.sentAt}`}>
              {t('privacy.breaches.notification', {
                audience: enumLabel(t, 'breachAudience', n.audience),
                kind: enumLabel(t, 'breachNoticeKind', n.kind),
                date: format.dateTime(n.sentAt),
              })}
            </li>
          ))}
        </ul>
      ) : (
        <p>{t('privacy.breaches.noNotifications')}</p>
      )}
      {done ? <Banner intent="success">{t('privacy.breaches.done')}</Banner> : null}
      {run.isError ? <Banner intent="danger">{t(governanceFailureKey(run.error))}</Banner> : null}
      <div className="consent-card__actions">
        {steps.map((name) => (
          <Button
            key={name}
            variant={step === name ? 'primary' : 'secondary'}
            loading={run.isPending && step === name}
            onClick={() => {
              setStep(name);
              if (name === 'triage' || name === 'contain' || name === 'close') perform({ step: name });
            }}
          >
            {t(`privacy.breaches.step.${name}` as const)}
          </Button>
        ))}
      </div>
      {step === 'recordNotification' || step === 'markNotABreach' ? (
        <BreachStepForm key={step} step={step} error={run.error} pending={run.isPending} onRun={perform} />
      ) : null}
      {mayUpdate && breachOpen(breach) ? <EditBreach breach={breach} onChanged={onChanged} /> : null}
    </Dialog>
  );
}

function BreachStepForm(props: {
  step: 'recordNotification' | 'markNotABreach';
  error: unknown;
  pending: boolean;
  onRun: (action: BreachStep) => void;
}) {
  const { t } = useI18n();
  const [audience, setAudience] = useState<NotificationAudience>('board');
  const [kind, setKind] = useState<NotificationKind>('initial');
  const [sentAt, setSentAt] = useState('');
  const [reason, setReason] = useState('');
  const submit = () =>
    props.step === 'markNotABreach'
      ? props.onRun({ step: 'markNotABreach', reason })
      : props.onRun({
          step: 'recordNotification',
          audience,
          kind,
          sentAt: sentAt ? new Date(sentAt) : new Date(),
        });
  return (
    <div className="consent-card">
      {props.step === 'recordNotification' ? (
        <>
          <SelectField
            label={t('privacy.breaches.audience')}
            value={audience}
            options={AUDIENCES.map((value) => ({ value, label: enumLabel(t, 'breachAudience', value) }))}
            onChange={(event) => setAudience(event.currentTarget.value as NotificationAudience)}
          />
          <SelectField
            label={t('privacy.breaches.kind')}
            value={kind}
            options={KINDS.map((value) => ({ value, label: enumLabel(t, 'breachNoticeKind', value) }))}
            onChange={(event) => setKind(event.currentTarget.value as NotificationKind)}
          />
          <TextInput
            type="datetime-local"
            label={t('privacy.breaches.sentAt')}
            hint={t('privacy.breaches.sentAtHint')}
            value={sentAt}
            error={fieldError(props.error, 'sentAt')}
            onChange={(event) => setSentAt(event.currentTarget.value)}
          />
        </>
      ) : (
        <TextArea
          label={t('privacy.breaches.notABreachReason')}
          value={reason}
          maxLength={4000}
          error={fieldError(props.error, 'reason')}
          onChange={(event) => setReason(event.currentTarget.value)}
        />
      )}
      <div className="consent-card__actions">
        <Button
          variant="primary"
          loading={props.pending}
          disabled={props.step === 'markNotABreach' && !reason.trim()}
          onClick={submit}
        >
          {t('privacy.breaches.confirm')}
        </Button>
      </div>
    </div>
  );
}

/** What is known of an open breach, saved under its ETag; the Board's clock is fixed once reported. */
function EditBreach({ breach, onChanged }: { breach: Breach; onChanged: (changed: Breach) => void }) {
  const { t } = useI18n();
  const update = useUpdateBreach();
  const [description, setDescription] = useState(breach.description);
  const [estimated, setEstimated] = useState(
    breach.estimatedPrincipals === undefined ? '' : String(breach.estimatedPrincipals),
  );
  const [consequences, setConsequences] = useState(breach.likelyConsequences ?? '');
  const [mitigation, setMitigation] = useState(breach.mitigation ?? '');
  const [saved, setSaved] = useState(false);
  // the form follows the stored breach whenever it changes (after a save, a step, or a reload after a conflict)
  useEffect(() => {
    setDescription(breach.description);
    setEstimated(breach.estimatedPrincipals === undefined ? '' : String(breach.estimatedPrincipals));
    setConsequences(breach.likelyConsequences ?? '');
    setMitigation(breach.mitigation ?? '');
  }, [
    breach.description,
    breach.estimatedPrincipals,
    breach.likelyConsequences,
    breach.mitigation,
    breach.etag,
  ]);
  const changes = breachChanges(breach, {
    description: description.trim(),
    ...(estimated ? { estimatedPrincipals: Number(estimated) } : {}),
    likelyConsequences: consequences.trim(),
    mitigation: mitigation.trim(),
  });
  const conflict = isApiError(update.error) && update.error.status === 412;
  const { id: breachId, etag } = breach;
  const save = () => {
    if (!breachId || !etag) return;
    setSaved(false);
    update.mutate(
      { breachId, etag, changes },
      {
        onSuccess: (changed) => {
          setSaved(true);
          onChanged(changed);
        },
      },
    );
  };
  return (
    <Card title={t('privacy.breaches.edit')}>
      <div className="consent-card">
        <TextArea
          label={t('privacy.breaches.description')}
          value={description}
          maxLength={4000}
          error={fieldError(update.error, 'description')}
          onChange={(event) => setDescription(event.currentTarget.value)}
        />
        <TextInput
          type="number"
          min={0}
          label={t('privacy.breaches.estimatedPrincipals')}
          value={estimated}
          error={fieldError(update.error, 'estimatedPrincipals')}
          onChange={(event) => setEstimated(event.currentTarget.value)}
        />
        <TextArea
          label={t('privacy.breaches.consequences')}
          value={consequences}
          maxLength={4000}
          onChange={(event) => setConsequences(event.currentTarget.value)}
        />
        <TextArea
          label={t('privacy.breaches.mitigation')}
          value={mitigation}
          maxLength={4000}
          onChange={(event) => setMitigation(event.currentTarget.value)}
        />
        {saved ? <Banner intent="success">{t('privacy.breaches.saved')}</Banner> : null}
        {update.error && !fieldError(update.error, 'description') ? (
          <Banner intent="danger">{t(governanceFailureKey(update.error))}</Banner>
        ) : null}
        <div className="consent-card__actions">
          {conflict ? (
            <Button variant="ghost" onClick={() => update.reset()}>
              {t('common.conflict.reload')}
            </Button>
          ) : null}
          <Button
            variant="primary"
            loading={update.isPending}
            disabled={Object.keys(changes).length === 0}
            onClick={save}
          >
            {t('common.action.save')}
          </Button>
        </div>
      </div>
    </Card>
  );
}
