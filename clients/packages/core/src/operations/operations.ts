import type { BaseAPI } from '@mysms/sdk/runtime';
import type { platform } from '@mysms/sdk';
import { OperationApi as AdmissionOperationApi } from '@mysms/sdk/admission/apis/OperationApi';
import { OperationApi as AssessmentOperationApi } from '@mysms/sdk/assessment/apis/OperationApi';
import { OperationApi as CommsOperationApi } from '@mysms/sdk/comms/apis/OperationApi';
import { OperationApi as FinanceOperationApi } from '@mysms/sdk/finance/apis/OperationApi';
import { OperationApi as IamOperationApi } from '@mysms/sdk/iam/apis/OperationApi';
import { OperationApi as LmsOperationApi } from '@mysms/sdk/lms/apis/OperationApi';
import { OperationApi as PayrollOperationApi } from '@mysms/sdk/payroll/apis/OperationApi';
import { OperationApi as PlatformOperationApi } from '@mysms/sdk/platform/apis/OperationApi';
import { OperationApi as SisOperationApi } from '@mysms/sdk/sis/apis/OperationApi';
import { OperationApi as TimetableOperationApi } from '@mysms/sdk/timetable/apis/OperationApi';
import type { ApiClass, ApiClient } from '../api/client';
import { call } from '../api/unwrap';
import { OPERATION_MODULES, type OperationModule } from '../generated/operations';

export type Operation = platform.Operation;

/** Terminal states; polling stops here. */
export const DONE_STATUSES: readonly string[] = ['succeeded', 'failed', 'cancelled'];
export const isDone = (op: Pick<Operation, 'status'>): boolean => DONE_STATUSES.includes(op.status);

export function isOperationModule(module: string): module is OperationModule {
  return (OPERATION_MODULES as readonly string[]).includes(module);
}

/** What every module's generated `OperationApi` offers (the contract is identical in each module). */
interface OperationClient extends BaseAPI {
  getOperation(request: { operationId: string }): Promise<Operation>;
  cancelOperation(request: { operationId: string }): Promise<Operation>;
}

/**
 * The OperationService client of each operation-owning module. Keyed by the generated module list, so a
 * module gaining or losing operations in the contract is a compile error here until this table follows.
 */
const OPERATION_APIS: Record<OperationModule, ApiClass<OperationClient>> = {
  admission: AdmissionOperationApi,
  assessment: AssessmentOperationApi,
  comms: CommsOperationApi,
  finance: FinanceOperationApi,
  iam: IamOperationApi,
  lms: LmsOperationApi,
  payroll: PayrollOperationApi,
  platform: PlatformOperationApi,
  sis: SisOperationApi,
  timetable: TimetableOperationApi,
};

function ownerOf(op: Pick<Operation, 'module'>): ApiClass<OperationClient> {
  if (!isOperationModule(op.module)) throw new Error(`module ${op.module} does not own operations`);
  return OPERATION_APIS[op.module];
}

/** Live status from the module that owns the operation (never from the job-centre index). */
export function getOperation(
  api: ApiClient,
  op: Pick<Operation, 'module' | 'operationId'>,
): Promise<Operation> {
  return call(api.get(ownerOf(op)).getOperation({ operationId: op.operationId }));
}

export function cancelOperation(
  api: ApiClient,
  op: Pick<Operation, 'module' | 'operationId'>,
): Promise<Operation> {
  return call(api.get(ownerOf(op)).cancelOperation({ operationId: op.operationId }));
}

/** Poll interval: quick at first, then every 5 s (keeps long jobs cheap). */
export const pollInterval = (pollCount: number): number => (pollCount < 5 ? 1_000 : 5_000);
