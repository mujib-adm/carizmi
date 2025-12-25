import { useCallback, useState } from "react";
import { searchPayments } from "../apiclient/paymentApi";
import { GlobalResponse, PaginationMeta, Payment, PaymentSearchParams } from "../constants/types";

export function usePaginatedPayments(initialParams: PaymentSearchParams = {}) {
    const [payments, setPayments] = useState<Payment[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [loading, setLoading] = useState(false);
    const [params, setParams] = useState<PaymentSearchParams>(initialParams);

    const fetchPayments = useCallback(
        async (override?: Partial<PaymentSearchParams>) => {
            setLoading(true);
            try {
                const merged = { ...params, ...override };
                const resp: GlobalResponse<Payment[]> = await searchPayments(merged);
                setPayments(resp.responseData ?? []);
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
        payments,
        meta,
        loading,
        fetchPayments,
        params,
        setParams,
    };
}