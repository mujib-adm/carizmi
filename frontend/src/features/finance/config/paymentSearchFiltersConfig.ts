const feeTypes = [
  { value: 'Membership Fee', label: 'Membership Fee' },
  { value: 'Registration Fee', label: 'Registration Fee' },
];

export const paymentSearchFiltersConfig = [
  {
    name: 'memberID',
    label: 'Member ID',
    type: 'number',
  },
  {
    name: 'feeType',
    label: 'Fee Type',
    type: 'select',
    options: feeTypes,
  },
  {
    name: 'dateRange',
    label: 'Date',
    type: 'dateRange',
  },
];