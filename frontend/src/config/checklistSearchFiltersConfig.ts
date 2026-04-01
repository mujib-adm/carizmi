export const checklistSearchFiltersConfig = [
  {
    name: 'year',
    label: 'Year',
    type: 'select',
    width: 120,
    options: Array.from({ length: 5 }, (_, i) => {
      const y = new Date().getFullYear() - i;
      return { value: y, label: String(y) };
    }),
  },
];