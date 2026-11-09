import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { OperationApi } from '@mysms/sdk/platform/apis/OperationApi';
import { call } from '../api/unwrap';
import {
  cancelOperation,
  getOperation,
  isDone,
  pollInterval,
  type Operation,
} from '../operations/operations';
import { queryKey } from '../query/keys';
import { useRuntime } from '../react/useRuntime';

/** Polls an operation at its owning module until it finishes (api/README "Asynchronous operations"). */
export function useOperation(op: Pick<Operation, 'module' | 'operationId'> | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(op?.module ?? '-', 'operation', op?.operationId),
    queryFn: () => getOperation(api, op as Operation),
    enabled: op !== null,
    staleTime: 0,
    refetchInterval: (query) => {
      const data = query.state.data;
      return data && isDone(data) ? false : pollInterval(query.state.dataUpdateCount);
    },
  });
}

export interface JobCentreParams {
  kind?: string;
  pageSize?: number;
  pageToken?: string;
}

/** The job centre: the caller's operations across all modules (platform index; may lag the owner). */
export function useJobCentre(params: JobCentreParams = {}) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('platform', 'operations', params),
    queryFn: () => call(api.get(OperationApi).listOperations(params)),
  });
}

export function useCancelOperation() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (op: Pick<Operation, 'module' | 'operationId'>) => cancelOperation(api, op),
    onSuccess: (op) => {
      qc.setQueryData(queryKey(op.module, 'operation', op.operationId), op);
      void qc.invalidateQueries({ queryKey: queryKey('platform', 'operations') });
    },
  });
}
