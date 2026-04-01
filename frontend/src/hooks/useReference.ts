import { createContext, useContext } from 'react';
import { ReferenceDescDto } from '../api/generated/types';

export type ReferenceContextType = {
  references: Record<string, ReferenceDescDto[]>;
  getReference: (name: string) => ReferenceDescDto[];
  toCode: (name: string, display: string) => string | undefined;
  toDisplay: (name: string, code: string) => string | undefined;
  isLoading: boolean;
};

export const ReferenceContext = createContext<ReferenceContextType>({
  references: {},
  getReference: () => [],
  toCode: () => undefined,
  toDisplay: () => undefined,
  isLoading: true,
});

export function useReference() {
  return useContext(ReferenceContext);
}