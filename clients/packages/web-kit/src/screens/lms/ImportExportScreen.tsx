import {
  lmsFailureText,
  useCan,
  useCoursePackageTools,
  useI18n,
  useImportExportJobs,
  type ImportExportJob,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { UUID } from '../ids';

/**
 * LMS-51 Course import / export: a course exported as a MySMS package (202), an uploaded package imported into a
 * course, the jobs with their outcome, a finished package fetched by its short-lived link, and a finished cohort
 * archive restored once.
 */
export function ImportExportScreen() {
  const { t } = useI18n();
  const mayRead = useCan('lms.import_export_job.read');
  const jobs = useImportExportJobs();
  const title = t('screen.LMS51.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (jobs.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (jobs.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void jobs.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return <JobsPanel jobs={jobs.data.items} />;
}

function JobsPanel({ jobs }: { jobs: ImportExportJob[] }) {
  const { t } = useI18n();
  const tools = useCoursePackageTools();
  const mayExport = useCan('lms.course.export_course');
  const mayImport = useCan('lms.course.import_course_package');
  const mayRestore = useCan('lms.import_export_job.restore_cohort_archive');
  const mayPackage = useCan('lms.import_export_job.get_job_package');
  const [courseId, setCourseId] = useState('');
  const [fileId, setFileId] = useState('');
  const failure =
    tools.exportCourse.error ?? tools.importPackage.error ?? tools.restore.error ?? tools.packageLink.error;
  return (
    <Card title={t('screen.LMS51.title')}>
      {failure !== undefined && failure !== null && (
        <Banner intent="danger">{lmsFailureText(t, failure)}</Banner>
      )}
      {(mayExport || mayImport) && (
        <div role="group" aria-label={t('lms.jobs.start')}>
          <TextInput
            label={t('lms.jobs.courseId')}
            value={courseId}
            onChange={(event) => setCourseId(event.target.value)}
            error={courseId !== '' && !UUID.test(courseId) ? t('common.validation.pattern') : undefined}
          />
          {mayExport && (
            <Button
              variant="secondary"
              onClick={() => tools.exportCourse.mutate({ courseId })}
              disabled={tools.exportCourse.isPending || !UUID.test(courseId)}
            >
              {t('lms.jobs.export')}
            </Button>
          )}
          {mayImport && (
            <>
              <TextInput
                label={t('lms.jobs.fileId')}
                hint={t('lms.jobs.fileHint')}
                value={fileId}
                onChange={(event) => setFileId(event.target.value)}
                error={fileId !== '' && !UUID.test(fileId) ? t('common.validation.pattern') : undefined}
              />
              <Button
                variant="secondary"
                onClick={() => tools.importPackage.mutate({ courseId, request: { fileId } })}
                disabled={tools.importPackage.isPending || !UUID.test(courseId) || !UUID.test(fileId)}
              >
                {t('lms.jobs.import')}
              </Button>
            </>
          )}
        </div>
      )}
      <DataTable
        caption={t('lms.jobs.list')}
        columns={[
          {
            key: 'what',
            header: t('lms.jobs.what'),
            render: (row: ImportExportJob) => `${row.direction ?? ''} ${row.packageFormat ?? ''}`,
          },
          {
            key: 'course',
            header: t('lms.jobs.course'),
            render: (row: ImportExportJob) => row.courseTitle ?? row.courseId ?? row.cohortCode ?? '',
          },
          {
            key: 'status',
            header: t('lms.jobs.status'),
            render: (row: ImportExportJob) => (
              <StatusChip
                intent={row.status === 'complete' ? 'success' : row.status === 'error' ? 'danger' : 'neutral'}
                label={row.status ?? ''}
              />
            ),
          },
          {
            key: 'actions',
            header: t('lms.bank.actions'),
            render: (row: ImportExportJob) => (
              <>
                {mayPackage &&
                  row.status === 'complete' &&
                  row.packageFileId !== undefined &&
                  row.packageFileId !== null &&
                  row.id && (
                    <Button
                      variant="secondary"
                      onClick={() =>
                        tools.packageLink.mutate(
                          { importExportJobId: row.id as string },
                          {
                            onSuccess: (link) => {
                              if (link.url) window.open(link.url, '_blank', 'noopener');
                            },
                          },
                        )
                      }
                      disabled={tools.packageLink.isPending}
                    >
                      {t('lms.jobs.download')}
                    </Button>
                  )}
                {mayRestore &&
                  row.direction === 'export' &&
                  row.packageFormat === 'cohort_archive' &&
                  row.status === 'complete' &&
                  row.unarchiveStatus !== 'complete' &&
                  row.id && (
                    <Button
                      variant="secondary"
                      onClick={() => tools.restore.mutate({ importExportJobId: row.id as string })}
                      disabled={tools.restore.isPending}
                    >
                      {t('lms.jobs.restore')}
                    </Button>
                  )}
                {row.errorLog !== undefined && row.errorLog !== null && (
                  <span className="hint">{row.errorLog}</span>
                )}
              </>
            ),
          },
        ]}
        rows={jobs}
        rowKey={(row) => row.id ?? ''}
        loadingLabel={t('common.state.loading')}
        emptyText={t('lms.jobs.empty')}
      />
    </Card>
  );
}
