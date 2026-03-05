import { useCallback, useState } from 'react';
import { searchExpenses } from '../apiclient/expenseApi';
import { Expense, ExpenseSearchRequest, GlobalResponse, PaginationMeta } from '../constants/types';

export function usePaginatedExpenses(initialRequest: ExpenseSearchRequest = {}) {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [meta, setMeta] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchExpenses = useCallback(async (request: ExpenseSearchRequest = {}) => {
    setLoading(true);
    try {
      const mergedRequest = { ...initialRequest, ...request };
      const resp: GlobalResponse<Expense[]> = await searchExpenses(mergedRequest);
      setExpenses(resp.responseData ?? []);
      setMeta(resp.meta ?? null);
    } catch (e: any) {
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { expenses, meta, loading, fetchExpenses, setExpenses };
}
