import {
  isApiError,
  localDay,
  resourceDef,
  useCan,
  useClassRoster,
  useClassSectionOptions,
  useClassSubjects,
  useI18n,
  useRecord,
  useRoomNames,
  useStaffNames,
  useStudentNames,
  useSubjectNames,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, SegmentedControl, Skeleton } from '@mysms/ui-web';
import { Link, useParams, useSearchParams } from 'react-router';
import { UUID } from '../ids';
import { RelatedRecords } from '../RelatedRecords';
import { WeekView } from '../timetable/WeekView';

type Tab = 'students' | 'subjects' | 'timetable' | 'attendance' | 'homework';
const TAB_LABEL: Readonly<Record<Tab, MessageKey>> = {
  students: 'aca.class.tab.students',
  subjects: 'aca.class.tab.subjects',
  timetable: 'aca.class.tab.timetable',
  attendance: 'aca.class.tab.attendance',
  homework: 'aca.class.tab.homework',
};

/** The students placed in the class now (sis.ListClassRoster), by roll number, each opening their 360° page. */
function Students({ classSectionId }: { classSectionId: string }) {
  const { t } = useI18n();
  const roster = useClassRoster(classSectionId);
  const items = roster.data?.items ?? [];
  const names = useStudentNames(items.map((row) => row.studentId));
  if (roster.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void roster.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return (
    <Card title={t('aca.class.tab.students')}>
      <DataTable
        caption={t('common.list.count', { count: items.length })}
        columns={[
          { key: 'roll', header: t('aca.class.roll'), render: (row) => row.rollNumber ?? '—' },
          {
            key: 'name',
            header: t('aca.class.student'),
            render: (row) => (
              <Link to={`/app/sis/students/${row.studentId}`}>
                {names.get(row.studentId) ?? t('aca.class.unnamed')}
              </Link>
            ),
          },
        ]}
        rows={[...items].sort((a, b) =>
          (a.rollNumber ?? '').localeCompare(b.rollNumber ?? '', undefined, { numeric: true }),
        )}
        rowKey={(row) => row.id ?? row.studentId}
        loading={roster.isPending}
        loadingLabel={t('common.state.loading')}
        emptyText={t('aca.class.noStudents')}
      />
    </Card>
  );
}

/** The class's subjects and who teaches each, with their periods a week; a role without assignments sees subjects. */
function Subjects({ classSectionId }: { classSectionId: string }) {
  const { t, format } = useI18n();
  const withTeachers = useCan('academics.teaching_assignment.read');
  const lines = useClassSubjects(classSectionId, withTeachers);
  const subjectNames = useSubjectNames(useCan('academics.subject.read'));
  const staffNames = useStaffNames(
    (lines.data ?? []).flatMap((line) => line.teachers.map((teacher) => teacher.staffId)),
    useCan('hr.staff.read'),
  );
  if (lines.isError) return <Banner intent="danger">{t('common.error.generic')}</Banner>;
  return (
    <Card title={t('aca.class.tab.subjects')}>
      <DataTable
        caption={t('common.list.count', { count: lines.data?.length ?? 0 })}
        columns={[
          {
            key: 'subject',
            header: t('aca.class.subject'),
            render: (line) => subjectNames.get(line.subjectId) ?? t('aca.class.unnamedSubject'),
          },
          {
            key: 'teachers',
            header: t('aca.class.teachers'),
            render: (line) =>
              !withTeachers
                ? t('aca.class.teachersHidden')
                : line.teachers.length === 0
                  ? t('aca.class.noTeacher')
                  : line.teachers
                      .map((teacher) =>
                        teacher.periodsPerWeek === undefined
                          ? (staffNames.get(teacher.staffId) ?? t('aca.class.unnamedTeacher'))
                          : t('aca.class.teacherPeriods', {
                              name: staffNames.get(teacher.staffId) ?? t('aca.class.unnamedTeacher'),
                              periods: format.number(teacher.periodsPerWeek),
                            }),
                      )
                      .join(', '),
          },
        ]}
        rows={(lines.data ?? []).filter((line) => line.isActive)}
        rowKey={(line) => line.id}
        loading={lines.isPending}
        loadingLabel={t('common.state.loading')}
        emptyText={t('aca.class.noSubjects')}
      />
    </Card>
  );
}

/**
 * ACA-06 Class section workspace: one class — its students, its subjects and teachers, its week of timetable, its
 * attendance registers and its homework — each tab shown only to a role that may read it. The class is named by its
 * grade and section; a class the server does not show the caller (another tenant, outside their scope) is not
 * available.
 */
export function ClassSectionScreen() {
  const { t, timeZone } = useI18n();
  const { classSectionId: param = '' } = useParams();
  const [search, setSearch] = useSearchParams();
  const classSectionId = UUID.test(param) ? param : null;
  const mayRead = useCan('academics.class_section.read');
  const read = useRecord(resourceDef('academics.ClassSection'), mayRead ? classSectionId : null);
  const row = read.data?.data;
  const mayGrades = useCan('academics.grade_level.read');
  const maySections = useCan('academics.section.read');
  const options = useClassSectionOptions(
    typeof row?.institutionId === 'string' ? row.institutionId : null,
    mayGrades && maySections,
  );
  const may: Readonly<Record<Tab, boolean>> = {
    students: useCan('sis.student_enrollment.list_class_roster'),
    subjects: useCan('academics.class_subject.read'),
    timetable: useCan('timetable.session.get_timetable'),
    attendance: useCan('attendance.attendance_register.read'),
    homework: useCan('assessment.homework.read'),
  };
  const tabs = (Object.keys(TAB_LABEL) as Tab[]).filter((tab) => may[tab]);
  const asked = search.get('tab') as Tab | null;
  const tab = asked && tabs.includes(asked) ? asked : tabs[0];
  const subjectNames = useSubjectNames(useCan('academics.subject.read'));
  const roomNames = useRoomNames(useCan('facility.room.read') && tab === 'timetable');
  const today = localDay(new Date(), timeZone);
  const week = search.get('week') ?? today;
  const go = (changes: Record<string, string>) =>
    setSearch(
      (previous) => {
        const next = new URLSearchParams(previous);
        for (const [key, value] of Object.entries(changes)) next.set(key, value);
        return next;
      },
      { replace: true },
    );
  if (!mayRead) return <Banner intent="info">{t('adm.resource.noAccess')}</Banner>;
  if (!classSectionId || (isApiError(read.error) && read.error.status === 404))
    return <Banner intent="warning">{t('common.state.notAvailable')}</Banner>;
  if (read.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (read.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void read.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const label =
    options.data?.find((option) => option.id === classSectionId)?.label ?? t('aca.class.unnamedClass');
  const scope = `classSectionId = "${classSectionId}"`;
  return (
    <div className="page">
      <header className="page__header">
        <p className="hint">
          <Link to="/app/academics/class-sections">{t('common.action.back')}</Link>
        </p>
        <h1 className="page__title">{label}</h1>
        {typeof row?.capacity === 'number' ? (
          <p className="page__subtitle">{t('aca.class.capacity', { count: row.capacity })}</p>
        ) : null}
      </header>
      {tabs.length === 0 || !tab ? (
        <Banner intent="info">{t('aca.class.noTabs')}</Banner>
      ) : (
        <>
          <SegmentedControl
            label={t('adm.detail.tabs')}
            options={tabs.map((value) => ({ value, label: t(TAB_LABEL[value]) }))}
            value={tab}
            onChange={(value) => go({ tab: value })}
          />
          {tab === 'students' ? <Students classSectionId={classSectionId} /> : null}
          {tab === 'subjects' ? <Subjects classSectionId={classSectionId} /> : null}
          {tab === 'timetable' ? (
            <WeekView
              subjects={[{ classSectionId }]}
              week={week}
              today={today}
              onWeek={(day) => go({ week: day })}
              describe={(session) => ({
                what:
                  session.title ||
                  (session.subjectId && subjectNames.get(session.subjectId)) ||
                  t('tt.my.session'),
                where: (session.roomId && roomNames.get(session.roomId)) || '',
              })}
            />
          ) : null}
          {tab === 'attendance' ? (
            <RelatedRecords
              resource="attendance.AttendanceRegister"
              title={t('aca.class.tab.attendance')}
              scope={scope}
              presets={{ classSectionId, registerScope: 'class_section', institutionId: row?.institutionId }}
              columns={['name', 'code', 'registerScope', 'isActive']}
            />
          ) : null}
          {tab === 'homework' ? (
            <RelatedRecords
              resource="assessment.Homework"
              title={t('aca.class.tab.homework')}
              scope={scope}
              columns={['subjectId', 'dueAt', 'status']}
              names={{ subjectId: subjectNames }}
              filterField="status"
              rowLink={(homework) => `/app/assessment/homeworks/${homework.id}`}
            />
          ) : null}
        </>
      )}
    </div>
  );
}
