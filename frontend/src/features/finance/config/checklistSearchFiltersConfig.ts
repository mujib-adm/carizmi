export interface SearchFilterOption {
  value: number | string;
  label: string;
}

export function buildChecklistSearchFiltersConfig(yearOptions: SearchFilterOption[]) {
  return [
    {
      name: 'year',
      label: 'Year',
      type: 'select',
      width: 120,
      options: yearOptions,
    },
  ];
}

export const checklistSearchFiltersConfig = buildChecklistSearchFiltersConfig(
  Array.from({ length: 5 }, (_, i) => {
    const y = new Date().getFullYear() - i;
    return { value: y, label: String(y) };
  })
);