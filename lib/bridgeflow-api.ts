import type { components, operations } from "@/lib/generated/bridgeflow-api";

export type Project = components["schemas"]["ProjectResponse"];
export type RequirementPage = components["schemas"]["RequirementPageResponse"];
export type Revision = components["schemas"]["RevisionResponse"];
export type Requirement = components["schemas"]["RequirementResponse"];
export type AuthUser = components["schemas"]["UserResponse"];
export type LoginResponse = components["schemas"]["LoginResponse"];
export type GlossaryTerm = components["schemas"]["GlossaryTermResponse"];
export type ProjectDocument = components["schemas"]["DocumentResponse"];
export type DocumentVersion = components["schemas"]["DocumentVersionResponse"];
export type AiJob = components["schemas"]["AiJobResponse"];
export type ArtifactReviewStatus = "DRAFT" | "APPROVED" | "REJECTED";
export type AnalysisAiJob = AiJob & {
  documentVersionId: string | null;
  requirementRevisionId: string | null;
  clarificationQuestionIds: string[];
  acceptanceCriterionIds: string[];
};
export type ClarificationQuestion = {
  id: string;
  requirementRevisionId: string;
  aiJobId: string;
  japaneseText: string;
  vietnameseText: string;
  rationale: string | null;
  answerJapanese: string | null;
  answerVietnamese: string | null;
  status: ArtifactReviewStatus;
  createdBy: string;
  createdAt: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
  answeredBy: string | null;
  answeredAt: string | null;
};
export type AcceptanceCriterion = {
  id: string;
  requirementRevisionId: string;
  aiJobId: string;
  japaneseText: string;
  vietnameseText: string;
  status: ArtifactReviewStatus;
  createdBy: string;
  createdAt: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
};
export type RequirementAnalysis = {
  job: AnalysisAiJob | null;
  questions: ClarificationQuestion[];
  acceptanceCriteria: AcceptanceCriterion[];
};

type CreateProjectRequest = components["schemas"]["CreateProjectRequest"];
type UpdateProjectRequest = components["schemas"]["UpdateProjectRequest"];
type CreateRequirementRequest = components["schemas"]["CreateRequirementRequest"];
type CreateRevisionRequest = components["schemas"]["CreateRevisionRequest"];
type ListRequirementOptions = NonNullable<operations["listRequirements"]["parameters"]["query"]>;
type LoginRequest = components["schemas"]["LoginRequest"];
type SaveGlossaryTermRequest = components["schemas"]["SaveGlossaryTermRequest"];

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8080/api/v1";
const TOKEN_KEY = "bridgeflow.access-token";
let accessToken: string | null = null;

function currentToken() {
  if (accessToken) return accessToken;
  if (typeof window === "undefined") return null;
  accessToken = window.sessionStorage.getItem(TOKEN_KEY);
  return accessToken;
}

function storeToken(token: string | null) {
  accessToken = token;
  if (typeof window === "undefined") return;
  if (token) window.sessionStorage.setItem(TOKEN_KEY, token);
  else window.sessionStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = currentToken();
  const isFormData = typeof FormData !== "undefined" && init?.body instanceof FormData;
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      ...(!isFormData ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });
  if (!response.ok) {
    if (response.status === 401 && path !== "/auth/login") storeToken(null);
    const body = await response.json().catch(() => null) as { message?: string } | null;
    throw new Error(body?.message ?? `API trả về lỗi ${response.status}.`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

async function requestBlob(path: string) {
  const token = currentToken();
  const response = await fetch(`${API_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    if (response.status === 401) storeToken(null);
    const body = await response.json().catch(() => null) as { message?: string } | null;
    throw new Error(body?.message ?? `API trả về lỗi ${response.status}.`);
  }
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const quoted = disposition.match(/filename="([^"]+)"/i)?.[1];
  return {
    blob: await response.blob(),
    filename: encoded ? decodeURIComponent(encoded) : quoted ?? "document",
  };
}

export const bridgeFlowApi = {
  hasSession: () => currentToken() !== null,
  login: async (body: LoginRequest) => {
    const result = await request<LoginResponse>("/auth/login", {
      method: "POST", body: JSON.stringify(body),
    });
    storeToken(result.accessToken);
    return result;
  },
  me: () => request<AuthUser>("/auth/me"),
  logout: async () => {
    try { await request<void>("/auth/logout", { method: "POST" }); }
    finally { storeToken(null); }
  },
  listProjects: () => request<Project[]>("/projects"),
  createProject: (body: CreateProjectRequest) =>
    request<Project>("/projects", { method: "POST", body: JSON.stringify(body) }),
  updateProject: (projectId: string, body: UpdateProjectRequest) =>
    request<Project>(`/projects/${projectId}`, { method: "PATCH", body: JSON.stringify(body) }),
  archiveProject: (projectId: string) =>
    request<Project>(`/projects/${projectId}/archive`, { method: "POST", body: "{}" }),
  listRequirements: (projectId: string, options: ListRequirementOptions = {}) => {
    const params = new URLSearchParams();
    if (options.status) params.set("status", options.status);
    if (options.includeArchived) params.set("includeArchived", "true");
    if (options.query) params.set("query", options.query);
    params.set("page", String(options.page ?? 0));
    params.set("size", String(options.size ?? 10));
    params.set("sortBy", options.sortBy ?? "displayKey");
    params.set("direction", options.direction ?? "asc");
    return request<RequirementPage>(`/projects/${projectId}/requirements?${params}`);
  },
  getRequirement: (requirementId: string) => request<Requirement>(`/requirements/${requirementId}`),
  createRequirement: (projectId: string, body: CreateRequirementRequest) =>
    request<Requirement>(`/projects/${projectId}/requirements`, { method: "POST", body: JSON.stringify(body) }),
  addRevision: (requirementId: string, body: CreateRevisionRequest) =>
    request<Requirement>(`/requirements/${requirementId}/revisions`, { method: "POST", body: JSON.stringify(body) }),
  confirmRevision: (requirementId: string, revisionId: string) =>
    request<Requirement>(`/requirements/${requirementId}/revisions/${revisionId}/confirm`, {
      method: "POST", body: "{}",
    }),
  archiveRequirement: (requirementId: string) =>
    request<Requirement>(`/requirements/${requirementId}/archive`, { method: "POST", body: "{}" }),
  listGlossaryTerms: (projectId: string, query = "") => {
    const params = new URLSearchParams();
    if (query.trim()) params.set("query", query.trim());
    const suffix = params.size ? `?${params}` : "";
    return request<GlossaryTerm[]>(`/projects/${projectId}/glossary${suffix}`);
  },
  createGlossaryTerm: (projectId: string, body: SaveGlossaryTermRequest) =>
    request<GlossaryTerm>(`/projects/${projectId}/glossary`, {
      method: "POST", body: JSON.stringify(body),
    }),
  updateGlossaryTerm: (projectId: string, termId: string, body: SaveGlossaryTermRequest) =>
    request<GlossaryTerm>(`/projects/${projectId}/glossary/${termId}`, {
      method: "PATCH", body: JSON.stringify(body),
    }),
  deleteGlossaryTerm: (projectId: string, termId: string) =>
    request<void>(`/projects/${projectId}/glossary/${termId}`, { method: "DELETE" }),
  listDocuments: (projectId: string, includeArchived = false) =>
    request<ProjectDocument[]>(`/projects/${projectId}/documents${includeArchived ? "?includeArchived=true" : ""}`),
  uploadDocument: (projectId: string, title: string, file: File) => {
    const body = new FormData();
    body.append("title", title);
    body.append("file", file);
    return request<ProjectDocument>(`/projects/${projectId}/documents`, { method: "POST", body });
  },
  uploadDocumentVersion: (documentId: string, file: File) => {
    const body = new FormData();
    body.append("file", file);
    return request<ProjectDocument>(`/documents/${documentId}/versions`, { method: "POST", body });
  },
  archiveDocument: (documentId: string) =>
    request<ProjectDocument>(`/documents/${documentId}/archive`, { method: "POST", body: "{}" }),
  downloadDocumentVersion: (documentId: string, versionId: string) =>
    requestBlob(`/documents/${documentId}/versions/${versionId}/content`),
  listAiJobs: (projectId: string) =>
    request<AiJob[]>(`/projects/${projectId}/ai-jobs`),
  extractRequirements: (documentId: string, versionId: string) =>
    request<AiJob>(`/documents/${documentId}/versions/${versionId}/ai-extractions`, {
      method: "POST", body: "{}",
    }),
  getRequirementAnalysis: (requirementId: string, revisionId: string) =>
    request<RequirementAnalysis>(`/requirements/${requirementId}/revisions/${revisionId}/analysis`),
  generateRequirementAnalysis: (requirementId: string, revisionId: string) =>
    request<RequirementAnalysis>(`/requirements/${requirementId}/revisions/${revisionId}/ai-analysis`, {
      method: "POST", body: "{}",
    }),
  answerClarificationQuestion: (
    requirementId: string,
    revisionId: string,
    questionId: string,
    body: { japaneseText: string; vietnameseText: string },
  ) => request<ClarificationQuestion>(
    `/requirements/${requirementId}/revisions/${revisionId}/questions/${questionId}/answer`,
    { method: "PATCH", body: JSON.stringify(body) },
  ),
  reviewClarificationQuestion: (
    requirementId: string,
    revisionId: string,
    questionId: string,
    decision: Exclude<ArtifactReviewStatus, "DRAFT">,
  ) => request<ClarificationQuestion>(
    `/requirements/${requirementId}/revisions/${revisionId}/questions/${questionId}/review`,
    { method: "POST", body: JSON.stringify({ decision }) },
  ),
  reviewAcceptanceCriterion: (
    requirementId: string,
    revisionId: string,
    criterionId: string,
    decision: Exclude<ArtifactReviewStatus, "DRAFT">,
  ) => request<AcceptanceCriterion>(
    `/requirements/${requirementId}/revisions/${revisionId}/acceptance-criteria/${criterionId}/review`,
    { method: "POST", body: JSON.stringify({ decision }) },
  ),
};
