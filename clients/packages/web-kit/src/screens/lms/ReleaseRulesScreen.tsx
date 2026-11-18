import {
  useCan,
  useEvaluateReleaseRules,
  useI18n,
  useReleaseRules,
  useReleaseRuleStep,
  lmsFailureText,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, StatusChip, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { LmsRetryBanner } from './lmsShared';

/**
 * LMS-29 Release rules: the cohort's conditional-release rules in their order, switching one on or off, and running
 * the evaluation now — the grants whose state changed are shown back. The rule builder itself (conditions and
 * targets) is edited on the rule's record screens.
 */
export function ReleaseRulesScreen() {
  const { t } = useI18n();
  const { cohortId = '' } = useParams();
  const rules = useReleaseRules(cohortId || null);
  const step = useReleaseRuleStep();
  const evaluate = useEvaluateReleaseRules();
  const mayToggle = useCan('lms.release_rule.enable_release_rule');
  const mayEvaluate = useCan('lms.cohort.evaluate_release_rules');
  const [problem, setProblem] = useState<string | null>(null);
  const [changed, setChanged] = useState<number | null>(null);
  const fail = (error: unknown) => setProblem(lmsFailureText(t, error));

  type Rule = NonNullable<typeof rules.data>['items'][number];
  const columns: Column<Rule>[] = [
    { key: 'order', header: t('lms.release.order'), render: (r) => String(r.sortOrder ?? '') },
    { key: 'title', header: t('lms.release.rule'), render: (r) => r.title ?? String(r.id) },
    {
      key: 'state',
      header: t('lms.release.state'),
      render: (r) =>
        r.isDisabled ? (
          <StatusChip intent="neutral" label={t('lms.release.disabled')} />
        ) : (
          <StatusChip intent="success" label={t('lms.release.enabled')} />
        ),
    },
    {
      key: 'stop',
      header: t('lms.release.stops'),
      render: (r) => (r.stopIfTrue ? t('lms.release.stopsYes') : ''),
    },
    {
      key: 'actions',
      header: t('lms.roster.actions'),
      render: (r) =>
        mayToggle ? (
          <Button
            onClick={() =>
              step.mutate(
                {
                  cohortId,
                  releaseRuleId: String(r.id),
                  step: r.isDisabled ? 'enable' : 'disable',
                  etag: r.etag ?? null,
                },
                { onError: fail },
              )
            }
            disabled={step.isPending}
          >
            {r.isDisabled ? t('lms.release.enable') : t('lms.release.disable')}
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS29.title')}</h1>
        <p className="page__subtitle">
          <Link to={`/app/lms/cohorts/${cohortId}`}>{t('lms.cohort.back')}</Link>
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {changed !== null ? <Banner intent="success">{t('lms.release.evaluated', { changed })}</Banner> : null}
      <Card
        title={t('lms.release.rules')}
        actions={
          mayEvaluate ? (
            <Button
              onClick={() => {
                setProblem(null);
                evaluate.mutate(
                  { cohortId },
                  { onSuccess: (grants) => setChanged((grants.items ?? []).length), onError: fail },
                );
              }}
              disabled={evaluate.isPending}
            >
              {t('lms.release.evaluate')}
            </Button>
          ) : null
        }
      >
        {rules.isError ? (
          <LmsRetryBanner onRetry={() => void rules.refetch()} />
        ) : (
          <DataTable
            caption={t('lms.release.rules')}
            columns={columns}
            rows={rules.data?.items}
            rowKey={(r) => String(r.id)}
            loading={rules.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('lms.release.none')}
          />
        )}
      </Card>
    </div>
  );
}
