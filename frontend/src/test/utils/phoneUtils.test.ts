import { describe, it, expect } from 'vitest';
import { formatPhoneNumber } from '../../utils/phoneUtils.ts';

describe('formatPhoneNumber', () => {
  it('should format raw 10 digits to XXX-XXX-XXXX', () => {
    expect(formatPhoneNumber('6126550830')).toBe('612-655-0830');
  });

  it('should preserve already formatted XXX-XXX-XXXX', () => {
    expect(formatPhoneNumber('612-655-0830')).toBe('612-655-0830');
  });

  it('should format parenthesized phone numbers', () => {
    expect(formatPhoneNumber('(612) 655-0830')).toBe('612-655-0830');
    expect(formatPhoneNumber('(612)-655-0830')).toBe('612-655-0830');
    expect(formatPhoneNumber('(612)655-0830')).toBe('612-655-0830');
  });

  it('should format dot separated phone numbers', () => {
    expect(formatPhoneNumber('612.655.0830')).toBe('612-655-0830');
  });

  it('should format space separated phone numbers', () => {
    expect(formatPhoneNumber('612 655 0830')).toBe('612-655-0830');
  });

  it('should format phone numbers with leading US country code 1 or +1', () => {
    expect(formatPhoneNumber('+1-612-655-0830')).toBe('612-655-0830');
    expect(formatPhoneNumber('+1 (612) 655-0830')).toBe('612-655-0830');
    expect(formatPhoneNumber('16126550830')).toBe('612-655-0830');
    expect(formatPhoneNumber('1-612-655-0830')).toBe('612-655-0830');
  });

  it('should return dash for null, undefined, or empty/blank strings', () => {
    expect(formatPhoneNumber(null)).toBe('—');
    expect(formatPhoneNumber(undefined)).toBe('—');
    expect(formatPhoneNumber('')).toBe('—');
    expect(formatPhoneNumber('   ')).toBe('—');
  });

  it('should fallback to trimmed string for invalid non-10 digit numbers', () => {
    expect(formatPhoneNumber('12345')).toBe('12345');
    expect(formatPhoneNumber('ext 102')).toBe('ext 102');
  });
});
