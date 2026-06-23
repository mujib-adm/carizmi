import { useEffect, useState } from 'react';
import { referenceDataApi } from '../api/generated/reference-data/reference-data';
import { STARTUP_REFERENCES } from '../constants/ReferenceConstants';
import { ReferenceDescDto } from '../api/generated/types';
import { useAuth } from '../hooks/useAuth';
import { useNotification } from '../hooks/useNotification';
import { ReferenceContext } from '../hooks/useReference';

export function ReferenceProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const notify = useNotification();
  const [references, setReferences] = useState<Record<string, ReferenceDescDto[]>>({});
  const [isLoading, setIsLoading] = useState(true);

  const fetchAllReferences = async () => {
    setIsLoading(true);
    try {
      // Define the reference names we need to fetch
      const referenceNames = STARTUP_REFERENCES;

      // Create promises for each fetch
      const promises = referenceNames.map((name) => referenceDataApi.getReferencesByName(name));

      // Execute all in parallel
      const results = await Promise.all(promises);

      const newReferences: Record<string, ReferenceDescDto[]> = {};

      results.forEach((res, index) => {
        const name = referenceNames[index];
        if (res.responseData) {
          newReferences[name] = res.responseData;
        } else {
          newReferences[name] = [];
        }
      });

      setReferences(newReferences);
    } catch (error) {
      notify.error({
        message: 'Reference Data Error',
        description: 'Failed to load reference data. Dropdowns may be empty.',
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      Promise.resolve().then(() => fetchAllReferences());
    } else {
      Promise.resolve().then(() => {
        setReferences({});
        setIsLoading(false);
      });
    }
  }, [isAuthenticated]);

  const getReference = (name: string) => {
    return references[name] || [];
  };

  const toCode = (name: string, display: string) => {
    const ref = references[name]?.find((r) => r.referenceDisplay === display);
    return ref?.referenceCode;
  };

  const toDisplay = (name: string, code: string) => {
    const ref = references[name]?.find((r) => r.referenceCode === code);
    return ref?.referenceDisplay || code;
  };

  return (
    <ReferenceContext.Provider value={{ references, getReference, toCode, toDisplay, isLoading }}>
      {children}
    </ReferenceContext.Provider>
  );
}