import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AcademicYearApi } from '@mysms/sdk/org/apis/AcademicYearApi';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { useRuntime } from '../react/useRuntime';

/** The cached academic years and terms (generic record reads and the typed ones) are stale after a year's step. */
function useRefreshYears() {
  const qc = useQueryClient();
  return () =>
    Promise.all([
      qc.invalidateQueries({ queryKey: queryKey('records', 'org.AcademicYear') }),
      qc.invalidateQueries({ queryKey: queryKey('records', 'org.AcademicYear#item') }),
      qc.invalidateQueries({ queryKey: queryKey('records', 'org.AcademicTerm') }),
      qc.invalidateQueries({ queryKey: ['org'] }),
    ]);
}

/**
 * org.ActivateAcademicYear under the etag the screen last read: the year becomes the institution's current one
 * (412 when someone changed it meanwhile, 409 when its state does not allow it).
 */
export function useActivateAcademicYear() {
  const { api } = useRuntime();
  const refresh = useRefreshYears();
  return useMutation({
    mutationFn: ({ academicYearId, etag }: { academicYearId: string; etag: string | null }) =>
      call(
        api.get(AcademicYearApi).activateAcademicYear({
          academicYearId,
          activateAcademicYearRequest: etag ? { etag } : {},
        }),
      ),
    onSettled: refresh,
  });
}

/** org.GenerateTerms: the server writes the year's terms from its term structure. */
export function useGenerateTerms() {
  const { api } = useRuntime();
  const refresh = useRefreshYears();
  return useMutation({
    mutationFn: ({ academicYearId }: { academicYearId: string }) =>
      call(api.get(AcademicYearApi).generateTerms({ academicYearId, body: {} })),
    onSettled: refresh,
  });
}
