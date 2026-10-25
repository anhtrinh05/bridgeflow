"use client";

import Link from "next/link";
import "./workspace.css";

import { FormEvent, ReactNode, useCallback, useEffect, useRef, useState } from "react";
import {
  AlertCircle, Archive, Bell, BookOpenText, Check, ChevronDown, FileText,
  FolderKanban, GitCompareArrows, Languages, LayoutDashboard, LoaderCircle,
  LogOut,
  MessageSquareText, PanelLeftClose, Plus, RefreshCw, Search,
  Pencil, Settings, Sparkles, TestTube2, Trash2, Users, X,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { GlossaryWorkspace } from "@/components/glossary-workspace";
import { DocumentWorkspace } from "@/components/document-workspace";
import { RequirementAnalysisWorkspace } from "@/components/requirement-analysis";
import { RequirementTestCases } from "@/components/requirement-test-cases";
import { RequirementTraceabilityWorkspace } from "@/components/requirement-traceability";
import { ProjectOverview, ProjectMembers, ProjectSettings, ProjectAiActivity } from "@/components/project-workspace";
import { defaultRequirementTab, readWorkspaceLocation, RequirementTab, WorkspaceView, workspaceMeta } from "@/lib/workspace-navigation";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AuthUser, bridgeFlowApi, Project, Requirement } from "@/lib/bridgeflow-api";
const nav = [
  [LayoutDashboard, "overview"], [FileText, "documents"],
  [Languages, "glossary"], [BookOpenText, "requirements"], [MessageSquareText, "questions"],
  [TestTube2, "test-cases"], [GitCompareArrows, "changes"],
] as const;
const workspaceNav = [[Users, "members"], [Settings, "settings"]] as const;
const statusMeta: Record<string, { label: string; colors: string }> = {
  DRAFT: { label: "Bản nháp", colors: "border-slate-200 bg-slate-50 text-slate-600" },
  REVIEWING: { label: "Cần xác nhận", colors: "border-amber-200 bg-amber-50 text-amber-700" },
  CONFIRMED: { label: "Đã xác nhận", colors: "border-emerald-200 bg-emerald-50 text-emerald-700" },
  REJECTED: { label: "Bị từ chối", colors: "border-red-200 bg-red-50 text-red-700" },
  ARCHIVED: { label: "Đã archive", colors: "border-slate-300 bg-slate-100 text-slate-500" },
};
type RequirementStatusFilter = "" | "DRAFT" | "REVIEWING" | "CONFIRMED" | "REJECTED" | "ARCHIVED";

function StatusBadge({ status }: { status: string }) {
  const meta = statusMeta[status] ?? statusMeta.DRAFT;
  return <Badge variant="outline" className={`font-medium ${meta.colors}`}><span className="size-1.5 rounded-full bg-current" />{meta.label}</Badge>;
}

function excerpt(text?: string) {
  if (!text) return "Chưa có nội dung";
  const sentence = text.split(/[。.!?]/)[0];
  return sentence.length > 58 ? `${sentence.slice(0, 58)}…` : sentence;
}

export default function WorkspacePage() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [projects, setProjects] = useState<Project[]>([]);
  const [project, setProject] = useState<Project | null>(null);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [selected, setSelected] = useState<Requirement | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<RequirementStatusFilter>("");
  const [sortBy, setSortBy] = useState<"displayKey" | "status" | "updatedAt">("displayKey");
  const [direction, setDirection] = useState<"asc" | "desc">("asc");
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [editor, setEditor] = useState<"create" | "revision" | null>(null);
  const [projectEditor, setProjectEditor] = useState<"create" | "edit" | null>(null);
  const [projectMenuOpen, setProjectMenuOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeView, setActiveView] = useState<WorkspaceView>("overview");
  const [detailTab, setDetailTab] = useState<RequirementTab>("analysis");
  const [selecting, setSelecting] = useState(false);
  const selectionRequest = useRef(0);
  const workspaceRequest = useRef(0);

  useEffect(() => {
    function syncLocation() {
      const location = readWorkspaceLocation(window.location.hash);
      setActiveView(location.view);
      setDetailTab(location.tab);
    }
    syncLocation();
    window.addEventListener("hashchange", syncLocation);
    return () => window.removeEventListener("hashchange", syncLocation);
  }, []);

  function navigate(view: WorkspaceView, tab = defaultRequirementTab(view)) {
    const requirementViews: WorkspaceView[] = ["requirements", "questions", "test-cases", "changes"];
    if (project && requirementViews.includes(view) && !requirementViews.includes(activeView)) {
      void loadRequirementPage(0);
    }
    setActiveView(view);
    setDetailTab(tab);
    window.history.pushState(null, "", `#${new URLSearchParams({ view, tab })}`);
  }

  const load = useCallback(async (preferredProjectId?: string) => {
    const requestId = ++workspaceRequest.current;
    selectionRequest.current += 1;
    setSelecting(false);
    setLoading(true);
    setError(null);
    try {
      const visibleProjects = await bridgeFlowApi.listProjects(true);
      if (requestId !== workspaceRequest.current) return;
      setProjects(visibleProjects);
      const activeProject = visibleProjects.find((item) => item.id === preferredProjectId)
        ?? visibleProjects.find((item) => item.status === "ACTIVE")
        ?? visibleProjects[0]
        ?? null;
      setProject(activeProject);
      if (!activeProject) { setRequirements([]); setSelected(null); setTotalElements(0); setTotalPages(0); return; }
      setQuery("");
      setStatusFilter("");
      setSortBy("displayKey");
      setDirection("asc");
      setPage(0);
      const result = await bridgeFlowApi.listRequirements(activeProject.id);
      const detail = result.items[0] ? await bridgeFlowApi.getRequirement(result.items[0].id) : null;
      if (requestId !== workspaceRequest.current) return;
      setRequirements(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      setSelected(detail);
    } catch (cause) {
      if (requestId === workspaceRequest.current) setError(cause instanceof Error ? cause.message : "Không thể kết nối backend.");
    } finally { if (requestId === workspaceRequest.current) setLoading(false); }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => {
      void (async () => {
        if (!bridgeFlowApi.hasSession()) {
          setLoading(false);
          setAuthReady(true);
          return;
        }
        try {
          setUser(await bridgeFlowApi.me());
          await load();
        } catch (cause) {
          setError(cause instanceof Error ? cause.message : "Phiên đăng nhập không còn hợp lệ.");
          setLoading(false);
        } finally {
          setAuthReady(true);
        }
      })();
    }, 0);
    return () => window.clearTimeout(initialLoad);
  }, [load]);

  async function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setError(null);
    try {
      const result = await bridgeFlowApi.login({
        email: String(form.get("email")), password: String(form.get("password")),
      });
      setUser(result.user);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể đăng nhập.");
    } finally { setSaving(false); }
  }

  async function logout() {
    try { await bridgeFlowApi.logout(); }
    finally {
      selectionRequest.current += 1;
      workspaceRequest.current += 1;
      setUser(null);
      setProjects([]);
      setProject(null);
      setRequirements([]);
      setSelected(null);
      setError(null);
    }
  }

  async function selectProject(nextProject: Project) {
    setProjectMenuOpen(false);
    await load(nextProject.id);
  }

  async function submitProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setError(null);
    try {
      const result = projectEditor === "create"
        ? await bridgeFlowApi.createProject({
            code: String(form.get("code")),
            name: String(form.get("name")),
            customerName: String(form.get("customerName")),
          })
        : await bridgeFlowApi.updateProject(project!.id, {
            name: String(form.get("name")),
            customerName: String(form.get("customerName")),
            aiEnabled: form.get("aiEnabled") === "on",
          });
      setProjectEditor(null);
      await load(result.id);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể lưu project.");
    } finally { setSaving(false); }
  }

  async function archiveCurrentProject() {
    if (!project || !window.confirm(`Archive project ${project.code}?`)) return;
    setSaving(true);
    setError(null);
    try {
      await bridgeFlowApi.archiveProject(project.id);
      setProjectMenuOpen(false);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể archive project.");
    } finally { setSaving(false); }
  }

  async function deleteCurrentProject() {
    if (!project || project.status !== "ARCHIVED") return;
    const confirmationCode = window.prompt(
      `Xóa vĩnh viễn toàn bộ dữ liệu và file của ${project.code}. Nhập đúng mã project để xác nhận:`,
    );
    if (confirmationCode === null) return;
    setSaving(true);
    setError(null);
    try {
      await bridgeFlowApi.deleteProject(project.id, confirmationCode);
      setProjectMenuOpen(false);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể xóa project.");
    } finally { setSaving(false); }
  }

  async function loadRequirementPage(
    pageIndex: number,
    nextStatus = statusFilter,
    nextQuery = query,
    nextSortBy = sortBy,
    nextDirection = direction,
    preferredId?: string,
  ) {
    if (!project) return;
    const requestId = ++workspaceRequest.current;
    selectionRequest.current += 1;
    setSelecting(false);
    setLoading(true);
    setError(null);
    try {
      const result = await bridgeFlowApi.listRequirements(project.id, {
        status: nextStatus || undefined,
        includeArchived: nextStatus === "ARCHIVED",
        query: nextQuery,
        page: pageIndex,
        size: 10,
        sortBy: nextSortBy,
        direction: nextDirection,
      });
      const target = result.items.find(item => item.id === preferredId) ?? result.items[0];
      const detail = target ? await bridgeFlowApi.getRequirement(target.id) : null;
      if (requestId !== workspaceRequest.current) return;
      setRequirements(result.items);
      setPage(result.page);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      setSelected(detail);
      if (!nextStatus && !nextQuery.trim()) {
        setProject((item) => item?.id === project.id ? { ...item, requirementCount: result.totalElements } : item);
        setProjects((items) => items.map((item) => item.id === project.id ? { ...item, requirementCount: result.totalElements } : item));
      }
    } catch (cause) {
      if (requestId === workspaceRequest.current) setError(cause instanceof Error ? cause.message : "Không thể tải requirements.");
    } finally { if (requestId === workspaceRequest.current) setLoading(false); }
  }

  function openOverviewRequirement(item: Requirement) {
    setQuery(item.displayKey);
    setStatusFilter("");
    setSortBy("displayKey");
    setDirection("asc");
    setActiveView("requirements");
    setDetailTab("analysis");
    window.history.pushState(null, "", "#view=requirements&tab=analysis");
    void loadRequirementPage(0, "", item.displayKey, "displayKey", "asc", item.id);
  }

  async function selectRequirement(item: Requirement) {
    const requestId = ++selectionRequest.current;
    setSelecting(true);
    setError(null);
    try {
      const detail = await bridgeFlowApi.getRequirement(item.id);
      if (requestId === selectionRequest.current) setSelected(detail);
    } catch (cause) {
      if (requestId === selectionRequest.current) setError(cause instanceof Error ? cause.message : "Không thể tải requirement.");
    } finally { if (requestId === selectionRequest.current) setSelecting(false); }
  }

  async function refreshRequirement(requirement: Requirement) {
    setSelected(requirement);
    if (project) {
      const result = await bridgeFlowApi.listRequirements(project.id, {
        status: statusFilter || undefined, query, page, size: 10, sortBy, direction,
        includeArchived: statusFilter === "ARCHIVED",
      });
      setRequirements(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      const visibleProjects = await bridgeFlowApi.listProjects(true);
      setProjects(visibleProjects);
      setProject(visibleProjects.find((item) => item.id === project.id) ?? project);
    }
  }

  async function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadRequirementPage(0);
  }

  async function changeStatus(nextStatus: RequirementStatusFilter) {
    setStatusFilter(nextStatus);
    await loadRequirementPage(0, nextStatus);
  }

  async function changeSort(value: string) {
    const [nextSortBy, nextDirection] = value.split(":") as [typeof sortBy, typeof direction];
    setSortBy(nextSortBy);
    setDirection(nextDirection);
    await loadRequirementPage(0, statusFilter, query, nextSortBy, nextDirection);
  }

  async function archiveRequirement() {
    if (!project || !selected || !window.confirm(`Archive requirement ${selected.displayKey}?`)) return;
    setSaving(true);
    setError(null);
    try {
      await bridgeFlowApi.archiveRequirement(selected.id);
      const activeProjects = await bridgeFlowApi.listProjects();
      setProjects(activeProjects);
      setProject(activeProjects.find((item) => item.id === project.id) ?? project);
      const nextPage = requirements.length === 1 && page > 0 ? page - 1 : page;
      await loadRequirementPage(nextPage);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể archive requirement.");
    } finally { setSaving(false); }
  }

  async function submitEditor(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!project) return;
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setError(null);
    try {
      const japaneseText = String(form.get("japaneseText"));
      const vietnameseText = String(form.get("vietnameseText"));
      const result = editor === "create"
        ? await bridgeFlowApi.createRequirement(project.id, {
            displayKey: String(form.get("displayKey")), japaneseText, vietnameseText,
          })
        : await bridgeFlowApi.addRevision(selected!.id, {
            japaneseText, vietnameseText, changeType: "MODIFIED",
          });
      await refreshRequirement(result);
      setEditor(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể lưu dữ liệu.");
    } finally { setSaving(false); }
  }

  async function confirmLatest() {
    if (!selected?.latestRevision) return;
    setSaving(true);
    setError(null);
    try {
      const result = await bridgeFlowApi.confirmRevision(selected.id, selected.latestRevision.id);
      await refreshRequirement(result);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể xác nhận revision.");
    } finally { setSaving(false); }
  }

  if (!authReady) {
    return <div className="bridgeflow-login grid min-h-screen place-items-center"><LoaderCircle className="size-7 animate-spin text-[#55d6c2]" /></div>;
  }
  if (!user) {
    return <LoginScreen saving={saving} error={error} onSubmit={submitLogin} />;
  }

  const canAdminProject = project?.role === "ADMIN";
  const canEditRequirement = project ? ["ADMIN", "BRSE", "DEVELOPER"].includes(project.role) : false;
  const viewMeta = workspaceMeta[activeView];

  return (
    <main className="bridgeflow-app bf-workspace min-h-screen text-slate-950">
      <header className="bridgeflow-topbar sticky top-0 z-30 flex h-16 items-center border-b px-4 lg:px-6">
        <Link href="/" className="mr-3 text-xs text-slate-300 underline-offset-4 hover:text-white hover:underline" aria-label="Về trang giới thiệu">←</Link>
        <div className="bf-topbar-brand flex items-center gap-3"><div className="bridgeflow-brand-mark grid size-9 place-items-center rounded-xl text-white"><Languages className="size-5" /></div><div><p className="text-base font-bold tracking-tight">BridgeFlow</p><p className="text-[10px] font-semibold tracking-[0.18em] text-slate-400">JP × VN REQUIREMENTS</p></div></div>
        <div className="ml-auto flex items-center gap-2"><Badge variant="outline" className={`bf-api-status hidden md:flex ${error ? "border-amber-200 bg-amber-50 text-amber-800" : "border-emerald-200 bg-emerald-50 text-emerald-800"}`}><span className="size-1.5 rounded-full bg-current" />{loading ? "Đang kết nối…" : error ? "Cần kiểm tra API" : "Workspace sẵn sàng"}</Badge><Button variant="ghost" size="icon-sm" aria-label="Hoạt động AI" onClick={() => navigate("activity")}><Bell /></Button><div className="hidden text-right sm:block"><p className="bf-user-name text-xs font-semibold">{user.displayName}</p><p className="text-[11px] text-slate-400">{user.email}</p></div><div className="ml-1 grid size-8 place-items-center rounded-full border border-white/10 bg-white/10 text-xs font-bold text-[#75e5d1]">{user.displayName.split(/\s+/).map((part) => part[0]).slice(-2).join("").toUpperCase()}</div><Button variant="ghost" size="icon-sm" aria-label="Đăng xuất" onClick={() => { void logout().catch(() => undefined); }}><LogOut /></Button></div>
      </header>

      <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-[1800px]">
        <aside className="bridgeflow-sidebar hidden w-64 shrink-0 border-r p-4 lg:flex lg:flex-col">
          <div className="relative mb-5">
            <button aria-expanded={projectMenuOpen} aria-label="Chọn dự án" onKeyDown={event => { if (event.key === "Escape") setProjectMenuOpen(false); }} onClick={() => setProjectMenuOpen((open) => !open)} className="bridgeflow-project-switcher flex w-full items-center gap-3 rounded-xl border p-3 text-left transition">
              <div className="bridgeflow-project-avatar grid size-9 place-items-center rounded-lg text-xs font-black">{project?.code.slice(0, 2) ?? "PJ"}</div>
              <div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold">{project?.name ?? "Chọn project"}</p><p className="truncate text-xs text-slate-400">{project?.customerName ?? `${projects.length} project đang hoạt động`}</p></div>
              <ChevronDown className={`size-4 text-slate-400 transition ${projectMenuOpen ? "rotate-180" : ""}`} />
            </button>
            {projectMenuOpen && <div className="absolute left-0 right-0 top-[calc(100%+8px)] z-40 overflow-hidden rounded-xl border border-slate-200 bg-white text-slate-800 shadow-xl" onKeyDown={event => { if (event.key === "Escape") setProjectMenuOpen(false); }}>
              <div className="max-h-64 overflow-y-auto p-1.5">{projects.map((item) => <button key={item.id} onClick={() => void selectProject(item)} className={`flex w-full items-center gap-3 rounded-lg p-2.5 text-left ${item.id === project?.id ? "bg-[#edf4fa]" : "hover:bg-slate-50"}`}><div className="grid size-8 shrink-0 place-items-center rounded-lg bg-slate-100 text-[11px] font-bold text-slate-600">{item.code.slice(0, 2)}</div><div className="min-w-0"><p className="truncate text-sm font-medium">{item.name}</p><p className="truncate text-xs text-slate-400">{item.code} · {item.requirementCount} requirements{item.status === "ARCHIVED" ? " · archived" : ""}</p></div></button>)}</div>
              <div className="grid grid-cols-2 gap-1 border-t border-slate-100 p-1.5"><Button variant="ghost" size="sm" onClick={() => { setProjectMenuOpen(false); setProjectEditor("create"); }}><Plus /> Tạo mới</Button><Button variant="ghost" size="sm" disabled={!canAdminProject} onClick={() => { setProjectMenuOpen(false); setProjectEditor("edit"); }}><Pencil /> Chỉnh sửa</Button></div>
              {project && <div className="border-t border-slate-100 p-1.5">{project.status === "ACTIVE" ? <Button variant="ghost" size="sm" disabled={saving || !canAdminProject} onClick={() => void archiveCurrentProject()} className="w-full justify-start text-red-600 hover:bg-red-50 hover:text-red-700"><Archive /> Archive project</Button> : <Button variant="ghost" size="sm" disabled={saving || !canAdminProject} onClick={() => void deleteCurrentProject()} className="w-full justify-start text-red-700 hover:bg-red-50 hover:text-red-800"><Trash2 /> Xóa vĩnh viễn</Button>}</div>}
            </div>}
          </div>
          <p className="bf-sidebar-label">DỰ ÁN</p>
          <nav aria-label="Điều hướng dự án" className="space-y-1">{nav.map(([Icon, view]) => <button key={view} aria-current={activeView === view ? "page" : undefined} onClick={() => navigate(view)} className="bridgeflow-nav-item flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition"><Icon className="size-[18px]" />{workspaceMeta[view].label}{view === "requirements" && <span className="bf-nav-count ml-auto rounded-md px-1.5 py-0.5 text-[11px]">{project?.requirementCount ?? 0}</span>}</button>)}</nav>
          <div className="mt-6 border-t border-slate-200/70 pt-5"><p className="mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.16em] text-slate-500">Không gian làm việc</p>{workspaceNav.map(([Icon, view]) => <button key={view} aria-current={activeView === view ? "page" : undefined} onClick={() => navigate(view)} className="bridgeflow-nav-item flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition"><Icon className="size-[18px]" />{workspaceMeta[view].label}</button>)}</div>
          <div className="bf-sidebar-footer"><Languages size={16} /><span>Nhật–Việt, cùng một cách hiểu</span></div>
        </aside>

        <section className="bridgeflow-content min-w-0 flex-1 p-4 md:p-6 xl:p-8">
          <div className="mb-5 grid gap-2 sm:grid-cols-2 lg:hidden"><select aria-label="Chọn project" value={project?.id ?? ""} onChange={(event) => { const item = projects.find((candidate) => candidate.id === event.target.value); if (item) void selectProject(item); }} className="min-w-0 rounded-lg border border-slate-200 bg-white p-2 text-sm"><option value="" disabled>Chọn project</option>{projects.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select><select aria-label="Chọn workspace" value={activeView} onChange={(event) => navigate(event.target.value as WorkspaceView)} className="rounded-lg border border-slate-200 bg-white p-2 text-sm">{Object.entries(workspaceMeta).map(([view, meta]) => <option key={view} value={view}>{meta.label}</option>)}</select><Button variant="outline" onClick={() => setProjectEditor("create")}><Plus />Tạo project</Button></div>
          <div className="bridgeflow-view-heading mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><div className="bridgeflow-view-kicker mb-2 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.12em]"><FolderKanban className="size-3.5" /> {project?.name ?? "Project"} <span>/</span> {viewMeta.label} {project && <Badge variant="outline">{project.role}</Badge>}</div><h1 className="text-2xl font-extrabold md:text-3xl">{activeView === "overview" && project ? project.name : viewMeta.title}</h1><p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">{viewMeta.description}</p></div>{activeView === "overview" && project && <div className="bf-heading-actions"><Button variant="outline" onClick={() => navigate("documents")}><FileText />Tài liệu</Button><Button disabled={!canEditRequirement || project.status === "ARCHIVED"} onClick={() => setEditor("create")}><Plus />Thêm yêu cầu</Button></div>}{activeView === "requirements" && <div className="flex gap-2"><Button variant="outline" disabled={!selected} onClick={() => navigate("changes")} className="border-slate-200 bg-white"><PanelLeftClose /> Truy vết</Button><Button disabled={!project || !canEditRequirement || project.status === "ARCHIVED"} onClick={() => setEditor("create")} className="bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Thêm yêu cầu</Button></div>}</div>
          {error && <div className="mb-4 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="size-4 shrink-0" /><span className="flex-1">{error}</span><Button variant="ghost" size="sm" onClick={() => void load()}><RefreshCw /> Thử lại</Button></div>}
          {loading ? <div role="status" className="grid min-h-96 place-items-center rounded-2xl border border-slate-200 bg-white"><div className="text-center text-sm text-slate-500"><LoaderCircle className="mx-auto mb-3 size-6 animate-spin text-[#2878ad]" />Đang tải workspace…</div></div> : !project ? <EmptyState title="Chưa có project" description="Tạo project đầu tiên để bắt đầu quản lý requirement." action={<Button onClick={() => setProjectEditor("create")} className="mt-4 bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Tạo project</Button>} /> : activeView === "overview" ? <ProjectOverview key={project.id} project={project} onNavigate={navigate} onOpenRequirement={openOverviewRequirement} /> : activeView === "members" ? <ProjectMembers key={project.id} project={project} user={user} /> : activeView === "settings" ? <ProjectSettings project={project} user={user} onEdit={() => setProjectEditor("edit")} /> : activeView === "activity" ? <ProjectAiActivity key={project.id} project={project} /> : activeView === "documents" ? <DocumentWorkspace key={project.id} project={project} /> : activeView === "glossary" ? <GlossaryWorkspace key={project.id} project={project} /> : (
            <div className="grid gap-4 xl:grid-cols-[minmax(350px,0.92fr)_minmax(520px,1.45fr)]">
              <section className="bridgeflow-panel overflow-hidden rounded-2xl border">
                <div className="space-y-2 border-b border-slate-200 p-3">
                  <form onSubmit={submitSearch} className="flex items-center gap-2"><div className="relative min-w-0 flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} aria-label="Tìm yêu cầu" placeholder="Tìm mã hoặc nội dung song ngữ..." className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm outline-none focus:border-[#2878ad]" /></div><Button type="submit" variant="outline" size="sm">Tìm</Button></form>
                  <div className="grid grid-cols-2 gap-2"><select aria-label="Lọc trạng thái" value={statusFilter} onChange={(event) => void changeStatus(event.target.value as RequirementStatusFilter)} className="h-9 rounded-lg border border-slate-200 bg-white px-2 text-xs outline-none"><option value="">Mọi trạng thái</option><option value="DRAFT">Bản nháp</option><option value="REVIEWING">Cần xác nhận</option><option value="CONFIRMED">Đã xác nhận</option><option value="REJECTED">Bị từ chối</option><option value="ARCHIVED">Đã archive</option></select><select aria-label="Sắp xếp yêu cầu" value={`${sortBy}:${direction}`} onChange={(event) => void changeSort(event.target.value)} className="h-9 rounded-lg border border-slate-200 bg-white px-2 text-xs outline-none"><option value="displayKey:asc">Mã A → Z</option><option value="displayKey:desc">Mã Z → A</option><option value="updatedAt:desc">Mới cập nhật</option><option value="updatedAt:asc">Cũ cập nhật</option><option value="status:asc">Theo trạng thái</option></select></div>
                  <p className="px-1 text-[11px] text-slate-400">{totalElements} requirement · Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</p>
                </div>
                <div className="divide-y divide-slate-100">{requirements.length === 0 ? <p className="p-8 text-center text-sm text-slate-500">Không tìm thấy requirement.</p> : requirements.map((item) => <button key={item.id} onClick={() => void selectRequirement(item)} className={`w-full border-l-[3px] p-4 pl-[13px] text-left transition hover:bg-slate-50 ${selected?.id === item.id ? "border-[#2878ad] bg-[#f2f7fb]" : "border-transparent"}`}><div className="mb-2 flex items-center justify-between gap-2"><span className="font-mono text-xs font-semibold text-[#2878ad]">{item.displayKey}</span><StatusBadge status={item.status} /></div><p lang="ja" className="line-clamp-1 text-sm font-semibold text-slate-900">{excerpt(item.latestRevision?.japaneseText)}</p><p className="mt-1 line-clamp-1 text-sm text-slate-500">{excerpt(item.latestRevision?.vietnameseText)}</p><p className="mt-2 text-xs text-slate-400">Revision {item.latestRevision?.revisionNumber ?? 0}</p></button>)}</div>
                {totalPages > 1 && <div className="flex items-center justify-between border-t border-slate-200 p-3"><Button variant="outline" size="sm" disabled={page === 0} onClick={() => void loadRequirementPage(page - 1)}>Trang trước</Button><span className="text-xs text-slate-500">{page + 1} / {totalPages}</span><Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => void loadRequirementPage(page + 1)}>Trang sau</Button></div>}
              </section>
              {selecting ? <div role="status" className="grid min-h-48 place-items-center rounded-2xl border border-slate-200 bg-white"><LoaderCircle className="size-6 animate-spin" /></div> : selected ? <RequirementDetail key={selected.id} requirement={selected} project={project} saving={saving} tab={detailTab} onTabChange={(tab) => navigate(activeView, tab)} onEdit={() => setEditor("revision")} onConfirm={() => void confirmLatest()} onArchive={() => void archiveRequirement()} /> : <EmptyState title="Chưa có requirement" description="Thêm requirement hoặc trích xuất từ tài liệu để sử dụng Q&A, test cases và truy vết." action={<Button variant="outline" className="mt-4" onClick={() => navigate("documents")}>Mở tài liệu</Button>} />}
            </div>
          )}
        </section>
      </div>
      {editor && <Editor mode={editor} requirement={selected} saving={saving} onClose={() => setEditor(null)} onSubmit={submitEditor} />}
      {projectEditor && <ProjectEditor mode={projectEditor} project={project} saving={saving} onClose={() => setProjectEditor(null)} onSubmit={submitProject} />}
    </main>
  );
}

function RequirementDetail({ requirement, project, saving, tab, onTabChange, onEdit, onConfirm, onArchive }: {
  requirement: Requirement; project: Project; saving: boolean; tab: RequirementTab;
  onTabChange: (tab: RequirementTab) => void; onEdit: () => void; onConfirm: () => void; onArchive: () => void;
}) {
  const revision = requirement.latestRevision;
  const active = project.status === "ACTIVE" && requirement.status !== "ARCHIVED";
  const canEdit = active && ["ADMIN", "BRSE", "DEVELOPER"].includes(project.role);
  const canReview = active && ["ADMIN", "BRSE"].includes(project.role);
  const canConfirm = canReview && revision && revision.reviewStatus !== "CONFIRMED";
  return <section className="bridgeflow-panel min-w-0 overflow-hidden rounded-2xl border">
    <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-5 py-4">
      <span className="font-mono text-sm font-bold text-[#2878ad]">{requirement.displayKey}</span>
      <StatusBadge status={requirement.status} />
      {revision?.documentVersionId && <Badge variant="outline" className="border-violet-200 bg-violet-50 text-violet-700"><Sparkles />Trích xuất từ tài liệu</Badge>}
      <span className="text-xs text-slate-400">Stable ID · {requirement.id.slice(0, 8)}</span>
      {canEdit && <Button variant="ghost" size="icon-sm" aria-label="Tạo revision mới" onClick={onEdit} className="ml-auto"><Pencil /></Button>}
    </div>
    <Tabs value={tab} onValueChange={(value) => onTabChange(value as RequirementTab)} className="gap-0">
      <div className="overflow-x-auto"><TabsList variant="line" className="h-12 min-w-max w-full justify-start gap-5 border-b border-slate-200 px-5">
        <TabsTrigger value="analysis" className="px-0">Phân tích song ngữ</TabsTrigger>
        <TabsTrigger value="artifacts" className="px-0">Q&amp;A &amp; tiêu chí</TabsTrigger>
        <TabsTrigger value="test-cases" className="px-0">Kiểm thử</TabsTrigger>
        <TabsTrigger value="traceability" className="px-0">Truy vết</TabsTrigger>
        <TabsTrigger value="history" className="px-0">Lịch sử ({requirement.revisions.length})</TabsTrigger>
      </TabsList></div>
      <TabsContent value="analysis" className="p-5 md:p-6">
        <div className="grid gap-4 md:grid-cols-2">
          <LanguageCard label="原文 · Tiếng Nhật" lang="ja" text={revision?.japaneseText} />
          <LanguageCard label="Bản dịch · Tiếng Việt" lang="vi" text={revision?.vietnameseText} translated />
        </div>
        <div className="mt-5 rounded-xl border border-blue-200 bg-blue-50/70 p-4 text-sm text-blue-950">
          <p className="font-semibold">Revision {revision?.revisionNumber ?? 0} · {revision?.changeType ?? "—"}</p>
          <p className="mt-1 text-blue-800">{revision?.sourceAnchor ? `Nguồn tài liệu: ${revision.sourceAnchor}. Nội dung phải được BrSE/Admin kiểm tra trước khi xác nhận.` : "Requirement ID được giữ ổn định; mỗi lần sửa tạo một revision mới để truy vết thay đổi."}</p>
        </div>
        {(canEdit || canReview) && <div className="mt-5 flex flex-wrap gap-2 border-t border-slate-100 pt-5">
          {canEdit && <Button variant="outline" onClick={onEdit}>Tạo revision mới</Button>}
          {canConfirm && <Button disabled={saving} onClick={onConfirm} className="bg-[#2878ad] hover:bg-[#226994]">{saving ? <LoaderCircle className="animate-spin" /> : <Check />}BrSE xác nhận revision</Button>}
          {canReview && <Button variant="ghost" disabled={saving} onClick={onArchive} className="ml-auto text-red-600 hover:bg-red-50 hover:text-red-700"><Archive />Archive</Button>}
        </div>}
      </TabsContent>
      <TabsContent value="artifacts" className="p-5 md:p-6">{revision && <RequirementAnalysisWorkspace key={revision.id} project={project} requirement={requirement} revision={revision} />}</TabsContent>
      <TabsContent value="test-cases" className="p-5 md:p-6">{revision && <RequirementTestCases key={revision.id} project={project} requirement={requirement} revision={revision} />}</TabsContent>
      <TabsContent value="traceability" className="p-5 md:p-6">{revision && <RequirementTraceabilityWorkspace key={revision.id} requirement={requirement} revision={revision} />}</TabsContent>
      <TabsContent value="history" className="p-6">
        <ol className="space-y-3">{[...requirement.revisions].reverse().map((item) => <li key={item.id} className="flex gap-3 rounded-xl border border-slate-200 p-4">
          <div className="grid size-8 shrink-0 place-items-center rounded-full bg-slate-100 text-xs font-bold">v{item.revisionNumber}</div>
          <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><p className="text-sm font-semibold">{item.changeType}</p><StatusBadge status={item.reviewStatus} />{item.documentVersionId && <Badge variant="outline" className="border-violet-200 text-violet-700">Tài liệu</Badge>}</div>
            <p lang="ja" className="mt-1 break-words text-sm text-slate-600">{item.japaneseText}</p>
            <p className="mt-1 text-xs text-slate-400">{item.sourceAnchor ? `${item.sourceAnchor} · ` : ""}{new Date(item.createdAt).toLocaleString("vi-VN")}</p>
          </div>
        </li>)}</ol>
      </TabsContent>
    </Tabs>
  </section>;
}

function LoginScreen({ saving, error, onSubmit }: { saving: boolean; error: string | null; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <main className="bridgeflow-login min-h-screen p-4 md:p-8">
    <Link href="/" className="relative z-10 inline-flex rounded-lg px-2 py-1 text-sm text-slate-300 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-emerald-300">← Về trang giới thiệu</Link>
    <div className="bridgeflow-login-orb" aria-hidden="true" />
    <div className="relative mx-auto grid min-h-[calc(100vh-2rem)] w-full max-w-6xl items-center gap-12 lg:grid-cols-[1.15fr_0.85fr]">
      <section className="hidden max-w-xl text-white lg:block">
        <div className="mb-8 flex items-center gap-3"><div className="bridgeflow-brand-mark grid size-11 place-items-center rounded-2xl text-white"><Languages className="size-6" /></div><div><h1 className="text-xl font-bold tracking-tight">BridgeFlow</h1><p className="text-[10px] font-semibold tracking-[0.2em] text-slate-400">JP × VN REQUIREMENTS</p></div></div>
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-[#72dfcc]">From ambiguity to alignment</p>
        <h2 className="mt-5 text-5xl font-extrabold leading-[1.05] tracking-[-0.055em]">Biến yêu cầu song ngữ thành một luồng làm việc rõ ràng.</h2>
        <p className="mt-6 max-w-lg text-base leading-7 text-slate-300">Một workspace chung để nhóm Nhật–Việt quản lý tài liệu, chuẩn hóa thuật ngữ, review yêu cầu và giữ mọi quyết định có thể truy vết.</p>
        <div className="mt-9 grid grid-cols-3 gap-3 text-xs">
          {["01 · Tài liệu", "02 · Human review", "03 · Traceability"].map((step) => <div key={step} className="rounded-xl border border-white/10 bg-white/[0.055] px-3 py-3 text-slate-300 backdrop-blur-sm">{step}</div>)}
        </div>
      </section>
      <div className="bridgeflow-login-card mx-auto w-full max-w-md rounded-[1.75rem] p-7 md:p-9">
        <div className="mb-7 flex items-center gap-3 lg:hidden"><div className="bridgeflow-brand-mark grid size-11 place-items-center rounded-xl text-white"><Languages className="size-6" /></div><div><h1 className="text-xl font-bold tracking-tight">BridgeFlow</h1><p className="text-[10px] font-semibold tracking-[0.18em] text-slate-500">JP × VN REQUIREMENTS</p></div></div>
        <Badge variant="outline" className="mb-5 border-[#b8e8df] bg-[#eaf8f5] text-[#176a70]"><Sparkles /> AI hỗ trợ · Con người phê duyệt</Badge>
        <div><h2 className="text-2xl font-extrabold tracking-[-0.035em] text-[#0b1620]">Chào mừng trở lại</h2><p className="mt-2 text-sm leading-6 text-slate-500">Đăng nhập để tiếp tục làm việc trong project được phân quyền.</p></div>
        {error && <div className="mt-5 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mt-0.5 size-4 shrink-0" />{error}</div>}
        <form onSubmit={onSubmit} className="mt-7 space-y-4">
          <label className="block text-sm font-semibold text-slate-700">Email<input name="email" type="email" required autoComplete="username" placeholder="Email tài khoản của bạn" className="mt-1.5 h-12 w-full rounded-xl border border-slate-200 bg-slate-50/80 px-3 font-normal outline-none transition focus:border-[#2878ad] focus:bg-white" /></label>
          <label className="block text-sm font-semibold text-slate-700">Mật khẩu<input name="password" type="password" required autoComplete="current-password" className="mt-1.5 h-12 w-full rounded-xl border border-slate-200 bg-slate-50/80 px-3 font-normal outline-none transition focus:border-[#2878ad] focus:bg-white" /></label>
          <Button disabled={saving} className="mt-2 h-12 w-full rounded-xl bg-[#0e2940] hover:bg-[#123852]">{saving && <LoaderCircle className="animate-spin" />} Đăng nhập workspace</Button>
        </form>
        <div className="mt-6 flex items-center justify-center gap-2 text-xs text-slate-400"><Check className="size-3.5 text-emerald-600" />Dữ liệu được giới hạn theo quyền project</div>
      </div>
    </div>
  </main>;
}

function LanguageCard({ label, lang, text, translated = false }: { label: string; lang: string; text?: string; translated?: boolean }) {
  return <article className={`rounded-xl border p-4 ${translated ? "border-[#dbe7f1] bg-[#f4f8fb]" : "border-slate-200 bg-slate-50/70"}`}><span className={`text-xs font-bold uppercase tracking-wider ${translated ? "text-[#436987]" : "text-slate-500"}`}>{label}</span><p lang={lang} className="mt-3 text-[15px] font-medium leading-7 text-slate-800">{text ?? "Chưa có nội dung"}</p></article>;
}

function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="bridgeflow-panel grid min-h-80 place-items-center rounded-2xl border border-dashed p-8 text-center"><div><div className="mx-auto mb-4 grid size-12 place-items-center rounded-2xl bg-[#e8f4f3] text-[#176a86]"><BookOpenText className="size-6" /></div><h2 className="font-semibold">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p>{action}</div></div>;
}

function ProjectEditor({ mode, project, saving, onClose, onSubmit }: { mode: "create" | "edit"; project: Project | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><h2 className="text-lg font-bold">{mode === "create" ? "Tạo project" : "Chỉnh sửa project"}</h2><p className="mt-1 text-sm text-slate-500">Workspace riêng cho requirement và lịch sử revision.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4">{mode === "create" && <label className="block text-sm font-medium">Mã project<input name="code" required maxLength={40} placeholder="EC-RENEWAL" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 font-mono uppercase outline-none focus:border-[#2878ad]" /></label>}<label className="block text-sm font-medium">Tên project<input name="name" required maxLength={160} defaultValue={mode === "edit" ? project?.name : ""} placeholder="EC Portal Renewal" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Khách hàng<input name="customerName" maxLength={160} defaultValue={mode === "edit" ? project?.customerName ?? "" : ""} placeholder="株式会社みらい" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label>{mode === "edit" && project?.role === "ADMIN" && <label className="flex items-start gap-3 rounded-xl border border-violet-200 bg-violet-50 p-3 text-sm"><input name="aiEnabled" type="checkbox" defaultChecked={project.aiEnabled} className="mt-0.5 size-4 rounded border-violet-300" /><span><strong className="block text-violet-900">Cho phép xử lý bằng AI</strong><span className="mt-0.5 block text-xs leading-5 text-violet-700">Nội dung cần thiết của tài liệu có thể được gửi tới provider đã cấu hình và luôn tạo bản nháp để người phụ trách review.</span></span></label>}</div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} {mode === "create" ? "Tạo project" : "Lưu thay đổi"}</Button></div></form></div>;
}

function Editor({ mode, requirement, saving, onClose, onSubmit }: { mode: "create" | "revision"; requirement: Requirement | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><h2 className="text-lg font-bold">{mode === "create" ? "Thêm requirement" : `Tạo revision mới · ${requirement?.displayKey}`}</h2><p className="mt-1 text-sm text-slate-500">Nhập nội dung song ngữ để nhóm Nhật–Việt cùng review.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4">{mode === "create" && <label className="block text-sm font-medium">Mã requirement<input name="displayKey" required maxLength={40} placeholder="REQ-018" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 font-mono outline-none focus:border-[#2878ad]" /></label>}<label className="block text-sm font-medium">Nội dung tiếng Nhật<textarea name="japaneseText" required defaultValue={mode === "revision" ? requirement?.latestRevision?.japaneseText : ""} rows={4} lang="ja" className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Bản dịch tiếng Việt<textarea name="vietnameseText" required defaultValue={mode === "revision" ? requirement?.latestRevision?.vietnameseText : ""} rows={4} className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label></div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} Lưu {mode === "create" ? "requirement" : "revision"}</Button></div></form></div>;
}
