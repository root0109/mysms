import {
  allOf,
  isApiError,
  resourceDef,
  useAllRecords,
  useCan,
  useCreateRecord,
  useEmailApplicant,
  useI18n,
  useInfiniteRecords,
  useMyApplications,
  useRecord,
  useSession,
  useUpdateApplicationStatus,
  type JobApplicationStatus,
  type RecordRow,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, Skeleton, TextArea, TextInput } from '@mysms/ui-web';
import { Fragment, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { FileDownloadButton, FileUploadButton } from '../../files/FileTransfer';
import { UUID } from '../ids';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';

const OPENINGS = resourceDef('careers.JobOpening');
const APPLICATIONS = resourceDef('careers.JobApplication');
const STATUSES: readonly JobApplicationStatus[] = [
  'submitted',
  'shortlisted',
  'rejected',
  'hired',
  'withdrawn',
];

/** What an opening says about itself in one line: company, place, kind of work. */
function openingLine(t: ReturnType<typeof useI18n>['t'], row: RecordRow): string {
  return [
    String(row.companyName ?? ''),
    String(row.location ?? ''),
    enumLabel(t, 'employmentType', String(row.employmentType ?? '')),
    row.workMode ? enumLabel(t, 'workMode', String(row.workMode)) : '',
  ]
    .filter(Boolean)
    .join(' · ');
}

/**
 * CAR-01 Job board: the school's open openings, newest first, readable without signing in (as a guest of the
 * school this deployment names; the server shows guests open openings only, without internal fields). Each opens
 * its detail (CAR-02); a signed-in poster also finds the employer console (CAR-03).
 */
export function JobBoardScreen() {
  const { t } = useI18n();
  const signedIn = useSession((s) => s.status === 'signedIn');
  const mayPost = useCan('careers.job_opening.create');
  const openings = useInfiniteRecords(OPENINGS, {
    filter: 'status = "open"',
    sort: ['createTime,desc'],
    size: 20,
  });
  const rows = openings.data?.pages.flatMap((page) => page.data) ?? [];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.CAR01.title')}</h1>
        <p className="page__subtitle">{t('careers.board.intro')}</p>
        <div className="page__actions">
          {mayPost ? <Link to="/app/careers/job-openings?mine">{t('screen.CAR03.title')}</Link> : null}
          {signedIn ? <Link to="/app/careers/job-applications?mine">{t('screen.CAR04.title')}</Link> : null}
        </div>
      </header>
      {openings.isPending ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {openings.isError ? <Banner intent="danger">{problemText(t, openings.error)}</Banner> : null}
      {openings.isSuccess && rows.length === 0 ? <p className="hint">{t('careers.board.none')}</p> : null}
      <ul className="leave-list" aria-label={t('screen.CAR01.title')}>
        {rows.map((row) => (
          <li key={row.id} className="leave-list__item">
            <div className="leave-list__what">
              <Link to={`/app/careers/job-openings/${row.id}`}>{String(row.title ?? '')}</Link>
              <span className="hint">{openingLine(t, row)}</span>
            </div>
          </li>
        ))}
      </ul>
      {openings.hasNextPage ? (
        <Button
          variant="secondary"
          loading={openings.isFetchingNextPage}
          onClick={() => void openings.fetchNextPage()}
        >
          {t('common.list.more')}
        </Button>
      ) : null}
    </div>
  );
}

/** Applies to an opening with a résumé (careers.CreateJobApplication; the applicant is the caller, from the token). */
function Apply({ openingId }: { openingId: string }) {
  const { t } = useI18n();
  const create = useCreateRecord(APPLICATIONS as NonNullable<typeof APPLICATIONS>);
  return (
    <>
      {create.isSuccess ? <Banner intent="success">{t('careers.apply.done')}</Banner> : null}
      {create.error ? <Banner intent="danger">{problemText(t, create.error)}</Banner> : null}
      <FileUploadButton
        label={t('careers.apply.resume')}
        disabled={create.isPending}
        onUploaded={(file) => create.mutate({ parentId: openingId, values: { resumeFileId: file.id } })}
      />
    </>
  );
}

/**
 * CAR-02 Opening detail and apply: the opening as the reader may see it (a guest sees an open one, closed ones are
 * not found), and for a signed-in candidate their application to it — its state — or applying with a résumé.
 */
export function OpeningScreen() {
  const { t } = useI18n();
  const { jobOpeningId = '' } = useParams();
  const valid = UUID.test(jobOpeningId);
  const signedIn = useSession((s) => s.status === 'signedIn');
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayApply = useCan('careers.job_application.create');
  const mayReadApplications = useCan('careers.job_application.read');
  const opening = useRecord(OPENINGS, valid ? jobOpeningId : null);
  const mine = useAllRecords(
    'careers.JobApplication',
    { parentId: jobOpeningId, filter: `applicantUserId = "${me}"` },
    valid && signedIn && mayReadApplications && me !== '',
  );
  const application = mine.data?.items[0];
  if (!valid) return <Banner intent="warning">{t('common.state.notAvailable')}</Banner>;
  if (opening.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (opening.isError)
    return isApiError(opening.error) && opening.error.status === 404 ? (
      <Banner intent="warning">{t('common.state.notAvailable')}</Banner>
    ) : (
      <Banner intent="danger">{problemText(t, opening.error)}</Banner>
    );
  const row = opening.data.data;
  const open = row.status !== 'closed';
  return (
    <div className="page">
      <header className="page__header">
        <p className="hint">
          <Link to="/app/careers/job-openings">{t('common.action.back')}</Link>
        </p>
        <h1 className="page__title">{String(row.title ?? '')}</h1>
        <p className="page__subtitle">{openingLine(t, row)}</p>
      </header>
      <Card title={t('careers.opening.about')}>
        <p className="consent-notice">{String(row.description ?? '')}</p>
        <dl className="facts">
          {(
            [
              [t('careers.opening.company'), String(row.companyName ?? '')],
              [t('careers.opening.website'), String(row.companyWebsite ?? '')],
              [t('careers.opening.email'), String(row.companyEmail ?? '')],
            ] as const
          ).map(([label, value]) => (
            <Fragment key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </Fragment>
          ))}
        </dl>
      </Card>
      <Card title={t('careers.apply.title')}>
        {!signedIn ? (
          <p>
            <Link to="/app/auth/login">{t('careers.apply.signIn')}</Link>
          </p>
        ) : application ? (
          <p>
            {t('careers.apply.status', {
              status: enumLabel(t, 'applicationStatus', String(application.status ?? '')),
            })}
          </p>
        ) : !open ? (
          <p className="hint">{t('careers.apply.closed')}</p>
        ) : mayApply ? (
          <Apply openingId={row.id} />
        ) : (
          <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
        )}
      </Card>
    </div>
  );
}

/** One applicant of the poster's opening: their résumé, moving them along the pipeline, and writing to them. */
function Applicant({ openingId, row }: { openingId: string; row: RecordRow }) {
  const { t } = useI18n();
  const mayStatus = useCan('careers.job_application.update_application_status');
  const mayEmail = useCan('careers.job_application.email_applicant');
  const status = useUpdateApplicationStatus();
  const email = useEmailApplicant();
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const failure = status.error ?? email.error;
  return (
    <li>
      <Card title={enumLabel(t, 'applicationStatus', String(row.status ?? ''))}>
        {failure ? <Banner intent="danger">{problemText(t, failure)}</Banner> : null}
        {email.isSuccess ? <Banner intent="success">{t('careers.console.emailed')}</Banner> : null}
        <div className="chip-row">
          {typeof row.resumeFileId === 'string' ? (
            <FileDownloadButton fileId={row.resumeFileId} label={t('careers.console.resume')} />
          ) : null}
          {mayStatus ? (
            <SelectField
              label={t('careers.console.status')}
              value={String(row.status ?? 'submitted')}
              options={STATUSES.map((value) => ({ value, label: enumLabel(t, 'applicationStatus', value) }))}
              onChange={(e) =>
                status.mutate({
                  jobOpeningId: openingId,
                  jobApplicationId: row.id,
                  status: e.target.value as JobApplicationStatus,
                  etag: row.etag ?? null,
                })
              }
            />
          ) : null}
        </div>
        {mayEmail ? (
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              email.mutate(
                { applicationId: row.id, subject: subject.trim(), message: message.trim() },
                {
                  onSuccess: () => {
                    setSubject('');
                    setMessage('');
                  },
                },
              );
            }}
          >
            <TextInput
              label={t('careers.console.subject')}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
            />
            <TextArea
              label={t('careers.console.message')}
              value={message}
              rows={3}
              onChange={(e) => setMessage(e.target.value)}
            />
            <div className="form__actions">
              <Button
                type="submit"
                variant="secondary"
                disabled={!subject.trim() || !message.trim()}
                loading={email.isPending}
              >
                {t('careers.console.email')}
              </Button>
            </div>
          </form>
        ) : null}
      </Card>
    </li>
  );
}

/** The applicants of one of the poster's openings, newest first. */
function Applicants({ openingId }: { openingId: string }) {
  const { t } = useI18n();
  const mayRead = useCan('careers.job_application.read');
  const applications = useInfiniteRecords(mayRead ? APPLICATIONS : undefined, {
    parentId: openingId,
    sort: ['createTime,desc'],
    size: 20,
  });
  const rows = applications.data?.pages.flatMap((page) => page.data) ?? [];
  return (
    <Card title={t('careers.console.applicants')}>
      {!mayRead ? <Banner intent="info">{t('adm.resource.noAccess')}</Banner> : null}
      {applications.isError ? <Banner intent="danger">{problemText(t, applications.error)}</Banner> : null}
      {applications.isSuccess && rows.length === 0 ? (
        <p className="hint">{t('careers.console.noApplicants')}</p>
      ) : null}
      <ul className="leave-list">
        {rows.map((row) => (
          <Applicant key={row.id} openingId={openingId} row={row} />
        ))}
      </ul>
      {applications.hasNextPage ? (
        <Button variant="secondary" onClick={() => void applications.fetchNextPage()}>
          {t('common.list.more')}
        </Button>
      ) : null}
    </Card>
  );
}

/**
 * CAR-03 Employer console: the openings the signed-in poster put up (create, edit, close and reopen in their
 * editor), and for the one chosen (`?opening=`) its applicant pipeline.
 */
export function EmployerConsoleScreen() {
  const { t } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const [search, setSearch] = useSearchParams();
  const asked = search.get('opening') ?? '';
  const openingId = UUID.test(asked) ? asked : '';
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.CAR03.title')}</h1>
        <p className="page__subtitle">{t('careers.console.intro')}</p>
      </header>
      <RelatedRecords
        resource="careers.JobOpening"
        title={t('careers.console.openings')}
        scope={allOf(me ? `postedByUserId = "${me}"` : undefined)}
        columns={['title', 'location', 'employmentType', 'status']}
        filterField="status"
        rowTool={(row) => (
          <Button
            variant={row.id === openingId ? 'primary' : 'secondary'}
            aria-pressed={row.id === openingId}
            onClick={() => setSearch({ mine: '', opening: row.id })}
          >
            {t('careers.console.applicants')}
          </Button>
        )}
      />
      {openingId ? <Applicants openingId={openingId} /> : null}
    </div>
  );
}

/**
 * CAR-04 My applications and preferences: the candidate's applications — one read per open or closed opening on the
 * board, as the contract keeps applications under their opening — each with its state, and the candidate's own
 * preferences (what they are open to, where, how they like to work).
 */
export function MyApplicationsScreen() {
  const { t } = useI18n();
  const personId = useSession((s) => s.me?.user?.personId ?? '');
  const mayPreferences = useCan('careers.candidate_preference.read');
  const openings = useAllRecords('careers.JobOpening', { sort: ['createTime,desc'] });
  const titleOf = new Map((openings.data?.items ?? []).map((row) => [row.id, String(row.title ?? '')]));
  const mine = useMyApplications((openings.data?.items ?? []).map((row) => row.id));
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.CAR04.title')}</h1>
        <p className="page__subtitle">{t('careers.mine.intro')}</p>
      </header>
      <Card title={t('careers.mine.applications')}>
        {openings.isPending || mine.isPending ? (
          <Skeleton label={t('common.state.loading')} lines={3} />
        ) : null}
        {openings.isError || mine.isError ? (
          <Banner intent="danger">{t('common.error.generic')}</Banner>
        ) : null}
        {openings.isSuccess && !mine.isPending && mine.applications.length === 0 ? (
          <p className="hint">{t('careers.mine.none')}</p>
        ) : null}
        <ul className="leave-list">
          {mine.applications.map((row) => (
            <li key={row.id} className="leave-list__item">
              <div className="leave-list__what">
                <Link to={`/app/careers/job-openings/${String(row.jobOpeningId)}`}>
                  {titleOf.get(String(row.jobOpeningId)) || t('careers.mine.opening')}
                </Link>
                <span className="hint">{enumLabel(t, 'applicationStatus', String(row.status ?? ''))}</span>
              </div>
            </li>
          ))}
        </ul>
      </Card>
      {mayPreferences && personId ? (
        <RelatedRecords
          resource="careers.CandidatePreference"
          title={t('careers.mine.preferences')}
          scope={`personId = "${personId}"`}
          presets={{ personId }}
          columns={['openTo', 'preferredLocation', 'workHours', 'companyType']}
        />
      ) : null}
    </div>
  );
}
