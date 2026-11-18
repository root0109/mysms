import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { lms } from '@mysms/sdk';
import { BadgeAwardApi } from '@mysms/sdk/lms/apis/BadgeAwardApi';
import { BadgeApi } from '@mysms/sdk/lms/apis/BadgeApi';
import { CertificateApi } from '@mysms/sdk/lms/apis/CertificateApi';
import { CertificateEvaluationApi } from '@mysms/sdk/lms/apis/CertificateEvaluationApi';
import { CertificateRequestApi } from '@mysms/sdk/lms/apis/CertificateRequestApi';
import { EnrollmentApi } from '@mysms/sdk/lms/apis/EnrollmentApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Certificate = lms.Certificate;
export type CertificateRequest = lms.CertificateRequest;
export type CertificateEvaluation = lms.CertificateEvaluation;
export type CertificateVerification = lms.CertificateVerification;
export type BadgeAward = lms.BadgeAward;
export type MemberProfile = lms.MemberProfile;

const CERTIFICATES = (filter: string | null) => queryKey('lms', 'certificates', filter);
const AWARDS = (filter: string | null) => queryKey('lms', 'badge-awards', filter);
const BOOKINGS = (filter: string | null) => queryKey('lms', 'certificate-requests', filter);
const EVALUATIONS = (filter: string | null) => queryKey('lms', 'certificate-evaluations', filter);
const PROFILE = (userId: string | null) => queryKey('lms', 'member-profile', userId);
const VERIFICATION = (certificateId: string | null) =>
  queryKey('lms', 'certificate-verification', certificateId);

/** lms.ListCertificates, newest first; `mine` narrows to the signed-in user's own. */
export function useCertificates(userId?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: CERTIFICATES(userId ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(CertificateApi).listCertificates({
            size: 200,
            sort: ['createTime desc'],
            ...(userId ? { filter: `userId = "${userId}"` } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.DownloadCertificate: a fresh link to the certificate's PDF. */
export function useDownloadCertificate() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ certificateId }: { certificateId: string }) =>
      call(api.get(CertificateApi).downloadCertificate({ certificateId })),
  });
}

/** lms.VerifyCertificate: whether the certificate is real and current (nothing more is told). */
export function useVerifyCertificate(certificateId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: VERIFICATION(certificateId),
    queryFn: () =>
      call(api.get(CertificateApi).verifyCertificate({ certificateId: certificateId as string })),
    enabled: certificateId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListBadgeAwards, newest first; for one user when given. */
export function useBadgeAwards(userId?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: AWARDS(userId ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(BadgeAwardApi).listBadgeAwards({
            size: 200,
            sort: ['createTime desc'],
            ...(userId ? { filter: `userId = "${userId}"` } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.AwardBadge: awards the badge to a user today. */
export function useAwardBadge() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ badgeId, userId }: { badgeId: string; userId: string }) =>
      call(api.get(BadgeApi).awardBadge({ badgeId, awardBadgeRequest: { userId } })),
    onSettled: () => qc.invalidateQueries({ queryKey: AWARDS(null).slice(0, 2) }),
  });
}

/** lms.ListCertificateRequests (evaluation bookings), soonest first. */
export function useCertificateRequests(filter?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: BOOKINGS(filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(CertificateRequestApi).listCertificateRequests({
            size: 200,
            sort: ['requestDate'],
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.CreateCertificateRequest: books an evaluation slot. */
export function useCreateCertificateRequest() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ booking }: { booking: lms.CertificateRequest }) =>
      call(api.get(CertificateRequestApi).createCertificateRequest({ certificateRequest: booking })),
    onSettled: () => qc.invalidateQueries({ queryKey: BOOKINGS(null).slice(0, 2) }),
  });
}

/** lms.CancelCertificateRequest: cancels an upcoming booking, under its etag. */
export function useCancelCertificateRequest() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ bookingId, etag, reason }: { bookingId: string; etag: string | null; reason?: string }) =>
      call(
        api.get(CertificateRequestApi).cancelCertificateRequest({
          certificateRequestId: bookingId,
          cancelCertificateRequestRequest: {
            ...(etag ? { etag } : {}),
            ...(reason?.trim() ? { reason: reason.trim() } : {}),
          },
        }),
      ),
    onSettled: () => qc.invalidateQueries({ queryKey: BOOKINGS(null).slice(0, 2) }),
  });
}

/** lms.StartEvaluation: opens the evaluation of an upcoming booking (the booking completes). */
export function useStartEvaluation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ bookingId }: { bookingId: string }) =>
      call(api.get(CertificateRequestApi).startEvaluation({ certificateRequestId: bookingId, body: {} })),
    onSettled: () =>
      Promise.all([
        qc.invalidateQueries({ queryKey: BOOKINGS(null).slice(0, 2) }),
        qc.invalidateQueries({ queryKey: EVALUATIONS(null).slice(0, 2) }),
      ]),
  });
}

/** lms.ListCertificateEvaluations, newest first; the evaluator's own with their user id. */
export function useCertificateEvaluations(filter?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: EVALUATIONS(filter ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(CertificateEvaluationApi).listCertificateEvaluations({
            size: 200,
            sort: ['createTime desc'],
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.UpdateCertificateEvaluation: the evaluator's verdict (status, rating, summary) under If-Match. */
export function useUpdateCertificateEvaluation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      evaluationId,
      etag,
      changes,
    }: {
      evaluationId: string;
      etag: string;
      changes: lms.CertificateEvaluationUpdate;
    }) =>
      call(
        api.get(CertificateEvaluationApi).updateCertificateEvaluation({
          certificateEvaluationId: evaluationId,
          ifMatch: etag,
          certificateEvaluationUpdate: changes,
        }),
      ),
    onSettled: () => qc.invalidateQueries({ queryKey: EVALUATIONS(null).slice(0, 2) }),
  });
}

/** lms.IssueCertificateFromEvaluation: the certificate a passed evaluation earns. */
export function useIssueCertificateFromEvaluation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      evaluationId,
      issueDate,
      expiryDate,
      templateCode,
    }: {
      evaluationId: string;
      issueDate: string;
      expiryDate?: string;
      templateCode: string;
    }) =>
      call(
        api.get(CertificateEvaluationApi).issueCertificateFromEvaluation({
          certificateEvaluationId: evaluationId,
          issueCertificateFromEvaluationRequest: {
            issueDate,
            templateCode,
            ...(expiryDate ? { expiryDate } : {}),
          },
        }),
      ),
    onSettled: () =>
      Promise.all([
        qc.invalidateQueries({ queryKey: EVALUATIONS(null).slice(0, 2) }),
        qc.invalidateQueries({ queryKey: CERTIFICATES(null).slice(0, 2) }),
      ]),
  });
}

/** lms.GetMemberProfile: a member's public card with their published certificates and badges. */
export function useMemberProfile(userId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: PROFILE(userId),
    queryFn: () => call(api.get(EnrollmentApi).getMemberProfile({ userId: userId as string })),
    enabled: userId !== null,
    staleTime: STALE_TIME.detail,
  });
}
