import {
  isApiError,
  useAttendanceSummary,
  useCan,
  useI18n,
  useStudent,
  useStudentNames,
  type AttendanceStatus,
  type AttendanceSummary,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, Skeleton, StatusChip, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { isoDay } from '../dates';
import { UUID } from '../ids';
import { STATUS_INTENT } from './status';

const MONTH = /^\d{4}-(0[1-9]|1[0-2])$/;

/** The mark that describes a day with several sheets: any absence shows first, then lateness, then an excuse. */
function dayStatus(day: AttendanceSummary): AttendanceStatus {
  if (day.absent > 0) return 'absent';
  if (day.late > 0) return 'late';
  if (day.excused > 0) return 'excused';
  return 'present';
}

/** The days of a month (`2026-03`) as ISO dates, and how many cells precede the 1st in a Monday-first week. */
function monthDays(month: string): { days: string[]; lead: number } {
  const [year = 0, monthNumber = 1] = month.split('-').map(Number);
  const count = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate();
  const days = Array.from({ length: count }, (_, index) => `${month}-${String(index + 1).padStart(2, '0')}`);
  const lead = (new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay() + 6) % 7;
  return { days, lead };
}

/**
 * ATT-04 Student attendance summary: one student's month as a calendar — every day with completed attendance shows
 * its mark in words and colour — with the month's percentage. The summary is read inside the caller's institutions;
 * a student the role may not see is simply not available.
 */
export function StudentAttendanceScreen() {
  const { t, format, formatLocale } = useI18n();
  const { studentId: param = '' } = useParams();
  const maySummarize = useCan('attendance.attendance_record.summarize_attendance');
  const mayReadStudent = useCan('sis.student.read');
  const studentId = maySummarize && mayReadStudent && UUID.test(param) ? param : null;
  const student = useStudent(studentId);
  const names = useStudentNames(studentId ? [studentId] : []);
  const [month, setMonth] = useState(() => isoDay(0).slice(0, 7));
  const monthOk = MONTH.test(month);
  const { days, lead } = monthDays(monthOk ? month : isoDay(0).slice(0, 7));
  const institutionId = student.data?.institutionId ?? null;
  const summary = useAttendanceSummary(
    studentId && institutionId && monthOk
      ? {
          institutionId,
          studentId,
          groupBy: 'date',
          fromDate: days[0] as string,
          toDate: days[days.length - 1] as string,
        }
      : null,
  );
  const title = t('screen.ATT04.title');
  const back = maySummarize ? <Link to="/app/attendance/summary">{t('att.student.back')}</Link> : null;

  if (!maySummarize || !mayReadStudent)
    return (
      <Card title={title}>
        <p className="hint">{t('att.summary.noAccess')}</p>
      </Card>
    );
  if (studentId === null || (isApiError(student.error) && student.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('att.student.unavailable')}</p>
        {back}
      </Card>
    );
  if (student.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (student.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void student.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );

  const byDay = new Map(
    (summary.data?.items ?? []).flatMap((item) => (item.date ? [[item.date, item]] : [])),
  );
  const totals = { present: 0, absent: 0, late: 0, excused: 0 };
  for (const item of byDay.values()) {
    totals.present += item.present;
    totals.absent += item.absent;
    totals.late += item.late;
    totals.excused += item.excused;
  }
  const marks = totals.present + totals.absent + totals.late + totals.excused;
  const monthLabel = new Intl.DateTimeFormat(formatLocale, {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(`${days[0] as string}T00:00:00Z`));
  const weekday = new Intl.DateTimeFormat(formatLocale, { weekday: 'short', timeZone: 'UTC' });
  // 2024-01-01 was a Monday: the seven days from it name the columns of a Monday-first week
  const weekdays = Array.from({ length: 7 }, (_, index) =>
    weekday.format(new Date(Date.UTC(2024, 0, 1 + index))),
  );

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{names.get(studentId) ?? title}</h1>
        <p className="page__subtitle">{[title, student.data.admissionNumber].filter(Boolean).join(' · ')}</p>
        {back}
      </header>
      <Card title={monthLabel}>
        <TextInput
          label={t('att.student.month')}
          type="month"
          value={month}
          error={monthOk ? undefined : t('common.validation.pattern')}
          onChange={(event) => setMonth(event.target.value)}
        />
        {summary.isPending && monthOk ? (
          <Skeleton label={t('common.state.loading')} lines={5} />
        ) : summary.isError ? (
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
        ) : monthOk ? (
          <>
            <p role="status">
              {marks === 0
                ? t('att.student.none')
                : t('att.student.total', {
                    percent: format.percentExact((totals.present + totals.late) / marks),
                    days: byDay.size,
                  })}
            </p>
            {marks > 0 ? <p className="hint">{t('att.take.counts', totals)}</p> : null}
            <div
              className="attendance-month"
              role="group"
              aria-label={t('att.student.calendar', { month: monthLabel })}
            >
              <div className="attendance-month__week" aria-hidden="true">
                {weekdays.map((name) => (
                  <span key={name} className="attendance-month__weekday">
                    {name}
                  </span>
                ))}
              </div>
              <ol className="attendance-month__days">
                {days.map((date, index) => {
                  const item = byDay.get(date);
                  const status = item ? dayStatus(item) : undefined;
                  const label = status ? enumLabel(t, 'attendance', status) : t('att.student.noRecord');
                  return (
                    <li
                      key={date}
                      className="attendance-month__day"
                      style={index === 0 ? { gridColumnStart: lead + 1 } : undefined}
                    >
                      <span aria-hidden="true" className="attendance-month__number">
                        {index + 1}
                      </span>
                      <span className="sr-only">
                        {t('att.student.day', { date: format.day(date), status: label })}
                      </span>
                      {status ? (
                        <span aria-hidden="true">
                          <StatusChip intent={STATUS_INTENT[status]} label={label} />
                        </span>
                      ) : null}
                    </li>
                  );
                })}
              </ol>
            </div>
          </>
        ) : null}
      </Card>
    </div>
  );
}
