import {
  governanceFailureKey,
  isApiError,
  newIdempotencyKey,
  useCan,
  useI18n,
  useLegalHolds,
  usePlaceLegalHold,
  useReleaseLegalHold,
  useUpdateLegalHold,
  type LegalHold,
  type LegalHoldReason,
  type LegalHoldScope,
  type LegalHoldView,
} from '@mysms/core';
import { enumLabel, type Translate } from '@mysms/i18n';
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
} from '@mysms/ui-web';
import { useState } from 'react';

const REASONS: readonly LegalHoldReason[] = ['litigation', 'regulator', 'safeguarding', 'audit'];
const SCOPES: readonly LegalHoldScope[] = ['subject', 'category', 'records'];

/** The server's sentence for one field of a refused change (422), already in the user's language. */
const fieldError = (error: unknown, field: string) =>
  isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;

/** What a hold covers, in words: the person, the data category, or the records of a module. */
function target(t: Translate, hold: LegalHold): string {
  switch (hold.scope) {
    case 'subject':
      return t('privacy.holds.target.subject', { person: hold.subjectPersonId ?? '' });
    case 'category':
      return t('privacy.holds.target.category', { category: hold.categoryCode ?? '' });
    default:
      return t('privacy.holds.target.records', {
        count: hold.entityIds?.length ?? 0,
        entityType: hold.entityType ?? '',
        module: hold.module ?? '',
      });
  }
}

/**
 * ADM-21 Legal holds (ui/02; design PRIVACY-AND-CONSENT-CENTRE §10): what the school must keep despite an erasure
 * request or a retention policy — a person's data, a data category, or named records of one module — with the
 * reason and the date it is reviewed by. The active holds come nearest review first, a hold past its review date
 * marked; a hold is placed, its review date moved and released, each under its own permission. What a hold covers is
 * fixed once placed; a released hold changes no more.
 */
export function LegalHoldsScreen() {
  const { t, format } = useI18n();
  const [view, setView] = useState<LegalHoldView>('active');
  const holds = useLegalHolds(view);
  const mayPlace = useCan('privacy.legal_hold.place');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [changed, setChanged] = useState<LegalHold | null>(null);
  // the list's copy when it still shows the hold, else the one last answered (a released hold leaves the active view)
  const listed = holds.data?.items.find((h) => h.id === selectedId);
  const open = selectedId ? (listed ?? (changed?.id === selectedId ? changed : null)) : null;

  const columns: Column<LegalHold>[] = [
    {
      key: 'reason',
      header: t('privacy.holds.column.reason'),
      render: (h) => enumLabel(t, 'legalHoldReason', h.reasonCode),
    },
    { key: 'target', header: t('privacy.holds.column.target'), render: (h) => target(t, h) },
    {
      key: 'review',
      header: t('privacy.holds.column.reviewBy'),
      render: (h) =>
        h.reviewDue ? (
          <StatusChip
            intent="warning"
            label={t('privacy.holds.reviewDue', { date: format.day(h.reviewBy) })}
          />
        ) : (
          format.day(h.reviewBy)
        ),
    },
    {
      key: 'status',
      header: t('privacy.holds.column.status'),
      render: (h) => enumLabel(t, 'legalHoldStatus', h.status),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.holds.title')}</h1>
        <p className="page__subtitle">{t('privacy.holds.intro')}</p>
      </header>
      {mayPlace ? <PlaceHold /> : null}
      <SegmentedControl
        label={t('privacy.holds.view')}
        options={[
          { value: 'active', label: t('privacy.holds.view.active') },
          { value: 'all', label: t('privacy.holds.view.all') },
        ]}
        value={view}
        onChange={(value) => setView(value as LegalHoldView)}
      />
      {holds.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void holds.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : (
        <DataTable
          caption={t('privacy.holds.caption')}
          columns={columns}
          rows={holds.data?.items}
          rowKey={(h) => h.id ?? ''}
          loading={holds.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('privacy.holds.empty')}
          onRowClick={(h) => setSelectedId(h.id ?? null)}
        />
      )}
      {open ? <HoldDetail hold={open} onChanged={setChanged} onClose={() => setSelectedId(null)} /> : null}
    </div>
  );
}

/** The form that places a hold: its reason and review date, and the targets its scope names. */
function PlaceHold() {
  const { t } = useI18n();
  const place = usePlaceLegalHold();
  const [key, setKey] = useState(newIdempotencyKey);
  const [reasonCode, setReasonCode] = useState<LegalHoldReason>('litigation');
  const [scope, setScope] = useState<LegalHoldScope>('subject');
  const [subjectPersonId, setSubjectPersonId] = useState('');
  const [categoryCode, setCategoryCode] = useState('');
  const [module, setModule] = useState('');
  const [entityType, setEntityType] = useState('');
  const [entityIds, setEntityIds] = useState('');
  const [reviewBy, setReviewBy] = useState('');
  const [placed, setPlaced] = useState(false);
  const submit = () => {
    setPlaced(false);
    place.mutate(
      {
        draft: { reasonCode, scope, reviewBy, subjectPersonId, categoryCode, module, entityType, entityIds },
        idempotencyKey: key,
      },
      {
        onSuccess: () => {
          setPlaced(true);
          setKey(newIdempotencyKey());
          setSubjectPersonId('');
          setCategoryCode('');
          setModule('');
          setEntityType('');
          setEntityIds('');
          setReviewBy('');
        },
      },
    );
  };
  return (
    <Card title={t('privacy.holds.place')}>
      <div className="consent-card">
        <SelectField
          label={t('privacy.holds.reason')}
          value={reasonCode}
          options={REASONS.map((value) => ({ value, label: enumLabel(t, 'legalHoldReason', value) }))}
          onChange={(event) => setReasonCode(event.currentTarget.value as LegalHoldReason)}
        />
        <SelectField
          label={t('privacy.holds.scope')}
          value={scope}
          options={SCOPES.map((value) => ({ value, label: enumLabel(t, 'legalHoldScope', value) }))}
          onChange={(event) => setScope(event.currentTarget.value as LegalHoldScope)}
        />
        {scope === 'subject' ? (
          <TextInput
            label={t('privacy.holds.subject')}
            value={subjectPersonId}
            error={fieldError(place.error, 'subjectPersonId')}
            onChange={(event) => setSubjectPersonId(event.currentTarget.value)}
          />
        ) : null}
        {scope === 'category' ? (
          <TextInput
            label={t('privacy.holds.category')}
            value={categoryCode}
            error={fieldError(place.error, 'categoryCode')}
            onChange={(event) => setCategoryCode(event.currentTarget.value)}
          />
        ) : null}
        {scope === 'records' ? (
          <>
            <TextInput
              label={t('privacy.holds.module')}
              value={module}
              error={fieldError(place.error, 'module')}
              onChange={(event) => setModule(event.currentTarget.value)}
            />
            <TextInput
              label={t('privacy.holds.entityType')}
              value={entityType}
              error={fieldError(place.error, 'entityType')}
              onChange={(event) => setEntityType(event.currentTarget.value)}
            />
            <TextArea
              label={t('privacy.holds.entityIds')}
              hint={t('privacy.holds.entityIdsHint')}
              value={entityIds}
              maxLength={20000}
              error={fieldError(place.error, 'entityIds')}
              onChange={(event) => setEntityIds(event.currentTarget.value)}
            />
          </>
        ) : null}
        <TextInput
          type="date"
          label={t('privacy.holds.reviewBy')}
          value={reviewBy}
          error={fieldError(place.error, 'reviewBy')}
          onChange={(event) => setReviewBy(event.currentTarget.value)}
        />
        {placed ? <Banner intent="success">{t('privacy.holds.placed')}</Banner> : null}
        {place.error && !isApiError(place.error) ? (
          <Banner intent="danger">{t('common.error.generic')}</Banner>
        ) : null}
        <div className="consent-card__actions">
          <Button variant="primary" loading={place.isPending} disabled={!reviewBy} onClick={submit}>
            {t('privacy.holds.place')}
          </Button>
        </div>
      </div>
    </Card>
  );
}

/** One hold: what it covers, who placed it, its review date (moved while active) and its release. */
function HoldDetail({
  hold,
  onChanged,
  onClose,
}: {
  hold: LegalHold;
  onChanged: (changed: LegalHold) => void;
  onClose: () => void;
}) {
  const { t, format } = useI18n();
  const mayUpdate = useCan('privacy.legal_hold.update');
  const mayRelease = useCan('privacy.legal_hold.release');
  const update = useUpdateLegalHold();
  const release = useReleaseLegalHold();
  const [reviewBy, setReviewBy] = useState(hold.reviewBy);
  const [confirming, setConfirming] = useState(false);
  const [done, setDone] = useState(false);
  const active = !hold.releasedAt;
  const { id: legalHoldId, etag } = hold;
  const error = update.error ?? release.error;
  const changed = (next: LegalHold) => {
    setDone(true);
    setConfirming(false);
    onChanged(next);
  };
  return (
    <Dialog
      open
      title={t('privacy.holds.detail', { reason: enumLabel(t, 'legalHoldReason', hold.reasonCode) })}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          {t('common.action.close')}
        </Button>
      }
    >
      <p>{target(t, hold)}</p>
      {hold.entityIds?.length ? <p className="hint">{hold.entityIds.join(', ')}</p> : null}
      <p className="hint">
        {hold.placedAt ? t('privacy.holds.placedAt', { date: format.dateTime(hold.placedAt) }) : null}
        {hold.releasedAt
          ? ` · ${t('privacy.holds.releasedAt', { date: format.dateTime(hold.releasedAt) })}`
          : ''}
      </p>
      {done ? <Banner intent="success">{t('privacy.holds.done')}</Banner> : null}
      {error && !fieldError(error, 'reviewBy') ? (
        <Banner intent="danger">{t(governanceFailureKey(error))}</Banner>
      ) : null}
      {active && mayUpdate ? (
        <div className="consent-card">
          <TextInput
            type="date"
            label={t('privacy.holds.reviewBy')}
            value={reviewBy}
            error={fieldError(update.error, 'reviewBy')}
            onChange={(event) => setReviewBy(event.currentTarget.value)}
          />
          <div className="consent-card__actions">
            <Button
              variant="primary"
              loading={update.isPending}
              disabled={!reviewBy || reviewBy === hold.reviewBy || !legalHoldId || !etag}
              onClick={() =>
                legalHoldId && etag && update.mutate({ legalHoldId, etag, reviewBy }, { onSuccess: changed })
              }
            >
              {t('privacy.holds.moveReview')}
            </Button>
          </div>
        </div>
      ) : null}
      {active && mayRelease ? (
        <div className="consent-card__actions">
          {confirming ? (
            <>
              <p>{t('privacy.holds.releaseConfirm')}</p>
              <Button
                variant="danger"
                loading={release.isPending}
                onClick={() => release.mutate(hold, { onSuccess: changed })}
              >
                {t('privacy.holds.release')}
              </Button>
              <Button variant="ghost" onClick={() => setConfirming(false)}>
                {t('common.action.cancel')}
              </Button>
            </>
          ) : (
            <Button variant="secondary" onClick={() => setConfirming(true)}>
              {t('privacy.holds.release')}
            </Button>
          )}
        </div>
      ) : null}
    </Dialog>
  );
}
