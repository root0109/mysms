import {
  lmsFailureText,
  useBatchEnroll,
  useCan,
  useCohortRoster,
  useI18n,
  useImportCohortMembers,
  useSetCohortMembers,
  type CohortRosterEntry,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  StatusChip,
  Switch,
  TextArea,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { LmsRetryBanner } from './lmsShared';

/** User ids pasted one per line (or comma-separated), each a UUID; anything else is reported back. */
function parseUserIds(text: string): { ids: string[]; bad: string[] } {
  const ids: string[] = [];
  const bad: string[] = [];
  for (const raw of text.split(/[\n,]/)) {
    const value = raw.trim();
    if (!value) continue;
    if (/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)) ids.push(value);
    else bad.push(value);
  }
  return { ids, bad };
}

/**
 * LMS-23 Roster: the cohort's students and staff with their names and state, adding members by user id (students or
 * a named staff role), removing them, and batch enrolment with its preview. The CSV import takes the file already
 * uploaded to the file store by its id, as the operation does.
 */
export function CohortRosterScreen() {
  const { t } = useI18n();
  const { cohortId = '' } = useParams();
  const roster = useCohortRoster(cohortId || null);
  const setMembers = useSetCohortMembers();
  const batch = useBatchEnroll();
  const importMembers = useImportCohortMembers();
  const maySet = useCan('lms.cohort.set_cohort_members');
  const mayBatch = useCan('lms.cohort.batch_enroll_in_cohort');
  const mayImport = useCan('lms.cohort.import_cohort_members');
  const [adding, setAdding] = useState<null | 'students' | 'staff'>(null);
  const [pasted, setPasted] = useState('');
  const [roleLabel, setRoleLabel] = useState('teacher');
  const [importing, setImporting] = useState(false);
  const [fileId, setFileId] = useState('');
  const [dryRun, setDryRun] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);
  const [problem, setProblem] = useState<string | null>(null);

  const columns: Column<CohortRosterEntry>[] = [
    { key: 'name', header: t('lms.roster.name'), render: (r) => r.displayName },
    {
      key: 'role',
      header: t('lms.roster.role'),
      render: (r) => t(r.roleCode === 'teacher' ? 'lms.roster.teacher' : 'lms.roster.student'),
    },
    {
      key: 'state',
      header: t('lms.roster.state'),
      render: (r) =>
        r.isActive ? (
          <StatusChip intent="success" label={t('enum.status.active')} />
        ) : (
          <StatusChip intent="neutral" label={t('enum.status.inactive')} />
        ),
    },
    {
      key: 'remove',
      header: t('lms.roster.actions'),
      render: (r) =>
        maySet && r.isActive ? (
          <Button
            onClick={() =>
              setMembers.mutate(
                { cohortId, members: [], removeUserIds: [String(r.userId)] },
                {
                  onSuccess: () => setNotice(t('lms.roster.removed')),
                  onError: (error) => setProblem(lmsFailureText(t, error)),
                },
              )
            }
            disabled={setMembers.isPending}
          >
            {t('lms.roster.remove')}
          </Button>
        ) : null,
    },
  ];

  const add = () => {
    const { ids, bad } = parseUserIds(pasted);
    if (bad.length > 0 || ids.length === 0) {
      setProblem(t('lms.roster.badIds'));
      return;
    }
    setProblem(null);
    const act = () =>
      adding === 'staff'
        ? setMembers.mutate(
            { cohortId, members: ids.map((userId) => ({ userId, roleLabel })) },
            { onSuccess: done, onError: fail },
          )
        : setMembers.mutate(
            { cohortId, members: ids.map((userId) => ({ userId })) },
            { onSuccess: done, onError: fail },
          );
    const done = () => {
      setAdding(null);
      setPasted('');
      setNotice(t('lms.roster.added', { count: ids.length }));
    };
    const fail = (error: unknown) => setProblem(lmsFailureText(t, error));
    act();
  };

  const runImport = () => {
    setProblem(null);
    importMembers.mutate(
      { cohortId, fileId: fileId.trim(), memberKind: 'student', validateOnly: dryRun },
      {
        onSuccess: (operation) => {
          setImporting(false);
          setNotice(t('lms.roster.imported', { result: JSON.stringify(operation.result ?? {}) }));
        },
        onError: (error) => setProblem(lmsFailureText(t, error)),
      },
    );
  };

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS23.title')}</h1>
        <p className="page__subtitle">
          <Link to={`/app/lms/cohorts/${cohortId}`}>{t('lms.cohort.back')}</Link>
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      <Card
        title={t('lms.roster.members')}
        actions={
          <>
            {maySet ? (
              <Button onClick={() => setAdding('students')}>{t('lms.roster.addStudents')}</Button>
            ) : null}
            {maySet ? <Button onClick={() => setAdding('staff')}>{t('lms.roster.addStaff')}</Button> : null}
            {mayImport ? <Button onClick={() => setImporting(true)}>{t('lms.roster.import')}</Button> : null}
          </>
        }
      >
        {roster.isError ? (
          <LmsRetryBanner onRetry={() => void roster.refetch()} />
        ) : (
          <DataTable
            caption={t('lms.roster.members')}
            columns={columns}
            rows={roster.data?.items}
            rowKey={(r) => `${r.userId}-${r.roleCode}`}
            loading={roster.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('lms.roster.empty')}
          />
        )}
      </Card>
      <Dialog
        open={adding !== null}
        title={adding === 'staff' ? t('lms.roster.addStaff') : t('lms.roster.addStudents')}
        onClose={() => setAdding(null)}
        footer={
          <>
            <Button onClick={() => setAdding(null)}>{t('common.action.cancel')}</Button>
            {adding === 'students' && mayBatch ? (
              <Button
                onClick={() => {
                  const { ids } = parseUserIds(pasted);
                  batch.mutate(
                    { cohortId, userIds: ids, validateOnly: true },
                    {
                      onSuccess: (preview) =>
                        setNotice(
                          t('lms.roster.preview', {
                            already: (preview.items ?? []).filter((p) => p.alreadyEnrolled).length,
                            fresh: (preview.items ?? []).filter((p) => !p.alreadyEnrolled).length,
                          }),
                        ),
                      onError: (error) => setProblem(lmsFailureText(t, error)),
                    },
                  );
                }}
                disabled={batch.isPending}
              >
                {t('lms.roster.previewAction')}
              </Button>
            ) : null}
            <Button onClick={add} disabled={setMembers.isPending}>
              {t('common.action.save')}
            </Button>
          </>
        }
      >
        <p className="hint">{t('lms.roster.pasteHint')}</p>
        <TextArea
          label={t('lms.roster.userIds')}
          value={pasted}
          onChange={(e) => setPasted(e.target.value)}
        />
        {adding === 'staff' ? (
          <TextInput
            label={t('lms.roster.roleLabel')}
            value={roleLabel}
            onChange={(e) => setRoleLabel(e.target.value)}
          />
        ) : null}
      </Dialog>
      <Dialog
        open={importing}
        title={t('lms.roster.import')}
        onClose={() => setImporting(false)}
        footer={
          <>
            <Button onClick={() => setImporting(false)}>{t('common.action.cancel')}</Button>
            <Button onClick={runImport} disabled={importMembers.isPending || !fileId.trim()}>
              {dryRun ? t('lms.roster.importCheck') : t('lms.roster.importRun')}
            </Button>
          </>
        }
      >
        <p className="hint">{t('lms.roster.importHint')}</p>
        <TextInput
          label={t('lms.roster.fileId')}
          value={fileId}
          onChange={(e) => setFileId(e.target.value)}
        />
        <Switch label={t('lms.roster.importDryRun')} checked={dryRun} onChange={setDryRun} />
      </Dialog>
    </div>
  );
}
