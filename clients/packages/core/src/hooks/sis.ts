import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { academics, sis } from '@mysms/sdk';
import { GradeLevelApi } from '@mysms/sdk/academics/apis/GradeLevelApi';
import { SectionApi } from '@mysms/sdk/academics/apis/SectionApi';
import { DisciplineIncidentApi } from '@mysms/sdk/sis/apis/DisciplineIncidentApi';
import { PersonApi } from '@mysms/sdk/people/apis/PersonApi';
import { GuardianApi } from '@mysms/sdk/sis/apis/GuardianApi';
import { PromotionApi } from '@mysms/sdk/sis/apis/PromotionApi';
import { StudentActivityApi } from '@mysms/sdk/sis/apis/StudentActivityApi';
import { StudentApi } from '@mysms/sdk/sis/apis/StudentApi';
import { StudentEnrollmentApi } from '@mysms/sdk/sis/apis/StudentEnrollmentApi';
import { StudentHealthRecordApi } from '@mysms/sdk/sis/apis/StudentHealthRecordApi';
import { SubjectRegistrationApi } from '@mysms/sdk/sis/apis/SubjectRegistrationApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { anyOf } from './attendance';

export type Student = sis.Student;
export type StudentSummary = sis.StudentSummary;
export type ChildCard = sis.ChildCard;
export type StudentStatus = NonNullable<Student['status']>;
export type StudentEnrollment = sis.StudentEnrollment;
export type StudentHealthRecord = sis.StudentHealthRecord;
export type StudentActivity = sis.StudentActivity;
export type DisciplineIncident = sis.DisciplineIncident;
export type Promotion = sis.Promotion;
export type SubjectRegistration = sis.SubjectRegistration;
export type SubjectRegistrationLine = sis.SubjectRegistrationLine;
export type GradeLevel = academics.GradeLevel;
export type Section = academics.Section;
/** The steps of a discipline incident (api/state-machines.md "DisciplineIncident"). */
export type IncidentStep = 'markActionTaken' | 'markSeen';
/** The steps of a subject registration a screen can ask for (api/state-machines.md "SubjectRegistration"). */
export type RegistrationStep = 'submit' | 'approve' | 'reject' | 'reset' | 'loadCourseSubjects';
export type PromotionStep = 'approve' | 'reject';

/** Every status a student record can have, current first (sis.ChangeStudentStatus). */
export const STUDENT_STATUSES: readonly StudentStatus[] = [
  'active',
  'inactive',
  'withdrawn',
  'transferred',
  'graduated',
  'alumni',
];
/** The statuses of a student who has left: they take an exit date and end the open placement. */
export const EXIT_STATUSES: readonly StudentStatus[] = ['withdrawn', 'transferred', 'graduated', 'alumni'];
export const isExitStatus = (status: StudentStatus) => EXIT_STATUSES.includes(status);

const SUMMARY = (studentId: string | null) => queryKey('sis', 'student-summary', studentId);
const OF_STUDENT = (resource: string, studentId: string | null) => queryKey('sis', resource, { studentId });
const LINES = (registrationId: string | null) =>
  queryKey('sis', 'subject-registration-lines', registrationId);

/** Marks the standard list and form of a record type stale: a step changed rows those screens may be showing. */
function staleRecords(qc: QueryClient, recordType: string): void {
  void qc.invalidateQueries({ queryKey: queryKey('records', recordType) });
  void qc.invalidateQueries({ queryKey: queryKey('records', `${recordType}#item`) });
}

/** After a change to a student: the summary, the student lists and every per-student list. */
async function refreshStudent(qc: QueryClient): Promise<void> {
  staleRecords(qc, 'sis.Student');
  staleRecords(qc, 'sis.Guardian');
  staleRecords(qc, 'sis.StudentEnrollment');
  await Promise.all([
    qc.invalidateQueries({ queryKey: queryKey('sis', 'student-summary') }),
    qc.invalidateQueries({ queryKey: queryKey('sis', 'student') }),
    qc.invalidateQueries({ queryKey: queryKey('sis', 'student-enrollments') }),
    qc.invalidateQueries({ queryKey: queryKey('sis', 'my-children') }),
    // a guardian's portal account shows on the guardian cards of the 360°
    qc.invalidateQueries({ queryKey: queryKey('sis', 'guardian-cards') }),
  ]);
}

/**
 * sis.GetStudentSummary: the student 360° overview — the student, their person, the placement of the year (else the
 * current one), course registrations, guardian links and open incidents. Staff in scope, the student and their
 * family may read it; the server leaves out what it may not fill (api/state-machines.md "Student").
 */
export function useStudentSummary(studentId: string | null, academicYearId?: string) {
  const { api } = useRuntime();
  return useQuery({
    enabled: studentId !== null,
    queryKey: [...SUMMARY(studentId), academicYearId ?? null],
    queryFn: () =>
      call(
        api.get(StudentApi).getStudentSummary({
          studentId: studentId as string,
          ...(academicYearId ? { academicYearId } : {}),
        }),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** sis.ListMyChildren: the signed-in guardian's children as cards, all of them (a family is small). */
export function useMyChildren(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('sis', 'my-children'),
    queryFn: () =>
      allPages((page) => call(api.get(GuardianApi).listMyChildren({ size: 50, ...pageQuery(page) }))),
    staleTime: STALE_TIME.list,
  });
}

/** The student's placements, newest first (sis.ListStudentEnrollments by student). */
export function useStudentEnrollments(studentId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && studentId !== null,
    queryKey: OF_STUDENT('student-enrollments', studentId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StudentEnrollmentApi).listStudentEnrollments({
            size: 100,
            filter: `studentId = "${studentId as string}"`,
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** The student's health records, one per academic year (sis.ListStudentHealthRecords by student). */
export function useStudentHealthRecords(studentId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && studentId !== null,
    queryKey: OF_STUDENT('student-health-records', studentId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StudentHealthRecordApi).listStudentHealthRecords({
            size: 100,
            filter: `studentId = "${studentId as string}"`,
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** The student's activities, newest first (sis.ListStudentActivities by student). */
export function useStudentActivities(studentId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && studentId !== null,
    queryKey: OF_STUDENT('student-activities', studentId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StudentActivityApi).listStudentActivities({
            size: 100,
            filter: `studentId = "${studentId as string}"`,
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * Discipline incidents, newest first: a student's (by id), or — without a student — the ones no action was taken on
 * yet, which is the teacher's working list (sis.ListDisciplineIncidents).
 */
export function useDisciplineIncidents(studentId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: OF_STUDENT('discipline-incidents', studentId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(DisciplineIncidentApi).listDisciplineIncidents({
            size: 100,
            filter: studentId ? `studentId = "${studentId}"` : 'actionTaken = false',
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** Subject registrations of these students (a student's own, or one student's on the 360°), newest first. */
export function useSubjectRegistrations(studentIds: readonly string[], enabled = true) {
  const { api } = useRuntime();
  const ids = [...new Set(studentIds)].sort();
  return useQuery({
    enabled: enabled && ids.length > 0,
    queryKey: queryKey('sis', 'subject-registrations', ids),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(SubjectRegistrationApi).listSubjectRegistrations({
            size: 100,
            filter: ids.map((id) => `studentId = "${id}"`).join(' OR '),
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** The lines (subjects) of one subject registration (sis.ListSubjectRegistrationLines). */
export function useSubjectRegistrationLines(registrationId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: registrationId !== null,
    queryKey: LINES(registrationId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(SubjectRegistrationApi).listSubjectRegistrationLines({
            subjectRegistrationId: registrationId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** Grade levels of an institution in the school's order (academics.ListGradeLevels), for a promotion's target. */
export function useGradeLevels(institutionId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && institutionId !== null,
    queryKey: queryKey('academics', 'grade-levels', institutionId),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(
          api.get(GradeLevelApi).listGradeLevels({
            size: 500,
            filter: `institutionId = "${institutionId as string}"`,
            ...pageQuery(page),
          }),
        ),
      );
      return read.items.sort(
        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name),
      );
    },
    staleTime: STALE_TIME.reference,
  });
}

/** Sections of an institution (academics.ListSections), for a promotion's target. */
export function useSections(institutionId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && institutionId !== null,
    queryKey: queryKey('academics', 'sections', institutionId),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(
          api.get(SectionApi).listSections({
            size: 500,
            filter: `institutionId = "${institutionId as string}"`,
            ...pageQuery(page),
          }),
        ),
      );
      return read.items.sort(
        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name),
      );
    },
    staleTime: STALE_TIME.reference,
  });
}

/** Promotions by id (sis.ListPromotions), forty at a time: the wizard re-reads the ones it reviews. */
export function usePromotions(promotionIds: readonly string[]) {
  const { api } = useRuntime();
  const ids = [...promotionIds];
  return useQuery({
    enabled: ids.length > 0,
    queryKey: queryKey('sis', 'promotions', ids),
    queryFn: async () => {
      const found: Promotion[] = [];
      for (let start = 0; start < ids.length; start += 40) {
        const chunk = ids.slice(start, start + 40);
        const page = await call(api.get(PromotionApi).listPromotions({ size: 40, filter: anyOf(chunk) }));
        found.push(...page.data);
      }
      const order = new Map(ids.map((id, index) => [id, index]));
      return found.sort((a, b) => (order.get(a.id ?? '') ?? 0) - (order.get(b.id ?? '') ?? 0));
    },
    staleTime: STALE_TIME.detail,
  });
}

/**
 * The promotions of an institution that still wait to be decided or applied (proposed or approved), oldest first:
 * the review list of a role that decides or applies promotions without proposing them (sis.ListPromotions).
 */
export function useOpenPromotions(institutionId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && institutionId !== null,
    queryKey: queryKey('sis', 'promotions', { institutionId, open: true }),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(PromotionApi).listPromotions({
            size: 500,
            filter: `institutionId = "${institutionId as string}" AND (status = "proposed" OR status = "approved")`,
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/**
 * sis.ChangeStudentStatus under the etag the screen last read. An exit status needs the exit date and ends the
 * student's open placement; going back to active or inactive clears the exit details (sent only for an exit).
 */
export function useChangeStudentStatus() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      studentId,
      etag,
      status,
      exitDate,
      exitReason,
    }: {
      studentId: string;
      etag: string | null;
      status: StudentStatus;
      exitDate?: string;
      exitReason?: string;
    }) =>
      call(
        api.get(StudentApi).changeStudentStatus({
          studentId,
          changeStudentStatusRequest: {
            status,
            ...(etag ? { etag } : {}),
            ...(isExitStatus(status) && exitDate ? { exitDate } : {}),
            ...(isExitStatus(status) && exitReason?.trim() ? { exitReason: exitReason.trim() } : {}),
          },
        }),
      ),
    onSettled: () => refreshStudent(qc),
  });
}

/**
 * sis.IssueBonafideCertificate: the next certificate number of the institution is recorded on the student; the
 * answer is the finished operation (the certificate document itself is not rendered yet).
 */
export function useIssueBonafideCertificate() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ studentId, purpose }: { studentId: string; purpose?: string }) =>
      call(
        api.get(StudentApi).issueBonafideCertificate({
          studentId,
          issueBonafideCertificateRequest: purpose?.trim() ? { purpose: purpose.trim() } : {},
        }),
      ),
    onSettled: () => refreshStudent(qc),
  });
}

/**
 * sis.CreateStudentPortalAccount / sis.CreatePortalAccount: the login account of a student or a guardian, opened in
 * iam and linked to the record; refused (409) when the record has one or lacks what the account signs in with.
 */
export function useCreatePortalAccount() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ who, id }: { who: 'student' | 'guardian'; id: string }) =>
      who === 'student'
        ? call(
            api
              .get(StudentApi)
              .createStudentPortalAccount({ studentId: id, createStudentPortalAccountRequest: {} }),
          )
        : call(api.get(GuardianApi).createPortalAccount({ guardianId: id, createPortalAccountRequest: {} })),
    onSettled: () => refreshStudent(qc),
  });
}

/**
 * sis.MarkIncidentActionTaken (with what the school did) and sis.MarkIncidentSeen (a guardian's acknowledgement),
 * under the etag the screen last read.
 */
export function useIncidentStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      incidentId,
      step,
      etag,
      response,
    }: {
      incidentId: string;
      step: IncidentStep;
      etag: string | null;
      response?: string;
    }) => {
      const incidents = api.get(DisciplineIncidentApi);
      const etagPart = etag ? { etag } : {};
      if (step === 'markSeen')
        return call(
          incidents.markIncidentSeen({ disciplineIncidentId: incidentId, markIncidentSeenRequest: etagPart }),
        );
      return call(
        incidents.markIncidentActionTaken({
          disciplineIncidentId: incidentId,
          markIncidentActionTakenRequest: {
            ...etagPart,
            ...(response?.trim() ? { response: response.trim() } : {}),
          },
        }),
      );
    },
    onSettled: async () => {
      staleRecords(qc, 'sis.DisciplineIncident');
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKey('sis', 'discipline-incidents') }),
        qc.invalidateQueries({ queryKey: queryKey('sis', 'student-summary') }),
      ]);
    },
  });
}

/** After a promotion step: the wizard's rows, the promotion and enrolment lists. */
async function refreshPromotions(qc: QueryClient): Promise<void> {
  staleRecords(qc, 'sis.Promotion');
  staleRecords(qc, 'sis.StudentEnrollment');
  await qc.invalidateQueries({ queryKey: queryKey('sis', 'promotions') });
}

/** sis.ProposePromotions: a proposal for every student of a class (or the listed ones); answers the class's promotions. */
export function useProposePromotions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: sis.ProposePromotionsRequest) =>
      call(api.get(PromotionApi).proposePromotions({ proposePromotionsRequest: request })),
    onSettled: () => refreshPromotions(qc),
  });
}

/** sis.ApprovePromotion / sis.RejectPromotion of one proposal, under its etag; a rejection keeps its reason. */
export function usePromotionStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      promotionId,
      step,
      etag,
      reason,
    }: {
      promotionId: string;
      step: PromotionStep;
      etag: string | null;
      reason?: string;
    }) => {
      const promotions = api.get(PromotionApi);
      const body = { ...(etag ? { etag } : {}) };
      if (step === 'approve')
        return call(promotions.approvePromotion({ promotionId, approvePromotionRequest: body }));
      return call(
        promotions.rejectPromotion({
          promotionId,
          rejectPromotionRequest: { ...body, ...(reason?.trim() ? { reason: reason.trim() } : {}) },
        }),
      );
    },
    onSettled: () => refreshPromotions(qc),
  });
}

/** sis.ApplyPromotions: next year's placements for the approved promotions, all or none; answers the operation. */
export function useApplyPromotions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (promotionIds: readonly string[]) =>
      call(
        api
          .get(PromotionApi)
          .applyPromotions({ applyPromotionsRequest: { promotionIds: [...promotionIds] } }),
      ),
    onSettled: async () => {
      await refreshPromotions(qc);
      await refreshStudent(qc);
    },
  });
}

/**
 * The steps of a subject registration: submit (the student), approve / reject / reset (a teacher), and load course
 * subjects, which fills a draft from the course's plan. The etag the screen last read goes with every step that
 * takes one.
 */
export function useSubjectRegistrationStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      registrationId,
      step,
      etag,
      reason,
    }: {
      registrationId: string;
      step: RegistrationStep;
      etag: string | null;
      reason?: string;
    }) => {
      const registrations = api.get(SubjectRegistrationApi);
      const subjectRegistrationId = registrationId;
      const body = { ...(etag ? { etag } : {}) };
      switch (step) {
        case 'submit':
          return call(
            registrations.submitSubjectRegistration({
              subjectRegistrationId,
              submitSubjectRegistrationRequest: body,
            }),
          );
        case 'approve':
          return call(
            registrations.approveSubjectRegistration({
              subjectRegistrationId,
              approveSubjectRegistrationRequest: body,
            }),
          );
        case 'reject':
          return call(
            registrations.rejectSubjectRegistration({
              subjectRegistrationId,
              rejectSubjectRegistrationRequest: {
                ...body,
                ...(reason?.trim() ? { reason: reason.trim() } : {}),
              },
            }),
          );
        case 'reset':
          return call(
            registrations.resetSubjectRegistration({
              subjectRegistrationId,
              resetSubjectRegistrationRequest: body,
            }),
          );
        default:
          return call(registrations.loadCourseSubjects({ subjectRegistrationId, body: {} }));
      }
    },
    onSettled: async (_data, _error, { registrationId }) => {
      staleRecords(qc, 'sis.SubjectRegistration');
      staleRecords(qc, 'sis.SubjectRegistrationLine');
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKey('sis', 'subject-registrations') }),
        qc.invalidateQueries({ queryKey: LINES(registrationId) }),
      ]);
    },
  });
}

/** sis.DeleteSubjectRegistrationLine: a subject taken off a draft (an elective the student does not choose). */
export function useRemoveRegistrationLine() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ registrationId, line }: { registrationId: string; line: SubjectRegistrationLine }) =>
      call(
        api.get(SubjectRegistrationApi).deleteSubjectRegistrationLine({
          subjectRegistrationId: registrationId,
          subjectRegistrationLineId: line.id ?? '',
          ifMatch: line.etag ?? '',
        }),
      ),
    onSettled: (_data, _error, { registrationId }) =>
      qc.invalidateQueries({ queryKey: LINES(registrationId) }),
  });
}

/** A guardian of a student as the 360° shows it: the record and, when people shows it, the person's name. */
export interface GuardianCard {
  guardian: sis.Guardian;
  name?: string;
}

/**
 * The guardian records behind a student's guardian links, with their names: sis.ListGuardians by id, then
 * people.ListPersons for the names when the role may read persons. Guardian id → card.
 */
export function useGuardianCards(guardianIds: readonly string[], withNames: boolean, enabled = true) {
  const { api } = useRuntime();
  const ids = [...new Set(guardianIds)].sort();
  return useQuery({
    enabled: enabled && ids.length > 0,
    queryKey: queryKey('sis', 'guardian-cards', { ids, withNames }),
    queryFn: async (): Promise<ReadonlyMap<string, GuardianCard>> => {
      const guardians = await call(api.get(GuardianApi).listGuardians({ size: 50, filter: anyOf(ids) }));
      const names = new Map<string, string>();
      const personIds = [...new Set(guardians.data.map((guardian) => guardian.personId))];
      if (withNames && personIds.length > 0) {
        const persons = await call(api.get(PersonApi).listPersons({ size: 50, filter: anyOf(personIds) }));
        for (const person of persons.data) if (person.id) names.set(person.id, person.displayName);
      }
      return new Map(
        guardians.data.flatMap((guardian) => {
          const name = names.get(guardian.personId);
          return guardian.id ? [[guardian.id, { guardian, ...(name ? { name } : {}) }] as const] : [];
        }),
      );
    },
    staleTime: STALE_TIME.list,
  });
}
