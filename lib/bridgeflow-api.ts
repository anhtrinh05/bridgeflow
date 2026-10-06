export type Project = {
  id: string;
  code: string;
  name: string;
  customerName: string | null;
  status: string;
  requirementCount: number;
  createdAt: string;
  updatedAt: string;
};

export type RequirementPage = {
  items: Requirement[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Revision = {
  id: string;
  revisionNumber: number;
  japaneseText: string;
  vietnameseText: string;
  changeType: string;
  reviewStatus: string;
  createdAt: string;
  confirmedBy: string | null;
  confirmedAt: string | null;
};

export type Requirement = {
  id: string;
  projectId: string;
  displayKey: string;
  status: string;
  currentRevisionId: string | null;
  latestRevision: Revision | null;
  revisions: Revision[];
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
};

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
  createProject: (body: { code: string; name: string; customerName: string }) =>
    request<Project>("/projects", { method: "POST", body: JSON.stringify(body) }),
  updateProject: (projectId: string, body: { name: string; customerName: string }) =>
    request<Project>(`/projects/${projectId}`, { method: "PATCH", body: JSON.stringify(body) }),
  archiveProject: (projectId: string) =>
    request<Project>(`/projects/${projectId}/archive`, { method: "POST", body: "{}" }),
  listRequirements: (projectId: string, options: {
    status?: string;
    includeArchived?: boolean;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: "displayKey" | "status" | "updatedAt";
    direction?: "asc" | "desc";
  } = {}) => {
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
  createRequirement: (projectId: string, body: { displayKey: string; japaneseText: string; vietnameseText: string }) =>
    request<Requirement>(`/projects/${projectId}/requirements`, { method: "POST", body: JSON.stringify(body) }),
  addRevision: (requirementId: string, body: { japaneseText: string; vietnameseText: string; changeType: string }) =>
    request<Requirement>(`/requirements/${requirementId}/revisions`, { method: "POST", body: JSON.stringify(body) }),
  confirmRevision: (requirementId: string, revisionId: string, reviewerId: string) =>
    request<Requirement>(`/requirements/${requirementId}/revisions/${revisionId}/confirm`, {
      method: "POST", body: JSON.stringify({ reviewerId }),
    }),
  archiveRequirement: (requirementId: string) =>
    request<Requirement>(`/requirements/${requirementId}/archive`, { method: "POST", body: "{}" }),
};
