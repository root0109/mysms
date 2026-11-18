/** Exam sessions and papers: their workflows, attendee population, room allocation and marks entry (ASM-08–10). */
import {
  RESOURCES,
  useAllocateExamRooms,
  useExamResults,
  useExamSessionStep,
  useExamStep,
  useInfiniteRecords,
  usePopulateExamResults,
  useRecord,
  useRecordExamMarks,
  useStudentNames,
  useCan,
  isApiError,
  useI18n,
  type Exam,
  type ExamSessionStep,
  type ExamStep,
  type ExamResult,
  type MarkEntry,
  type RecordRow,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, SwitchRow, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import type { AssessmentRoute } from '../assessmentRoutes';
import { QueryFeedback, WorkflowNotice, LoadingRows } from '../records';
import { firstText } from '../recordText';

export function ExamSessionWorkflow({ row, etag }: { row: RecordRow; etag: string | undefined }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const step = useExamSessionStep();
  const [reason, setReason] = useState('');
  const maySchedule = useCan('assessment.exam_session.schedule_exam_session');
  const mayHold = useCan('assessment.exam_session.mark_exam_session_held');
  const mayComplete = useCan('assessment.exam_session.complete_exam_session');
  const mayCancel = useCan('assessment.exam_session.cancel_exam_session');
  const mayReset = useCan('assessment.exam_session.reset_exam_session');
  const actions: {
    step: ExamSessionStep;
    permission: boolean;
    show: boolean;
    reason?: boolean;
    label: string;
  }[] = [
    { step: 'schedule', permission: maySchedule, show: status === 'draft', label: t('asm.native.schedule') },
    { step: 'markHeld', permission: mayHold, show: status === 'scheduled', label: t('asm.native.markHeld') },
    { step: 'complete', permission: mayComplete, show: status === 'held', label: t('asm.native.complete') },
    {
      step: 'cancel',
      permission: mayCancel,
      show: ['draft', 'scheduled'].includes(status),
      reason: true,
      label: t('common.action.cancel'),
    },
    { step: 'reset', permission: mayReset, show: status === 'cancelled', label: t('asm.native.reset') },
  ];
  const visible = actions.filter((action) => action.permission && action.show);
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {visible.some((action) => action.reason) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {visible.map((action) => (
        <Button
          key={action.step}
          loading={step.isPending}
          disabled={!etag}
          onPress={() =>
            etag &&
            step.mutate({
              examSessionId: row.id,
              step: action.step,
              etag,
              ...(action.reason && reason.trim() ? { reason } : {}),
            })
          }
        >
          {action.label}
        </Button>
      ))}
      <WorkflowNotice mutation={step} />
    </>
  );
}

export function ExamSessionExamsPanel({
  examSessionId,
  onNavigate,
}: {
  examSessionId: string;
  onNavigate: (route: AssessmentRoute) => void;
}) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.exam.read');
  const def = RESOURCES.find((resource) => resource.key === 'assessment.Exam');
  const examsQuery = useInfiniteRecords(mayRead ? def : undefined, {
    filter: `examSessionId = "${examSessionId}"`,
  });
  const exams = examsQuery.data?.pages.flatMap((page) => page.data) ?? [];
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.exams')}</Text>
      <QueryFeedback
        query={examsQuery}
        hasCachedRows={exams.length > 0}
        onRetry={() => void examsQuery.refetch()}
      />
      {examsQuery.isPending ? <LoadingRows tablet /> : null}
      {!examsQuery.isPending && !examsQuery.isError && exams.length === 0 ? (
        <Banner>{t('asm.native.noExams')}</Banner>
      ) : null}
      {exams.map((exam) => (
        <Button
          key={exam.id}
          variant="secondary"
          onPress={() => onNavigate({ screenId: 'ASM-09', params: { examId: exam.id } })}
        >
          {firstText(exam, ['name', 'code']) ?? exam.id}
        </Button>
      ))}
    </>
  );
}

export function ExamWorkflow({ row, etag }: { row: RecordRow; etag: string | undefined }) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const step = useExamStep();
  const populate = usePopulateExamResults();
  const allocate = useAllocateExamRooms();
  const [reason, setReason] = useState('');
  const [roomIds, setRoomIds] = useState<string[]>([]);
  const maySchedule = useCan('assessment.exam.schedule_exam');
  const mayHold = useCan('assessment.exam.mark_exam_held');
  const mayResultsUpdated = useCan('assessment.exam.mark_exam_results_updated');
  const mayComplete = useCan('assessment.exam.complete_exam');
  const mayCancel = useCan('assessment.exam.cancel_exam');
  const mayReset = useCan('assessment.exam.reset_exam');
  const mayPopulate = useCan('assessment.exam.populate_exam_results');
  const mayAllocate = useCan('assessment.exam.allocate_exam_rooms');
  const mayReadResults = useCan('assessment.exam_result.read');
  const mayReadRooms = useCan('facility.room.read');
  const resultQuery = useExamResults(mayReadResults ? row.id : null);
  const results = resultQuery.data?.items ?? [];
  const roomDef = RESOURCES.find((resource) => resource.key === 'facility.Room');
  const roomsQuery = useInfiniteRecords(mayReadRooms ? roomDef : undefined, { size: 100 });
  const rooms = roomsQuery.data?.pages.flatMap((page) => page.data) ?? [];
  const actions: { step: ExamStep; permission: boolean; show: boolean; reason?: boolean; label: string }[] = [
    { step: 'schedule', permission: maySchedule, show: status === 'draft', label: t('asm.native.schedule') },
    { step: 'markHeld', permission: mayHold, show: status === 'scheduled', label: t('asm.native.markHeld') },
    {
      step: 'resultsUpdated',
      permission: mayResultsUpdated,
      show: status === 'held',
      label: t('asm.native.markUpdated'),
    },
    {
      step: 'complete',
      permission: mayComplete,
      show: ['held', 'result_updated'].includes(status),
      label: t('asm.native.complete'),
    },
    {
      step: 'cancel',
      permission: mayCancel,
      show: ['draft', 'scheduled'].includes(status),
      reason: true,
      label: t('common.action.cancel'),
    },
    { step: 'reset', permission: mayReset, show: status === 'cancelled', label: t('asm.native.reset') },
  ];
  const visible = actions.filter((action) => action.permission && action.show);
  const toggleRoom = (id: string, value: boolean) =>
    setRoomIds((current) =>
      value ? [...current.filter((room) => room !== id), id] : current.filter((room) => room !== id),
    );
  return (
    <>
      <Text variant="caption">
        {t('asm.native.currentStatus')}: {enumLabel(t, 'assessment.status', status)}
      </Text>
      {visible.some((action) => action.reason) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {visible.map((action) => (
        <Button
          key={action.step}
          loading={step.isPending}
          disabled={!etag}
          onPress={() =>
            etag &&
            step.mutate({
              examId: row.id,
              step: action.step,
              etag,
              ...(action.reason && reason.trim() ? { reason } : {}),
            })
          }
        >
          {action.label}
        </Button>
      ))}
      {mayPopulate && ['scheduled', 'held'].includes(status) ? (
        <Button loading={populate.isPending} onPress={() => populate.mutate(row.id)}>
          {t('asm.native.populateResults')}
        </Button>
      ) : null}
      {mayAllocate && ['scheduled', 'held'].includes(status) ? (
        <>
          <Text variant="h3">{t('asm.native.roomSelection')}</Text>
          {!mayReadRooms ? <Banner>{t('asm.native.unavailable')}</Banner> : null}
          <QueryFeedback
            query={roomsQuery}
            hasCachedRows={rooms.length > 0}
            onRetry={() => void roomsQuery.refetch()}
          />
          {roomsQuery.isPending ? <LoadingRows tablet /> : null}
          {mayReadRooms && !roomsQuery.isPending && !roomsQuery.isError && rooms.length === 0 ? (
            <Banner>{t('common.state.empty')}</Banner>
          ) : null}
          {rooms.map((room) => (
            <SwitchRow
              key={room.id}
              label={firstText(room, ['name', 'code', 'roomNumber']) ?? room.id}
              value={roomIds.includes(room.id)}
              onChange={(value) => toggleRoom(room.id, value)}
            />
          ))}
          <Button
            loading={allocate.isPending}
            disabled={!roomIds.length || !mayReadRooms}
            onPress={() => allocate.mutate({ examId: row.id, roomIds })}
          >
            {t('asm.native.allocateRooms')}
          </Button>
        </>
      ) : null}
      {resultQuery.isError ? (
        <QueryFeedback
          query={resultQuery}
          hasCachedRows={results.length > 0}
          onRetry={() => void resultQuery.refetch()}
        />
      ) : null}
      <WorkflowNotice mutation={step} />
      <WorkflowNotice mutation={populate} />
      <WorkflowNotice mutation={allocate} />
    </>
  );
}

function examMarkRowErrors(error: unknown, studentIds: readonly string[]): Record<string, string[]> {
  if (!isApiError(error)) return {};
  const byStudent: Record<string, string[]> = {};
  for (const fieldError of error.errors) {
    const match = /^entries\[(\d+)\](?:\.|$)/.exec(fieldError.field);
    const studentId = match ? studentIds[Number(match[1])] : undefined;
    if (!studentId) continue;
    (byStudent[studentId] ??= []).push(fieldError.message);
  }
  return byStudent;
}

export function ExamMarks({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t } = useI18n();
  const theme = useTheme();
  const examId = route.params.examId ?? null;
  const mayReadExam = useCan('assessment.exam.read');
  const mayReadResults = useCan('assessment.exam_result.read');
  const mayRecord = useCan('assessment.exam.record_exam_marks');
  const mayMarkUpdated = useCan('assessment.exam.mark_exam_results_updated');
  const examDef = RESOURCES.find((resource) => resource.key === 'assessment.Exam');
  const exam = useRecord(examDef, mayReadExam ? examId : null);
  const results = useExamResults(mayReadResults ? examId : null);
  const rows = (results.data?.items ?? []) as ExamResult[];
  const students = rows.map((result) => result.studentId);
  const names = useStudentNames(mayReadResults ? students : []);
  const recordMarks = useRecordExamMarks();
  const examStep = useExamStep();
  const [values, setValues] = useState<Record<string, MarkEntry>>({});
  const [submittedStudentIds, setSubmittedStudentIds] = useState<string[]>([]);
  const examData = exam.data?.data as unknown as Exam | undefined;
  if (!examId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  if (!mayReadExam || !mayReadResults) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  const state = examData?.status ?? '';
  const editable = mayRecord && ['held', 'result_updated'].includes(state);
  const editedRows = rows.filter((row) => {
    const value = values[row.studentId];
    if (!value) return false;
    const attendanceStatus = row.attendanceStatus ?? 'present';
    const marks = row.marks ?? '';
    const note = row.note ?? '';
    return (
      value.attendanceStatus !== attendanceStatus ||
      (value.marksObtained ?? marks) !== marks ||
      (value.note ?? note) !== note
    );
  });
  const rowErrors = examMarkRowErrors(recordMarks.error, submittedStudentIds);
  const saveMarks = () => {
    const entries = editedRows.map((row) => {
      const value = values[row.studentId];
      const attendanceStatus = value?.attendanceStatus ?? row.attendanceStatus ?? 'present';
      const marksObtained = value?.marksObtained ?? row.marks ?? undefined;
      const note = value?.note ?? row.note ?? undefined;
      return {
        studentId: row.studentId,
        attendanceStatus: attendanceStatus as MarkEntry['attendanceStatus'],
        ...(attendanceStatus === 'present' && marksObtained?.trim()
          ? { marksObtained: marksObtained.trim() }
          : {}),
        ...(note?.trim() ? { note: note.trim() } : {}),
        // the version the row was read at: a row changed by someone else since is refused (412)
        etag: row.etag as string,
      };
    });
    if (entries.length === 0 || editedRows.some((row) => !row.etag)) return;
    setSubmittedStudentIds(editedRows.map((row) => row.studentId));
    recordMarks.mutate(
      { examId, entries },
      {
        onSuccess: () => {
          setValues({});
          setSubmittedStudentIds([]);
        },
      },
    );
  };
  return (
    <>
      <QueryFeedback query={exam} hasCachedRows={Boolean(exam.data)} onRetry={() => void exam.refetch()} />
      {exam.isPending ? <LoadingRows tablet={tablet} /> : null}
      {examData ? <Text variant="h2">{examData.name}</Text> : null}
      <QueryFeedback query={results} hasCachedRows={rows.length > 0} onRetry={() => void results.refetch()} />
      {results.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!results.isPending && !results.isError && rows.length === 0 ? (
        <Banner>{t('asm.native.noRows')}</Banner>
      ) : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
        {rows.map((row) => (
          <ExamMarkRow
            key={row.studentId}
            result={row}
            name={names.get(row.studentId) ?? row.studentId}
            maxMarks={row.maxMarks ?? examData?.totalMarks}
            value={values[row.studentId]}
            tablet={tablet}
            editable={editable}
            errors={rowErrors[row.studentId]}
            onChange={(value) => {
              if (recordMarks.isError) {
                recordMarks.reset();
                setSubmittedStudentIds([]);
              }
              setValues((current) => ({ ...current, [row.studentId]: value }));
            }}
          />
        ))}
      </View>
      {rows.length > 0 && editable ? (
        <Button loading={recordMarks.isPending} disabled={editedRows.length === 0} onPress={saveMarks}>
          {t('asm.native.saveMarks')}
        </Button>
      ) : null}
      {recordMarks.isSuccess ? <Banner>{t('asm.native.markEntrySaved')}</Banner> : null}
      <WorkflowNotice mutation={recordMarks} />
      {exam.data && state === 'held' && mayMarkUpdated ? (
        <Button
          variant="secondary"
          loading={examStep.isPending}
          disabled={!exam.data.etag}
          onPress={() =>
            exam.data?.etag && examStep.mutate({ examId, step: 'resultsUpdated', etag: exam.data.etag })
          }
        >
          {t('asm.native.markUpdated')}
        </Button>
      ) : null}
      <WorkflowNotice mutation={examStep} />
    </>
  );
}

function ExamMarkRow({
  result,
  name,
  maxMarks,
  value,
  tablet,
  editable,
  errors,
  onChange,
}: {
  result: ExamResult;
  name: string;
  maxMarks: string | undefined;
  value: MarkEntry | undefined;
  tablet: boolean;
  editable: boolean;
  errors: readonly string[] | undefined;
  onChange: (value: MarkEntry) => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const attendanceStatus = value?.attendanceStatus ?? result.attendanceStatus ?? 'present';
  const marks = value?.marksObtained ?? result.marks ?? '';
  const note = value?.note ?? result.note ?? '';
  return (
    <View
      style={{
        padding: theme.space[4],
        marginTop: theme.space[3],
        borderWidth: 1,
        borderColor: theme.colors.border,
        borderRadius: theme.radius.md,
        width: tablet ? '48%' : '100%',
      }}
    >
      <Text variant="h3">{name}</Text>
      <ChoiceGroup
        label={t('asm.native.attendance')}
        value={attendanceStatus}
        disabled={!editable}
        choices={[
          { value: 'present', label: t('asm.native.present') },
          { value: 'absent', label: t('asm.native.absent') },
          { value: 'exempted', label: t('enum.attendance.exempted') },
        ]}
        onChange={(next) =>
          onChange({
            studentId: result.studentId,
            attendanceStatus: next as MarkEntry['attendanceStatus'],
            marksObtained: marks,
            note,
          })
        }
      />
      {maxMarks ? (
        <Text muted>
          {t('asm.native.maxMarks')}: {maxMarks}
        </Text>
      ) : null}
      {attendanceStatus === 'present' ? (
        <TextInput
          label={t('asm.native.markValue')}
          value={marks}
          editable={editable}
          keyboardType="decimal-pad"
          onChangeText={(next) =>
            onChange({ studentId: result.studentId, attendanceStatus: 'present', marksObtained: next, note })
          }
        />
      ) : null}
      <TextInput
        label={t('asm.native.note')}
        value={note}
        editable={editable}
        multiline
        onChangeText={(next) =>
          onChange({
            studentId: result.studentId,
            attendanceStatus: attendanceStatus as MarkEntry['attendanceStatus'],
            marksObtained: marks,
            note: next,
          })
        }
      />
      {errors?.map((error, index) => (
        <Banner key={`${result.studentId}-error-${index}`} intent="danger">
          {error}
        </Banner>
      ))}
      {result.gradeLabel ? (
        <Text>
          {t('asm.native.grade')}: {result.gradeLabel}
        </Text>
      ) : null}
      {result.resultStatus ? <Text>{enumLabel(t, 'assessment.status', result.resultStatus)}</Text> : null}
    </View>
  );
}
