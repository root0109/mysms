import {
  lmsFailureText,
  useCan,
  useCohortGroups,
  useCohorts,
  useCreateCohortGroup,
  useDeleteCohortGroup,
  useI18n,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, SelectField, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { LmsRetryBanner } from './lmsShared';

/**
 * LMS-24 Cohort groups: pick one of your cohorts, see its groups, add one and remove one. Group membership for an
 * assignment lives with the assignment's groups, not here.
 */
export function CohortGroupsScreen() {
  const { t } = useI18n();
  const cohorts = useCohorts();
  const [cohortId, setCohortId] = useState('');
  const groups = useCohortGroups(cohortId || null);
  const create = useCreateCohortGroup();
  const remove = useDeleteCohortGroup();
  const mayCreate = useCan('lms.cohort_group.create');
  const mayDelete = useCan('lms.cohort_group.delete');
  const [name, setName] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const fail = (error: unknown) => setProblem(lmsFailureText(t, error));

  type Group = NonNullable<typeof groups.data>['items'][number];
  const columns: Column<Group>[] = [
    { key: 'name', header: t('lms.groups.name'), render: (g) => g.name },
    { key: 'description', header: t('lms.groups.description'), render: (g) => g.description ?? '' },
    {
      key: 'actions',
      header: t('lms.roster.actions'),
      render: (g) =>
        mayDelete ? (
          <Button
            onClick={() =>
              remove.mutate({ cohortId, cohortGroupId: String(g.id), etag: g.etag ?? '' }, { onError: fail })
            }
            disabled={remove.isPending}
          >
            {t('common.action.delete')}
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS24.title')}</h1>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <Card title={t('lms.groups.groups')}>
        <SelectField
          label={t('lms.groups.cohort')}
          value={cohortId}
          onChange={(e) => setCohortId(e.target.value)}
          options={[
            { value: '', label: t('lms.groups.pickCohort') },
            ...(cohorts.data?.items ?? []).map((c) => ({ value: String(c.id), label: c.title })),
          ]}
        />
        {cohortId === '' ? (
          <p className="hint">{t('lms.groups.pickFirst')}</p>
        ) : groups.isError ? (
          <LmsRetryBanner onRetry={() => void groups.refetch()} />
        ) : (
          <>
            <DataTable
              caption={t('lms.groups.groups')}
              columns={columns}
              rows={groups.data?.items}
              rowKey={(g) => String(g.id)}
              loading={groups.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.groups.none')}
            />
            {mayCreate ? (
              <div className="chip-row">
                <TextInput
                  label={t('lms.groups.newName')}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
                <Button
                  onClick={() =>
                    create.mutate(
                      { cohortId, name: name.trim() },
                      { onSuccess: () => setName(''), onError: fail },
                    )
                  }
                  disabled={create.isPending || !name.trim()}
                >
                  {t('lms.groups.add')}
                </Button>
              </div>
            ) : null}
          </>
        )}
      </Card>
    </div>
  );
}
