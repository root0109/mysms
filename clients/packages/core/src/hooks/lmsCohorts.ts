import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { lms } from '@mysms/sdk';
import { CohortApi } from '@mysms/sdk/lms/apis/CohortApi';
import { newIdempotencyKey } from '../api/ids';
import { allPages, pageQuery } from '../api/pages';
import { call, callWithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Cohort = lms.Cohort;
export type CohortRosterEntry = lms.RosterEntry;
export type MemberAssignment = lms.MemberAssignment;
export type CohortPerformance = lms.CohortPerformance;
export type CohortStudentProgress = lms.CohortStudentProgress;
export type TimetableEntry = lms.TimetableEntry;
export type CohortSession = lms.CohortSession;
export type CohortScheduleSlot = lms.CohortScheduleSlot;
export type CohortHoliday = lms.CohortHoliday;
export type ReleaseGrant = lms.ReleaseGrant;
export type StudentAlert = lms.StudentAlert;
/** The lifecycle steps of a cohort (api/state-machines.md "lms.Cohort"). */
export type CohortStep = 'publish' | 'unpublish' | 'activate' | 'deactivate' | 'archive';

const COHORT = (cohortId: string | null) => queryKey('lms', 'cohort', cohortId);
const COHORTS = (filter: string | null) => queryKey('lms', 'cohorts', filter);
const ROSTER = (cohortId: string | null) => queryKey('lms', 'cohort-roster', cohortId);
const PERFORMANCE = (cohortId: string | null) => queryKey('lms', 'cohort-performance', cohortId);
const PROGRESS = (cohortId: string | null) => queryKey('lms', 'cohort-progress', cohortId);
const TIMETABLE = (cohortId: string | null) => queryKey('lms', 'cohort-timetable', cohortId);
const SCHEDULE = (cohortId: string | null) => queryKey('lms', 'cohort-schedule', cohortId);
const ALERTS = (cohortId: string | null) => queryKey('lms', 'student-alerts', cohortId);

function staleCohort(qc: QueryClient, cohortId: string): Promise<unknown> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'lms.Cohort') });
  return Promise.all([
    qc.invalidateQueries({ queryKey: COHORT(cohortId) }),
    qc.invalidateQueries({ queryKey: COHORTS(null).slice(0, 2) }),
  ]);
}

/** lms.GetCohort with its ETag, which the lifecycle steps and member writes send back. */
export function useCohort(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: COHORT(cohortId),
    queryFn: () => callWithEtag(api.get(CohortApi).getCohortRaw({ cohortId: cohortId as string })),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListCohorts: the cohorts the caller may see (members see theirs, the catalogue shows published ones). */
export function useCohorts(filter?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: COHORTS(filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(CohortApi).listCohorts({ size: 200, ...(filter ? { filter } : {}), ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.GetCohortRoster: students and staff with their names, for the cohort's teaching staff. */
export function useCohortRoster(cohortId: string | null, includeInactive = false) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: [...ROSTER(cohortId), includeInactive],
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(CohortApi).getCohortRoster({
            cohortId: cohortId as string,
            includeInactive,
            size: 200,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.SetCohortMembers: places students and staff (and removes the named ones) in one call. */
export function useSetCohortMembers() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      members,
      removeUserIds,
    }: {
      cohortId: string;
      members: MemberAssignment[];
      removeUserIds?: string[];
    }) =>
      call(
        api.get(CohortApi).setCohortMembers({
          cohortId,
          setCohortMembersRequest: { members, ...(removeUserIds?.length ? { removeUserIds } : {}) },
        }),
      ),
    onSettled: (_r, _e, variables) => qc.invalidateQueries({ queryKey: ROSTER(variables.cohortId) }),
  });
}

/** lms.BatchEnrollInCohort: enrols many students at once; with validateOnly it only previews. */
export function useBatchEnroll() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      userIds,
      validateOnly,
    }: {
      cohortId: string;
      userIds: string[];
      validateOnly?: boolean;
    }) =>
      call(
        api.get(CohortApi).batchEnrollInCohort({
          cohortId,
          idempotencyKey: newIdempotencyKey(),
          batchEnrollInCohortRequest: { userIds, ...(validateOnly ? { validateOnly } : {}) },
        }),
      ),
    onSettled: (_r, _e, variables) =>
      variables.validateOnly ? undefined : qc.invalidateQueries({ queryKey: ROSTER(variables.cohortId) }),
  });
}

/** lms.EnrollInCohort: the caller takes a seat in a published, open cohort. */
export function useEnrollInCohort() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cohortId }: { cohortId: string }) =>
      call(api.get(CohortApi).enrollInCohort({ cohortId, enrollInCohortRequest: {} })),
    onSettled: (_r, _e, variables) => staleCohort(qc, variables.cohortId),
  });
}

/** lms.ImportCohortMembers: bulk-adds members from an uploaded CSV; answers the finished operation. */
export function useImportCohortMembers() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      fileId,
      memberKind,
      validateOnly,
    }: {
      cohortId: string;
      fileId: string;
      memberKind: lms.ImportCohortMembersRequest['memberKind'];
      validateOnly?: boolean;
    }) =>
      call(
        api.get(CohortApi).importCohortMembers({
          cohortId,
          importCohortMembersRequest: { fileId, memberKind, ...(validateOnly ? { validateOnly } : {}) },
        }),
      ),
    onSettled: (_r, _e, variables) =>
      variables.validateOnly ? undefined : qc.invalidateQueries({ queryKey: ROSTER(variables.cohortId) }),
  });
}

/** lms.SetStudentAccess: the window in which students reach the cohort. */
export function useSetStudentAccess() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      studentAccessFrom,
      studentAccessUntil,
    }: {
      cohortId: string;
      studentAccessFrom?: string;
      studentAccessUntil?: string;
    }) =>
      call(
        api.get(CohortApi).setStudentAccess({
          cohortId,
          setStudentAccessRequest: {
            ...(studentAccessFrom ? { studentAccessFrom } : {}),
            ...(studentAccessUntil ? { studentAccessUntil } : {}),
          },
        }),
      ),
    onSettled: (_r, _e, variables) => staleCohort(qc, variables.cohortId),
  });
}

/** lms.SendCohortAnnouncement: e-mails every active student; answers how many were queued. */
export function useSendCohortAnnouncement() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({
      cohortId,
      subject,
      content,
      replyTo,
    }: {
      cohortId: string;
      subject: string;
      content: string;
      replyTo?: string;
    }) =>
      call(
        api.get(CohortApi).sendCohortAnnouncement({
          cohortId,
          sendCohortAnnouncementRequest: { subject, content, ...(replyTo?.trim() ? { replyTo } : {}) },
        }),
      ),
  });
}

/** One of the cohort's lifecycle steps, under the etag the screen last read. */
export function useCohortStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      step,
      etag,
      reason,
    }: {
      cohortId: string;
      step: CohortStep;
      etag: string | null;
      reason?: string;
    }) => {
      const cohorts = api.get(CohortApi);
      const versioned = etag ? { etag } : {};
      if (step === 'publish')
        return call(cohorts.publishCohort({ cohortId, publishCohortRequest: versioned }));
      if (step === 'unpublish')
        return call(cohorts.unpublishCohort({ cohortId, unpublishCohortRequest: versioned }));
      if (step === 'activate')
        return call(cohorts.activateCohort({ cohortId, activateCohortRequest: versioned }));
      if (step === 'deactivate')
        return call(
          cohorts.deactivateCohort({
            cohortId,
            deactivateCohortRequest: { ...versioned, ...(reason?.trim() ? { reason: reason.trim() } : {}) },
          }),
        );
      return call(cohorts.archiveCohort({ cohortId, archiveCohortRequest: versioned }));
    },
    onSettled: (_r, _e, variables) => staleCohort(qc, variables.cohortId),
  });
}

/** lms.PublishFinalGrades: turns the gradebook totals into published final grades. */
export function usePublishFinalGrades() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cohortId, userIds }: { cohortId: string; userIds?: string[] }) =>
      call(
        api.get(CohortApi).publishFinalGrades({
          cohortId,
          publishFinalGradesRequest: userIds?.length ? { userIds } : {},
        }),
      ),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey('lms', 'gradebook') }),
  });
}

/** lms.ExportGrades: the gradebook (or one item) as a CSV file; the finished operation carries the link. */
export function useExportGrades() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ cohortId, contentItemId }: { cohortId: string; contentItemId?: string }) =>
      call(
        api.get(CohortApi).exportGrades({
          cohortId,
          exportGradesRequest: { format: 'csv', ...(contentItemId ? { contentItemId } : {}) },
        }),
      ),
  });
}

/** lms.GetCohortSyllabus: a fresh download link for the cohort's syllabus PDF. */
export function useCohortSyllabusLink() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ cohortId }: { cohortId: string }) =>
      call(api.get(CohortApi).getCohortSyllabus({ cohortId })),
  });
}

/** lms.GetCohortPerformance: per graded item, averages and who is missing — teaching staff only. */
export function useCohortPerformance(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: PERFORMANCE(cohortId),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(CohortApi)
            .getCohortPerformance({ cohortId: cohortId as string, size: 200, ...pageQuery(page) }),
        ),
      ),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.GetCohortProgress: each student's progress (staff), or the caller's own. */
export function useCohortProgress(cohortId: string | null, userId?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: [...PROGRESS(cohortId), userId ?? null],
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(CohortApi).getCohortProgress({
            cohortId: cohortId as string,
            ...(userId ? { userId } : {}),
            size: 200,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.GetCohortTimetable: the cohort's timetable entries in order. */
export function useCohortTimetable(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: TIMETABLE(cohortId),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(CohortApi)
            .getCohortTimetable({ cohortId: cohortId as string, size: 500, ...pageQuery(page) }),
        ),
      ),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.ApplyTimetableTemplate: stamps a template's entries onto the cohort. */
export function useApplyTimetableTemplate() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cohortId, timetableTemplateId }: { cohortId: string; timetableTemplateId: string }) =>
      call(
        api.get(CohortApi).applyTimetableTemplate({
          cohortId,
          applyTimetableTemplateRequest: { timetableTemplateId },
        }),
      ),
    onSettled: (_r, _e, variables) => qc.invalidateQueries({ queryKey: TIMETABLE(variables.cohortId) }),
  });
}

/** The cohort's schedule slots, sessions and holidays, each read whole (LMS-25). */
export function useCohortSchedule(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: SCHEDULE(cohortId),
    queryFn: async () => {
      const cohorts = api.get(CohortApi);
      const id = cohortId as string;
      const [slots, sessions, holidays] = await Promise.all([
        allPages((page) =>
          call(cohorts.listCohortScheduleSlots({ cohortId: id, size: 500, ...pageQuery(page) })),
        ),
        allPages((page) => call(cohorts.listCohortSessions({ cohortId: id, size: 500, ...pageQuery(page) }))),
        allPages((page) => call(cohorts.listCohortHolidays({ cohortId: id, size: 500, ...pageQuery(page) }))),
      ]);
      return { slots: slots.items, sessions: sessions.items, holidays: holidays.items };
    },
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.EvaluateReleaseRules: runs the cohort's rules now and answers the grants that changed. */
export function useEvaluateReleaseRules() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ cohortId, userId }: { cohortId: string; userId?: string }) =>
      call(
        api.get(CohortApi).evaluateReleaseRules({
          cohortId,
          evaluateReleaseRulesRequest: userId ? { userId } : {},
        }),
      ),
  });
}

/** lms.ListStudentAlerts of a cohort, newest first (LMS-30). */
export function useStudentAlerts(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: ALERTS(cohortId),
    queryFn: async () => {
      const { StudentAlertApi } = await import('@mysms/sdk/lms/apis/StudentAlertApi');
      return allPages((page) =>
        call(
          api.get(StudentAlertApi).listStudentAlerts({
            size: 200,
            filter: `cohortId = "${cohortId as string}"`,
            ...pageQuery(page),
          }),
        ),
      );
    },
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.ListReleaseRules of a cohort, in their order. */
export function useReleaseRules(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('lms', 'release-rules', cohortId),
    queryFn: async () => {
      const { ReleaseRuleApi } = await import('@mysms/sdk/lms/apis/ReleaseRuleApi');
      return allPages((page) =>
        call(
          api.get(ReleaseRuleApi).listReleaseRules({
            size: 500,
            sort: ['sortOrder'],
            filter: `cohortId = "${cohortId as string}"`,
            ...pageQuery(page),
          }),
        ),
      );
    },
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.EnableReleaseRule / DisableReleaseRule, under the rule's etag (lms-learn's steps, called by this screen). */
export function useReleaseRuleStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({
      releaseRuleId,
      step,
      etag,
    }: {
      cohortId: string;
      releaseRuleId: string;
      step: 'enable' | 'disable';
      etag: string | null;
    }) => {
      const { ReleaseRuleApi } = await import('@mysms/sdk/lms/apis/ReleaseRuleApi');
      const rules = api.get(ReleaseRuleApi);
      const body = etag ? { etag } : {};
      if (step === 'enable')
        return call(rules.enableReleaseRule({ releaseRuleId, enableReleaseRuleRequest: body }));
      return call(rules.disableReleaseRule({ releaseRuleId, disableReleaseRuleRequest: body }));
    },
    onSettled: (_r, _e, variables) =>
      qc.invalidateQueries({ queryKey: queryKey('lms', 'release-rules', variables.cohortId) }),
  });
}

/** The cohort's content items (its outline, flat), in chapter and sort order. */
export function useCohortOutline(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('lms', 'cohort-outline', cohortId),
    queryFn: async () => {
      const { ContentItemApi } = await import('@mysms/sdk/lms/apis/ContentItemApi');
      return allPages((page) =>
        call(
          api.get(ContentItemApi).listContentItems({
            size: 500,
            sort: ['sortOrder'],
            filter: `cohortId = "${cohortId as string}"`,
            ...pageQuery(page),
          }),
        ),
      );
    },
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.CopyCourseOutlineToCohort: copies the course template's chapters (all or the chosen ones) into the cohort. */
export function useCopyCourseOutline() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      courseId,
      shiftDatesFrom,
    }: {
      cohortId: string;
      courseId: string;
      shiftDatesFrom?: string;
    }) =>
      call(
        api.get(CohortApi).copyCourseOutlineToCohort({
          cohortId,
          copyCourseOutlineToCohortRequest: { courseId, ...(shiftDatesFrom ? { shiftDatesFrom } : {}) },
        }),
      ),
    onSettled: (_r, _e, variables) =>
      qc.invalidateQueries({ queryKey: queryKey('lms', 'cohort-outline', variables.cohortId) }),
  });
}

/** The cohort's groups, with creating and removing one (LMS-24). */
export function useCohortGroups(cohortId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('lms', 'cohort-groups', cohortId),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(CohortApi)
            .listCohortGroups({ cohortId: cohortId as string, size: 200, ...pageQuery(page) }),
        ),
      ),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** lms.CreateCohortGroup. */
export function useCreateCohortGroup() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cohortId, name, description }: { cohortId: string; name: string; description?: string }) =>
      call(
        api.get(CohortApi).createCohortGroup({
          cohortId,
          cohortGroup: { cohortId, name, ...(description?.trim() ? { description } : {}) },
        }),
      ),
    onSettled: (_r, _e, variables) =>
      qc.invalidateQueries({ queryKey: queryKey('lms', 'cohort-groups', variables.cohortId) }),
  });
}

/** lms.DeleteCohortGroup, under the group's etag. */
export function useDeleteCohortGroup() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cohortId,
      cohortGroupId,
      etag,
    }: {
      cohortId: string;
      cohortGroupId: string;
      etag: string;
    }) => call(api.get(CohortApi).deleteCohortGroup({ cohortId, cohortGroupId, ifMatch: etag })),
    onSettled: (_r, _e, variables) =>
      qc.invalidateQueries({ queryKey: queryKey('lms', 'cohort-groups', variables.cohortId) }),
  });
}
