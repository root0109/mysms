import {
  isApiError,
  newIdempotencyKey,
  rightsFailureKey,
  useCan,
  useI18n,
  useMyAccessSummary,
  useMyChildren,
  useMyNominee,
  useMyRequests,
  useRevokeMyNominee,
  useSession,
  useSetMyNominee,
  useSubmitMyRequest,
  type AccessSummary,
  type GrievanceCategory,
  type MyRightsRequest,
  type RightsRequestType,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  Skeleton,
  StatusChip,
  TextArea,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';

/** The rights a requester asks for here; a nominee's request is made with the school's privacy staff. */
const TYPES: readonly RightsRequestType[] = ['access', 'correction', 'grievance'];
const GRIEVANCE_CATEGORIES: readonly GrievanceCategory[] = [
  'consent',
  'access',
  'correction',
  'data_security',
  'communication',
  'other',
];
/** The modules a correction can name, in the words a family uses. */
const MODULES = ['people', 'sis', 'admission', 'assessment', 'finance', 'hr'] as const;

/** The server's sentence for one field of a refused request (422), already in the user's language. */
const fieldError = (error: unknown, field: string) =>
  isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;

/**
 * ACC-15 My privacy requests (ui/02; design PRIVACY-AND-CONSENT-CENTRE §9.6, §12): the user asks what the school
 * holds about them or a child they are the legal guardian of, asks for a correction (naming the record and what
 * should change) or raises a complaint, and follows each request — its deadline, its outcome, the school's answer
 * and, for a fulfilled access request, the summary sent. An adult also names, changes or removes their nominee.
 */
export function MyRequestsScreen() {
  const { t } = useI18n();
  const requests = useMyRequests();
  const [open, setOpen] = useState<MyRightsRequest | null>(null);
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.myRequests.title')}</h1>
        <p className="page__subtitle">{t('privacy.myRequests.intro')}</p>
      </header>
      <NewRequestCard />
      <RequestList
        requests={requests.data?.items}
        loading={requests.isPending}
        failed={requests.isError}
        onRetry={() => void requests.refetch()}
        onOpen={setOpen}
      />
      <NomineeCard />
      {open ? <RequestDialog request={open} onClose={() => setOpen(null)} /> : null}
    </div>
  );
}

function NewRequestCard() {
  const { t, format } = useI18n();
  const submit = useSubmitMyRequest();
  const mayListChildren = useCan('sis.guardian.list_my_children');
  const children = useMyChildren(mayListChildren);
  // "about me" names the signed-in person (the account's linked person)
  const me = useSession((session) => session.me?.user?.personId);
  const [type, setType] = useState<RightsRequestType>('access');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<GrievanceCategory | ''>('');
  const [module, setModule] = useState<string>(MODULES[0]);
  const [recordType, setRecordType] = useState('');
  const [field, setField] = useState('');
  const [value, setValue] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const [key, setKey] = useState(newIdempotencyKey);
  const [sent, setSent] = useState<MyRightsRequest | null>(null);

  const subjects = [
    { value: me ?? '', label: t('privacy.myRequests.subject.self') },
    ...(children.data?.items ?? []).map((card) => ({
      value: card.person.id ?? '',
      label: card.person.displayName ?? '',
    })),
  ];
  const chosenSubject = subject || subjects[0]?.value || '';

  const send = () => {
    setProblem(null);
    if (!description.trim()) return setProblem(t('privacy.myRequests.descriptionRequired'));
    if (type === 'correction' && (!recordType.trim() || !field.trim() || !value.trim()))
      return setProblem(t('privacy.myRequests.correction.required'));
    submit.mutate(
      {
        idempotencyKey: key,
        draft: {
          type,
          subjectPersonId: chosenSubject,
          description,
          ...(type === 'correction'
            ? {
                correction: {
                  module,
                  recordType: recordType.trim(),
                  fields: [{ field: field.trim(), requestedValue: value.trim() }],
                },
              }
            : {}),
          ...(type === 'grievance' && category ? { grievanceCategory: category } : {}),
        },
      },
      {
        onSuccess: (request) => {
          setSent(request);
          setDescription('');
          setKey(newIdempotencyKey());
        },
      },
    );
  };

  return (
    <Card title={t('privacy.myRequests.new')}>
      <div className="consent-card">
        <SelectField
          label={t('privacy.myRequests.type')}
          value={type}
          options={TYPES.map((value) => ({ value, label: enumLabel(t, 'rightsRequestType', value) }))}
          onChange={(event) => setType(event.currentTarget.value as RightsRequestType)}
        />
        {subjects.length > 1 ? (
          <SelectField
            label={t('privacy.myRequests.subject')}
            value={chosenSubject}
            options={subjects}
            onChange={(event) => setSubject(event.currentTarget.value)}
          />
        ) : null}
        {type === 'correction' ? (
          <>
            <SelectField
              label={t('privacy.myRequests.correction.module')}
              value={module}
              options={MODULES.map((value) => ({
                value,
                label: t(`privacy.myRequests.module.${value}` as const),
              }))}
              onChange={(event) => setModule(event.currentTarget.value)}
            />
            <TextInput
              label={t('privacy.myRequests.correction.recordType')}
              value={recordType}
              error={fieldError(submit.error, 'correction.recordType')}
              onChange={(event) => setRecordType(event.currentTarget.value)}
            />
            <TextInput
              label={t('privacy.myRequests.correction.field')}
              value={field}
              onChange={(event) => setField(event.currentTarget.value)}
            />
            <TextInput
              label={t('privacy.myRequests.correction.value')}
              value={value}
              onChange={(event) => setValue(event.currentTarget.value)}
            />
          </>
        ) : null}
        {type === 'grievance' ? (
          <SelectField
            label={t('privacy.myRequests.grievanceCategory')}
            value={category}
            options={[
              { value: '', label: '' },
              ...GRIEVANCE_CATEGORIES.map((value) => ({
                value,
                label: enumLabel(t, 'grievanceCategory', value),
              })),
            ]}
            error={fieldError(submit.error, 'grievanceCategory')}
            onChange={(event) => setCategory(event.currentTarget.value as GrievanceCategory | '')}
          />
        ) : null}
        <TextArea
          label={t('privacy.myRequests.description')}
          value={description}
          maxLength={4000}
          error={fieldError(submit.error, 'description')}
          onChange={(event) => setDescription(event.currentTarget.value)}
        />
        {problem ? <Banner intent="warning">{problem}</Banner> : null}
        {submit.isError ? <Banner intent="danger">{t(rightsFailureKey(submit.error))}</Banner> : null}
        {sent ? (
          <Banner intent="success">
            {t('privacy.myRequests.sent', {
              number: sent.number,
              date: sent.dueAt ? format.date(sent.dueAt) : '',
            })}
          </Banner>
        ) : null}
        <div className="consent-card__actions">
          <Button variant="primary" loading={submit.isPending} onClick={send}>
            {t('privacy.myRequests.send')}
          </Button>
        </div>
      </div>
    </Card>
  );
}

function RequestList(props: {
  requests: readonly MyRightsRequest[] | undefined;
  loading: boolean;
  failed: boolean;
  onRetry: () => void;
  onOpen: (request: MyRightsRequest) => void;
}) {
  const { t, format } = useI18n();
  if (props.failed)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={props.onRetry}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const columns: Column<MyRightsRequest>[] = [
    { key: 'number', header: t('privacy.queue.column.number'), render: (r) => r.number },
    {
      key: 'type',
      header: t('privacy.queue.column.type'),
      render: (r) => enumLabel(t, 'rightsRequestType', r.type),
    },
    {
      key: 'status',
      header: t('privacy.queue.column.status'),
      render: (r) => (
        <StatusChip
          intent={r.overdue ? 'danger' : r.closedAt ? 'neutral' : 'info'}
          label={
            r.outcomeCode
              ? enumLabel(t, 'rightsOutcome', r.outcomeCode)
              : enumLabel(t, 'rightsRequestStatus', r.status)
          }
        />
      ),
    },
    {
      key: 'due',
      header: t('privacy.queue.column.due'),
      render: (r) => (r.closedAt || !r.dueAt ? '' : format.date(r.dueAt)),
    },
  ];
  return (
    <DataTable
      caption={t('privacy.myRequests.list')}
      columns={columns}
      rows={props.requests}
      rowKey={(r) => r.id}
      loading={props.loading}
      loadingLabel={t('common.state.loading')}
      emptyText={t('privacy.myRequests.empty')}
      onRowClick={props.onOpen}
    />
  );
}

function RequestDialog({ request, onClose }: { request: MyRightsRequest; onClose: () => void }) {
  const { t, format } = useI18n();
  const fulfilledAccess = request.type === 'access' && request.outcomeCode === 'fulfilled';
  const summary = useMyAccessSummary(fulfilledAccess ? request.id : null);
  return (
    <Dialog
      open
      title={t('privacy.myRequests.detail', { number: request.number })}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          {t('common.action.close')}
        </Button>
      }
    >
      <p>{enumLabel(t, 'rightsRequestType', request.type)}</p>
      <p>{request.description}</p>
      {request.correction ? (
        <ul>
          {request.correction.fields.map((f) => (
            <li key={f.field}>
              {f.field}: {f.requestedValue}
            </li>
          ))}
        </ul>
      ) : null}
      {request.closedAt ? null : request.overdue ? (
        <Banner intent="danger">{t('privacy.rights.overdue', { date: format.date(request.dueAt) })}</Banner>
      ) : (
        <p>{t('privacy.rights.due', { date: format.date(request.dueAt) })}</p>
      )}
      {request.closedAt ? (
        <section aria-label={t('privacy.rights.response')}>
          <h3>{t('privacy.rights.response')}</h3>
          <p>{request.response ?? t('privacy.rights.noResponse')}</p>
        </section>
      ) : null}
      {fulfilledAccess ? (
        summary.data ? (
          <SummaryView summary={summary.data} />
        ) : (
          <Skeleton label={t('common.state.loading')} lines={3} />
        )
      ) : null}
    </Dialog>
  );
}

/** An access summary: each processing activity that concerns the person, its legal basis and kinds of data. */
export function SummaryView({ summary, title }: { summary: AccessSummary; title?: string }) {
  const { t } = useI18n();
  return (
    <section aria-label={title ?? t('privacy.rights.summary.title')}>
      <h3>{title ?? t('privacy.rights.summary.title')}</h3>
      {(summary.activities ?? []).length === 0 ? (
        <p>{t('privacy.rights.summary.empty')}</p>
      ) : (
        <ul>
          {(summary.activities ?? []).map((activity) => (
            <li key={activity.activityCode}>
              <strong>{enumLabel(t, 'privacyPurpose', activity.purposeCode)}</strong>
              <div>
                {t('privacy.rights.summary.basis', {
                  basis: enumLabel(t, 'legalBasis', activity.legalBasis),
                })}
              </div>
              <div>
                {t('privacy.rights.summary.categories', {
                  categories: (activity.dataCategories ?? []).join(', '),
                })}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function NomineeCard() {
  const { t } = useI18n();
  const nominee = useMyNominee();
  const set = useSetMyNominee();
  const revoke = useRevokeMyNominee();
  const [name, setName] = useState('');
  const [contact, setContact] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [done, setDone] = useState<'privacy.nominee.saved' | 'privacy.nominee.revoked' | null>(null);
  if (nominee.isPending) return <Skeleton label={t('common.state.loading')} lines={2} />;
  const current = nominee.data ?? null;
  const save = () => {
    setProblem(null);
    setDone(null);
    if (!name.trim() || !contact.trim()) return setProblem(t('privacy.nominee.required'));
    set.mutate(
      {
        nomineeName: name.trim(),
        nomineeContact: contact.trim(),
        ...(current ? { etag: current.etag } : {}),
      },
      {
        onSuccess: () => {
          setName('');
          setContact('');
          setDone('privacy.nominee.saved');
        },
      },
    );
  };
  return (
    <Card title={t('privacy.nominee.title')}>
      <div className="consent-card">
        <p>{t('privacy.nominee.intro')}</p>
        {current ? (
          <>
            <p>
              {t('privacy.nominee.current', {
                name: current.nomineeNameMasked,
                contact: current.nomineeContactMasked,
              })}
            </p>
            {current.activatedAt ? <p>{t('privacy.nominee.active')}</p> : null}
          </>
        ) : (
          <p>{t('privacy.nominee.none')}</p>
        )}
        <TextInput
          label={t('privacy.nominee.name')}
          value={name}
          autoComplete="off"
          error={fieldError(set.error, 'nomineeName')}
          onChange={(event) => setName(event.currentTarget.value)}
        />
        <TextInput
          label={t('privacy.nominee.contact')}
          value={contact}
          autoComplete="off"
          error={fieldError(set.error, 'nomineeContact')}
          onChange={(event) => setContact(event.currentTarget.value)}
        />
        {problem ? <Banner intent="warning">{problem}</Banner> : null}
        {set.isError ? <Banner intent="danger">{t(rightsFailureKey(set.error))}</Banner> : null}
        {done ? <Banner intent="success">{t(done)}</Banner> : null}
        <div className="consent-card__actions">
          <Button variant="primary" loading={set.isPending} onClick={save}>
            {current ? t('privacy.nominee.replace') : t('privacy.nominee.save')}
          </Button>
          {current ? (
            <Button variant="secondary" onClick={() => setConfirming(true)}>
              {t('privacy.nominee.revoke')}
            </Button>
          ) : null}
        </div>
      </div>
      <Dialog
        open={confirming}
        title={t('privacy.nominee.revokeTitle')}
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={revoke.isPending}
              onClick={() =>
                current &&
                revoke.mutate(current.etag, {
                  onSettled: () => setConfirming(false),
                  onSuccess: () => setDone('privacy.nominee.revoked'),
                })
              }
            >
              {t('privacy.nominee.revoke')}
            </Button>
          </>
        }
      >
        <p>{t('privacy.nominee.revokeBody')}</p>
      </Dialog>
    </Card>
  );
}
