import { useCallback, useMemo, useState } from 'react';
import { paymentsApi } from '../api/generated/payments/payments';
import {
  PaginationMeta,
  PaymentDto,
  PaymentSearchRequestDto,
} from '../api/generated/types';

export function usePaginatedPayments(initialRequest: PaymentSearchRequestDto = {}) {
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  // Stabilize the input request
  const memoInitialRequest = useMemo(() => initialRequest, [JSON.stringify(initialRequest)]);

  const fetchPayments = useCallback(async (request: PaymentSearchRequestDto = {}) => {
    setLoading(true);
    try {
      const mergedRequest = { ...memoInitialRequest, ...request };
      const resp = await paymentsApi.searchPayments(mergedRequest);
      setPayments(resp.responseData ?? []);
      setMeta(resp.meta ?? null);
    } catch (e: any) {
      throw e;
    } finally {
      setLoading(false);
    }
  }, [memoInitialRequest]);

  return { payments, meta, loading, fetchPayments, setPayments };
}