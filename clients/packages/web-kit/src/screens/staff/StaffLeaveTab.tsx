import {
  academicYearOn,
  localDay,
  useAcademicYears,
  useCan,
  useI18n,
  useLeaveApplications,
  useLeavePosition,
  useLeaveTypes,
  type LeavePosition,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  StatusChip,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link } from 'react-router';
import { screenLink, staffProblem } from './staffText';

const STATUS: Readonly<Record<string, MessageKey>> = {
  pending: 'hr.leave.status.pending',
  approved: 'hr.leave.status.approved',
  rejected: 'hr.leave.status.rejected',
  cancelled: 'hr.leave.status.cancelled',
  completed: 'hr.leave.status.completed',
};

/**
 * HR-02 "Leave": a staff member's position for an academic year of their school (hr.GetStaffLeaveBalance: entitled,
 * taken, asked for, left) and their applications, read-only; deciding and applying happen in HR-04, which the tab
 * links to.
 */
export function StaffLeaveTab({ staffId, institutionId }: { staffId: string; institutionId: string }) {
  const { t, format, timeZone } = useI18n();
  const mayPosition = useCan('hr.staff.get_leave_balance');
  const mayApplications = useCan('hr.leave_application.read');
  const mayYears = useCan('org.academic_year.read');
  const mayTypes = useCan('hr.leave_type.read');
  const years = useAcademicYears(mayYears && mayPosition);
  const ownYears = (years.data?.items ?? []).filter((year) => year.institutionId === institutionId);
  const current = academicYearOn(ownYears, institutionId, localDay(new Date(), timeZone));
  const [chosen, setChosen] = useState('');
  const yearId = chosen || current?.id || ownYears[0]?.id || null;
  const position = useLeavePosition(mayPosition ? staffId : null, yearId);
  const applications = useLeaveApplications(mayApplications ? staffId : null);
  const types = useLeaveTypes(institutionId, mayTypes);
  const typeName = new Map((types.data ?? []).flatMap((type) => (type.id ? [[type.id, type.name]] : [])));

  const columns: Column<LeavePosition>[] = [
    {
      key: 'type',
      header: t('hr.leave.type'),
      render: (line) => typeName.get(line.leaveTypeId) ?? t('hr.leave.untyped'),
    },
    ...(['entitled', 'taken', 'pending', 'available'] as const).map((key) => ({
      key,
      header: t(`hr.leave.position.${key}`),
      align: 'end' as const,
      render: (line: LeavePosition) => format.number(line[key]),
    })),
  ];

  return (
    <div className="page__grid">
      {mayPosition && mayYears ? (
        <Card title={t('hr.staff.leave.position')}>
          {years.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={3} />
          ) : ownYears.length === 0 ? (
            <p className="hint">{t('hr.staff.leave.noYears')}</p>
          ) : (
            <>
              <SelectField
                label={t('hr.staff.leave.year')}
                value={yearId ?? ''}
                onChange={(event) => setChosen(event.target.value)}
                options={ownYears.map((year) => ({ value: year.id ?? '', label: year.name }))}
              />
              {position.isError ? (
                <Banner
                  intent="danger"
                  action={
                    <Button variant="secondary" onClick={() => void position.refetch()}>
                      {t('common.action.retry')}
                    </Button>
                  }
                >
                  {staffProblem(t, position.error)}
                </Banner>
              ) : (
                <DataTable
                  caption={t('hr.staff.leave.position')}
                  columns={columns}
                  rows={position.data}
                  rowKey={(line) => line.leaveTypeId}
                  loading={position.isPending}
                  loadingLabel={t('common.state.loading')}
                  emptyText={t('hr.leave.position.none')}
                />
              )}
            </>
          )}
        </Card>
      ) : null}
      {mayApplications ? (
        <Card
          title={t('hr.staff.leave.applications')}
          actions={<Link to={screenLink('HR-04')}>{t('hr.staff.leave.openApplications')}</Link>}
        >
          {applications.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={3} />
          ) : applications.isError ? (
            <Banner intent="danger">{staffProblem(t, applications.error)}</Banner>
          ) : applications.data.items.length === 0 ? (
            <p className="hint">{t('hr.staff.leave.noApplications')}</p>
          ) : (
            <ul className="leave-list">
              {applications.data.items.map((application) => {
                const first = application.fromAt.slice(0, 10);
                const last = application.toAt.slice(0, 10);
                return (
                  <li key={application.id} className="leave-list__item">
                    <span className="leave-list__what">
                      <strong>
                        {(application.leaveTypeId && typeName.get(application.leaveTypeId)) ||
                          t('hr.leave.untyped')}
                      </strong>{' '}
                      {first === last ? format.day(first) : `${format.day(first)} – ${format.day(last)}`}
                    </span>
                    <StatusChip
                      label={t(STATUS[application.status ?? ''] ?? 'hr.leave.status.pending')}
                      intent={application.status === 'approved' ? 'success' : 'neutral'}
                    />
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
      ) : null}
    </div>
  );
}
