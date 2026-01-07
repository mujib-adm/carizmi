import { useCallback, useState } from "react";
import { searchPayments } from "../apiclient/paymentApi";
import { GlobalResponse, PaginationMeta, Payment, PaymentSearchParams } from "../constants/types";

export function usePaginatedPayments(initialParams: PaymentSearchParams = {}) {
    const [payments, setPayments] = useState<Payment[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [loading, setLoading] = useState(false);

    const fetchPayments = useCallback(async (override: PaymentSearchParams = {}) => {
        setLoading(true);
        try {
            const merged = { ...initialParams, ...override };
            const resp: GlobalResponse<Payment[]> = await searchPayments(merged);
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