import { describe, it, expect } from 'vitest';
import { formatPhoneInput, formatPhoneNumber } from '../../utils/phoneUtils.ts';

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
    expect(formatPhoneNumber('(720)3002453')).toBe('720-300-2453');
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

  it('should return dash for null, undefined, or empty/blank strings by default', () => {
    expect(formatPhoneNumber(null)).toBe('—');
    expect(formatPhoneNumber(undefined)).toBe('—');
    expect(formatPhoneNumber('')).toBe('—');
    expect(formatPhoneNumber('   ')).toBe('—');
  });

  it('should support custom emptyFallback', () => {
    expect(formatPhoneNumber(null, '')).toBe('');
    expect(formatPhoneNumber('', '')).toBe('');
    expect(formatPhoneNumber(undefined, 'N/A')).toBe('N/A');
  });

  it('should fallback to trimmed string for invalid non-10 digit numbers', () => {
    expect(formatPhoneNumber('12345')).toBe('12345');
    expect(formatPhoneNumber('ext 102')).toBe('ext 102');
  });
});

describe('formatPhoneInput', () => {
  it('should return empty string for empty input', () => {
    expect(formatPhoneInput('')).toBe('');
    expect(formatPhoneInput(null)).toBe('');
    expect(formatPhoneInput(undefined)).toBe('');
  });

  it('should format as-you-type progressively', () => {
    expect(formatPhoneInput('7')).toBe('7');
    expect(formatPhoneInput('72')).toBe('72');
    expect(formatPhoneInput('720')).toBe('720');
    expect(formatPhoneInput('7203')).toBe('720-3');
    expect(formatPhoneInput('72030')).toBe('720-30');
    expect(formatPhoneInput('720300')).toBe('720-300');
    expect(formatPhoneInput('7203002')).toBe('720-300-2');
    expect(formatPhoneInput('7203002453')).toBe('720-300-2453');
  });

  it('should handle already formatted or parenthesized strings when pasted', () => {
    expect(formatPhoneInput('(720)3002453')).toBe('720-300-2453');
    expect(formatPhoneInput('(720) 300-2453')).toBe('720-300-2453');
    expect(formatPhoneInput('720.300.2453')).toBe('720-300-2453');
    expect(formatPhoneInput('720 300 2453')).toBe('720-300-2453');
  });

  it('should handle leading 1 country code', () => {
    expect(formatPhoneInput('17203002453')).toBe('720-300-2453');
  });

  it('should truncate digits beyond 10', () => {
    expect(formatPhoneInput('7203002453999')).toBe('720-300-2453');
  });
});
