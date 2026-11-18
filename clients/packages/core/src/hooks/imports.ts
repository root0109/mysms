import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { platform } from '@mysms/sdk';
import { TenantApi } from '@mysms/sdk/platform/apis/TenantApi';
import { newIdempotencyKey } from '../api/ids';
import type { PermissionCode } from '../generated/permissions';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { useRuntime } from '../react/useRuntime';

// ---- bulk imports (ADM-12): platform.GetImportTemplate, platform.StartImport and the operation's result ----

/** The entity types a CSV file can be imported as (a course package is a ZIP the lms module will take, not CSV). */
export type ImportEntityType = Exclude<platform.StartImportRequestEntityTypeEnum, 'lms_course_package'>;

/** Where an entity type's records belong: one institution, the whole tenant, or either (holidays). */
export type ImportScope = 'institution' | 'tenant' | 'either';

export interface ImportType {
  entityType: ImportEntityType;
  /** The owning module's create permission the importer needs (the platform checks it for the institution). */
  permission: PermissionCode;
  scope: ImportScope;
}

/** The platform's import templates (backend platform `ImportTemplates`), in the order the screen offers them. */
export const IMPORT_TYPES: readonly ImportType[] = [
  { entityType: 'students', permission: 'sis.student.create', scope: 'institution' },
  { entityType: 'guardians', permission: 'sis.guardian.create', scope: 'tenant' },
  { entityType: 'staff', permission: 'hr.staff.create', scope: 'institution' },
  { entityType: 'library_media', permission: 'library.media.create', scope: 'institution' },
  { entityType: 'holidays', permission: 'org.holiday.create', scope: 'either' },
  { entityType: 'promotions', permission: 'sis.promotion.create', scope: 'institution' },
];

/** Why a row of the file was refused: its row as a spreadsheet numbers it, the column (none: the row) and why. */
export interface ImportRowError {
  rowNumber: number;
  field?: string;
  message: string;
}

/** The result an import operation carries: what was imported where, its totals and the first row errors. */
export interface ImportResult {
  entityType: string;
  institutionId?: string;
  dryRun: boolean;
  rowCount: number;
  createdCount: number;
  /** All row errors; `errors` lists the first 1,000 of them. */
  errorCount: number;
  errors: ImportRowError[];
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const count = (value: unknown): number => (typeof value === 'number' && value >= 0 ? value : 0);

/** The import result of an operation, or null when it carries none (another kind of operation). */
export function importResult(
  operation: Pick<platform.Operation, 'result'> | null | undefined,
): ImportResult | null {
  const result: unknown = operation?.result;
  if (!isRecord(result) || typeof result['entityType'] !== 'string') return null;
  const errors = Array.isArray(result['errors']) ? result['errors'] : [];
  return {
    entityType: result['entityType'],
    ...(typeof result['institutionId'] === 'string' ? { institutionId: result['institutionId'] } : {}),
    dryRun: result['dryRun'] === true,
    rowCount: count(result['rowCount']),
    createdCount: count(result['createdCount']),
    errorCount: count(result['errorCount']),
    errors: errors.filter(isRecord).map((error) => ({
      rowNumber: count(error['rowNumber']),
      ...(typeof error['field'] === 'string' ? { field: error['field'] } : {}),
      message: typeof error['message'] === 'string' ? error['message'] : '',
    })),
  };
}

/** platform.GetImportTemplate: the entity type's CSV template, as a link signed now (opened at once, never kept). */
export function useImportTemplate() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (entityType: ImportEntityType) => call(api.get(TenantApi).getImportTemplate({ entityType })),
  });
}

export interface StartImport {
  entityType: ImportEntityType;
  fileId: string;
  /** The target institution; left out for tenant-level records. */
  institutionId?: string;
  /** Check every row against the template and send nothing. */
  dryRun: boolean;
}

/**
 * platform.StartImport: the answer is the import's operation, finished at once for a dry run, running while the
 * owning module creates the rows. Each start is its own attempt (a fresh Idempotency-Key), so a network retry of
 * the same request never starts a second import.
 */
export function useStartImport() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ entityType, fileId, institutionId, dryRun }: StartImport) =>
      call(
        api.get(TenantApi).startImport({
          idempotencyKey: newIdempotencyKey(),
          startImportRequest: { entityType, fileId, dryRun, ...(institutionId ? { institutionId } : {}) },
        }),
      ),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey('platform', 'operations') }),
  });
}
