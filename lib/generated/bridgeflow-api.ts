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
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/test-cases/{testCaseId}/review": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Approve or reject a generated test case */
        post: operations["reviewTestCase"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/test-cases/ai-generation": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Generate drafts from approved acceptance criteria */
        post: operations["generateRevisionTestCases"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/questions/{questionId}/review": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Approve or reject a clarification question */
        post: operations["reviewClarificationQuestion"];
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
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/ai-analysis": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Generate AI draft questions and criteria */
        post: operations["generateRequirementAnalysis"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/acceptance-criteria/{criterionId}/review": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Approve or reject an acceptance criterion */
        post: operations["reviewAcceptanceCriterion"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/relations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List incoming and outgoing requirement relations */
        get: operations["listRequirementRelations"];
        put?: never;
        /** Create a project-scoped requirement relation */
        post: operations["createRequirementRelation"];
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
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/questions/{questionId}/answer": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /** Answer a clarification question */
        patch: operations["answerClarificationQuestion"];
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
        /** Permanently delete an archived project */
        delete: operations["deleteProject"];
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
    "/api/v1/requirements/{requirementId}/traceability": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Trace a requirement across revisions and artifacts */
        get: operations["getRequirementTraceability"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/test-cases": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get test-case drafts for a requirement revision */
        get: operations["getRevisionTestCases"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/change-impact": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Compare a revision and list artifacts to revalidate */
        get: operations["getRequirementChangeImpact"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/requirements/{requirementId}/revisions/{revisionId}/analysis": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Get AI drafts for a requirement revision */
        get: operations["getRequirementAnalysis"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/members": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List members of an accessible project */
        get: operations["listProjectMembers"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/exports/requirements.md": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Export active requirements as Markdown */
        get: operations["exportRequirementsMarkdown"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/projects/{projectId}/exports/requirements.csv": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Export active requirements as UTF-8 CSV */
        get: operations["exportRequirementsCsv"];
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
    "/api/v1/requirements/{requirementId}/relations/{relationId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /** Delete a requirement relation */
        delete: operations["deleteRequirementRelation"];
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
        ReviewTestCaseRequest: {
            /** @enum {string} */
            decision: "DRAFT" | "APPROVED" | "REJECTED";
        };
        TestCaseResponse: {
            /** Format: uuid */
            id?: string;
            /** Format: uuid */
            requirementRevisionId?: string;
            /** Format: uuid */
            acceptanceCriterionId?: string;
            /** Format: uuid */
            aiJobId?: string;
            titleJapanese?: string;
            titleVietnamese?: string;
            preconditionsJapanese?: string;
            preconditionsVietnamese?: string;
            stepsJapanese?: string;
            stepsVietnamese?: string;
            expectedResultJapanese?: string;
            expectedResultVietnamese?: string;
            /** @enum {string} */
            priority?: "HIGH" | "MEDIUM" | "LOW";
            /** @enum {string} */
            status?: "DRAFT" | "APPROVED" | "REJECTED";
            /** Format: uuid */
            createdBy?: string;
            /** Format: date-time */
            createdAt?: string;
            /** Format: uuid */
            reviewedBy?: string | null;
            /** Format: date-time */
            reviewedAt?: string | null;
        };
        AiJobResponse: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            projectId: string;
            /** Format: uuid */
            documentVersionId: string | null;
            /** Format: uuid */
            requirementRevisionId: string | null;
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
            clarificationQuestionIds: string[];
            acceptanceCriterionIds: string[];
            testCaseIds: string[];
            errorCode: string | null;
            errorMessage: string | null;
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            startedAt: string | null;
            /** Format: date-time */
            completedAt: string | null;
        };
        TestCaseWorkspaceResponse: {
            job: components["schemas"]["AiJobResponse"];
            testCases: components["schemas"]["TestCaseResponse"][];
        };
        ReviewArtifactRequest: {
            /** @enum {string} */
            decision: "DRAFT" | "APPROVED" | "REJECTED";
        };
        ClarificationQuestionResponse: {
            /** Format: uuid */
            id?: string;
            /** Format: uuid */
            requirementRevisionId?: string;
            /** Format: uuid */
            aiJobId?: string;
            japaneseText?: string;
            vietnameseText?: string;
            rationale?: string | null;
            answerJapanese?: string | null;
            answerVietnamese?: string | null;
            /** @enum {string} */
            status?: "DRAFT" | "APPROVED" | "REJECTED";
            /** Format: uuid */
            createdBy?: string;
            /** Format: date-time */
            createdAt?: string;
            /** Format: uuid */
            reviewedBy?: string | null;
            /** Format: date-time */
            reviewedAt?: string | null;
            /** Format: uuid */
            answeredBy?: string | null;
            /** Format: date-time */
            answeredAt?: string | null;
        };
        AcceptanceCriterionResponse: {
            /** Format: uuid */
            id?: string;
            /** Format: uuid */
            requirementRevisionId?: string;
            /** Format: uuid */
            aiJobId?: string;
            japaneseText?: string;
            vietnameseText?: string;
            /** @enum {string} */
            status?: "DRAFT" | "APPROVED" | "REJECTED";
            /** Format: uuid */
            createdBy?: string;
            /** Format: date-time */
            createdAt?: string;
            /** Format: uuid */
            reviewedBy?: string | null;
            /** Format: date-time */
            reviewedAt?: string | null;
        };
        RequirementAnalysisResponse: {
            job: components["schemas"]["AiJobResponse"];
            questions: components["schemas"]["ClarificationQuestionResponse"][];
            acceptanceCriteria: components["schemas"]["AcceptanceCriterionResponse"][];
        };
        CreateRequirementRelationRequest: {
            /** Format: uuid */
            targetRequirementId: string;
            /** @enum {string} */
            relationType: "DEPENDS_ON" | "SUPERSEDES" | "SPLIT_INTO" | "MERGED_INTO" | "DUPLICATES";
        };
        RequirementRelationResponse: {
            /** Format: uuid */
            id?: string;
            /** Format: uuid */
            sourceRequirementId?: string;
            sourceDisplayKey?: string;
            /** Format: uuid */
            targetRequirementId?: string;
            targetDisplayKey?: string;
            /** @enum {string} */
            relationType?: "DEPENDS_ON" | "SUPERSEDES" | "SPLIT_INTO" | "MERGED_INTO" | "DUPLICATES";
            /** Format: uuid */
            createdBy?: string;
            /** Format: date-time */
            createdAt?: string;
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
        AnswerQuestionRequest: {
            japaneseText: string;
            vietnameseText: string;
        };
        UpdateProjectRequest: {
            name: string;
            customerName?: string;
            aiEnabled?: boolean;
        };
        ArtifactTraceResponse: {
            /** Format: uuid */
            id?: string;
            type?: string;
            status?: string;
            /** Format: uuid */
            sourceArtifactId?: string | null;
        };
        RelationTraceResponse: {
            /** Format: uuid */
            relationId?: string;
            direction?: string;
            relationType?: string;
            /** Format: uuid */
            relatedRequirementId?: string;
            relatedDisplayKey?: string;
        };
        RequirementTraceabilityResponse: {
            /** Format: uuid */
            requirementId?: string;
            displayKey?: string;
            /** Format: uuid */
            currentRevisionId?: string | null;
            revisions?: components["schemas"]["RevisionTraceResponse"][];
            relations?: components["schemas"]["RelationTraceResponse"][];
        };
        RevisionTraceResponse: {
            /** Format: uuid */
            revisionId?: string;
            /** Format: int32 */
            revisionNumber?: number;
            changeType?: string;
            reviewStatus?: string;
            /** Format: uuid */
            documentVersionId?: string | null;
            sourceAnchor?: string | null;
            artifacts?: components["schemas"]["ArtifactTraceResponse"][];
        };
        AffectedArtifactResponse: {
            /** Format: uuid */
            id?: string;
            type?: string;
            status?: string;
            /** Format: uuid */
            sourceRevisionId?: string;
            recommendedAction?: string;
        };
        ChangeImpactResponse: {
            /** Format: uuid */
            requirementId?: string;
            /** Format: uuid */
            targetRevisionId?: string;
            /** Format: uuid */
            baselineRevisionId?: string | null;
            impactLevel?: string;
            japaneseChanged?: boolean;
            vietnameseChanged?: boolean;
            requiresArtifactRegeneration?: boolean;
            reasons?: string[];
            affectedArtifacts?: components["schemas"]["AffectedArtifactResponse"][];
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
        ProjectMemberResponse: {
            /** Format: uuid */
            userId: string;
            email: string;
            displayName: string;
            role: string;
        };
        HealthResponse: {
            status?: string;
            service?: string;
            /** Format: date-time */
            timestamp?: string;
        };
        DeleteProjectRequest: {
            confirmationCode: string;
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
    reviewTestCase: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
                revisionId: string;
                testCaseId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReviewTestCaseRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TestCaseResponse"];
                };
            };
        };
    };
    generateRevisionTestCases: {
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
                    "*/*": components["schemas"]["TestCaseWorkspaceResponse"];
                };
            };
        };
    };
    reviewClarificationQuestion: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
                revisionId: string;
                questionId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReviewArtifactRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ClarificationQuestionResponse"];
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
    generateRequirementAnalysis: {
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
                    "*/*": components["schemas"]["RequirementAnalysisResponse"];
                };
            };
        };
    };
    reviewAcceptanceCriterion: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
                revisionId: string;
                criterionId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReviewArtifactRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AcceptanceCriterionResponse"];
                };
            };
        };
    };
    listRequirementRelations: {
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
                    "*/*": components["schemas"]["RequirementRelationResponse"][];
                };
            };
        };
    };
    createRequirementRelation: {
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
                "application/json": components["schemas"]["CreateRequirementRelationRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RequirementRelationResponse"];
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
    answerClarificationQuestion: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
                revisionId: string;
                questionId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AnswerQuestionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ClarificationQuestionResponse"];
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
    deleteProject: {
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
                "application/json": components["schemas"]["DeleteProjectRequest"];
            };
        };
        responses: {
            /** @description Project and private document files deleted */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
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
    getRequirementTraceability: {
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
                    "*/*": components["schemas"]["RequirementTraceabilityResponse"];
                };
            };
        };
    };
    getRevisionTestCases: {
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
                    "*/*": components["schemas"]["TestCaseWorkspaceResponse"];
                };
            };
        };
    };
    getRequirementChangeImpact: {
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
                    "*/*": components["schemas"]["ChangeImpactResponse"];
                };
            };
        };
    };
    getRequirementAnalysis: {
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
                    "*/*": components["schemas"]["RequirementAnalysisResponse"];
                };
            };
        };
    };
    listProjectMembers: {
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
                    "*/*": components["schemas"]["ProjectMemberResponse"][];
                };
            };
        };
    };
    exportRequirementsMarkdown: {
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
                    "text/markdown": string;
                };
            };
        };
    };
    exportRequirementsCsv: {
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
                    "text/csv": string;
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
    deleteRequirementRelation: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                requirementId: string;
                relationId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
}
