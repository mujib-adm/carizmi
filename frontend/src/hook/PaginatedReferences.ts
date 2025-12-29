import { useCallback, useState } from "react";
import { searchReferences } from "../apiclient/referenceApi";
import { GlobalResponse, PaginationMeta, Reference, ReferenceSearchParams } from "../constants/types";

export function usePaginatedReferences(initialParams: ReferenceSearchParams = {}) {
    const [references, setReferences] = useState<Reference[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | undefined>(undefined);
    const [loading, setLoading] = useState(false);

    const fetchReferences = useCallback(async (params: ReferenceSearchParams = {}) => {
        setLoading(true);
        try {
            const mergedParams = { ...initialParams, ...params };
            const resp: GlobalResponse<Reference[]> = await searchReferences(mergedParams);
            if (resp && resp.responseData) {
                setReferences(resp.responseData);
                setMeta(resp.meta);
            }
        } catch (error) {
            console.error("Failed to fetch references", error);
            throw error;
        } finally {
            setLoading(false);
        }
    }, []);

    return { references, meta, loading, fetchReferences, setReferences };
}