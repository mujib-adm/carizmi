import { useCallback, useState } from "react";
import { searchPayments } from "../apiclient/paymentApi";
import { GlobalResponse, PaginationMeta, Payment, PaymentSearchRequest } from "../constants/types";

export function usePaginatedPayments(initialRequest: PaymentSearchRequest = {}) {
    const [payments, setPayments] = useState<Payment[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [loading, setLoading] = useState(false);

    const fetchPayments = useCallback(async (request: PaymentSearchRequest = {}) => {
        setLoading(true);
        try {
            const mergedRequest = { ...initialRequest, ...request };
            const resp: GlobalResponse<Payment[]> = await searchPayments(mergedRequest);
            setPayments(resp.responseData ?? []);
            setMeta(resp.meta ?? null);
        } catch (e: any) {
            throw e;
        } finally {
            setLoading(false);
        }
    }, [] );

    return { payments, meta, loading, fetchPayments, setPayments };
}