import type { platform } from '@mysms/sdk';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';
import { patched } from './patch';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/** The tenant administration the platform screens use (ADM-07, 12, 13, 14, 15, ACC-11). */
export const PLATFORM_ADMIN = [
  'platform.theme.read',
  'platform.theme.create',
  'platform.theme.update',
  'platform.theme.delete',
  'platform.audit_log.read',
  'platform.tenant_subscription.read',
  'platform.tenant_subscription.start_subscription_checkout',
  'platform.tenant_subscription.cancel_subscription',
  'platform.saas_plan.read',
  'platform.tenant.global_search',
  'platform.tenant.list_report_definitions',
  'platform.tenant.run_report',
  'platform.operation.read',
  'platform.file_object.create_upload_url',
  'platform.file_object.complete_upload',
  'platform.file_object.get_download_url',
  'platform.tenant.get_import_template',
  'platform.tenant.start_import',
  'sis.student.create',
  'sis.guardian.create',
] as const;

export const platformFixture = {
  themeId: id('a71'),
  subscriptionId: id('a72'),
  planId: id('a73'),
  auditId: id('a74'),
  fileId: id('a75'),
  outputFileId: id('a76'),
  operationId: id('a77'),
  institutionId: id('a79'),
} as const;

type Theme = { -readonly [K in keyof platform.Theme]: platform.Theme[K] } & { version: number };
type Subscription = { -readonly [K in keyof platform.TenantSubscription]: platform.TenantSubscription[K] } & {
  version: number;
};

/**
 * The platform's tenant-administration endpoints over in-memory rows, with the server's rules the screens rely on:
 * If-Match on theme writes (412 when stale), one default theme, the audit list's mandatory range (400 without it),
 * cancellation only while pending or active (409) with the body etag (412), reports as finished operations whose
 * result is a file, signed URLs for the file store, and imports: a template link per type (422 for the course
 * package), a dry run finished at once with its row errors, a run that is running on its first read and finished
 * (its rows created) on the next.
 */
export function fakePlatform(): Record<string, Handler> {
  const f = platformFixture;
  let sequence = 0;
  const operations = new Map<string, platform.Operation>();
  let importReads = 0;
  const themes = new Map<string, Theme>([
    [
      f.themeId,
      { id: f.themeId, name: 'Classic', isDefault: true, tokens: { 'color.primary': '#0F766E' }, version: 1 },
    ],
  ]);
  const subscriptions = new Map<string, Subscription>([
    [
      f.subscriptionId,
      {
        id: f.subscriptionId,
        requestedByUserId: id('a78'),
        status: 'active',
        startDate: '2026-04-01',
        endDate: '2027-03-31',
        price: '12000.00',
        version: 1,
      },
    ],
  ]);
  const themeOut = ({ version, ...row }: Theme) => ({ ...row, etag: `"${version}"` });
  const subscriptionOut = ({ version, ...row }: Subscription) => ({ ...row, etag: `"${version}"` });
  const saveTheme = (row: Theme) => {
    if (row.isDefault) for (const other of themes.values()) if (other.id !== row.id) other.isDefault = false;
    themes.set(row.id as string, row);
    return json(themeOut(row), { headers: { ETag: `"${row.version}"` } });
  };
  return {
    'GET /v1/platform/themes': (req) => onePage(req, [...themes.values()].map(themeOut)),
    'POST /v1/platform/themes': async (req) => {
      const body = (await req.json()) as platform.Theme;
      sequence += 1;
      return saveTheme({ ...body, id: id(`b${sequence}`), version: 1 });
    },
    'PATCH /v1/platform/themes/{themeId}': async (req, { themeId }) => {
      const row = themes.get(themeId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (req.headers.get('If-Match') !== `"${row.version}"`) return problem(412, 'ETAG_MISMATCH');
      const body = (await req.json()) as platform.ThemeUpdate;
      return saveTheme({ ...patched(row, body), version: row.version + 1 });
    },
    'GET /v1/platform/branding': () =>
      json({
        tenantName: 'Sunrise Schools',
        primaryColor: '#0F766E',
        locale: 'en',
        timezone: 'Asia/Kolkata',
      }),
    'GET /v1/platform/audit-logs': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      if (!filter.includes('occurredAt >=')) return problem(400, 'INVALID_FILTER');
      return onePage(req, [
        {
          id: f.auditId,
          description: 'update sis.student',
          event: 'update',
          subjectType: 'sis.student',
          subjectId: id('a79'),
          causerId: id('a80'),
          properties: { changedFields: ['phone'], reason: 'Parent asked' },
          occurredAt: '2026-11-02T09:30:00Z',
        },
      ]);
    },
    'GET /v1/platform/tenant-subscriptions': (req) =>
      onePage(req, [...subscriptions.values()].map(subscriptionOut)),
    'GET /v1/platform/saas-plans': (req) =>
      onePage(req, [
        { id: f.planId, code: 'gold', displayName: 'Gold', billingCycleMonths: 12, price: '12000.00' },
      ]),
    'POST /v1/platform/tenant-subscriptions/{tenantSubscriptionId}:cancel': async (
      req,
      { tenantSubscriptionId },
    ) => {
      const row = subscriptions.get(tenantSubscriptionId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string };
      if (body.etag && body.etag !== `"${row.version}"`) return problem(412, 'ETAG_MISMATCH');
      if (row.status !== 'pending' && row.status !== 'active')
        return problem(409, 'INVALID_STATE_TRANSITION');
      const next = { ...row, status: 'cancelled' as const, version: row.version + 1 };
      subscriptions.set(row.id as string, next);
      return json(subscriptionOut(next));
    },
    'GET /v1/platform/search': (req) => {
      const query = new URL(req.url).searchParams.get('query') ?? '';
      const hits = [
        {
          entityType: 'platform.file_object',
          entityId: f.fileId,
          title: 'Annual day photos.zip',
          subtitle: 'archive',
        },
        { entityType: 'platform.tag', entityId: id('a81'), title: 'Annual day', subtitle: 'general' },
      ];
      return onePage(
        req,
        hits.filter((hit) => hit.title.toLowerCase().includes(query.toLowerCase())),
      );
    },
    'GET /v1/platform/reports': (req) =>
      onePage(req, [
        {
          reportCode: 'platform.audit_log',
          module: 'platform',
          title: 'Audit log',
          formats: ['csv'],
          parameters: { type: 'object', properties: { from: { type: 'string' }, to: { type: 'string' } } },
        },
      ]),
    'POST /v1/platform/reports:run': () => {
      const report: platform.Operation = {
        operationId: f.operationId,
        module: 'platform',
        kind: 'platform.RunReport',
        status: 'succeeded',
        resultFileId: f.outputFileId,
        createTime: '2026-11-11T10:00:00Z',
      };
      operations.set(report.operationId, report);
      return json(report, { status: 202 });
    },
    'GET /v1/platform/imports/templates/{entityType}': (_req, { entityType }) =>
      entityType === 'lms_course_package'
        ? problem(422, 'IMPORT_TYPE_UNAVAILABLE')
        : json({
            url: `https://files.example/import-template-${entityType}.csv?X-Goog-Signature=1`,
            expiresAt: '2026-11-11T10:10:00Z',
          }),
    'POST /v1/platform/imports': async (req) => {
      const body = (await req.json()) as platform.StartImportRequest;
      if (!req.headers.get('Idempotency-Key')) return problem(400, 'INVALID_ARGUMENT');
      if (body.entityType === 'students' && !body.institutionId) {
        return problem(422, 'VALIDATION_FAILED', 'Error', {
          errors: [{ field: 'institutionId', message: 'required', code: 'NotNull' }],
        });
      }
      importReads = 0;
      const started: platform.Operation = {
        operationId: id(`c${(sequence += 1)}`),
        module: 'platform',
        kind: 'platform.StartImport',
        status: body.dryRun ? 'succeeded' : 'running',
        progressPercent: body.dryRun ? 100 : 0,
        createTime: '2026-11-11T10:00:00Z',
        result: {
          entityType: body.entityType,
          ...(body.institutionId ? { institutionId: body.institutionId } : {}),
          fileId: body.fileId,
          dryRun: Boolean(body.dryRun),
          rowCount: 3,
          createdCount: 0,
          errorCount: 1,
          errors: [{ rowNumber: 3, field: 'personId', message: 'required' }],
        },
      };
      operations.set(started.operationId, started);
      return json(started, { status: 202 });
    },
    'GET /v1/platform/operations/{operationId}': (_req, params) => {
      const operationId = params['operationId'] ?? '';
      const found = operations.get(operationId);
      if (!found) return problem(404, 'NOT_FOUND');
      if (found.kind === 'platform.StartImport' && found.status === 'running' && ++importReads > 1) {
        const result = found.result as Record<string, unknown>;
        operations.set(operationId, {
          ...found,
          status: 'succeeded',
          progressPercent: 100,
          result: { ...result, createdCount: 2 },
        });
      }
      return json(operations.get(operationId));
    },
    'GET /v1/platform/files/{fileObjectId}:downloadUrl': (_req, { fileObjectId }) =>
      json({
        url: `https://files.example/${fileObjectId}?X-Goog-Signature=1`,
        expiresAt: '2026-11-11T10:10:00Z',
      }),
    'POST /v1/platform/files:createUploadUrl': async (req) => {
      const body = (await req.json()) as platform.CreateUploadUrlRequest;
      return json({
        fileId: f.fileId,
        uploadUrl: `https://files.example/upload/${f.fileId}?X-Goog-Signature=1`,
        uploadHeaders: { 'Content-Type': body.mimeType },
        expiresAt: '2026-11-11T10:15:00Z',
      });
    },
    'POST /v1/platform/files/{fileObjectId}:complete': (_req, { fileObjectId }) =>
      json({ id: fileObjectId, fileName: 'logo.png', sizeBytes: '4', status: 'available', etag: '"2"' }),
  };
}
