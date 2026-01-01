import { useCallback, useState } from "react";
import { searchMembers } from "../apiclient/memberApi";
import { GlobalResponse, Member, MemberSearchParams, PaginationMeta } from "../constants/types";

export function usePaginatedMembers(initialParams: MemberSearchParams = {}) {
  const [members, setMembers] = useState<Member[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchMembers = useCallback(
    async (override: MemberSearchParams = {}) => {
      setLoading(true);
      try {
        const merged = { ...initialParams, ...override };
        const resp: GlobalResponse<Member[]> = await searchMembers(merged);
        setMembers(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
      } catch (e: any) {
        throw e;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return {
    members,
    meta,
    loading,
    fetchMembers,
    setMembers
  };
}