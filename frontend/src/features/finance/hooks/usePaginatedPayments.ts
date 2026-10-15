import { useCallback, useMemo, useState } from 'react';
import { paymentsApi } from '../../../api/generated/payments/payments';
import {
  PaginationMeta,
  PaymentDto,
  PaymentSearchRequestDto,
  PaymentSummaryDto,
} from '../../../api/generated/types';

export function usePaginatedPayments(initialRequest: PaymentSearchRequestDto = {}) {
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [summary, setSummary] = useState<PaymentSummaryDto | null>(null);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  // Stabilize the input request
  const requestString = JSON.stringify(initialRequest);
  const memoInitialRequest = useMemo(() => initialRequest, [requestString]);

  const fetchPayments = useCallback(
    async (request: PaymentSearchRequestDto = {}) => {
      setLoading(true);
      try {
        const mergedRequest = { ...memoInitialRequest, ...request };
        const resp = await paymentsApi.searchPayments(mergedRequest);
        setPayments(resp.responseData?.payments ?? []);
        setSummary(resp.responseData?.summary ?? null);
        setMeta(resp.meta ?? null);
      } finally {
        setLoading(false);
      }
    },
    [memoInitialRequest]
  );

  return { payments, summary, meta, loading, fetchPayments, setPayments, setSummary };
}