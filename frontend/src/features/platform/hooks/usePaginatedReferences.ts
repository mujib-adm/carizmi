import { useCallback, useMemo, useState } from 'react';
import { referenceDataApi } from '../../../api/generated/reference-data/reference-data';
import { PaginationMeta, ReferenceDto, ReferenceSearchRequestDto } from '../../../api/generated/types';

export function usePaginatedReferences(initialRequest: ReferenceSearchRequestDto = {}) {
  const [references, setReferences] = useState<ReferenceDto[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  // Stabilize the input request
  const memoInitialRequest = useMemo(() => initialRequest, [JSON.stringify(initialRequest)]);

  const fetchReferences = useCallback(
    async (request: ReferenceSearchRequestDto = {}) => {
      setLoading(true);
      try {
        const mergedRequest = { ...memoInitialRequest, ...request };
        const resp = await referenceDataApi.searchReferences(mergedRequest);
        setReferences(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
      } catch (error) {
        throw error;
      } finally {
        setLoading(false);
      }
    },
    [memoInitialRequest]
  );

  return { references, meta, loading, fetchReferences, setReferences };
}