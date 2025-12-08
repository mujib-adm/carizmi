import { AxiosError } from "axios";
import { GlobalResponse, NormalizedResponse } from "../constants/types";

export function normalizeAxiosResponse(error: GlobalResponse): NormalizedResponse {
    const response: NormalizedResponse = {
        fieldMessages: {},
        globalMessages: [],
        statusCode: undefined,
        statusDesc: undefined,
    };

    // Map globalMessages
    if (Array.isArray(error.globalMessages)) {
        response.globalMessages = error.globalMessages.map((gm) => gm.message);
    }

    // Map fieldMessages
    if (Array.isArray(error.fieldMessages)) {
        error.fieldMessages.forEach((fm) => {
            if (fm.fieldName && fm.message) {
                response.fieldMessages[fm.fieldName] = fm.message;
            }
        });
    }

    response.statusCode = error.statusCode;
    response.statusDesc = error.statusDesc;

    return response;
}