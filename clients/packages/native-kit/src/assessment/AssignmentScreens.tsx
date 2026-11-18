/** Assignments: the teacher's workflow and grading (ASM-05/06), the student's own submission (ASM-07). */
import {
  RESOURCES,
  useAssignmentStep,
  useAssignmentSubmissionStep,
  useAssignmentSubmissions,
  useCreateRecord,
  useInfiniteRecords,
  useMyStudentIds,
  useRecord,
  useStudentNames,
  useUpdateRecord,
  useCan,
  useI18n,
  type Assignment,
  type AssignmentStep,
  type AssignmentSubmissionStep,
  type AssignmentSubmission as AssignmentSubmissionData,
  type RecordRow,
  type ResourceDef,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import type { AssessmentRoute } from '../assessmentRoutes';
import { QueryFeedback, WorkflowNotice, LoadingRows } from '../records';
import { textAreaHeight } from '../recordText';

export function AssignmentWorkflow({
  row,
  etag,
  tablet,
}: {
  row: RecordRow;
  etag: string | undefined;
  tablet: boolean;
}) {
  const { t } = useI18n();
  const status = String(row.status ?? '');
  const step = useAssignmentStep();
  const [reason, setReason] = useState('');
  const mayReadApprovals = useCan('assessment.assignment_approval.read');
  const mayPublish = useCan('assessment.assignment.publish_assignment');
  const mayApprove = useCan('assessment.assignment.approve_assignment');
  const mayReject = useCan('assessment.assignment.reject_assignment');
  const mayFinish = useCan('assessment.assignment.finish_assignment');
  const mayCancel = useCan('assessment.assignment.cancel_assignment');
  const mayReset = useCan('assessment.assignment.reset_assignment');
  const approvalDef = RESOURCES.find((resource) => resource.key === 'assessment.AssignmentApproval');
  const approvals = useInfiniteRecords(approvalDef, {
    parentId: mayReadApprovals ? row.id : undefined,
    filter: 'decision = "pending"',
    size: 100,
  });
  const pendingApprovals = (approvals.data?.pages.flatMap((page) => page.data) ?? []).filter(
    (approval) => approval.decision === 'pending',
  );
  const hasPendingApproval = pendingApprovals.length > 0;
  const readyToPublish = !mayReadApprovals || (approvals.isSuccess && !hasPendingApproval);
  const actions: {
    step: AssignmentStep;
    permission: boolean;
    show: boolean;
    reason?: boolean;
    label: string;
  }[] = [
    {
      step: 'publish',
      permission: mayPublish,
      show: status === 'draft' && readyToPublish,
      label: t('asm.native.publish'),
    },
    {
      step: 'approve',
      permission: mayApprove,
      show: status === 'draft' && mayReadApprovals && hasPendingApproval,
      reason: true,
      label: t('asm.native.approve'),
    },
    {
      step: 'reject',
      permission: mayReject,
      show: status === 'draft' && mayReadApprovals && hasPendingApproval,
      reason: true,
      label: t('asm.native.reject'),
    },
    {
      step: 'finish',
      permission: mayFinish,
      show: ['published', 'ongoing'].includes(status),
      label: t('asm.native.finish'),
    },
    {
      step: 'cancel',
      permission: mayCancel,
      show: ['draft', 'published', 'ongoing'].includes(status),
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
      {status === 'draft' && mayReadApprovals ? (
        <>
          <QueryFeedback
            query={approvals}
            hasCachedRows={pendingApprovals.length > 0}
            onRetry={() => void approvals.refetch()}
          />
          {approvals.isPending ? <LoadingRows tablet={tablet} /> : null}
          {pendingApprovals.map((approval) => (
            <View key={approval.id}>
              <Banner>{t('asm.native.pendingAction')}</Banner>
              {typeof approval.comments === 'string' && approval.comments ? (
                <Text>
                  {t('asm.native.approvalNote')}: {approval.comments}
                </Text>
              ) : null}
            </View>
          ))}
        </>
      ) : null}
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
            step.mutate(
              {
                assignmentId: row.id,
                step: action.step,
                etag,
                ...(action.reason && reason.trim() ? { reason } : {}),
              },
              {
                onSuccess: () => {
                  setReason('');
                  if (mayReadApprovals) void approvals.refetch();
                },
              },
            )
          }
        >
          {action.label}
        </Button>
      ))}
      <WorkflowNotice mutation={step} />
    </>
  );
}

export function AssignmentSubmissionsPanel({
  assignmentId,
  tablet,
}: {
  assignmentId: string;
  tablet: boolean;
}) {
  const { t } = useI18n();
  const mayRead = useCan('assessment.assignment_submission.read');
  const submissionsQuery = useAssignmentSubmissions(mayRead ? assignmentId : null);
  const submissions = ((submissionsQuery.data?.items ?? []) as AssignmentSubmissionData[]).filter(
    (submission): submission is AssignmentSubmissionData & { id: string } => Boolean(submission.id),
  );
  const names = useStudentNames(mayRead ? submissions.map((submission) => submission.studentId) : []);
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  return (
    <>
      <Text variant="h2">{t('asm.native.submissions')}</Text>
      <QueryFeedback
        query={submissionsQuery}
        hasCachedRows={submissions.length > 0}
        onRetry={() => void submissionsQuery.refetch()}
      />
      {submissionsQuery.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!submissionsQuery.isPending && !submissionsQuery.isError && submissions.length === 0 ? (
        <Banner>{t('asm.native.noSubmissions')}</Banner>
      ) : null}
      {submissions.map((submission) => (
        <AssignmentSubmissionReview
          key={submission.id}
          assignmentId={assignmentId}
          submission={submission}
          studentName={names.get(submission.studentId) ?? submission.studentId}
        />
      ))}
    </>
  );
}

function AssignmentSubmissionReview({
  assignmentId,
  submission,
  studentName,
}: {
  assignmentId: string;
  submission: AssignmentSubmissionData & { id: string };
  studentName: string;
}) {
  const { t, format } = useI18n();
  const mutation = useAssignmentSubmissionStep();
  const mayAccept = useCan('assessment.assignment_submission.accept_assignment_submission');
  const mayReject = useCan('assessment.assignment_submission.reject_assignment_submission');
  const mayRequest = useCan('assessment.assignment_submission.request_submission_changes');
  const mayGrade = useCan('assessment.assignment_submission.grade_assignment_submission');
  const status = submission.status ?? '';
  const [marks, setMarks] = useState(submission.marksObtained ?? '');
  const [feedback, setFeedback] = useState(submission.feedback ?? '');
  const [reason, setReason] = useState('');
  const submitted = status === 'submitted';
  const run = (step: AssignmentSubmissionStep) =>
    submission.etag &&
    mutation.mutate({
      assignmentId,
      submissionId: submission.id,
      step,
      etag: submission.etag,
      ...(reason.trim() ? { reason: reason.trim() } : {}),
      ...(step === 'grade' ? { marks: marks.trim(), feedback: feedback.trim() } : {}),
    });
  return (
    <View>
      <Text variant="h3">{studentName}</Text>
      <Text variant="caption">{enumLabel(t, 'assessment.assignment_submission_status', status)}</Text>
      {submission.content ? <Text>{submission.content}</Text> : null}
      {submission.submittedAt ? (
        <Text muted>
          {t('asm.native.submitted')}: {format.dateTime(submission.submittedAt)}
        </Text>
      ) : null}
      {submitted && (mayReject || mayRequest) ? (
        <TextInput label={t('asm.native.approvalNote')} value={reason} onChangeText={setReason} multiline />
      ) : null}
      {mayGrade && ['submitted', 'accepted'].includes(status) ? (
        <>
          <TextInput
            label={t('asm.native.marks')}
            value={marks}
            onChangeText={setMarks}
            keyboardType="decimal-pad"
          />
          <TextInput label={t('asm.native.feedback')} value={feedback} onChangeText={setFeedback} multiline />
          <Button
            loading={mutation.isPending}
            disabled={!submission.etag || !marks.trim()}
            onPress={() => run('grade')}
          >
            {t('asm.native.saveMarks')}
          </Button>
        </>
      ) : null}
      {mayAccept && submitted ? (
        <Button
          variant="secondary"
          loading={mutation.isPending}
          disabled={!submission.etag}
          onPress={() => run('accept')}
        >
          {t('asm.native.approve')}
        </Button>
      ) : null}
      {mayRequest && submitted ? (
        <Button
          variant="secondary"
          loading={mutation.isPending}
          disabled={!submission.etag || !reason.trim()}
          onPress={() => run('requestChanges')}
        >
          {t('asm.native.requestChanges')}
        </Button>
      ) : null}
      {mayReject && submitted ? (
        <Button
          variant="secondary"
          loading={mutation.isPending}
          disabled={!submission.etag || !reason.trim()}
          onPress={() => run('reject')}
        >
          {t('asm.native.reject')}
        </Button>
      ) : null}
      {submission.marksObtained ? (
        <Text>
          {t('asm.native.marks')}: {submission.marksObtained}
        </Text>
      ) : null}
      {submission.feedback ? (
        <Text>
          {t('asm.native.feedback')}: {submission.feedback}
        </Text>
      ) : null}
      <WorkflowNotice mutation={mutation} />
    </View>
  );
}

export function AssignmentSubmission({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const assignmentId = route.params.assignmentId ?? null;
  const studentId = useMyStudentIds()[0] ?? null;
  const mayReadAssignment = useCan('assessment.assignment.read');
  const mayReadSubmissions = useCan('assessment.assignment_submission.read');
  const mayCreate = useCan('assessment.assignment_submission.create');
  const mayUpdate = useCan('assessment.assignment_submission.update');
  const maySubmit = useCan('assessment.assignment_submission.submit_assignment_submission');
  const mayReset = useCan('assessment.assignment_submission.reset_assignment_submission');
  const assignmentDef = RESOURCES.find((resource) => resource.key === 'assessment.Assignment');
  const submissionDef = RESOURCES.find(
    (resource) => resource.key === 'assessment.AssignmentSubmission',
  ) as ResourceDef;
  const assignment = useRecord(assignmentDef, mayReadAssignment ? assignmentId : null);
  // only the student's own submission is asked for (the service shows a student nothing else)
  const submissions = useAssignmentSubmissions(
    mayReadSubmissions && studentId ? assignmentId : null,
    studentId,
  );
  const ownCandidate = submissions.data?.items.find((submission) => submission.studentId === studentId);
  const own = ownCandidate?.id ? (ownCandidate as AssignmentSubmissionData & { id: string }) : undefined;
  const create = useCreateRecord(submissionDef);
  const update = useUpdateRecord(submissionDef);
  const submit = useAssignmentSubmissionStep();
  const [content, setContent] = useState('');
  const [editing, setEditing] = useState(false);
  if (!assignmentId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  if (!mayReadAssignment) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  const data = assignment.data?.data as unknown as Assignment | undefined;
  const status = own?.status ?? '';
  const isEditable = Boolean(own && mayUpdate && ['draft', 'change_requested'].includes(status));
  const editableContent = editing ? content : (own?.content ?? '');
  const createSubmission = () => {
    if (!studentId) return;
    create.mutate(
      { values: { assignmentId, studentId, content: content.trim() } },
      {
        onSuccess: () => {
          setContent('');
          void submissions.refetch();
        },
      },
    );
  };
  const saveContent = () => {
    if (!own?.id || !own.etag) return;
    update.mutate(
      { id: own.id, etag: own.etag, values: { content: content.trim() } },
      {
        onSuccess: () => {
          setEditing(false);
          void submissions.refetch();
        },
      },
    );
  };
  return (
    <View style={{ gap: theme.space[4] }}>
      <QueryFeedback
        query={assignment}
        hasCachedRows={Boolean(data)}
        onRetry={() => void assignment.refetch()}
      />
      {assignment.isPending ? <LoadingRows tablet={tablet} /> : null}
      {data ? (
        <>
          <Text variant="h2">{data.title}</Text>
          {data.description ? <Text>{data.description}</Text> : null}
          {data.dueAt ? (
            <Text muted>{t('asm.native.due', { date: format.dateTime(data.dueAt) })}</Text>
          ) : null}
          {data.maxMarks ? (
            <Text muted>
              {t('asm.native.maxMarks')}: {data.maxMarks}
            </Text>
          ) : null}
          <Banner>{t('asm.native.attachmentsUnavailable')}</Banner>
          {mayReadSubmissions ? (
            <QueryFeedback
              query={submissions}
              hasCachedRows={Boolean(own)}
              onRetry={() => void submissions.refetch()}
            />
          ) : mayCreate ? (
            <Banner>{t('asm.native.unavailable')}</Banner>
          ) : null}
          {own ? (
            <>
              <Text variant="h3">{enumLabel(t, 'assessment.assignment_submission_status', status)}</Text>
              {isEditable ? (
                <>
                  <TextInput
                    label={t('asm.native.submit')}
                    value={editableContent}
                    onChangeText={setContent}
                    multiline
                    style={{ minHeight: textAreaHeight(theme, 5) }}
                  />
                  {editing ? (
                    <Button loading={update.isPending} disabled={!own.etag} onPress={saveContent}>
                      {t('common.action.save')}
                    </Button>
                  ) : (
                    <Button
                      variant="secondary"
                      onPress={() => {
                        setContent(own.content ?? '');
                        setEditing(true);
                      }}
                    >
                      {t('common.action.edit')}
                    </Button>
                  )}
                </>
              ) : own.content ? (
                <Text>{own.content}</Text>
              ) : null}
              {own.feedback ? (
                <Text>
                  {t('asm.native.feedback')}: {own.feedback}
                </Text>
              ) : null}
              {maySubmit && ['draft', 'change_requested'].includes(status) ? (
                <Button
                  loading={submit.isPending}
                  disabled={!own.etag}
                  onPress={() =>
                    own.etag &&
                    submit.mutate({ submissionId: own.id, assignmentId, step: 'submit', etag: own.etag })
                  }
                >
                  {t('asm.native.submit')}
                </Button>
              ) : null}
              {mayReset && ['rejected', 'change_requested'].includes(status) ? (
                <Button
                  variant="secondary"
                  loading={submit.isPending}
                  disabled={!own.etag}
                  onPress={() =>
                    own.etag &&
                    submit.mutate({ submissionId: own.id, assignmentId, step: 'reset', etag: own.etag })
                  }
                >
                  {t('asm.native.reset')}
                </Button>
              ) : null}
            </>
          ) : mayCreate &&
            mayReadSubmissions &&
            !submissions.isPending &&
            !submissions.isError &&
            studentId &&
            ['published', 'ongoing'].includes(data.status ?? '') ? (
            <>
              <TextInput
                label={t('asm.native.submit')}
                value={content}
                onChangeText={setContent}
                multiline
                style={{ minHeight: textAreaHeight(theme, 5) }}
              />
              <Button loading={create.isPending} disabled={!content.trim()} onPress={createSubmission}>
                {t('asm.native.submit')}
              </Button>
            </>
          ) : !studentId ? (
            <Banner>{t('common.state.notAvailable')}</Banner>
          ) : mayReadSubmissions && !submissions.isPending && !submissions.isError ? (
            <Banner>{t('asm.native.noSubmission')}</Banner>
          ) : null}
          <WorkflowNotice mutation={create} />
          <WorkflowNotice mutation={update} />
          <WorkflowNotice mutation={submit} />
        </>
      ) : null}
    </View>
  );
}
