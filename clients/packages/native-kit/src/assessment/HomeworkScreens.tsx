/** Homework on the phone and tablet: the teacher's review and check (ASM-03), the student's submission (ASM-04). */
import {
  useCheckHomeworkSubmission,
  useCreateHomeworkSubmission,
  useHomework,
  useHomeworkStep,
  useHomeworkSubmissions,
  useMyStudentIds,
  useReplyToHomeworkFeedback,
  useStudentNames,
  useCan,
  useI18n,
  type HomeworkSubmission as HomeworkSubmissionData,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import type { AssessmentRoute } from '../assessmentRoutes';
import { QueryFeedback, WorkflowNotice, LoadingRows } from '../records';
import { textAreaHeight } from '../recordText';

export function HomeworkReview({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const id = route.params.homeworkId ?? null;
  const mayRead = useCan('assessment.homework.read');
  const mayReadSubmissions = useCan('assessment.homework_submission.read');
  const mayPublish = useCan('assessment.homework.publish_homework');
  const mayApprove = useCan('assessment.homework.approve_homework');
  const mayReject = useCan('assessment.homework.reject_homework');
  const homework = useHomework(mayRead ? id : null);
  const submissions = useHomeworkSubmissions(mayReadSubmissions ? id : null);
  const data = homework.data?.data;
  const studentIds = mayReadSubmissions ? (submissions.data?.items.map((item) => item.studentId) ?? []) : [];
  const names = useStudentNames(studentIds);
  const mutation = useHomeworkStep();
  const [reason, setReason] = useState('');
  const status = data?.status ?? 'draft';
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  if (!id) return <Banner>{t('common.state.notAvailable')}</Banner>;
  return (
    <>
      <QueryFeedback query={homework} hasCachedRows={Boolean(data)} onRetry={() => void homework.refetch()} />
      {homework.isPending ? <LoadingRows tablet={tablet} /> : null}
      {data ? (
        <>
          {data.description ? <Text>{data.description}</Text> : null}
          <Text variant="caption">{enumLabel(t, 'assessment.status', status)}</Text>
          {data.dueAt ? (
            <Text muted>{t('asm.native.due', { date: format.dateTime(data.dueAt) })}</Text>
          ) : null}
          <WorkflowNotice mutation={mutation} />
          {(status === 'draft' && mayPublish) || (status === 'pending' && (mayApprove || mayReject)) ? (
            <TextInput
              label={t('asm.native.approvalNote')}
              value={reason}
              onChangeText={setReason}
              multiline
              style={{ minHeight: textAreaHeight(theme, 2) }}
            />
          ) : null}
          {status === 'draft' && mayPublish ? (
            <Button
              loading={mutation.isPending}
              disabled={!homework.data?.etag}
              onPress={() =>
                mutation.mutate({ homeworkId: id, step: 'publish', etag: homework.data?.etag ?? null })
              }
            >
              {t('asm.native.publish')}
            </Button>
          ) : null}
          {status === 'pending' && mayApprove ? (
            <Button
              loading={mutation.isPending}
              disabled={!homework.data?.etag}
              onPress={() =>
                mutation.mutate({
                  homeworkId: id,
                  step: 'approve',
                  etag: homework.data?.etag ?? null,
                  reason,
                })
              }
            >
              {t('asm.native.approve')}
            </Button>
          ) : null}
          {status === 'pending' && mayReject ? (
            <Button
              variant="secondary"
              loading={mutation.isPending}
              disabled={!homework.data?.etag}
              onPress={() =>
                mutation.mutate({ homeworkId: id, step: 'reject', etag: homework.data?.etag ?? null, reason })
              }
            >
              {t('asm.native.reject')}
            </Button>
          ) : null}
          {mayReadSubmissions ? (
            <HomeworkSubmissionRows homeworkId={id} query={submissions} names={names} tablet={tablet} />
          ) : null}
        </>
      ) : null}
    </>
  );
}

export function HomeworkSubmission({ route, tablet }: { route: AssessmentRoute; tablet: boolean }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const studentId = useMyStudentIds()[0] ?? null;
  const homeworkId = route.params.homeworkId ?? null;
  const mayRead = useCan('assessment.homework.read');
  const mayReadSubmissions = useCan('assessment.homework_submission.read');
  const mayCreate = useCan('assessment.homework_submission.create');
  const homework = useHomework(mayRead ? homeworkId : null);
  const submissions = useHomeworkSubmissions(mayReadSubmissions && studentId ? homeworkId : null);
  const mineCandidate = submissions.data?.items.find((submission) => submission.studentId === studentId);
  const mine = mineCandidate?.id ? (mineCandidate as HomeworkSubmissionData & { id: string }) : undefined;
  const create = useCreateHomeworkSubmission();
  const reply = useReplyToHomeworkFeedback();
  const [studentReply, setStudentReply] = useState('');
  if (!mayRead) return <Banner>{t('adm.resource.noAccess')}</Banner>;
  if (!homeworkId) return <Banner>{t('common.state.notAvailable')}</Banner>;
  return (
    <>
      <QueryFeedback
        query={homework}
        hasCachedRows={Boolean(homework.data)}
        onRetry={() => void homework.refetch()}
      />
      {homework.isPending ? <LoadingRows tablet={tablet} /> : null}
      {homework.data ? (
        <>
          {homework.data.data.description ? <Text>{homework.data.data.description}</Text> : null}
          {homework.data.data.dueAt ? (
            <Text muted>{t('asm.native.due', { date: format.dateTime(homework.data.data.dueAt) })}</Text>
          ) : null}
          <Banner>{t('asm.native.attachmentsUnavailable')}</Banner>
          {mayReadSubmissions ? (
            <QueryFeedback
              query={submissions}
              hasCachedRows={Boolean(mine)}
              onRetry={() => void submissions.refetch()}
            />
          ) : mayCreate ? (
            <Banner>{t('asm.native.unavailable')}</Banner>
          ) : null}
          <WorkflowNotice mutation={create} />
          {!mine &&
          mayReadSubmissions &&
          !submissions.isPending &&
          !submissions.isError &&
          mayCreate &&
          studentId &&
          homework.data.data.status === 'published' ? (
            <Button loading={create.isPending} onPress={() => create.mutate({ homeworkId, studentId })}>
              {t('asm.native.submit')}
            </Button>
          ) : null}
          {mine ? <HomeworkSubmissionReceipt submission={mine} /> : null}
          {!mine && mayReadSubmissions && !submissions.isPending && !submissions.isError ? (
            <Banner>{t('asm.native.noSubmission')}</Banner>
          ) : null}
          {mine?.teacherComments ? (
            <>
              <Text variant="h3">{t('asm.native.feedback')}</Text>
              <Text>{mine.teacherComments}</Text>
              {!mine.studentReply ? (
                <>
                  <TextInput
                    label={t('asm.native.replyLabel')}
                    value={studentReply}
                    onChangeText={setStudentReply}
                    multiline
                    style={{ minHeight: textAreaHeight(theme, 3) }}
                  />
                  <Button
                    loading={reply.isPending}
                    disabled={!studentReply.trim() || !mine.id || !mine.etag}
                    onPress={() => {
                      if (!homeworkId || !mine.id || !mine.etag) return;
                      reply.mutate({ homeworkId, submissionId: mine.id, etag: mine.etag, studentReply });
                    }}
                  >
                    {t('asm.native.reply')}
                  </Button>
                </>
              ) : (
                <Text>{mine.studentReply}</Text>
              )}
              <WorkflowNotice mutation={reply} />
            </>
          ) : null}
        </>
      ) : null}
    </>
  );
}

function HomeworkSubmissionReceipt({ submission }: { submission: HomeworkSubmissionData }) {
  const { t } = useI18n();
  return (
    <>
      <Text variant="h3">{t('asm.native.submitted')}</Text>
      <Text>{enumLabel(t, 'assessment.status', submission.status)}</Text>
    </>
  );
}

function HomeworkSubmissionRows({
  homeworkId,
  query,
  names,
  tablet,
}: {
  homeworkId: string;
  query: ReturnType<typeof useHomeworkSubmissions>;
  names: ReadonlyMap<string, string>;
  tablet: boolean;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const mayCheck = useCan('assessment.homework_submission.check_homework_submission');
  const check = useCheckHomeworkSubmission();
  const [comments, setComments] = useState<Record<string, string>>({});
  const submissions = query.data?.items ?? [];
  return (
    <>
      <Text variant="h2">{t('asm.native.submissions')}</Text>
      <QueryFeedback
        query={query}
        hasCachedRows={submissions.length > 0}
        onRetry={() => void query.refetch()}
      />
      {query.isPending ? <LoadingRows tablet={tablet} /> : null}
      {!query.isPending && !query.isError && submissions.length === 0 ? (
        <Banner>{t('asm.native.noSubmissions')}</Banner>
      ) : null}
      {submissions.map((submission) => {
        const submissionId = submission.id;
        if (!submissionId) return null;
        return (
          <View key={submissionId}>
            <Text variant="h3">{names.get(submission.studentId) ?? submission.studentId}</Text>
            <Text>{enumLabel(t, 'assessment.status', submission.status)}</Text>
            {submission.studentReply ? <Text>{submission.studentReply}</Text> : null}
            {mayCheck ? (
              <>
                <TextInput
                  label={t('asm.native.comments')}
                  value={comments[submissionId] ?? submission.teacherComments ?? ''}
                  onChangeText={(value) => setComments((current) => ({ ...current, [submissionId]: value }))}
                  multiline
                  style={{ minHeight: textAreaHeight(theme, 2) }}
                />
                <Button
                  loading={check.isPending}
                  disabled={!submission.etag}
                  onPress={() =>
                    check.mutate({
                      homeworkId,
                      submissionId,
                      etag: submission.etag ?? null,
                      teacherComments: comments[submissionId] ?? submission.teacherComments ?? '',
                    })
                  }
                >
                  {t('asm.native.markChecked')}
                </Button>
              </>
            ) : null}
          </View>
        );
      })}
      <WorkflowNotice mutation={check} />
    </>
  );
}
