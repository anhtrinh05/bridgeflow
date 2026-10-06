import type { components, operations } from "@/lib/generated/bridgeflow-api";

export type Project = components["schemas"]["ProjectResponse"];
export type RequirementPage = components["schemas"]["RequirementPageResponse"];
export type Revision = components["schemas"]["RevisionResponse"];
export type Requirement = components["schemas"]["RequirementResponse"];

type CreateProjectRequest = components["schemas"]["CreateProjectRequest"];
type UpdateProjectRequest = components["schemas"]["UpdateProjectRequest"];
type CreateRequirementRequest = components["schemas"]["CreateRequirementRequest"];
type CreateRevisionRequest = components["schemas"]["CreateRevisionRequest"];
type ListRequirementOptions = NonNullable<operations["listRequirements"]["parameters"]["query"]>;

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8080/api/v1";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null;
    throw new Error(body?.message ?? `API trả về lỗi ${response.status}.`);
  }
  return response.json() as Promise<T>;
}

export const bridgeFlowApi = {
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
  confirmRevision: (requirementId: string, revisionId: string, reviewerId: string) =>
    request<Requirement>(`/requirements/${requirementId}/revisions/${revisionId}/confirm`, {
      method: "POST", body: JSON.stringify({ reviewerId }),
    }),
  archiveRequirement: (requirementId: string) =>
    request<Requirement>(`/requirements/${requirementId}/archive`, { method: "POST", body: "{}" }),
};
