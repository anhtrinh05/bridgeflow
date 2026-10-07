export interface paths {
    "/api/v1/requirements/{requirementId}/revisions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Add a requirement revision */
        post: operations["addRequirementRevision"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/confirm": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Confirm a requirement revision */
        post: operations["confirmRequirementRevision"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Archive a requirement */
        post: operations["archiveRequirement"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List projects */
        get: operations["listProjects"];
        put?: never;
        /** Create a project */
        post: operations["createProject"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/requirements": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Search and page project requirements */
        get: operations["listRequirements"];
        put?: never;
        /** Create a requirement with its first revision */
        post: operations["createRequirement"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/glossary": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Search project glossary terms */
        get: operations["listGlossaryTerms"];
        put?: never;
        /** Create a glossary term */
        post: operations["createGlossaryTerm"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/documents": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List project documents */
        get: operations["listDocuments"];
        put?: never;
        /** Upload a new document and its first version */
        post: operations["uploadDocument"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Archive a project */
        post: operations["archiveProject"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/documents/{documentId}/versions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Upload an immutable document version */
        post: operations["uploadDocumentVersion"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/documents/{documentId}/versions/{versionId}/ai-extractions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Extract draft requirements from a document version */
        post: operations["extractRequirements"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/documents/{documentId}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Archive a document */
        post: operations["archiveDocument"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Revoke the current access token */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Login with email and password */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get a project */
        get: operations["getProject"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /** Update a project */
        patch: operations["updateProject"];
        trace?: never;
    };
    "/api/v1/projects/{projectId}/glossary/{termId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /** Delete a glossary term */
        delete: operations["deleteGlossaryTerm"];
        options?: never;
        head?: never;
        /** Update a glossary term */
        patch: operations["updateGlossaryTerm"];
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get a requirement and its revision history */
        get: operations["getRequirement"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/ai-jobs": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List AI jobs for a project */
        get: operations["listAiJobs"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/health": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["health"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/documents/{documentId}/versions/{versionId}/content": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Download a document version */
        get: operations["downloadDocumentVersion"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get the authenticated user */
        get: operations["getCurrentUser"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        CreateRevisionRequest: {
            japaneseText: string;
            vietnameseText: string;
            /** @enum {string} */
            changeType: "ADDED" | "MODIFIED" | "UNCHANGED" | "DELETED" | "SPLIT" | "MERGED";
        };
        RequirementResponse: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            projectId: string;
            displayKey: string;
            status: string;
            /** Format: uuid */
            currentRevisionId: string | null;
            latestRevision: components["schemas"]["RevisionResponse"];
            revisions: components["schemas"]["RevisionResponse"][];
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            updatedAt: string;
            /** Format: date-time */
            archivedAt: string | null;
        };
        RevisionResponse: {
            /** Format: uuid */
            id: string;
            /** Format: int32 */
            revisionNumber: number;
            japaneseText: string;
            vietnameseText: string;
            changeType: string;
            reviewStatus: string;
            /** Format: uuid */
            documentVersionId: string | null;
            sourceAnchor: string | null;
            /** Format: date-time */
            createdAt: string;
            /** Format: uuid */
            confirmedBy: string | null;
            /** Format: date-time */
            confirmedAt: string | null;
        };
        CreateProjectRequest: {
            code: string;
            name: string;
            customerName?: string;
        };
        ProjectResponse: {
            /** Format: uuid */
            id: string;
            code: string;
            name: string;
            customerName: string | null;
            status: string;
            role: string;
            aiEnabled: boolean;
            /** Format: int64 */
            requirementCount: number;
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            updatedAt: string;
        };
        CreateRequirementRequest: {
            displayKey: string;
            japaneseText: string;
            vietnameseText: string;
        };
        SaveGlossaryTermRequest: {
            japaneseTerm: string;
            vietnameseTerm: string;
            notes?: string;
        };
        GlossaryTermResponse: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            projectId: string;
            japaneseTerm: string;
            vietnameseTerm: string;
            notes: string | null;
            /** Format: uuid */
            createdBy: string;
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            updatedAt: string;
        };
        DocumentResponse: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            projectId: string;
            title: string;
            status: string;
            latestVersion: components["schemas"]["DocumentVersionResponse"];
            versions: components["schemas"]["DocumentVersionResponse"][];
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            updatedAt: string;
            /** Format: date-time */
            archivedAt: string | null;
        };
        DocumentVersionResponse: {
            /** Format: uuid */
            id: string;
            /** Format: int32 */
            versionNumber: number;
            originalFilename: string;
            contentType: string;
            /** Format: int64 */
            sizeBytes: number;
            sha256: string;
            /** Format: uuid */
            uploadedBy: string;
            /** Format: date-time */
            createdAt: string;
        };
        AiJobResponse: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            projectId: string;
            /** Format: uuid */
            documentVersionId: string;
            purpose: string;
            status: string;
            provider: string;
            model: string;
            /** Format: uuid */
            correlationId: string;
            /** Format: uuid */
            requestedBy: string;
            /** Format: int32 */
            candidateCount: number;
            requirementIds: string[];
            errorCode: string | null;
            errorMessage: string | null;
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            startedAt: string | null;
            /** Format: date-time */
            completedAt: string | null;
        };
        LoginRequest: {
            /** Format: email */
            email: string;
            password: string;
        };
        LoginResponse: {
            accessToken: string;
            /** Format: date-time */
            expiresAt: string;
            user: components["schemas"]["UserResponse"];
        };
        UserResponse: {
            /** Format: uuid */
            id: string;
            email: string;
            displayName: string;
        };
        UpdateProjectRequest: {
            name: string;
            customerName?: string;
            aiEnabled?: boolean;
        };
        RequirementPageResponse: {
            items: components["schemas"]["RequirementResponse"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            size: number;
            /** Format: int64 */
            totalElements: number;
            /** Format: int32 */
            totalPages: number;
        };
        HealthResponse: {
            status?: string;
            service?: string;
            /** Format: date-time */
            timestamp?: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    addRequirementRevision: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateRevisionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementResponse"];
                };
            };
        };
    };
    confirmRequirementRevision: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
                revisionId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementResponse"];
                };
            };
        };
    };
    archiveRequirement: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementResponse"];
                };
            };
        };
    };
    listProjects: {
        parameters: {
            query?: {
                includeArchived?: boolean;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProjectResponse"][];
                };
            };
        };
    };
    createProject: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateProjectRequest"];
            };
        };
        responses: {
            /** @description Project created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProjectResponse"];
                };
            };
        };
    };
    listRequirements: {
        parameters: {
            query?: {
                status?: "DRAFT" | "REVIEWING" | "CONFIRMED" | "REJECTED" | "ARCHIVED";
                includeArchived?: boolean;
                query?: string;
                page?: number;
                size?: number;
                sortBy?: "displayKey" | "status" | "updatedAt";
                direction?: "asc" | "desc";
            };
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementPageResponse"];
                };
            };
        };
    };
    createRequirement: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateRequirementRequest"];
            };
        };
        responses: {
            /** @description Requirement created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementResponse"];
                };
            };
        };
    };
    listGlossaryTerms: {
        parameters: {
            query?: {
                query?: string;
            };
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["GlossaryTermResponse"][];
                };
            };
        };
    };
    createGlossaryTerm: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SaveGlossaryTermRequest"];
            };
        };
        responses: {
            /** @description Glossary term created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["GlossaryTermResponse"];
                };
            };
        };
    };
    listDocuments: {
        parameters: {
            query?: {
                includeArchived?: boolean;
            };
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["DocumentResponse"][];
                };
            };
        };
    };
    uploadDocument: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": {
                    title: string;
                    /** Format: binary */
                    file: string;
                };
            };
        };
        responses: {
            /** @description Document uploaded */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["DocumentResponse"];
                };
            };
        };
    };
    archiveProject: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProjectResponse"];
                };
            };
        };
    };
    uploadDocumentVersion: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                documentId: string;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /** Format: binary */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["DocumentResponse"];
                };
            };
        };
    };
    extractRequirements: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                documentId: string;
                versionId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AiJobResponse"];
                };
            };
        };
    };
    archiveDocument: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                documentId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["DocumentResponse"];
                };
            };
        };
    };
    logout: {
        parameters: {
            query?: never;
            header: {
                Authorization: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    login: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LoginRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["LoginResponse"];
                };
            };
        };
    };
    getProject: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProjectResponse"];
                };
            };
        };
    };
    updateProject: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateProjectRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProjectResponse"];
                };
            };
        };
    };
    deleteGlossaryTerm: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
                termId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Glossary term deleted */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    updateGlossaryTerm: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
                termId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SaveGlossaryTermRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["GlossaryTermResponse"];
                };
            };
        };
    };
    getRequirement: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementResponse"];
                };
            };
        };
    };
    listAiJobs: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                projectId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AiJobResponse"][];
                };
            };
        };
    };
    health: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["HealthResponse"];
                };
            };
        };
    };
    downloadDocumentVersion: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                documentId: string;
                versionId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getCurrentUser: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserResponse"];
                };
            };
        };
    };
}
