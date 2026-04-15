import { useCallback, useMemo, useState } from 'react';
import { expensesApi } from '../../../api/generated/expenses/expenses';
import { ExpenseDto, ExpenseSearchRequestDto, PaginationMeta } from '../../../api/generated/types';

export function usePaginatedExpenses(initialRequest: ExpenseSearchRequestDto = {}) {
  const [expenses, setExpenses] = useState<ExpenseDto[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  // Stabilize the input request
  const memoInitialRequest = useMemo(() => initialRequest, [JSON.stringify(initialRequest)]);

  const fetchExpenses = useCallback(
    async (request: ExpenseSearchRequestDto = {}) => {
      setLoading(true);
      try {
        const mergedRequest = { ...memoInitialRequest, ...request };
        const resp = await expensesApi.searchExpenses(mergedRequest);
        setExpenses(resp.responseData ?? []);
        setMeta(resp.meta ?? null);
      } finally {
        setLoading(false);
      }
    },
    [memoInitialRequest]
  );

  return { expenses, meta, loading, fetchExpenses, setExpenses };
}
