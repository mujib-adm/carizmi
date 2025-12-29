import { createContext, useContext, useEffect, useState } from "react";
import { getReferencesByName } from "../apiclient/referenceApi";
import { STARTUP_REFERENCES } from "../constants/referenceConstants";
import { ReferenceData } from "../constants/types";
import { useAuth } from "./AuthContext";

type ReferenceContextType = {
    references: Record<string, ReferenceData[]>;
    getReference: (name: string) => ReferenceData[];
    toCode: (name: string, display: string) => string | undefined;
    toDisplay: (name: string, code: string) => string | undefined;
    isLoading: boolean;
};

const ReferenceContext = createContext<ReferenceContextType>({
    references: {},
    getReference: () => [],
    toCode: () => undefined,
    toDisplay: () => undefined,
    isLoading: true,
});

export function ReferenceProvider({ children }: { children: React.ReactNode }) {
    const { token } = useAuth();
    const [references, setReferences] = useState<Record<string, ReferenceData[]>>({});
    const [isLoading, setIsLoading] = useState(true);

    const fetchAllReferences = async () => {
        setIsLoading(true);
        try {
            // Define the reference names we need to fetch
            const referenceNames = STARTUP_REFERENCES;

            // Create promises for each fetch
            const promises = referenceNames.map(name => getReferencesByName(name));

            // Execute all in parallel
            const results = await Promise.all(promises);

            const newReferences: Record<string, ReferenceData[]> = {};

            results.forEach((res, index) => {
                const name = referenceNames[index];
                if (res.responseData) {
                    newReferences[name] = res.responseData.map(r => ({
                        code: r.referenceCode,
                        display: r.referenceDisplay
                    }));
                } else {
                    newReferences[name] = [];
                }
            });

            setReferences(newReferences);
        } catch (error) {
            console.error("Failed to fetch references:", error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (token) {
            fetchAllReferences();
        } else {
            setReferences({});
            setIsLoading(false);
        }
    }, [token]);


    const getReference = (name: string) => {
        return references[name] || [];
    };

    const toCode = (name: string, display: string) => {
        const ref = references[name]?.find(r => r.display === display);
        return ref?.code;
    }

    const toDisplay = (name: string, code: string) => {
        const ref = references[name]?.find(r => r.code === code);
        return ref?.display || code;
    }

    return (
        <ReferenceContext.Provider value={{ references, getReference, toCode, toDisplay, isLoading }}>
            {children}
        </ReferenceContext.Provider>
    );
}

export function useReference() {
    return useContext(ReferenceContext);
}