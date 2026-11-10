import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { assessment } from '@mysms/sdk';
import { HomeworkApi } from '@mysms/sdk/assessment/apis/HomeworkApi';
import { HomeworkSubmissionApi } from '@mysms/sdk/assessment/apis/HomeworkSubmissionApi';
import { allPages, pageQuery } from '../api/pages';
import { call, callWithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Homework = assessment.Homework;
export type HomeworkSubmission = assessment.HomeworkSubmission;
export type HomeworkApproval = assessment.HomeworkApproval;
/** The steps of the homework lifecycle a screen can ask for (api/state-machines.md "Homework"). */
export type HomeworkStep = 'publish' | 'approve' | 'reject';

const HOMEWORK = (homeworkId: string | null) => queryKey('assessment', 'homework', homeworkId);
const SUBMISSIONS = (homeworkId: string | null) => queryKey('assessment', 'homework-submissions', homeworkId);
const APPROVALS = (homeworkId: string | null) => queryKey('assessment', 'homework-approvals', homeworkId);

/** Marks the standard list and form of a record type stale: a step changed rows those screens may be showing. */
function staleRecords(qc: QueryClient, recordType: string): void {
  void qc.invalidateQueries({ queryKey: queryKey('records', recordType) });
  void qc.invalidateQueries({ queryKey: queryKey('records', `${recordType}#item`) });
}

/** Re-reads a homework, its reviews and the homework lists after a step. */
async function refreshHomework(qc: QueryClient, homeworkId: string): Promise<void> {
  staleRecords(qc, 'assessment.Homework');
  staleRecords(qc, 'assessment.HomeworkApproval');
  await Promise.all([
    qc.invalidateQueries({ queryKey: HOMEWORK(homeworkId) }),
    qc.invalidateQueries({ queryKey: APPROVALS(homeworkId) }),
  ]);
}

/** assessment.GetHomework with its ETag, which the lifecycle steps send back. */
export function useHomework(homeworkId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: HOMEWORK(homeworkId),
    queryFn: () => callWithEtag(api.get(HomeworkApi).getHomeworkRaw({ homeworkId: homeworkId as string })),
    enabled: homeworkId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** assessment.ListHomeworkApprovals: the reviews of a homework, oldest first (the last one is the current word). */
export function useHomeworkApprovals(homeworkId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: APPROVALS(homeworkId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(HomeworkApi).listHomeworkApprovals({
            homeworkId: homeworkId as string,
            size: 500,
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: homeworkId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** assessment.ListHomeworkSubmissions of one homework: what the class handed in. */
export function useHomeworkSubmissions(homeworkId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: SUBMISSIONS(homeworkId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(HomeworkSubmissionApi).listHomeworkSubmissions({
            size: 500,
            filter: `homeworkId = "${homeworkId as string}"`,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: homeworkId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/**
 * assessment.PublishHomework / ApproveHomework / RejectHomework, under the etag the screen last read. Publishing
 * answers a homework that is published, or pending when the caller's role needs a reviewer; the reason of an
 * approval or rejection is kept with the review.
 */
export function useHomeworkStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      homeworkId,
      step,
      etag,
      reason,
    }: {
      homeworkId: string;
      step: HomeworkStep;
      etag: string | null;
      reason?: string;
    }) => {
      const homeworks = api.get(HomeworkApi);
      const versioned = etag ? { etag } : {};
      const decided = { ...versioned, ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      if (step === 'publish')
        return call(homeworks.publishHomework({ homeworkId, publishHomeworkRequest: versioned }));
      if (step === 'approve')
        return call(homeworks.approveHomework({ homeworkId, approveHomeworkRequest: decided }));
      return call(homeworks.rejectHomework({ homeworkId, rejectHomeworkRequest: decided }));
    },
    onSettled: (_result, _error, variables) => refreshHomework(qc, variables.homeworkId),
  });
}

/**
 * assessment.CheckHomeworkSubmission: the submission is checked, with the teacher's comments (sending none keeps the
 * ones already given), under the submission's etag.
 */
export function useCheckHomeworkSubmission() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      submissionId,
      etag,
      teacherComments,
    }: {
      homeworkId: string;
      submissionId: string;
      etag: string | null;
      teacherComments?: string;
    }) =>
      call(
        api.get(HomeworkSubmissionApi).checkHomeworkSubmission({
          homeworkSubmissionId: submissionId,
          checkHomeworkSubmissionRequest: {
            ...(etag ? { etag } : {}),
            ...(teacherComments?.trim() ? { teacherComments: teacherComments.trim() } : {}),
          },
        }),
      ),
    onSettled: (_result, _error, variables) => {
      staleRecords(qc, 'assessment.HomeworkSubmission');
      return qc.invalidateQueries({ queryKey: SUBMISSIONS(variables.homeworkId) });
    },
  });
}
