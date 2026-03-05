import { useCallback, useState } from 'react';
import { searchReferences } from '../apiclient/referenceApi';
import {
  GlobalResponse,
  PaginationMeta,
  Reference,
  ReferenceSearchRequest,
} from '../constants/types';

export function usePaginatedReferences(initialRequest: ReferenceSearchRequest = {}) {
  const [references, setReferences] = useState<Reference[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchReferences = useCallback(async (request: ReferenceSearchRequest = {}) => {
    setLoading(true);
    try {
      const mergedRequest = { ...initialRequest, ...request };
      const resp: GlobalResponse<Reference[]> = await searchReferences(mergedRequest);
      setReferences(resp.responseData ?? []);
      setMeta(resp.meta ?? null);
    } catch (error) {
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  return { references, meta, loading, fetchReferences, setReferences };
}