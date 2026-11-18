import {
  PIPELINE,
  buildPath,
  screenById,
  useAdmissionCycles,
  useApplications,
  useCan,
  useI18n,
  type Application,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SegmentedControl,
  SelectField,
  Skeleton,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { ApplicationStatusChip } from './ApplicationStatusChip';
import { APPLICATION_STATUS } from './status';

type View = 'board' | 'table';

/**
 * ADMN-02 Applications pipeline: every application the office reaches, as a board with one column per status (in
 * the order an application moves; refused and withdrawn ones last) or as a table, for one cycle or all of them.
 * Each application opens its detail (ADMN-03), where the decisions are taken. The cycle and the view live in the
 * address, so a link or a reload opens the same pipeline.
 */
export function ApplicationsPipelineScreen() {
  const { t, format } = useI18n();
  const mayRead = useCan('admission.application.read');
  const mayCycles = useCan('admission.admission_cycle.read');
  const [query, setQuery] = useSearchParams();
  const cycleId = query.get('cycle') || null;
  const [view, setView] = useState<View>(query.get('view') === 'table' ? 'table' : 'board');
  const cycles = useAdmissionCycles(mayRead && mayCycles);
  const applications = useApplications(cycleId, mayRead);
  const detail = screenById('ADMN-03')?.routes[0];
  const title = t('screen.ADMN02.title');
  const set = (name: string, value: string | null) =>
    setQuery(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value) next.set(name, value);
        else next.delete(name);
        return next;
      },
      { replace: true },
    );
  const open = (application: Application) =>
    detail && application.id ? `/app${buildPath(detail, { applicationId: application.id })}` : undefined;
  const name = (application: Application) => {
    const to = open(application);
    return to ? <Link to={to}>{application.fullName}</Link> : application.fullName;
  };

  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('admission.pipeline.noAccess')}</Banner>
      </div>
    );

  const items = applications.data?.items ?? [];
  const columns: Column<Application>[] = [
    { key: 'number', header: t('admission.field.applicationNumber'), render: (a) => a.applicationNumber },
    { key: 'name', header: t('admission.field.fullName'), render: name },
    {
      key: 'status',
      header: t('admission.field.status'),
      render: (a) => <ApplicationStatusChip t={t} application={a} />,
    },
    {
      key: 'applied',
      header: t('admission.field.applicationDate'),
      render: (a) => (a.applicationDate ? format.day(a.applicationDate.slice(0, 10)) : ''),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('admission.pipeline.intro')}</p>
      </header>
      <div className="admission-toolbar">
        {mayCycles ? (
          <SelectField
            label={t('admission.pipeline.cycle')}
            value={cycleId ?? ''}
            onChange={(event) => set('cycle', event.target.value || null)}
            options={[
              { value: '', label: t('admission.pipeline.allCycles') },
              ...(cycles.data?.items ?? []).map((cycle) => ({ value: cycle.id ?? '', label: cycle.name })),
            ]}
          />
        ) : null}
        <SegmentedControl<View>
          label={t('admission.pipeline.view')}
          value={view}
          onChange={(next) => {
            setView(next);
            set('view', next === 'table' ? 'table' : null);
          }}
          options={[
            { value: 'board', label: t('admission.pipeline.board') },
            { value: 'table', label: t('admission.pipeline.table') },
          ]}
        />
      </div>
      {applications.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void applications.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : applications.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={6} />
      ) : items.length === 0 ? (
        <Card>
          <p className="hint">{t('admission.pipeline.none')}</p>
        </Card>
      ) : view === 'table' ? (
        <Card title={t('admission.pipeline.table')}>
          <DataTable
            caption={title}
            columns={columns}
            rows={items}
            rowKey={(a) => a.id ?? a.applicationNumber}
            loadingLabel={t('common.state.loading')}
            emptyText={t('admission.pipeline.none')}
          />
        </Card>
      ) : (
        <div className="admission-board">
          {PIPELINE.map((status) => {
            const here = items.filter((a) => (a.status ?? 'draft') === status);
            const label = APPLICATION_STATUS[status]?.label;
            const heading = label ? t(label) : status;
            return (
              <section key={status} className="admission-board__column" aria-label={heading}>
                <h2 className="admission-board__heading">
                  {heading} <span className="hint">{format.number(here.length)}</span>
                </h2>
                {here.length === 0 ? (
                  <p className="hint">{t('admission.pipeline.emptyColumn')}</p>
                ) : (
                  <ul className="admission-board__cards">
                    {here.map((application) => (
                      <li key={application.id} className="admission-board__card">
                        <strong>{name(application)}</strong>
                        <span className="hint">{application.applicationNumber}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
