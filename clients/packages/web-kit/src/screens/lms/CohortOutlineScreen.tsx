import { useCan, useCohortOutline, useCopyCourseOutline, useI18n, lmsFailureText } from '@mysms/core';
import { Banner, Button, Card, DataTable, Dialog, StatusChip, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { LmsRetryBanner } from './lmsShared';

/**
 * LMS-22 Cohort outline: the cohort's content items in their order, with their kind, dates and visibility, and the
 * step that copies a course template's outline into the cohort. Moving, copying and re-dating single items ships
 * with the content operations (still 501); until then rows link to their record screens.
 */
export function CohortOutlineScreen() {
  const { t, format } = useI18n();
  const { cohortId = '' } = useParams();
  const outline = useCohortOutline(cohortId || null);
  const copy = useCopyCourseOutline();
  const mayCopy = useCan('lms.cohort.copy_course_outline_to_cohort');
  const [copying, setCopying] = useState(false);
  const [courseId, setCourseId] = useState('');
  const [problem, setProblem] = useState<string | null>(null);

  type Item = NonNullable<typeof outline.data>['items'][number];
  const columns: Column<Item>[] = [
    { key: 'title', header: t('lms.outline.item'), render: (i) => i.title ?? String(i.id) },
    { key: 'kind', header: t('lms.outline.kind'), render: (i) => String(i.itemKind ?? '') },
    {
      key: 'visible',
      header: t('lms.outline.visible'),
      render: (i) =>
        i.isActive ? (
          <StatusChip intent="success" label={t('lms.outline.active')} />
        ) : (
          <StatusChip intent="neutral" label={t('lms.outline.inactive')} />
        ),
    },
    {
      key: 'due',
      header: t('lms.outline.due'),
      render: (i) => (i.dueAt ? format.day(i.dueAt) : ''),
    },
    {
      key: 'points',
      header: t('lms.outline.points'),
      render: (i) => (i.isGraded ? String(i.points ?? '') : ''),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS22.title')}</h1>
        <p className="page__subtitle">
          <Link to={`/app/lms/cohorts/${cohortId}`}>{t('lms.cohort.back')}</Link>
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <Card
        title={t('lms.outline.items')}
        actions={mayCopy ? <Button onClick={() => setCopying(true)}>{t('lms.outline.copy')}</Button> : null}
      >
        {outline.isError ? (
          <LmsRetryBanner onRetry={() => void outline.refetch()} />
        ) : (
          <DataTable
            caption={t('lms.outline.items')}
            columns={columns}
            rows={outline.data?.items}
            rowKey={(i) => String(i.id)}
            loading={outline.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('lms.outline.empty')}
          />
        )}
      </Card>
      <Dialog
        open={copying}
        title={t('lms.outline.copy')}
        onClose={() => setCopying(false)}
        footer={
          <>
            <Button onClick={() => setCopying(false)}>{t('common.action.cancel')}</Button>
            <Button
              onClick={() => {
                setProblem(null);
                copy.mutate(
                  { cohortId, courseId: courseId.trim() },
                  {
                    onSuccess: () => setCopying(false),
                    onError: (error) => setProblem(lmsFailureText(t, error)),
                  },
                );
              }}
              disabled={copy.isPending || !courseId.trim()}
            >
              {t('lms.outline.copyRun')}
            </Button>
          </>
        }
      >
        <p className="hint">{t('lms.outline.copyHint')}</p>
        <TextInput
          label={t('lms.outline.courseId')}
          value={courseId}
          onChange={(e) => setCourseId(e.target.value)}
        />
      </Dialog>
    </div>
  );
}
