import {
  RESOURCES,
  isApiError,
  buildPath,
  useCan,
  useExamResults,
  useExamStep,
  useI18n,
  useInfiniteRecords,
  useMarksheetLines,
  useRecord,
  useRecordExamMarks,
  useStudentNames,
  useStudentResults,
  type MarkEntry,
  type RecordRow,
  type ResultCard,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  SegmentedControl,
  Skeleton,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useEffect, useState } from 'react';
import { useOnline } from './assessmentText';
import { mergeRows, RECORDED, sameAs, type MarkRow } from './marksGrid';
import { ErrorState, OfflineBanner } from './assessmentUi';
import { Link, useParams } from 'react-router';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { ManagedResourcePanel } from './ManagedResourcePanel';

export function ExamMarksScreen() {
  const { t } = useI18n();
  const { examId = '' } = useParams();
  const mayRead = useCan('assessment.exam_result.read');
  const mayReadExam = useCan('assessment.exam.read');
  const mayWrite = useCan('assessment.exam.record_exam_marks');
  const mayFinalize = useCan('assessment.exam.mark_exam_results_updated');
  const examDef = RESOURCES.find((item) => item.key === 'assessment.Exam');
  const exam = useRecord(mayReadExam ? examDef : undefined, mayReadExam ? examId : null);
  const results = useExamResults(mayRead ? examId : null);
  const saveMarks = useRecordExamMarks();
  const updateExam = useExamStep();
  const [rows, setRows] = useState<MarkRow[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [failureAction, setFailureAction] = useState<'save' | 'finalize'>('save');
  const online = useOnline();
  useEffect(() => {
    const items = results.data?.items;
    if (!items) return;
    setRows((current) => mergeRows(current, items));
  }, [results.data]);
  const names = useStudentNames(rows.map((row) => row.studentId));
  if (!mayRead || !mayReadExam)
    return (
      <Card title={t('screen.ASM10.title')}>
        <p className="hint">{t('asm.web.noAccess')}</p>
      </Card>
    );
  if (exam.isPending || results.isPending) return <Skeleton label={t('common.state.loading')} lines={7} />;
  if (exam.isError) return <ErrorState error={exam.error} retry={() => void exam.refetch()} />;
  if (results.isError) return <ErrorState error={results.error} retry={() => void results.refetch()} />;
  const detail = exam.data?.data as RecordRow | undefined;
  if (!detail)
    return (
      <Card title={t('screen.ASM10.title')}>
        <p>{t('asm.web.noExam')}</p>
      </Card>
    );
  // marks are entered once the exam is held, and corrected after results were updated (that reopens entry)
  const canEnter = mayWrite && (detail.status === 'held' || detail.status === 'result_updated');
  const onField = (studentId: string, key: 'marksObtained' | 'note', value: string) => {
    setRows((current) =>
      current.map((row) => (row.studentId === studentId ? { ...row, [key]: value } : row)),
    );
  };
  const onAttendance = (studentId: string, value: string) => {
    if (!RECORDED.includes(value)) return;
    setRows((current) =>
      current.map((row) => {
        if (row.studentId !== studentId) return row;
        // only a present student has marks
        const { marksObtained: _dropped, ...rest } = row;
        return value === 'present'
          ? { ...row, attendanceStatus: value }
          : { ...rest, attendanceStatus: value };
      }),
    );
  };
  const dirtyRows = rows.filter((row) => !sameAs(row, row.existing));
  const canSave =
    canEnter &&
    online &&
    dirtyRows.length > 0 &&
    dirtyRows.every((row) => RECORDED.includes(row.attendanceStatus) && Boolean(row.existing.etag));
  const resultsAreValid =
    rows.length > 0 &&
    rows.every(
      (row) =>
        row.attendanceStatus !== 'expected' &&
        (row.attendanceStatus !== 'present' || Boolean(row.marksObtained?.trim())),
    );
  const canFinalize = mayFinalize && detail.status === 'held' && dirtyRows.length === 0 && resultsAreValid;
  const save = async () => {
    setFailure(null);
    setNotice(null);
    if (!canSave) return;
    try {
      await saveMarks.mutateAsync({
        examId,
        entries: dirtyRows.map((row) => ({
          studentId: row.studentId,
          attendanceStatus: row.attendanceStatus as MarkEntry['attendanceStatus'],
          ...(row.marksObtained?.trim() ? { marksObtained: row.marksObtained.trim() } : {}),
          ...(row.note?.trim() ? { note: row.note.trim() } : {}),
          // the version each row was read at: a row changed by someone else since is refused (412)
          etag: row.existing.etag as string,
        })),
      });
      setNotice(t('asm.native.markEntrySaved'));
      await Promise.all([results.refetch(), exam.refetch()]);
    } catch (error) {
      setFailureAction('save');
      setFailure(error);
      if (isApiError(error)) {
        const rowErrors: Record<string, string> = {};
        for (const issue of error.errors) {
          const match = /^entries\[(\d+)\]/.exec(issue.field);
          const studentId = match?.[1] ? dirtyRows[Number(match[1])]?.studentId : undefined;
          if (studentId) rowErrors[studentId] = issue.message;
        }
        setRows((current) => current.map((row) => ({ ...row, error: rowErrors[row.studentId] })));
      }
    }
  };
  const finalize = async () => {
    setFailure(null);
    setNotice(null);
    const etag = exam.data?.etag;
    if (!canFinalize || !online || !etag) return;
    try {
      await updateExam.mutateAsync({ examId, step: 'resultsUpdated', etag });
      setNotice(t('asm.web.resultsUpdated'));
      await exam.refetch();
    } catch (error) {
      setFailureAction('finalize');
      setFailure(error);
    }
  };
  const columns: Column<MarkRow>[] = [
    {
      key: 'student',
      header: t('asm.native.student'),
      render: (row) => names.get(row.studentId) ?? row.studentId,
    },
    {
      key: 'attendance',
      header: t('asm.native.attendance'),
      render: (row) => (
        <SelectField
          label={t('asm.native.attendance')}
          value={row.attendanceStatus}
          disabled={!canEnter}
          options={[
            { value: 'expected', label: t('asm.web.attendanceExpected') },
            { value: 'present', label: t('enum.attendance.present') },
            { value: 'absent', label: t('enum.attendance.absent') },
            { value: 'exempted', label: t('enum.attendance.exempted') },
          ]}
          onChange={(event) => onAttendance(row.studentId, event.target.value)}
        />
      ),
    },
    {
      key: 'marks',
      header: t('asm.native.marks'),
      align: 'end',
      render: (row) => (
        <>
          <TextInput
            label={t('asm.native.marks')}
            inputMode="decimal"
            value={row.marksObtained ?? ''}
            disabled={!canEnter || row.attendanceStatus !== 'present'}
            onChange={(event) => onField(row.studentId, 'marksObtained', event.target.value)}
          />
          <span className="hint">
            {t('asm.native.maxMarks')}: {row.existing.maxMarks ?? t('common.state.notAvailable')}
          </span>
        </>
      ),
    },
    {
      key: 'note',
      header: t('asm.native.note'),
      render: (row) => (
        <>
          <TextInput
            label={t('asm.native.note')}
            value={row.note ?? ''}
            disabled={!canEnter}
            onChange={(event) => onField(row.studentId, 'note', event.target.value)}
          />
          {row.error ? (
            <p className="sms-field__error" role="alert">
              {row.error}
            </p>
          ) : null}
        </>
      ),
    },
  ];
  return (
    <div className="page">
      <OfflineBanner />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM10.title')}</h1>
        <p className="page__subtitle">{String(detail.name ?? detail.title ?? t('screen.ASM10.title'))}</p>
        <Link to="/app/assessment/exams">{t('screen.ASM09.title')}</Link>
      </header>
      <ManagedResourcePanel
        resource="assessment.ExamInvigilator"
        title={t('asm.web.examInvigilators')}
        parentId={examId}
      />
      <Card title={t('asm.web.marksGrid')}>
        {!mayWrite ? <p className="hint">{t('asm.web.noAccess')}</p> : null}
        {!canEnter && mayWrite ? <p className="hint">{t('asm.web.markEntryHeldOnly')}</p> : null}
        <DataTable
          caption={t('asm.web.marksGrid')}
          rows={rows}
          rowKey={(row) => row.studentId}
          loading={false}
          loadingLabel={t('common.state.loading')}
          emptyText={t('asm.web.noMarkRows')}
          columns={columns}
        />
        {mayWrite ? (
          <Button
            loading={saveMarks.isPending}
            disabled={!canSave || saveMarks.isPending}
            onClick={() => void save()}
          >
            {t('asm.native.saveMarks')}
          </Button>
        ) : null}
        {canFinalize ? (
          <Button
            variant="secondary"
            loading={updateExam.isPending}
            disabled={!online}
            onClick={() => void finalize()}
          >
            {t('asm.web.resultsUpdated')}
          </Button>
        ) : null}
      </Card>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {failure ? (
        <ErrorState error={failure} retry={() => void (failureAction === 'finalize' ? finalize() : save())} />
      ) : null}
    </div>
  );
}

function TemplateBandsPanel() {
  const { t } = useI18n();
  const templateDef = RESOURCES.find((item) => item.key === 'assessment.ResultTemplate');
  const templates = useInfiniteRecords(templateDef, { size: 100 });
  const [templateId, setTemplateId] = useState('');
  const rows = templates.data?.pages.flatMap((page) => page.data) ?? [];
  return (
    <>
      {templates.isPending ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {templates.isError ? (
        <ErrorState error={templates.error} retry={() => void templates.refetch()} />
      ) : null}
      <SelectField
        label={t('asm.web.template')}
        value={templateId}
        onChange={(event) => setTemplateId(event.target.value)}
        options={[
          { value: '', label: t('asm.web.chooseTemplate') },
          ...rows.map((row) => ({ value: row.id, label: String(row.name ?? row.id) })),
        ]}
      />
      {templateId ? (
        <ManagedResourcePanel
          resource="assessment.ResultTemplateBand"
          title={t('asm.web.templateBands')}
          parentId={templateId}
        />
      ) : null}
    </>
  );
}

function MarksheetLinesPanel() {
  const { t } = useI18n();
  const mayReadSheets = useCan('assessment.marksheet.read');
  const mayReadLines = useCan('assessment.marksheet_line.read');
  const markSheetDef = RESOURCES.find((item) => item.key === 'assessment.Marksheet');
  const sheets = useInfiniteRecords(mayReadSheets ? markSheetDef : undefined, { size: 100 });
  const [marksheetId, setMarksheetId] = useState('');
  const lines = useMarksheetLines(mayReadLines ? marksheetId || null : null);
  const mayReadResults = useCan('assessment.marksheet_line.get_student_results');
  const names = useStudentNames(marksheetId ? (lines.data?.items ?? []).map((line) => line.studentId) : []);
  const sheetRows = sheets.data?.pages.flatMap((page) => page.data) ?? [];
  if (!mayReadSheets || !mayReadLines)
    return (
      <Card title={t('asm.web.marksheetLines')}>
        <p className="hint">{t('adm.records.forbidden')}</p>
      </Card>
    );
  if (sheets.isPending) return <Skeleton label={t('common.state.loading')} lines={4} />;
  if (sheets.isError) return <ErrorState error={sheets.error} retry={() => void sheets.refetch()} />;
  const lineRows = lines.data?.items ?? [];
  const columns: Column<(typeof lineRows)[number]>[] = [
    {
      key: 'student',
      header: t('asm.native.student'),
      // staff open the student's report card (ASM-12) from the line
      render: (row) => {
        const name = names.get(row.studentId) ?? row.studentId;
        return mayReadResults ? (
          <Link to={'/app' + buildPath('/assessment/results/{studentId}', { studentId: row.studentId })}>
            {name}
          </Link>
        ) : (
          name
        );
      },
    },
    {
      key: 'total',
      header: t('asm.native.marks'),
      align: 'end',
      render: (row) => row.totalMarks ?? t('common.state.notAvailable'),
    },
    {
      key: 'percent',
      header: t('asm.web.percentage'),
      align: 'end',
      render: (row) => row.percentage ?? t('common.state.notAvailable'),
    },
    {
      key: 'grade',
      header: t('asm.native.grade'),
      render: (row) => row.gradeLabel ?? t('common.state.notAvailable'),
    },
    {
      key: 'result',
      header: t('asm.native.currentStatus'),
      render: (row) => enumLabel(t, 'assessment.status', row.resultStatus),
    },
  ];
  return (
    <Card title={t('asm.web.marksheetLines')}>
      <SelectField
        label={t('asm.web.marksheets')}
        value={marksheetId}
        onChange={(event) => setMarksheetId(event.target.value)}
        options={[
          { value: '', label: t('asm.web.chooseMarksheet') },
          ...sheetRows.map((row) => ({ value: row.id, label: String(row.name ?? row.id) })),
        ]}
      />
      {lines.isPending && marksheetId ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {lines.isError ? <ErrorState error={lines.error} retry={() => void lines.refetch()} /> : null}
      <DataTable
        caption={t('asm.web.marksheetLines')}
        rows={marksheetId ? lineRows : []}
        rowKey={(row) => row.id ?? row.studentId}
        loading={false}
        loadingLabel={t('common.state.loading')}
        emptyText={t('asm.native.noLines')}
        columns={columns}
      />
    </Card>
  );
}
export function MarksheetsScreen() {
  const { t } = useI18n();
  const [tab, setTab] = useState<'templates' | 'marksheets' | 'bands'>('templates');
  const markSheetScreen = RESOURCE_SCREENS.find((screen) => screen.id === 'ASM-11');
  const templateScreen = {
    id: 'ASM-11',
    resource: 'assessment.ResultTemplate',
    group: 'assessment' as const,
    tool: 'generateResults' as const,
  };
  const options = [
    { value: 'templates', label: t('asm.web.marksheetTemplates') },
    { value: 'marksheets', label: t('asm.web.marksheets') },
    { value: 'bands', label: t('asm.web.templateBands') },
  ];
  return (
    <div className="page">
      <OfflineBanner />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM11.title')}</h1>
      </header>
      <SegmentedControl
        label={t('screen.ASM11.title')}
        options={options}
        value={tab}
        onChange={(value) => setTab(value as typeof tab)}
      />
      {tab === 'templates' ? <ResourceScreen screen={templateScreen} /> : null}
      {tab === 'marksheets' && markSheetScreen ? (
        <>
          <ResourceScreen screen={markSheetScreen} />
          <MarksheetLinesPanel />
        </>
      ) : null}
      {tab === 'bands' ? (
        <Card title={t('asm.web.templateBands')}>
          <TemplateBandsPanel />
        </Card>
      ) : null}
    </div>
  );
}

type StudentResultRow = {
  id: string;
  session: string;
  student: string;
  total: string;
  percentage: string;
  grade: string;
  status: string;
  sgpa: string;
  cgpa: string;
};

function resultRows(cards: ResultCard[]): StudentResultRow[] {
  return cards.flatMap((card, index) => {
    const line = card.marksheetLine;
    if (line)
      return [
        {
          id: line.id ?? card.examSessionId + '-' + index,
          session: card.examSessionId,
          student: line.studentId,
          total: String(line.totalMarks ?? ''),
          percentage: String(line.percentage ?? ''),
          grade: String(line.gradeLabel ?? ''),
          status: String(line.resultStatus ?? ''),
          sgpa: String(line.sgpa ?? ''),
          cgpa: String(line.cgpa ?? ''),
        },
      ];
    return (card.results ?? []).map((result) => ({
      id: result.id ?? card.examSessionId + '-' + result.studentId,
      session: card.examSessionId,
      student: result.studentId,
      total: String(result.marks ?? ''),
      percentage: '',
      grade: String(result.gradeLabel ?? ''),
      status: String(result.resultStatus ?? ''),
      sgpa: '',
      cgpa: '',
    }));
  });
}

export function StudentResultsScreen() {
  const { t } = useI18n();
  const { studentId = '' } = useParams();
  const mayRead = useCan('assessment.marksheet_line.get_student_results');
  // staff read every validated result of a student of their school, published or not; students and guardians
  // read published ones only (the service decides; the screen says which view it shows)
  const staff = useCan('assessment.marksheet_line.read');
  const results = useStudentResults(mayRead ? studentId : null);
  const names = useStudentNames(mayRead && studentId ? [studentId] : []);
  const rows = resultRows(results.data?.items ?? []);
  if (!mayRead)
    return (
      <Card title={t('screen.ASM12.title')}>
        <p className="hint">{t('asm.web.noAccess')}</p>
      </Card>
    );
  if (results.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (results.isError) return <ErrorState error={results.error} retry={() => void results.refetch()} />;
  const columns: Column<StudentResultRow>[] = [
    { key: 'session', header: t('asm.native.examSession'), render: (row) => row.session },
    {
      key: 'student',
      header: t('asm.native.student'),
      render: (row) => names.get(row.student) ?? row.student,
    },
    {
      key: 'total',
      header: t('asm.native.marks'),
      align: 'end',
      render: (row) => row.total || t('common.state.notAvailable'),
    },
    { key: 'percentage', header: t('asm.web.percentage'), align: 'end', render: (row) => row.percentage },
    { key: 'grade', header: t('asm.native.grade'), render: (row) => row.grade },
    {
      key: 'status',
      header: t('asm.native.currentStatus'),
      render: (row) => enumLabel(t, 'assessment.status', row.status),
    },
    { key: 'sgpa', header: t('asm.native.sgpa'), align: 'end', render: (row) => row.sgpa },
    { key: 'cgpa', header: t('asm.native.cgpa'), align: 'end', render: (row) => row.cgpa },
  ];
  return (
    <div className="page">
      <OfflineBanner />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM12.title')}</h1>
        <p className="page__subtitle">{names.get(studentId) ?? studentId}</p>
        {staff ? <Link to="/app/assessment/marksheets">{t('screen.ASM11.title')}</Link> : null}
      </header>
      {staff ? <p className="hint">{t('asm.web.staffResults')}</p> : null}
      <Card title={t(staff ? 'asm.web.validatedResults' : 'asm.web.studentResults')}>
        <DataTable
          caption={t(staff ? 'asm.web.validatedResults' : 'asm.web.studentResults')}
          rows={rows}
          rowKey={(row) => row.id}
          loading={false}
          loadingLabel={t('common.state.loading')}
          emptyText={t('asm.web.noResults')}
          columns={columns}
        />
      </Card>
    </div>
  );
}
