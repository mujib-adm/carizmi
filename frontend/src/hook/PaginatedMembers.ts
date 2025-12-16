import { useState, useEffect, useCallback } from "react";
import { GlobalResponse, PaginationMeta } from "../constants/types";
import { Member, MemberSearchParams } from "../constants/types";
import { searchMembers } from "../apiclient/memberApi";
import { useApiMessages } from "./ApiResponseHandler";

export function usePaginatedMembers(initialParams: MemberSearchParams = {}) {
  const [members, setMembers] = useState<Member[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);
  const [params, setParams] = useState<MemberSearchParams>(initialParams);

  const { globalMessages, handleResponse, handleError, resetMessages } = useApiMessages<any>();

  const fetchMembers = useCallback(
    async (override?: Partial<MemberSearchParams>) => {
      setLoading(true);
      try {
        resetMessages();
        const merged = { ...params, ...override };
        const resp: GlobalResponse<Member[]> = await searchMembers(merged);
        handleResponse(resp);
        setMembers(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
        setParams(merged);
      } catch (e: any) {
        handleError(e);
      } finally {
        setLoading(false);
      }
    },
    [params, handleResponse, handleError, resetMessages]
  );

  useEffect(() => {
    fetchMembers();
  }, []); // initial load

  return {
    members,
    meta,
    loading,
    globalMessages,
    fetchMembers,
    params,
    setParams,
  };
}