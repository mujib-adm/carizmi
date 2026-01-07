import { useCallback, useState } from "react";
import { searchReferences } from "../apiclient/referenceApi";
import { GlobalResponse, PaginationMeta, Reference, ReferenceSearchParams } from "../constants/types";

export function usePaginatedReferences(initialParams: ReferenceSearchParams = {}) {
    const [references, setReferences] = useState<Reference[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [loading, setLoading] = useState(false);

    const fetchReferences = useCallback(async (params: ReferenceSearchParams = {}) => {
        setLoading(true);
        try {
            const mergedParams = { ...initialParams, ...params };
            const resp: GlobalResponse<Reference[]> = await searchReferences(mergedParams);
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