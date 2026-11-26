import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { admission } from '@mysms/sdk';
import { useState } from 'react';
import { AdmissionCycleApi } from '@mysms/sdk/admission/apis/AdmissionCycleApi';
import { ApplicationApi } from '@mysms/sdk/admission/apis/ApplicationApi';
import { newIdempotencyKey } from '../api/ids';
import { isApiError } from '../api/problem';
import { allPages, pageQuery } from '../api/pages';
import { patchOf } from '../api/patch';
import { call, callWithEtag } from '../api/unwrap';
import type { PermissionCode } from '../generated/permissions';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type AdmissionCycle = admission.AdmissionCycle;
export type Application = admission.Application;
export type ApplicationGuardian = admission.ApplicationGuardian;
export type ApplicationEmergencyContact = admission.ApplicationEmergencyContact;
export type EnrollApplicantResult = admission.EnrollApplicantResult;
export type ApplicationStatus = NonNullable<Application['status']>;
export type CycleStatus = NonNullable<AdmissionCycle['status']>;
export type GuardianRelationship = ApplicationGuardian['relationship'];

/** The stages a cycle passes through, in order (api/state-machines.md "admission.AdmissionCycle"). */
export const CYCLE_STAGES: readonly CycleStatus[] = ['draft', 'confirmed', 'open', 'admission', 'closed'];

/**
 * Where a cycle stands on its timeline: the index of its stage in {@link CYCLE_STAGES}, or -1 for a cancelled
 * cycle (which leaves the timeline).
 */
export const cycleStage = (status: AdmissionCycle['status']): number =>
  status ? CYCLE_STAGES.indexOf(status) : 0;

/** The pipeline of applications, in the order an application moves (ui/02 ADMN-02: one column per status). */
export const PIPELINE: readonly ApplicationStatus[] = [
  'draft',
  'submitted',
  'pending',
  'confirmed',
  'approved',
  'admitted',
  'done',
  'rejected',
  'cancelled',
];

/** A decision step of an application. */
export type ApplicationStep =
  'submit' | 'confirm' | 'markPending' | 'approve' | 'confirmAdmission' | 'reject' | 'cancel' | 'reset';

/**
 * The steps of an application with the states they are taken from and their permission — the server's table
 * (api/state-machines.md "admission.Application"); `reason` marks the steps that keep one.
 */
export const APPLICATION_STEPS: Readonly<
  Record<ApplicationStep, { from: readonly ApplicationStatus[]; permission: PermissionCode; reason: boolean }>
> = {
  submit: { from: ['draft'], permission: 'admission.application.submit_application', reason: false },
  confirm: {
    from: ['submitted', 'pending'],
    permission: 'admission.application.confirm_application',
    reason: false,
  },
  markPending: {
    from: ['submitted', 'confirmed'],
    permission: 'admission.application.mark_application_pending',
    reason: true,
  },
  approve: { from: ['confirmed'], permission: 'admission.application.approve_application', reason: false },
  confirmAdmission: {
    from: ['approved'],
    permission: 'admission.application.confirm_admission',
    reason: false,
  },
  reject: {
    from: ['submitted', 'pending', 'confirmed', 'approved'],
    permission: 'admission.application.reject_application',
    reason: true,
  },
  cancel: {
    from: ['draft', 'submitted', 'pending', 'confirmed', 'approved', 'admitted'],
    permission: 'admission.application.cancel_application',
    reason: true,
  },
  reset: {
    from: ['rejected', 'cancelled'],
    permission: 'admission.application.reset_application',
    reason: false,
  },
};

/** The steps an application in this state allows, in the order the screens offer them. */
export function stepsFor(status: Application['status']): ApplicationStep[] {
  return (Object.keys(APPLICATION_STEPS) as ApplicationStep[]).filter((step) =>
    APPLICATION_STEPS[step].from.includes(status ?? 'draft'),
  );
}

/**
 * The application number a form sends: the contract asks the client for one. For an applicant's own application the
 * server replaces it with the next of the school's series; for the office it stands, unique in the school (day and
 * six random characters keep collisions out of reach).
 */
export function newApplicationNumber(now: Date = new Date(), random: () => number = Math.random): string {
  const day = now.toISOString().slice(0, 10).replace(/-/g, '');
  const alphabet = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
  let tail = '';
  for (let i = 0; i < 6; i++) tail += alphabet[Math.floor(random() * alphabet.length)] ?? '2';
  return `W-${day}-${tail}`;
}

const CYCLE = (id: string | null) => queryKey('admission', 'admission-cycle', id);
const APPLICATIONS = (params: unknown) => queryKey('admission', 'applications', params);
const APPLICATION = (id: string | null) => queryKey('admission', 'application', id);
const GUARDIANS = (id: string | null) => queryKey('admission', 'application-guardians', id);
const CONTACTS = (id: string | null) => queryKey('admission', 'application-contacts', id);

/** After a change to an application: its detail, its parts, every list and the standard screens are stale. */
async function refreshApplications(qc: QueryClient, applicationId?: string): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'admission.Application') });
  void qc.invalidateQueries({ queryKey: queryKey('records', 'admission.Application#item') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: queryKey('admission', 'applications') }),
    ...(applicationId
      ? [
          qc.invalidateQueries({ queryKey: APPLICATION(applicationId) }),
          qc.invalidateQueries({ queryKey: GUARDIANS(applicationId) }),
          qc.invalidateQueries({ queryKey: CONTACTS(applicationId) }),
        ]
      : []),
  ]);
}

/** admission.GetAdmissionCycle: what the school publishes about a cycle (an applicant reads it while it is open). */
export function useAdmissionCycle(admissionCycleId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && admissionCycleId !== null,
    queryKey: CYCLE(admissionCycleId),
    queryFn: () =>
      call(api.get(AdmissionCycleApi).getAdmissionCycle({ admissionCycleId: admissionCycleId as string })),
    staleTime: STALE_TIME.reference,
    retry: false,
  });
}

/** admission.ListAdmissionCycles of the caller's institutions, newest first (the pipeline's cycle filter). */
export function useAdmissionCycles(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('admission', 'admission-cycles', { all: true }),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(AdmissionCycleApi)
            .listAdmissionCycles({ size: 200, sort: ['startDate,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * admission.ListApplications, whole: of one cycle, or of every cycle the caller reaches. The office reaches its
 * institutions; an applicant only their own applications.
 */
export function useApplications(admissionCycleId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: APPLICATIONS({ admissionCycleId }),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ApplicationApi).listApplications({
            size: 200,
            sort: ['createTime,desc'],
            ...(admissionCycleId ? { filter: `admissionCycleId = "${admissionCycleId}"` } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * The caller's draft in a cycle, if they have started one: how an applicant resumes their form. The server lists an
 * applicant's own applications only, so the newest draft of the cycle is theirs.
 */
export function useMyDraft(admissionCycleId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && admissionCycleId !== null,
    queryKey: APPLICATIONS({ admissionCycleId, draft: true }),
    queryFn: async () => {
      const page = await call(
        api.get(ApplicationApi).listApplications({
          size: 1,
          sort: ['createTime,desc'],
          filter: `admissionCycleId = "${admissionCycleId as string}" AND status = "draft"`,
        }),
      );
      return page.data[0] ?? null;
    },
    staleTime: STALE_TIME.detail,
  });
}

/** admission.GetApplication with its ETag for the next step or change. */
export function useApplication(applicationId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && applicationId !== null,
    queryKey: APPLICATION(applicationId),
    queryFn: () =>
      callWithEtag(api.get(ApplicationApi).getApplicationRaw({ applicationId: applicationId as string })),
    staleTime: STALE_TIME.detail,
    retry: false,
  });
}

/** admission.ListApplicationGuardians of one application (at most one per relationship). */
export function useApplicationGuardians(applicationId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && applicationId !== null,
    queryKey: GUARDIANS(applicationId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ApplicationApi).listApplicationGuardians({
            applicationId: applicationId as string,
            size: 50,
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** admission.ListApplicationEmergencyContacts of one application, by priority. */
export function useApplicationContacts(applicationId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && applicationId !== null,
    queryKey: CONTACTS(applicationId),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(
          api.get(ApplicationApi).listApplicationEmergencyContacts({
            applicationId: applicationId as string,
            size: 50,
            ...pageQuery(page),
          }),
        ),
      );
      return read.items.sort((a, b) => a.priority - b.priority);
    },
    staleTime: STALE_TIME.detail,
  });
}

/**
 * One decision step of an application under the etag the screen last read (412 when someone changed it meanwhile,
 * 409 when its state no longer allows the step); the reason is kept by the steps that keep one.
 */
export function useApplicationStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      applicationId,
      step,
      etag,
      reason,
    }: {
      applicationId: string;
      step: ApplicationStep;
      etag: string | null;
      reason?: string;
    }) => {
      const applications = api.get(ApplicationApi);
      const plain = etag ? { etag } : {};
      const kept = { ...plain, ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      switch (step) {
        case 'submit':
          return call(applications.submitApplication({ applicationId, submitApplicationRequest: plain }));
        case 'confirm':
          return call(applications.confirmApplication({ applicationId, confirmApplicationRequest: plain }));
        case 'markPending':
          return call(
            applications.markApplicationPending({ applicationId, markApplicationPendingRequest: kept }),
          );
        case 'approve':
          return call(applications.approveApplication({ applicationId, approveApplicationRequest: plain }));
        case 'confirmAdmission':
          return call(applications.confirmAdmission({ applicationId, confirmAdmissionRequest: plain }));
        case 'reject':
          return call(applications.rejectApplication({ applicationId, rejectApplicationRequest: kept }));
        case 'cancel':
          return call(applications.cancelApplication({ applicationId, cancelApplicationRequest: kept }));
        case 'reset':
          return call(applications.resetApplication({ applicationId, resetApplicationRequest: plain }));
      }
    },
    onSettled: (_data, _error, variables) => refreshApplications(qc, variables.applicationId),
  });
}

/** What the enrolment wizard sends: a class section (K-12) or a batch, a roll number, and the portal accounts. */
export interface Enrolment {
  applicationId: string;
  classSectionId?: string;
  batchId?: string;
  rollNumber?: string;
  createPortalAccounts: boolean;
}

/**
 * Whether a failed enrolment is retried with the same Idempotency-Key. The server keeps the answer to a key unless it
 * was a server failure (5xx) or a try-again answer (408, 425, 429), and a network failure leaves no answer at all:
 * those are retried as the same attempt, so a half-finished enrolment is completed and never started twice. Any
 * other answer (409, 412, 422 …) is kept by the server and would only be replayed, or refused for a changed
 * request (IDEMPOTENCY_KEY_REUSED): the corrected request is a new attempt with a new key.
 */
export function retriesSameAttempt(error: unknown): boolean {
  if (!isApiError(error)) return true;
  return error.status >= 500 || error.status === 408 || error.status === 425 || error.status === 429;
}

/**
 * admission.EnrollApplicant. The Idempotency-Key is kept across retries of one attempt (see
 * {@link retriesSameAttempt}) and drawn anew after a success or an answer the server keeps.
 */
export function useEnrollApplicant() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  // one key per logical attempt
  const [attempt, setAttempt] = useState(newIdempotencyKey);
  return useMutation({
    mutationFn: ({ applicationId, classSectionId, batchId, rollNumber, createPortalAccounts }: Enrolment) =>
      call(
        api.get(ApplicationApi).enrollApplicant({
          applicationId,
          idempotencyKey: attempt,
          enrollApplicantRequest: {
            ...(classSectionId ? { classSectionId } : {}),
            ...(batchId ? { batchId } : {}),
            ...(rollNumber?.trim() ? { rollNumber: rollNumber.trim() } : {}),
            createPortalAccounts,
          },
        }),
      ),
    onSuccess: () => setAttempt(newIdempotencyKey()),
    onError: (error) => {
      if (!retriesSameAttempt(error)) setAttempt(newIdempotencyKey());
    },
    onSettled: (_data, _error, variables) => refreshApplications(qc, variables.applicationId),
  });
}

/** What an applicant (or the office) writes on the form; the server keeps the office's fields to itself. */
export type ApplicationForm = Pick<
  Application,
  | 'fullName'
  | 'firstName'
  | 'lastName'
  | 'dateOfBirth'
  | 'gender'
  | 'email'
  | 'mobile'
  | 'addressLine1'
  | 'addressLine2'
  | 'cityName'
  | 'postalCode'
  | 'previousInstitution'
  | 'previousCourse'
  | 'previousResult'
  | 'medicalDetails'
>;

/** admission.CreateApplication: a draft in a cycle, numbered by {@link newApplicationNumber}. */
export function useStartApplication() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      cycle,
      form,
    }: {
      cycle: Pick<AdmissionCycle, 'id' | 'institutionId'>;
      form: ApplicationForm;
    }) =>
      callWithEtag(
        api.get(ApplicationApi).createApplicationRaw({
          application: {
            ...form,
            institutionId: cycle.institutionId,
            admissionCycleId: cycle.id,
            applicationNumber: newApplicationNumber(),
          },
        }),
      ),
    onSettled: () => refreshApplications(qc),
  });
}

/**
 * The PATCH body of an edited application form (JAVA-CODING-RULES §7.2): only the fields that differ from the
 * application as saved, a field the applicant blanked or removed sent as null so it is cleared.
 */
export function applicationChanges(
  saved: ApplicationForm,
  form: ApplicationForm,
): admission.ApplicationUpdate {
  const changes: Record<string, unknown> = {};
  const fields = new Set([...Object.keys(saved), ...Object.keys(form)]) as Set<keyof ApplicationForm>;
  for (const field of fields) {
    if (form[field] !== saved[field]) changes[field] = form[field];
  }
  return patchOf(changes) as admission.ApplicationUpdate;
}

/** admission.UpdateApplication of the fields the form changed, under the ETag last read. */
export function useSaveApplication() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      applicationId,
      etag,
      saved,
      form,
    }: {
      applicationId: string;
      etag: string;
      saved: ApplicationForm;
      form: ApplicationForm;
    }) =>
      callWithEtag(
        api.get(ApplicationApi).updateApplicationRaw({
          applicationId,
          ifMatch: etag,
          applicationUpdate: applicationChanges(saved, form),
        }),
      ),
    onSettled: (_data, _error, variables) => refreshApplications(qc, variables.applicationId),
  });
}

/** admission.CreateApplicationGuardian / DeleteApplicationGuardian on a draft. */
export function useApplicationGuardianChange() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (
      change:
        | {
            applicationId: string;
            add: Pick<ApplicationGuardian, 'relationship' | 'name' | 'mobile' | 'email' | 'occupation'>;
          }
        | { applicationId: string; remove: ApplicationGuardian },
    ) =>
      'add' in change
        ? call(
            api.get(ApplicationApi).createApplicationGuardian({
              applicationId: change.applicationId,
              applicationGuardian: change.add,
            }),
          ).then(() => undefined)
        : call(
            api.get(ApplicationApi).deleteApplicationGuardian({
              applicationId: change.applicationId,
              applicationGuardianId: change.remove.id ?? '',
              ifMatch: change.remove.etag ?? '',
            }),
          ),
    onSettled: (_data, _error, variables) => refreshApplications(qc, variables.applicationId),
  });
}

/** admission.CreateApplicationEmergencyContact / DeleteApplicationEmergencyContact on a draft. */
export function useApplicationContactChange() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (
      change:
        | {
            applicationId: string;
            add: Pick<ApplicationEmergencyContact, 'priority' | 'contactNumber' | 'relationship'>;
          }
        | { applicationId: string; remove: ApplicationEmergencyContact },
    ) =>
      'add' in change
        ? call(
            api.get(ApplicationApi).createApplicationEmergencyContact({
              applicationId: change.applicationId,
              applicationEmergencyContact: change.add,
            }),
          ).then(() => undefined)
        : call(
            api.get(ApplicationApi).deleteApplicationEmergencyContact({
              applicationId: change.applicationId,
              applicationEmergencyContactId: change.remove.id ?? '',
              ifMatch: change.remove.etag ?? '',
            }),
          ),
    onSettled: (_data, _error, variables) => refreshApplications(qc, variables.applicationId),
  });
}
