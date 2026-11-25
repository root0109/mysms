import { useMutation, useQueries, useQueryClient } from '@tanstack/react-query';
import type { careers } from '@mysms/sdk';
import { JobOpeningApi } from '@mysms/sdk/careers/apis/JobOpeningApi';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { listRecords } from '../records/api';
import { resourceDef } from '../records/hooks';
import type { RecordRow, ResourceDef } from '../records/types';
import { useRuntime } from '../react/useRuntime';
import { useSession } from './session';

export type JobApplicationStatus = careers.UpdateApplicationStatusRequestStatusEnum;

const refreshApplications = (qc: ReturnType<typeof useQueryClient>) =>
  qc.invalidateQueries({ queryKey: queryKey('records', 'careers.JobApplication') });

/** careers.UpdateApplicationStatus: the poster moves an application along its pipeline, under its etag. */
export function useUpdateApplicationStatus() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: {
      jobOpeningId: string;
      jobApplicationId: string;
      status: JobApplicationStatus;
      etag: string | null;
    }) =>
      call(
        api.get(JobOpeningApi).updateApplicationStatus({
          jobOpeningId: v.jobOpeningId,
          jobApplicationId: v.jobApplicationId,
          updateApplicationStatusRequest: { status: v.status, ...(v.etag ? { etag: v.etag } : {}) },
        }),
      ),
    onSettled: () => refreshApplications(qc),
  });
}

/** careers.EmailApplicant: a message to the applicant at their account's address (never shown to the poster). */
export function useEmailApplicant() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (v: { applicationId: string; subject: string; message: string }) =>
      call(
        api.get(JobOpeningApi).emailApplicant({
          applicationId: v.applicationId,
          emailApplicantRequest: { subject: v.subject, message: v.message },
        }),
      ),
  });
}

/** How many openings "my applications" looks through: the contract reads applications per opening only. */
export const MY_APPLICATIONS_OPENINGS = 50;

/**
 * CAR-04 "my applications": the signed-in candidate's application to each of the given openings (the server answers
 * an applicant with their own applications only), one read per opening, at most {@link MY_APPLICATIONS_OPENINGS}.
 */
export function useMyApplications(jobOpeningIds: readonly string[]) {
  const { api } = useRuntime();
  const me = useSession((s) => s.me?.user?.id ?? null);
  const def = resourceDef('careers.JobApplication') as ResourceDef;
  return useQueries({
    queries: jobOpeningIds.slice(0, MY_APPLICATIONS_OPENINGS).map((jobOpeningId) => ({
      queryKey: queryKey('records', 'careers.JobApplication', { mine: me, jobOpeningId }),
      queryFn: () =>
        listRecords(api, def, {
          parentId: jobOpeningId,
          filter: `applicantUserId = "${me as string}"`,
          size: 5,
        }),
      enabled: me !== null,
      staleTime: STALE_TIME.list,
    })),
    combine: (results) => ({
      applications: results.flatMap((result) => result.data?.data ?? []) as RecordRow[],
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  });
}
