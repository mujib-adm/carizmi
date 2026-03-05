import { useCallback, useState } from 'react';
import { searchMembers } from '../apiclient/memberApi';
import { GlobalResponse, Member, MemberSearchRequest, PaginationMeta } from '../constants/types';

export function usePaginatedMembers(initialRequest: MemberSearchRequest = {}) {
  const [members, setMembers] = useState<Member[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchMembers = useCallback(async (request: MemberSearchRequest = {}) => {
    setLoading(true);
    try {
      const mergedRequest = { ...initialRequest, ...request };
      const resp: GlobalResponse<Member[]> = await searchMembers(mergedRequest);
      setMembers(resp.responseData ?? []);
      setMeta(resp.meta ?? null);
    } catch (e: any) {
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { members, meta, loading, fetchMembers, setMembers };
}