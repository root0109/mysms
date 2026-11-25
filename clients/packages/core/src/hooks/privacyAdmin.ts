import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { privacy } from '@mysms/sdk';
import { NoticeApi } from '@mysms/sdk/privacy/apis/NoticeApi';
import { PurposeApi } from '@mysms/sdk/privacy/apis/PurposeApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Purpose = privacy.Purpose;
export type PurposeUpdate = privacy.PurposeUpdate;
export type Notice = privacy.Notice;
export type NoticeVersion = privacy.NoticeVersion;
export type NoticeVersionUpdate = privacy.NoticeVersionUpdate;
export type NoticeTranslation = privacy.NoticeTranslation;
export type NoticeTranslationUpdate = privacy.NoticeTranslationUpdate;
export type GrievanceContact = privacy.GrievanceContact;

const PURPOSES = queryKey('privacy', 'purposes');
const NOTICES = queryKey('privacy', 'notices');
const VERSIONS = (noticeId: string) => queryKey('privacy', 'notice-versions', noticeId);
const TRANSLATIONS = (noticeVersionId: string) => queryKey('privacy', 'notice-translations', noticeVersionId);

/** The fields a partial update names: those present in the change, in the request's update mask. */
const maskOf = (changes: object) => Object.keys(changes).join(',');

/**
 * A change of the school's settings changes what families are asked: the purposes, and the consent centre's rows
 * (its notices and widths), are read again.
 */
function refreshPurposes(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: PURPOSES });
  void qc.invalidateQueries({ queryKey: queryKey('privacy', 'my-consents') });
}

/** ADM-17: the purpose catalogue with the school's setting of each (privacy.ListPurposes), all of it (a few dozen). */
export function usePurposes(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: PURPOSES,
    queryFn: () =>
      allPages((page) =>
        call(api.get(PurposeApi).listPurposes({ size: 500, sort: ['code'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * ADM-17: privacy.UpdatePurpose — the school's switch, width ceiling and notice of one purpose, under the row's ETag
 * ("0" while the school has never set it). The server checks the rules (a notice whose active version lists the
 * purpose before it is switched on; no switch for a platform purpose) and answers each failing field.
 */
export function useUpdatePurpose() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ purposeId, etag, changes }: { purposeId: string; etag: string; changes: PurposeUpdate }) =>
      call(
        api
          .get(PurposeApi)
          .updatePurpose({ purposeId, ifMatch: etag, purposeUpdate: changes, updateMask: maskOf(changes) }),
      ),
    onSettled: () => refreshPurposes(qc),
  });
}

/** ADM-17 / ADM-18: the school's notices (privacy.ListNotices), all of them, by code. */
export function useNotices(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: NOTICES,
    queryFn: () =>
      allPages((page) =>
        call(api.get(NoticeApi).listNotices({ size: 500, sort: ['code'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** ADM-18: privacy.CreateNotice — a code (fixed once created) and a title for the school's staff. */
export function useCreateNotice() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (notice: Pick<Notice, 'code' | 'title'>) => call(api.get(NoticeApi).createNotice({ notice })),
    onSettled: () => void qc.invalidateQueries({ queryKey: NOTICES }),
  });
}

/** ADM-18: privacy.UpdateNotice — the title, under the row's ETag. */
export function useUpdateNotice() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ noticeId, etag, title }: { noticeId: string; etag: string; title: string }) =>
      call(
        api
          .get(NoticeApi)
          .updateNotice({ noticeId, ifMatch: etag, noticeUpdate: { title }, updateMask: 'title' }),
      ),
    onSettled: () => void qc.invalidateQueries({ queryKey: NOTICES }),
  });
}

/**
 * ADM-18: privacy.DeleteNotice — refused (409) while a purpose names the notice or it has an active version, which
 * is kept as consent evidence.
 */
export function useDeleteNotice() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ noticeId, etag }: { noticeId: string; etag: string }) =>
      call(api.get(NoticeApi).deleteNotice({ noticeId, ifMatch: etag })),
    onSettled: () => void qc.invalidateQueries({ queryKey: NOTICES }),
  });
}

/** ADM-18: the notice's versions, newest first (privacy.ListNoticeVersions). */
export function useNoticeVersions(noticeId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: noticeId !== null,
    queryKey: VERSIONS(noticeId ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(NoticeApi).listNoticeVersions({
            noticeId: noticeId as string,
            size: 100,
            sort: ['version,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** What a version asks: the purposes, whether everyone is asked again, and the grievance contact. */
export type NoticeVersionDraft = Required<Pick<NoticeVersion, 'purposeCodes' | 'grievanceContact'>> &
  Pick<NoticeVersion, 'requiresReconsent'>;

/** ADM-18: privacy.CreateNoticeVersion — the next version of the notice, as a draft. */
export function useCreateNoticeVersion() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ noticeId, draft }: { noticeId: string; draft: NoticeVersionDraft }) =>
      call(api.get(NoticeApi).createNoticeVersion({ noticeId, noticeVersion: draft })),
    onSettled: (_data, _error, { noticeId }) => void qc.invalidateQueries({ queryKey: VERSIONS(noticeId) }),
  });
}

/** ADM-18: privacy.UpdateNoticeVersion — a draft only; a published version is frozen (409). */
export function useUpdateNoticeVersion() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      noticeId,
      noticeVersionId,
      etag,
      changes,
    }: {
      noticeId: string;
      noticeVersionId: string;
      etag: string;
      changes: NoticeVersionUpdate;
    }) =>
      call(
        api.get(NoticeApi).updateNoticeVersion({
          noticeId,
          noticeVersionId,
          ifMatch: etag,
          noticeVersionUpdate: changes,
          updateMask: maskOf(changes),
        }),
      ),
    onSettled: (_data, _error, { noticeId }) => void qc.invalidateQueries({ queryKey: VERSIONS(noticeId) }),
  });
}

/**
 * ADM-18: privacy.PublishNoticeVersion — a draft with at least one translation becomes the active version (the one
 * before it retires); one that asks everyone again makes earlier grants of its purposes count no more.
 */
export function usePublishNoticeVersion() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      noticeId,
      noticeVersionId,
      etag,
    }: {
      noticeId: string;
      noticeVersionId: string;
      etag?: string;
    }) =>
      call(
        api.get(NoticeApi).publishNoticeVersion({
          noticeId,
          noticeVersionId,
          publishNoticeVersionRequest: etag ? { etag } : {},
        }),
      ),
    onSettled: (_data, _error, { noticeId }) => {
      void qc.invalidateQueries({ queryKey: VERSIONS(noticeId) });
      refreshPurposes(qc);
    },
  });
}

/** ADM-18: the version's translations (privacy.ListNoticeTranslations). */
export function useNoticeTranslations(noticeId: string, noticeVersionId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: noticeVersionId !== null,
    queryKey: TRANSLATIONS(noticeVersionId ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(NoticeApi).listNoticeTranslations({
            noticeId,
            noticeVersionId: noticeVersionId as string,
            size: 100,
            sort: ['language'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** The wording of one language. */
export type NoticeWording = Pick<NoticeTranslation, 'language' | 'bodyMarkdown' | 'summaryForChildren'>;

interface TranslationOf {
  noticeId: string;
  noticeVersionId: string;
}

/** ADM-18: privacy.CreateNoticeTranslation — a draft version's wording in one more language. */
export function useCreateNoticeTranslation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ noticeId, noticeVersionId, wording }: TranslationOf & { wording: NoticeWording }) =>
      call(
        api.get(NoticeApi).createNoticeTranslation({ noticeId, noticeVersionId, noticeTranslation: wording }),
      ),
    onSettled: (_data, _error, { noticeVersionId }) =>
      void qc.invalidateQueries({ queryKey: TRANSLATIONS(noticeVersionId) }),
  });
}

/** ADM-18: privacy.UpdateNoticeTranslation — while the version is a draft. */
export function useUpdateNoticeTranslation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      noticeId,
      noticeVersionId,
      noticeTranslationId,
      etag,
      changes,
    }: TranslationOf & { noticeTranslationId: string; etag: string; changes: NoticeTranslationUpdate }) =>
      call(
        api.get(NoticeApi).updateNoticeTranslation({
          noticeId,
          noticeVersionId,
          noticeTranslationId,
          ifMatch: etag,
          noticeTranslationUpdate: changes,
          updateMask: maskOf(changes),
        }),
      ),
    onSettled: (_data, _error, { noticeVersionId }) =>
      void qc.invalidateQueries({ queryKey: TRANSLATIONS(noticeVersionId) }),
  });
}

/** ADM-18: privacy.DeleteNoticeTranslation — while the version is a draft. */
export function useDeleteNoticeTranslation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      noticeId,
      noticeVersionId,
      noticeTranslationId,
      etag,
    }: TranslationOf & { noticeTranslationId: string; etag: string }) =>
      call(
        api
          .get(NoticeApi)
          .deleteNoticeTranslation({ noticeId, noticeVersionId, noticeTranslationId, ifMatch: etag }),
      ),
    onSettled: (_data, _error, { noticeVersionId }) =>
      void qc.invalidateQueries({ queryKey: TRANSLATIONS(noticeVersionId) }),
  });
}
