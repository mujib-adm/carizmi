import { useCallback, useMemo, useState } from 'react';
import { membersApi } from '../api/generated/members/members';
import { MemberDto, MemberSearchRequestDto, PaginationMeta } from '../api/generated/types';

export function usePaginatedMembers(initialRequest: MemberSearchRequestDto = {}) {
  const [members, setMembers] = useState<MemberDto[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  // Stabilize the input request
  const memoInitialRequest = useMemo(() => initialRequest, [JSON.stringify(initialRequest)]);

  const fetchMembers = useCallback(
    async (request: MemberSearchRequestDto = {}) => {
      setLoading(true);
      try {
        const mergedRequest = { ...memoInitialRequest, ...request };
        const resp = await membersApi.searchMembers(mergedRequest);
        setMembers(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
      } catch (e: any) {
        throw e;
      } finally {
        setLoading(false);
      }
    },
    [memoInitialRequest]
  );

  return { members, meta, loading, fetchMembers, setMembers };
}