import type { components, operations } from "@/lib/generated/bridgeflow-api";

export type Project = components["schemas"]["ProjectResponse"];
export type RequirementPage = components["schemas"]["RequirementPageResponse"];
export type Revision = components["schemas"]["RevisionResponse"];
export type Requirement = components["schemas"]["RequirementResponse"];
export type AuthUser = components["schemas"]["UserResponse"];
export type LoginResponse = components["schemas"]["LoginResponse"];

type CreateProjectRequest = components["schemas"]["CreateProjectRequest"];
type UpdateProjectRequest = components["schemas"]["UpdateProjectRequest"];
type CreateRequirementRequest = components["schemas"]["CreateRequirementRequest"];
type CreateRevisionRequest = components["schemas"]["CreateRevisionRequest"];
type ListRequirementOptions = NonNullable<operations["listRequirements"]["parameters"]["query"]>;
type LoginRequest = components["schemas"]["LoginRequest"];

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
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
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
};
