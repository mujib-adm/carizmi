import { useCallback, useState } from "react";
import { searchMembers } from "../apiclient/memberApi";
import { GlobalResponse, Member, MemberSearchParams, PaginationMeta } from "../constants/types";

export function usePaginatedMembers(initialParams: MemberSearchParams = {}) {
  const [members, setMembers] = useState<Member[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);
  const [params, setParams] = useState<MemberSearchParams>(initialParams);

  const fetchMembers = useCallback(
    async (override?: Partial<MemberSearchParams>) => {
      setLoading(true);
      try {
        const merged = { ...params, ...override };
        const resp: GlobalResponse<Member[]> = await searchMembers(merged);
        setMembers(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
        setParams(merged);
      } catch (e: any) {
        throw e;
      } finally {
        setLoading(false);
      }
    },
    [params]
  );

  return {
    members,
    meta,
    loading,
    fetchMembers,
    params,
    setParams,
  };
}