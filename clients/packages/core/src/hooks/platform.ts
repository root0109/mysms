import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query';
import type { platform } from '@mysms/sdk';
import { AuditLogApi } from '@mysms/sdk/platform/apis/AuditLogApi';
import { FileObjectApi } from '@mysms/sdk/platform/apis/FileObjectApi';
import { SaasPlanApi } from '@mysms/sdk/platform/apis/SaasPlanApi';
import { TenantApi } from '@mysms/sdk/platform/apis/TenantApi';
import { TenantSubscriptionApi } from '@mysms/sdk/platform/apis/TenantSubscriptionApi';
import { ThemeApi } from '@mysms/sdk/platform/apis/ThemeApi';
import type { ApiClient } from '../api/client';
import { newIdempotencyKey } from '../api/ids';
import { allPages, pageQuery, type PageParam } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type FileObject = platform.FileObject;
export type Theme = platform.Theme;
export type Branding = platform.Branding;
export type AuditLog = platform.AuditLog;
export type SaasPlan = platform.SaasPlan;
export type TenantSubscription = platform.TenantSubscription;
export type SubscriptionCheckout = platform.SubscriptionCheckout;
export type SearchHit = platform.SearchHit;
export type ReportDefinition = platform.ReportDefinition;
export type PlatformOperation = platform.Operation;

const KEY = (resource: string, params?: unknown) => queryKey('platform', resource, params);

/** After a write: the platform lists of `resources` and the standard screens of their record types are stale. */
async function refresh(qc: QueryClient, resources: readonly string[], records: readonly string[]) {
  for (const record of records) {
    void qc.invalidateQueries({ queryKey: queryKey('records', `platform.${record}`) });
    void qc.invalidateQueries({ queryKey: queryKey('records', `platform.${record}#item`) });
  }
  await Promise.all(resources.map((resource) => qc.invalidateQueries({ queryKey: KEY(resource) })));
}

// ---- files: CreateUploadUrl → the client's own PUT → CompleteUpload; downloads signed at click time (ui/05 §9) ----

/** What the uploader needs of a picked file (a browser `File`, or a blob with its name). */
export interface UploadSource {
  name: string;
  type: string;
  size: number;
  arrayBuffer(): Promise<ArrayBuffer>;
}

/** The record a file is attached to (`schema.table` and id), and its collection within the record. */
export interface UploadOwner {
  ownerEntityType?: string;
  ownerEntityId?: string;
  collectionName?: string;
}

/** Thrown when the store refuses the bytes (the signed URL expired, the network dropped): upload again. */
export class UploadTransferError extends Error {
  constructor(readonly status: number) {
    super(`the file store answered ${status} to the upload`);
    this.name = 'UploadTransferError';
  }
}

/** The SHA-256 of the bytes in lower-case hex, where the platform offers Web Crypto; the server checks it. */
async function sha256(bytes: ArrayBuffer): Promise<string | undefined> {
  // typed structurally: React Native's lib has no DOM `crypto` type, though browsers and Hermes may provide one
  const { crypto } = globalThis as {
    crypto?: { subtle?: { digest(algorithm: string, data: Uint8Array): Promise<ArrayBuffer> } };
  };
  const subtle = crypto?.subtle;
  if (!subtle) return undefined;
  try {
    const digest = new Uint8Array(await subtle.digest('SHA-256', new Uint8Array(bytes)));
    return Array.from(digest, (byte) => byte.toString(16).padStart(2, '0')).join('');
  } catch {
    // the checksum is optional: without it the server still checks the size and type
    return undefined;
  }
}

/** The picked file's bytes; older engines (and jsdom) lack `Blob.arrayBuffer`, so FileReader stands in. */
function readBytes(source: UploadSource): Promise<ArrayBuffer> {
  if (typeof source.arrayBuffer === 'function') return source.arrayBuffer();
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = () => reject(reader.error ?? new Error('the file could not be read'));
    reader.readAsArrayBuffer(source as unknown as Blob);
  });
}

/**
 * Uploads one file: the platform creates it pending and signs a PUT URL, the bytes go straight to the store (never
 * with the MySMS token: the URL is the credential), and completion makes the file available once the server has
 * checked size, type and SHA-256. The answer is the available file.
 */
export async function uploadFile(
  api: ApiClient,
  source: UploadSource,
  owner: UploadOwner = {},
): Promise<FileObject> {
  const bytes = await readBytes(source);
  const checksumSha256 = await sha256(bytes);
  const files = api.get(FileObjectApi);
  const target = await call(
    files.createUploadUrl({
      createUploadUrlRequest: {
        fileName: source.name,
        mimeType: source.type || 'application/octet-stream',
        sizeBytes: String(source.size),
        ...(checksumSha256 ? { checksumSha256 } : {}),
        ...owner,
      },
    }),
  );
  const headers = (target.uploadHeaders ?? {}) as Record<string, string>;
  const stored = await globalThis.fetch(target.uploadUrl, { method: 'PUT', headers, body: bytes });
  if (!stored.ok) throw new UploadTransferError(stored.status);
  return call(files.completeUpload({ fileObjectId: target.fileId, body: {} }));
}

/** platform.CreateUploadUrl + upload + platform.CompleteUpload, as one mutation. */
export function useUploadFile() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ source, owner }: { source: UploadSource; owner?: UploadOwner }) =>
      uploadFile(api, source, owner),
    onSettled: () => refresh(qc, ['files'], ['FileObject']),
  });
}

/** platform.GetDownloadUrl at the moment of use: signed URLs expire within minutes and are never cached. */
export function useDownloadUrl() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (fileObjectId: string) => call(api.get(FileObjectApi).getDownloadUrl({ fileObjectId })),
  });
}

/** platform.GetFileObject: a file's name, kind, size and state (for showing an attached file). */
export function useFileObject(fileObjectId: string | null | undefined) {
  const { api } = useRuntime();
  return useQuery({
    enabled: Boolean(fileObjectId),
    queryKey: KEY('files', fileObjectId),
    queryFn: () => call(api.get(FileObjectApi).getFileObject({ fileObjectId: fileObjectId as string })),
    staleTime: STALE_TIME.detail,
  });
}

// ---- themes and branding (ADM-07, ADM-15) ----

/** The design tokens a tenant may set (ui/04 §2.4); the server refuses any other key. */
export const THEME_COLOUR_TOKENS = [
  'color.primary',
  'color.on-primary',
  'color.primary-container',
  'color.secondary',
  'color.on-secondary',
  'color.secondary-container',
] as const;
export const THEME_FONTS = ['Inter', 'Noto Sans', 'Open Sans', 'Poppins', 'Roboto'] as const;
export const THEME_RADIUS_STEPS = ['-1', '0', '1'] as const;

/** A theme's tokens as key → value (the contract carries them as a JSON object). */
export const themeTokens = (theme: Pick<Theme, 'tokens'> | undefined): Record<string, string> =>
  theme?.tokens && typeof theme.tokens === 'object' ? (theme.tokens as Record<string, string>) : {};

/** platform.ListThemes: the tenant's themes, by name. */
export function useThemes(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('themes'),
    queryFn: () =>
      allPages((page) =>
        call(api.get(ThemeApi).listThemes({ size: 500, sort: ['name'], ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** A theme as saved: a new one, or a stored one with the etag it was read at (sent as If-Match). */
export type ThemeDraft = {
  /** Set, with etag, for an existing theme; absent for a new one. */
  id?: string;
  etag?: string;
  name: string;
  description: string;
  logoFileId: string | null;
  isDefault: boolean;
  tokens: Record<string, string>;
};

/** platform.CreateTheme / UpdateTheme (the whole editable set; a stale etag is a 412). */
export function useSaveTheme() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (draft: ThemeDraft) => {
      const themes = api.get(ThemeApi);
      const fields = {
        name: draft.name.trim(),
        description: draft.description.trim() || undefined,
        logoFileId: draft.logoFileId ?? undefined,
        isDefault: draft.isDefault,
        tokens: draft.tokens,
      };
      if (draft.id === undefined) return call(themes.createTheme({ theme: fields }));
      if (draft.etag === undefined) throw new Error('an existing theme is saved under its etag');
      return call(
        themes.updateTheme({
          themeId: draft.id,
          ifMatch: draft.etag,
          updateMask: 'name,description,logoFileId,isDefault,tokens',
          themeUpdate: fields,
        }),
      );
    },
    onSettled: () => refresh(qc, ['themes', 'branding'], ['Theme']),
  });
}

/** platform.DeleteTheme. */
export function useDeleteTheme() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(api.get(ThemeApi).deleteTheme({ themeId: id, ifMatch: etag })),
    onSettled: () => refresh(qc, ['themes', 'branding'], ['Theme']),
  });
}

/** platform.GetBranding (public): what the tenant's sign-in page shows. */
export function useBranding(tenantCode: string) {
  const { api } = useRuntime();
  const code = tenantCode.trim();
  return useQuery({
    enabled: code.length > 0,
    queryKey: KEY('branding', code),
    queryFn: () => call(api.get(TenantApi).getBranding({ tenantCode: code })),
    staleTime: STALE_TIME.detail,
    retry: false,
  });
}

// ---- audit trail (ADM-13): partitioned by month, so every read names its range ----

export interface AuditQuery {
  /** First and last day (inclusive, `YYYY-MM-DD`, UTC) — at most 366 days apart. */
  from: string;
  to: string;
  subjectType?: string;
  subjectId?: string;
  page: PageParam;
}

const dayAfter = (day: string): string => {
  const next = new Date(`${day}T00:00:00Z`);
  next.setUTCDate(next.getUTCDate() + 1);
  return next.toISOString().slice(0, 10);
};

/** The list filter of an audit query (the occurredAt range first, as the server requires). */
export function auditFilter(query: Omit<AuditQuery, 'page'>): string {
  const parts = [`occurredAt >= "${query.from}T00:00:00Z"`, `occurredAt < "${dayAfter(query.to)}T00:00:00Z"`];
  if (query.subjectType?.trim()) parts.push(`subjectType = "${query.subjectType.trim()}"`);
  if (query.subjectId?.trim()) parts.push(`subjectId = "${query.subjectId.trim()}"`);
  return parts.join(' AND ');
}

/** platform.ListAuditLogs: one page of the tenant's trail in the range, newest first. */
export function useAuditLogs(query: AuditQuery | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: query !== null,
    queryKey: KEY('audit-logs', query),
    queryFn: () =>
      call(
        api.get(AuditLogApi).listAuditLogs({
          size: 50,
          filter: auditFilter(query as AuditQuery),
          sort: ['occurredAt,desc'],
          ...pageQuery((query as AuditQuery).page),
        }),
      ),
    placeholderData: keepPreviousData,
    staleTime: STALE_TIME.list,
  });
}

// ---- subscription and billing (ADM-14) ----

/** platform.ListSaasPlans: the plans on sale, in their display order. */
export function useSaasPlans(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('saas-plans'),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(SaasPlanApi).listSaasPlans({ size: 500, filter: 'isActive = true', ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.reference,
  });
}

/** platform.ListTenantSubscriptions: the subscriptions the caller may see, newest first. */
export function useTenantSubscriptions(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('tenant-subscriptions'),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(TenantSubscriptionApi)
            .listTenantSubscriptions({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** platform.StartSubscriptionCheckout: a pending subscription and the gateway order the client's checkout opens. */
export function useStartSubscriptionCheckout() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, institutionId }: { planId: string; institutionId?: string }) =>
      call(
        api.get(TenantSubscriptionApi).startSubscriptionCheckout({
          // a money operation: the key is required (one per attempt; a click is a new attempt)
          idempotencyKey: newIdempotencyKey(),
          startSubscriptionCheckoutRequest: { planId, ...(institutionId ? { institutionId } : {}) },
        }),
      ),
    onSettled: () => refresh(qc, ['tenant-subscriptions'], ['TenantSubscription']),
  });
}

/** What the gateway's checkout hands back after a successful payment. */
export interface CheckoutProof {
  gatewayOrderId: string;
  gatewayPaymentId: string;
  gatewaySignature: string;
}

/** platform.ConfirmSubscriptionCheckout: the checkout signature activates the pending subscription. */
export function useConfirmSubscriptionCheckout() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ subscriptionId, proof }: { subscriptionId: string; proof: CheckoutProof }) =>
      call(
        api.get(TenantSubscriptionApi).confirmSubscriptionCheckout({
          tenantSubscriptionId: subscriptionId,
          confirmSubscriptionCheckoutRequest: proof,
        }),
      ),
    onSettled: () => refresh(qc, ['tenant-subscriptions'], ['TenantSubscription']),
  });
}

/** platform.CancelSubscription with the etag the row was read at (412 when stale) and the reason. */
export function useCancelSubscription() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, etag, reason }: { id: string; etag?: string; reason?: string }) =>
      call(
        api.get(TenantSubscriptionApi).cancelSubscription({
          tenantSubscriptionId: id,
          cancelSubscriptionRequest: {
            ...(etag ? { etag } : {}),
            ...(reason?.trim() ? { reason: reason.trim() } : {}),
          },
        }),
      ),
    onSettled: () => refresh(qc, ['tenant-subscriptions'], ['TenantSubscription']),
  });
}

// ---- global search (ACC-11) ----

/** platform.GlobalSearch: one page of what the caller may see whose title contains the text. */
export function useGlobalSearch(text: string, page: number) {
  const { api } = useRuntime();
  const query = text.trim();
  return useQuery({
    enabled: query.length >= 2,
    queryKey: KEY('search', { query, page }),
    queryFn: () => call(api.get(TenantApi).globalSearch({ query, page, size: 25 })),
    placeholderData: keepPreviousData,
    staleTime: STALE_TIME.list,
  });
}

// ---- reports (ADM-12 exports) ----

/** platform.ListReportDefinitions: the reports the caller may run. */
export function useReportDefinitions(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: KEY('reports'),
    queryFn: () =>
      allPages((page) => call(api.get(TenantApi).listReportDefinitions({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
}

/** platform.RunReport: the answer is the operation whose result is the output file. */
export function useRunReport() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ reportCode, parameters }: { reportCode: string; parameters: Record<string, string> }) =>
      call(api.get(TenantApi).runReport({ runReportRequest: { reportCode, format: 'csv', parameters } })),
    onSettled: () => refresh(qc, ['operations'], []),
  });
}

// ---- platform console (PLT-01) ----

/** platform.ClearDemoData (platform staff): announces the clearance to the modules holding the tenant's demo data. */
export function useClearDemoData() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (tenantId: string) => call(api.get(TenantApi).clearDemoData({ tenantId, body: {} })),
  });
}
