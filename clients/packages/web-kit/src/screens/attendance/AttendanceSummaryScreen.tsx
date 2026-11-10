import {
  MAX_RANGE_DAYS,
  buildPath,
  screenById,
  useAttendanceSummary,
  useCan,
  useClassSectionOptions,
  useI18n,
  useInstitutions,
  useStudentNames,
  type AttendanceSummary,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  StatusChip,
  Switch,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { isoDay } from '../dates';
import { percentOf } from './status';

/** Rows shown at once; the names of the students are read only for the rows on screen. */
const PAGE = 100;
const DEFAULT_THRESHOLD = '75';
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * ATT-05 Class summary & defaulters: every student's attendance over a date range, for an institution or one of its
 * classes, lowest first, with the students below a chosen minimum marked. Only completed sheets count, and only
 * inside the institutions the caller's role covers.
 */
export function AttendanceSummaryScreen() {
  const { t, format } = useI18n();
  const navigate = useNavigate();
  const maySummarize = useCan('attendance.attendance_record.summarize_attendance');
  const mayListInstitutions = useCan('org.institution.read');
  const mayReadClasses = useCan('academics.class_section.read');
  const mayReadGrades = useCan('academics.grade_level.read');
  const mayReadSections = useCan('academics.section.read');
  const mayOpenStudent = useCan('sis.student.read');
  const allowed = maySummarize && mayListInstitutions;
  const institutions = useInstitutions(allowed);
  const [chosenInstitution, setChosenInstitution] = useState('');
  const [classSectionId, setClassSectionId] = useState('');
  const [from, setFrom] = useState(() => isoDay(-30));
  const [to, setTo] = useState(() => isoDay(0));
  const [threshold, setThreshold] = useState(DEFAULT_THRESHOLD);
  const [onlyBelow, setOnlyBelow] = useState(false);
  const [shown, setShown] = useState(PAGE);

  const institutionOptions = (institutions.data?.items ?? []).flatMap((institution) =>
    institution.id ? [{ value: institution.id, label: institution.name }] : [],
  );
  const institutionId = chosenInstitution || institutionOptions[0]?.value || null;
  const classes = useClassSectionOptions(institutionId, mayReadClasses && mayReadGrades && mayReadSections);
  const span = (Date.parse(to) - Date.parse(from)) / 86_400_000;
  const rangeOk = ISO_DATE.test(from) && ISO_DATE.test(to) && span >= 0 && span < MAX_RANGE_DAYS;
  const minimum = Number(threshold);
  const thresholdOk = threshold.trim() !== '' && minimum >= 0 && minimum <= 100;
  const summary = useAttendanceSummary(
    allowed && institutionId && rangeOk
      ? {
          institutionId,
          fromDate: from,
          toDate: to,
          groupBy: 'student',
          ...(classSectionId ? { classSectionId } : {}),
        }
      : null,
  );
  const isBelow = (row: AttendanceSummary) => thresholdOk && percentOf(row) < minimum;
  const students = (summary.data?.items ?? [])
    .filter((row) => row.studentId !== undefined)
    .sort((a, b) => percentOf(a) - percentOf(b));
  const below = students.filter(isBelow);
  const listed = onlyBelow ? below : students;
  const visible = listed.slice(0, shown);
  const names = useStudentNames(visible.map((row) => row.studentId as string));
  const title = t('screen.ATT05.title');
  const studentRoute = screenById('ATT-04')?.routes[0];

  if (!allowed)
    return (
      <Card title={title}>
        <p className="hint">{t('att.summary.noAccess')}</p>
      </Card>
    );
  if (institutions.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (institutions.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void institutions.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  if (institutionOptions.length === 0)
    return (
      <Card title={title}>
        <p>{t('att.summary.noInstitutions')}</p>
      </Card>
    );

  const position = new Map(visible.map((row, index) => [row.studentId, index + 1]));
  const nameOf = (row: AttendanceSummary) =>
    names.get(row.studentId as string) ?? t('att.take.student', { number: position.get(row.studentId) ?? 0 });
  const count = (key: 'present' | 'absent' | 'late' | 'excused'): Column<AttendanceSummary> => ({
    key,
    header: t(`enum.attendance.${key}`),
    align: 'end',
    render: (row) => format.number(row[key]),
  });
  const columns: Column<AttendanceSummary>[] = [
    { key: 'student', header: t('att.summary.student'), render: nameOf },
    count('present'),
    count('absent'),
    count('late'),
    count('excused'),
    {
      key: 'percent',
      header: t('att.summary.percent'),
      align: 'end',
      render: (row) => format.percentExact(percentOf(row) / 100),
    },
    {
      key: 'flag',
      header: t('att.summary.threshold'),
      render: (row) =>
        isBelow(row) ? <StatusChip intent="danger" label={t('att.summary.belowChip')} /> : null,
    },
  ];
  const reset = () => setShown(PAGE);

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('att.summary.intro')}</p>
      </header>
      <Card>
        <div className="form">
          <SelectField
            label={t('att.summary.institution')}
            options={institutionOptions}
            value={institutionId ?? ''}
            onChange={(event) => {
              setChosenInstitution(event.target.value);
              setClassSectionId('');
              reset();
            }}
          />
          {classes.data && classes.data.length > 0 ? (
            <SelectField
              label={t('att.summary.classSection')}
              options={[
                { value: '', label: t('att.summary.allClasses') },
                ...classes.data.map((option) => ({ value: option.id, label: option.label })),
              ]}
              value={classSectionId}
              onChange={(event) => {
                setClassSectionId(event.target.value);
                reset();
              }}
            />
          ) : null}
          <TextInput
            label={t('att.summary.from')}
            type="date"
            value={from}
            onChange={(event) => {
              setFrom(event.target.value);
              reset();
            }}
          />
          <TextInput
            label={t('att.summary.to')}
            type="date"
            value={to}
            error={rangeOk ? undefined : t('att.summary.rangeInvalid', { days: MAX_RANGE_DAYS })}
            onChange={(event) => {
              setTo(event.target.value);
              reset();
            }}
          />
          <TextInput
            label={t('att.summary.threshold')}
            type="number"
            inputMode="decimal"
            min={0}
            max={100}
            value={threshold}
            error={thresholdOk ? undefined : t('att.summary.thresholdInvalid')}
            onChange={(event) => setThreshold(event.target.value)}
          />
          <Switch
            label={t('att.summary.onlyBelow')}
            checked={onlyBelow}
            disabled={!thresholdOk}
            onChange={(checked) => {
              setOnlyBelow(checked);
              reset();
            }}
          />
        </div>
        {!rangeOk ? null : summary.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void summary.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {t('common.error.generic')}
          </Banner>
        ) : (
          <>
            {summary.isPending || !thresholdOk ? null : (
              <p role="status">
                {t('att.summary.below', { count: below.length, threshold: format.number(minimum) })}
              </p>
            )}
            <DataTable
              caption={t('att.summary.caption', { count: listed.length })}
              columns={columns}
              rows={visible}
              rowKey={(row) => row.studentId as string}
              loading={summary.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('att.summary.empty')}
              {...(mayOpenStudent && studentRoute
                ? {
                    onRowClick: (row: AttendanceSummary) =>
                      void navigate(`/app${buildPath(studentRoute, { studentId: row.studentId as string })}`),
                  }
                : {})}
            />
            {listed.length > shown ? (
              <Button variant="ghost" onClick={() => setShown(shown + PAGE)}>
                {t('common.list.more')}
              </Button>
            ) : null}
          </>
        )}
      </Card>
    </div>
  );
}
