/**
 * Centralized utility for US phone numbers.
 * Formats any 10-digit US phone representation to standard XXX-XXX-XXXX.
 */
export function formatPhoneNumber(phone?: string | null): string {
  if (!phone || !phone.trim()) {
    return '—';
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
