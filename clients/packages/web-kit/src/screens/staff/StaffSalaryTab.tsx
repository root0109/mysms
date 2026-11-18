import { useI18n, useStaffSalaries, type StaffSalary } from '@mysms/core';
import { Banner, Button, Card, DataTable, type Column } from '@mysms/ui-web';
import { staffProblem } from './staffText';

/**
 * HR-02 "Salary": the staff member's salary rows as payroll keeps them (payroll.ListStaffSalaries), latest effective
 * first, read-only — salaries are changed in payroll's own screen (HR-06). Shown only to a role that may read them.
 */
export function StaffSalaryTab({ staffId }: { staffId: string }) {
  const { t, format } = useI18n();
  const salaries = useStaffSalaries({ staffId });
  const columns: Column<StaffSalary>[] = [
    {
      key: 'effective',
      header: t('hr.staff.salary.effective'),
      render: (row) => format.day(row.effectiveDate),
    },
    {
      key: 'gross',
      header: t('hr.staff.salary.gross'),
      align: 'end',
      render: (row) => format.number(Number(row.grossSalary)),
    },
    { key: 'remarks', header: t('hr.staff.salary.remarks'), render: (row) => row.remarks ?? '—' },
  ];
  return (
    <Card title={t('hr.staff.salary.title')}>
      <p className="hint">{t('hr.staff.salary.readOnly')}</p>
      {salaries.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void salaries.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {staffProblem(t, salaries.error)}
        </Banner>
      ) : (
        <DataTable
          caption={t('hr.staff.salary.title')}
          columns={columns}
          rows={salaries.data?.items}
          rowKey={(row) => row.id ?? row.effectiveDate}
          loading={salaries.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('hr.staff.salary.none')}
        />
      )}
    </Card>
  );
}
