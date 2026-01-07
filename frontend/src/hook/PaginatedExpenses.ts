import { useCallback, useState } from 'react';
import { searchExpenses } from '../apiclient/expenseApi';
import { Expense, ExpenseSearchParams, GlobalResponse, PaginationMeta } from '../constants/types';

export function usePaginatedExpenses(initialParams: ExpenseSearchParams = {}) {
    const [expenses, setExpenses] = useState<Expense[]>([]);
    const [meta, setMeta] = useState<PaginationMeta | null>(null);
    const [loading, setLoading] = useState(false);

    const fetchExpenses = useCallback(async (override: ExpenseSearchParams = {}) => {
        setLoading(true);
        try {
            const merged = { ...initialParams, ...override };
            const resp: GlobalResponse<Expense[]> = await searchExpenses(merged);
            setExpenses(resp.responseData ?? []);
            setMeta(resp.meta ?? null);
        } catch (e: any) {
            throw e;
        } finally {
            setLoading(false);
        }
    }, []);

    return { expenses, meta, loading, fetchExpenses, setExpenses };
};