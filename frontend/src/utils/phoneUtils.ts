/**
 * Centralized utility for US phone numbers.
 * Formats any 10-digit US phone representation to standard XXX-XXX-XXXX.
 */
export function formatPhoneNumber(phone?: string | null, emptyFallback = '—'): string {
  if (!phone || !phone.trim()) {
    return emptyFallback;
  }

  const trimmed = phone.trim();

  // Strip all non-digit characters
  const digits = trimmed.replace(/\D/g, '');

  // Handle optional leading US country code '1' (11 digits starting with 1)
  const tenDigits = digits.length === 11 && digits.startsWith('1') ? digits.slice(1) : digits;

  if (tenDigits.length === 10) {
    return `${tenDigits.slice(0, 3)}-${tenDigits.slice(3, 6)}-${tenDigits.slice(6)}`;
  }

  // Fallback to original trimmed value if it doesn't match standard 10 digits
  return trimmed;
}

/**
 * As-you-type phone formatter for input fields.
 * Formats progressive digit input to standard XXX-XXX-XXXX format.
 */
export function formatPhoneInput(value?: string | null): string {
  if (!value) return '';

  const trimmed = value.trim();
  // Strip all non-digit characters
  const digits = trimmed.replace(/\D/g, '');

  // Handle optional leading US country code '1' if > 10 digits
  const cleanDigits =
    digits.length === 11 && digits.startsWith('1')
      ? digits.slice(1)
      : digits.slice(0, 10);

  if (cleanDigits.length <= 3) {
    return cleanDigits;
  }
  if (cleanDigits.length <= 6) {
    return `${cleanDigits.slice(0, 3)}-${cleanDigits.slice(3)}`;
  }
  return `${cleanDigits.slice(0, 3)}-${cleanDigits.slice(3, 6)}-${cleanDigits.slice(6, 10)}`;
}
