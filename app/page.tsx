"use client";

import { FormEvent, ReactNode, useCallback, useEffect, useState } from "react";
import {
  AlertCircle, Archive, Bell, BookOpenText, Check, ChevronDown, FileText,
  FolderKanban, GitCompareArrows, Languages, LayoutDashboard, LoaderCircle,
  LogOut,
  MessageSquareText, MoreHorizontal, PanelLeftClose, Plus, RefreshCw, Search,
  Pencil, Settings, TestTube2, Users, X,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AuthUser, bridgeFlowApi, Project, Requirement } from "@/lib/bridgeflow-api";
const nav = [
  [LayoutDashboard, "Tổng quan"], [FileText, "Tài liệu"],
  [BookOpenText, "Requirements"], [MessageSquareText, "Q&A"],
  [TestTube2, "Test cases"], [GitCompareArrows, "Thay đổi"],
] as const;
const statusMeta: Record<string, { label: string; colors: string }> = {
  DRAFT: { label: "Bản nháp", colors: "border-slate-200 bg-slate-50 text-slate-600" },
  REVIEWING: { label: "Cần xác nhận", colors: "border-amber-200 bg-amber-50 text-amber-700" },
  CONFIRMED: { label: "Đã xác nhận", colors: "border-emerald-200 bg-emerald-50 text-emerald-700" },
  REJECTED: { label: "Bị từ chối", colors: "border-red-200 bg-red-50 text-red-700" },
  ARCHIVED: { label: "Đã archive", colors: "border-slate-300 bg-slate-100 text-slate-500" },
};

function StatusBadge({ status }: { status: string }) {
  const meta = statusMeta[status] ?? statusMeta.DRAFT;
  return <Badge variant="outline" className={`font-medium ${meta.colors}`}><span className="size-1.5 rounded-full bg-current" />{meta.label}</Badge>;
}

function excerpt(text?: string) {
  if (!text) return "Chưa có nội dung";
  const sentence = text.split(/[。.!?]/)[0];
  return sentence.length > 58 ? `${sentence.slice(0, 58)}…` : sentence;
}

export default function Home() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [projects, setProjects] = useState<Project[]>([]);
  const [project, setProject] = useState<Project | null>(null);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [selected, setSelected] = useState<Requirement | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [sortBy, setSortBy] = useState<"displayKey" | "status" | "updatedAt">("displayKey");
  const [direction, setDirection] = useState<"asc" | "desc">("asc");
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [editor, setEditor] = useState<"create" | "revision" | null>(null);
  const [projectEditor, setProjectEditor] = useState<"create" | "edit" | null>(null);
  const [projectMenuOpen, setProjectMenuOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async (preferredProjectId?: string) => {
    setLoading(true);
    setError(null);
    try {
      const activeProjects = await bridgeFlowApi.listProjects();
      setProjects(activeProjects);
      const activeProject = activeProjects.find((item) => item.id === preferredProjectId)
        ?? activeProjects[0]
        ?? null;
      setProject(activeProject);
      if (!activeProject) { setRequirements([]); setSelected(null); return; }
      setQuery("");
      setStatusFilter("");
      setSortBy("displayKey");
      setDirection("asc");
      setPage(0);
      const result = await bridgeFlowApi.listRequirements(activeProject.id);
      setRequirements(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      setSelected(result.items[0] ? await bridgeFlowApi.getRequirement(result.items[0].id) : null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể kết nối backend.");
    } finally { setLoading(false); }
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
    await bridgeFlowApi.logout();
    setUser(null);
    setProjects([]);
    setProject(null);
    setRequirements([]);
    setSelected(null);
    setError(null);
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

  async function loadRequirementPage(
    pageIndex: number,
    nextStatus = statusFilter,
    nextQuery = query,
    nextSortBy = sortBy,
    nextDirection = direction,
  ) {
    if (!project) return;
    setLoading(true);
    setError(null);
    try {
      const result = await bridgeFlowApi.listRequirements(project.id, {
        status: nextStatus || undefined,
        query: nextQuery,
        page: pageIndex,
        size: 10,
        sortBy: nextSortBy,
        direction: nextDirection,
      });
      setRequirements(result.items);
      setPage(result.page);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
      setSelected(result.items[0] ? await bridgeFlowApi.getRequirement(result.items[0].id) : null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải requirements.");
    } finally { setLoading(false); }
  }

  async function selectRequirement(item: Requirement) {
    setError(null);
    try { setSelected(await bridgeFlowApi.getRequirement(item.id)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể tải requirement."); }
  }

  async function refreshRequirement(requirement: Requirement) {
    setSelected(requirement);
    if (project) {
      const result = await bridgeFlowApi.listRequirements(project.id, {
        status: statusFilter || undefined, query, page, size: 10, sortBy, direction,
      });
      setRequirements(result.items);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
    }
  }

  async function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadRequirementPage(0);
  }

  async function changeStatus(nextStatus: string) {
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
    return <div className="grid min-h-screen place-items-center bg-[#f4f6f8]"><LoaderCircle className="size-7 animate-spin text-[#2878ad]" /></div>;
  }
  if (!user) {
    return <LoginScreen saving={saving} error={error} onSubmit={submitLogin} />;
  }

  const canAdminProject = project?.role === "ADMIN";
  const canEditRequirement = project ? ["ADMIN", "BRSE", "DEVELOPER"].includes(project.role) : false;

  return (
    <main className="min-h-screen bg-[#f4f6f8] text-slate-950">
      <header className="sticky top-0 z-30 flex h-16 items-center border-b border-slate-200 bg-white px-4 lg:px-6">
        <div className="flex w-64 items-center gap-3"><div className="grid size-9 place-items-center rounded-xl bg-[#123a63] text-white shadow-sm"><Languages className="size-5" /></div><div><p className="text-base font-bold tracking-tight">BridgeFlow</p><p className="text-[11px] font-medium tracking-wide text-slate-500">JP × VN REQUIREMENTS</p></div></div>
        <div className="ml-auto flex items-center gap-2"><Badge variant="outline" className="hidden border-emerald-200 bg-emerald-50 text-emerald-700 sm:flex"><span className="size-1.5 rounded-full bg-emerald-500" />LIVE API</Badge><Button variant="ghost" size="icon-sm" aria-label="Thông báo"><Bell /></Button><div className="hidden text-right sm:block"><p className="text-xs font-semibold">{user.displayName}</p><p className="text-[11px] text-slate-400">{user.email}</p></div><div className="ml-1 grid size-8 place-items-center rounded-full bg-[#e8eef5] text-xs font-bold text-[#123a63]">{user.displayName.split(/\s+/).map((part) => part[0]).slice(-2).join("").toUpperCase()}</div><Button variant="ghost" size="icon-sm" aria-label="Đăng xuất" onClick={() => void logout()}><LogOut /></Button></div>
      </header>

      <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-[1800px]">
        <aside className="hidden w-64 shrink-0 border-r border-slate-200 bg-[#f8fafc] p-4 lg:flex lg:flex-col">
          <div className="relative mb-5">
            <button onClick={() => setProjectMenuOpen((open) => !open)} className="flex w-full items-center gap-3 rounded-xl border border-slate-200 bg-white p-3 text-left shadow-sm transition hover:border-slate-300">
              <div className="grid size-9 place-items-center rounded-lg bg-[#e8f0f7] text-xs font-bold text-[#123a63]">{project?.code.slice(0, 2) ?? "PJ"}</div>
              <div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold">{project?.name ?? "Chọn project"}</p><p className="truncate text-xs text-slate-500">{project?.customerName ?? `${projects.length} project đang hoạt động`}</p></div>
              <ChevronDown className={`size-4 text-slate-400 transition ${projectMenuOpen ? "rotate-180" : ""}`} />
            </button>
            {projectMenuOpen && <div className="absolute left-0 right-0 top-[calc(100%+8px)] z-40 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl">
              <div className="max-h-64 overflow-y-auto p-1.5">{projects.map((item) => <button key={item.id} onClick={() => void selectProject(item)} className={`flex w-full items-center gap-3 rounded-lg p-2.5 text-left ${item.id === project?.id ? "bg-[#edf4fa]" : "hover:bg-slate-50"}`}><div className="grid size-8 shrink-0 place-items-center rounded-lg bg-slate-100 text-[11px] font-bold text-slate-600">{item.code.slice(0, 2)}</div><div className="min-w-0"><p className="truncate text-sm font-medium">{item.name}</p><p className="truncate text-xs text-slate-400">{item.code} · {item.requirementCount} requirements</p></div></button>)}</div>
              <div className="grid grid-cols-2 gap-1 border-t border-slate-100 p-1.5"><Button variant="ghost" size="sm" onClick={() => { setProjectMenuOpen(false); setProjectEditor("create"); }}><Plus /> Tạo mới</Button><Button variant="ghost" size="sm" disabled={!canAdminProject} onClick={() => { setProjectMenuOpen(false); setProjectEditor("edit"); }}><Pencil /> Chỉnh sửa</Button></div>
              {project && <div className="border-t border-slate-100 p-1.5"><Button variant="ghost" size="sm" disabled={saving || !canAdminProject} onClick={() => void archiveCurrentProject()} className="w-full justify-start text-red-600 hover:bg-red-50 hover:text-red-700"><Archive /> Archive project</Button></div>}
            </div>}
          </div>
          <nav className="space-y-1">{nav.map(([Icon, label]) => <button key={label} className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${label === "Requirements" ? "bg-[#e7eff7] text-[#123a63]" : "text-slate-600 hover:bg-white"}`}><Icon className="size-[18px]" />{label}{label === "Requirements" && <span className="ml-auto rounded-md bg-white/80 px-1.5 py-0.5 text-[11px] text-slate-500">{requirements.length}</span>}</button>)}</nav>
          <div className="mt-6 border-t border-slate-200 pt-5"><p className="mb-2 px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">Không gian làm việc</p><button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600"><Users className="size-[18px]" /> Thành viên</button><button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600"><Settings className="size-[18px]" /> Cài đặt</button></div>
        </aside>

        <section className="min-w-0 flex-1 p-4 md:p-6 xl:p-8">
          <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><div className="mb-2 flex items-center gap-2 text-xs font-medium text-slate-500"><FolderKanban className="size-3.5" /> {project?.name ?? "Project"} <span>/</span> Requirements {project && <Badge variant="outline">{project.role}</Badge>}</div><h1 className="text-2xl font-bold tracking-tight md:text-3xl">Phân tích yêu cầu</h1><p className="mt-1 text-sm text-slate-500">Dữ liệu song ngữ được đọc trực tiếp từ PostgreSQL qua Spring Boot API.</p></div><div className="flex gap-2"><Button variant="outline" className="border-slate-200 bg-white"><PanelLeftClose /> Traceability</Button><Button disabled={!project || !canEditRequirement} onClick={() => setEditor("create")} className="bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Thêm yêu cầu</Button></div></div>
          {error && <div className="mb-4 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="size-4 shrink-0" /><span className="flex-1">{error}</span><Button variant="ghost" size="sm" onClick={() => void load()}><RefreshCw /> Thử lại</Button></div>}
          {loading ? <div className="grid min-h-96 place-items-center rounded-2xl border border-slate-200 bg-white"><div className="text-center text-sm text-slate-500"><LoaderCircle className="mx-auto mb-3 size-6 animate-spin text-[#2878ad]" />Đang tải workspace…</div></div> : !project ? <EmptyState title="Chưa có project" description="Tạo project đầu tiên để bắt đầu quản lý requirement." action={<Button onClick={() => setProjectEditor("create")} className="mt-4 bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Tạo project</Button>} /> : (
            <div className="grid gap-4 xl:grid-cols-[minmax(350px,0.92fr)_minmax(520px,1.45fr)]">
              <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="space-y-2 border-b border-slate-200 p-3">
                  <form onSubmit={submitSearch} className="flex items-center gap-2"><div className="relative min-w-0 flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm mã hoặc nội dung song ngữ..." className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm outline-none focus:border-[#2878ad]" /></div><Button type="submit" variant="outline" size="sm">Tìm</Button></form>
                  <div className="grid grid-cols-2 gap-2"><select value={statusFilter} onChange={(event) => void changeStatus(event.target.value)} className="h-9 rounded-lg border border-slate-200 bg-white px-2 text-xs outline-none"><option value="">Mọi trạng thái</option><option value="DRAFT">Bản nháp</option><option value="REVIEWING">Cần xác nhận</option><option value="CONFIRMED">Đã xác nhận</option><option value="REJECTED">Bị từ chối</option></select><select value={`${sortBy}:${direction}`} onChange={(event) => void changeSort(event.target.value)} className="h-9 rounded-lg border border-slate-200 bg-white px-2 text-xs outline-none"><option value="displayKey:asc">Mã A → Z</option><option value="displayKey:desc">Mã Z → A</option><option value="updatedAt:desc">Mới cập nhật</option><option value="updatedAt:asc">Cũ cập nhật</option><option value="status:asc">Theo trạng thái</option></select></div>
                  <p className="px-1 text-[11px] text-slate-400">{totalElements} requirement · Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</p>
                </div>
                <div className="divide-y divide-slate-100">{requirements.length === 0 ? <p className="p-8 text-center text-sm text-slate-500">Không tìm thấy requirement.</p> : requirements.map((item) => <button key={item.id} onClick={() => void selectRequirement(item)} className={`w-full border-l-[3px] p-4 pl-[13px] text-left transition hover:bg-slate-50 ${selected?.id === item.id ? "border-[#2878ad] bg-[#f2f7fb]" : "border-transparent"}`}><div className="mb-2 flex items-center justify-between gap-2"><span className="font-mono text-xs font-semibold text-[#2878ad]">{item.displayKey}</span><StatusBadge status={item.status} /></div><p lang="ja" className="line-clamp-1 text-sm font-semibold text-slate-900">{excerpt(item.latestRevision?.japaneseText)}</p><p className="mt-1 line-clamp-1 text-sm text-slate-500">{excerpt(item.latestRevision?.vietnameseText)}</p><p className="mt-2 text-xs text-slate-400">Revision {item.latestRevision?.revisionNumber ?? 0}</p></button>)}</div>
                {totalPages > 1 && <div className="flex items-center justify-between border-t border-slate-200 p-3"><Button variant="outline" size="sm" disabled={page === 0} onClick={() => void loadRequirementPage(page - 1)}>Trang trước</Button><span className="text-xs text-slate-500">{page + 1} / {totalPages}</span><Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => void loadRequirementPage(page + 1)}>Trang sau</Button></div>}
              </section>
              {selected ? <RequirementDetail requirement={selected} projectRole={project.role} saving={saving} onEdit={() => setEditor("revision")} onConfirm={() => void confirmLatest()} onArchive={() => void archiveRequirement()} /> : <EmptyState title="Chưa có requirement" description="Thêm requirement đầu tiên cho project này." />}
            </div>
          )}
        </section>
      </div>
      {editor && <Editor mode={editor} requirement={selected} saving={saving} onClose={() => setEditor(null)} onSubmit={submitEditor} />}
      {projectEditor && <ProjectEditor mode={projectEditor} project={project} saving={saving} onClose={() => setProjectEditor(null)} onSubmit={submitProject} />}
    </main>
  );
}

function RequirementDetail({ requirement, projectRole, saving, onEdit, onConfirm, onArchive }: { requirement: Requirement; projectRole: string; saving: boolean; onEdit: () => void; onConfirm: () => void; onArchive: () => void }) {
  const revision = requirement.latestRevision;
  const canEdit = ["ADMIN", "BRSE", "DEVELOPER"].includes(projectRole);
  const canReview = ["ADMIN", "BRSE"].includes(projectRole);
  const canConfirm = canReview && revision && revision.reviewStatus !== "CONFIRMED";
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4"><span className="font-mono text-sm font-bold text-[#2878ad]">{requirement.displayKey}</span><StatusBadge status={requirement.status} /><span className="text-xs text-slate-400">Stable ID · {requirement.id.slice(0, 8)}</span><Button variant="ghost" size="icon-sm" className="ml-auto"><MoreHorizontal /></Button></div><Tabs defaultValue="analysis" className="gap-0"><TabsList variant="line" className="h-12 w-full justify-start gap-5 border-b border-slate-200 px-5"><TabsTrigger value="analysis" className="px-0">Phân tích song ngữ</TabsTrigger><TabsTrigger value="history" className="px-0">Lịch sử ({requirement.revisions.length})</TabsTrigger></TabsList><TabsContent value="analysis" className="p-5 md:p-6"><div className="grid gap-4 md:grid-cols-2"><LanguageCard label="原文 · Tiếng Nhật" lang="ja" text={revision?.japaneseText} /><LanguageCard label="Bản dịch · Tiếng Việt" lang="vi" text={revision?.vietnameseText} translated /></div><div className="mt-5 rounded-xl border border-blue-200 bg-blue-50/70 p-4 text-sm text-blue-950"><p className="font-semibold">Revision {revision?.revisionNumber ?? 0} · {revision?.changeType ?? "—"}</p><p className="mt-1 text-blue-800">Requirement ID được giữ ổn định; mỗi lần sửa tạo một revision mới để truy vết thay đổi.</p></div>{(canEdit || canReview) && <div className="mt-5 flex flex-wrap gap-2 border-t border-slate-100 pt-5">{canEdit && <Button variant="outline" onClick={onEdit}>Tạo revision mới</Button>}{canConfirm && <Button disabled={saving} onClick={onConfirm} className="bg-[#2878ad] hover:bg-[#226994]">{saving ? <LoaderCircle className="animate-spin" /> : <Check />} BrSE xác nhận revision</Button>}{canReview && <Button variant="ghost" disabled={saving} onClick={onArchive} className="ml-auto text-red-600 hover:bg-red-50 hover:text-red-700"><Archive /> Archive</Button>}</div>}</TabsContent><TabsContent value="history" className="p-6"><ol className="space-y-3">{[...requirement.revisions].reverse().map((item) => <li key={item.id} className="flex gap-3 rounded-xl border border-slate-200 p-4"><div className="grid size-8 shrink-0 place-items-center rounded-full bg-slate-100 text-xs font-bold">v{item.revisionNumber}</div><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><p className="text-sm font-semibold">{item.changeType}</p><StatusBadge status={item.reviewStatus} /></div><p lang="ja" className="mt-1 truncate text-sm text-slate-600">{item.japaneseText}</p><p className="mt-1 text-xs text-slate-400">{new Date(item.createdAt).toLocaleString("vi-VN")}</p></div></li>)}</ol></TabsContent></Tabs></section>;
}

function LoginScreen({ saving, error, onSubmit }: { saving: boolean; error: string | null; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <main className="grid min-h-screen place-items-center bg-[#eef3f7] p-4"><div className="w-full max-w-md rounded-3xl border border-white/80 bg-white p-7 shadow-xl shadow-slate-300/30 md:p-9"><div className="mb-7 flex items-center gap-3"><div className="grid size-11 place-items-center rounded-xl bg-[#123a63] text-white shadow-sm"><Languages className="size-6" /></div><div><h1 className="text-xl font-bold tracking-tight">BridgeFlow</h1><p className="text-xs font-medium tracking-wide text-slate-500">JP × VN REQUIREMENTS</p></div></div><div><h2 className="text-2xl font-bold tracking-tight">Đăng nhập workspace</h2><p className="mt-2 text-sm leading-6 text-slate-500">Dùng tài khoản thành viên để truy cập đúng project và quyền được cấp.</p></div>{error && <div className="mt-5 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mt-0.5 size-4 shrink-0" />{error}</div>}<form onSubmit={onSubmit} className="mt-6 space-y-4"><label className="block text-sm font-semibold text-slate-700">Email<input name="email" type="email" required autoComplete="username" defaultValue="brse@bridgeflow.local" className="mt-1.5 h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 font-normal outline-none transition focus:border-[#2878ad] focus:bg-white" /></label><label className="block text-sm font-semibold text-slate-700">Mật khẩu<input name="password" type="password" required autoComplete="current-password" defaultValue="bridgeflow-demo" className="mt-1.5 h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 font-normal outline-none transition focus:border-[#2878ad] focus:bg-white" /></label><Button disabled={saving} className="mt-2 h-11 w-full bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} Đăng nhập</Button></form><p className="mt-5 text-center text-xs text-slate-400">Tài khoản mẫu chỉ được tạo khi backend chạy profile dev.</p></div></main>;
}

function LanguageCard({ label, lang, text, translated = false }: { label: string; lang: string; text?: string; translated?: boolean }) {
  return <article className={`rounded-xl border p-4 ${translated ? "border-[#dbe7f1] bg-[#f4f8fb]" : "border-slate-200 bg-slate-50/70"}`}><span className={`text-xs font-bold uppercase tracking-wider ${translated ? "text-[#436987]" : "text-slate-500"}`}>{label}</span><p lang={lang} className="mt-3 text-[15px] font-medium leading-7 text-slate-800">{text ?? "Chưa có nội dung"}</p></article>;
}

function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="grid min-h-80 place-items-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center"><div><BookOpenText className="mx-auto mb-3 size-8 text-slate-300" /><h2 className="font-semibold">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p>{action}</div></div>;
}

function ProjectEditor({ mode, project, saving, onClose, onSubmit }: { mode: "create" | "edit"; project: Project | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><h2 className="text-lg font-bold">{mode === "create" ? "Tạo project" : "Chỉnh sửa project"}</h2><p className="mt-1 text-sm text-slate-500">Workspace riêng cho requirement và lịch sử revision.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4">{mode === "create" && <label className="block text-sm font-medium">Mã project<input name="code" required maxLength={40} placeholder="EC-RENEWAL" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 font-mono uppercase outline-none focus:border-[#2878ad]" /></label>}<label className="block text-sm font-medium">Tên project<input name="name" required maxLength={160} defaultValue={mode === "edit" ? project?.name : ""} placeholder="EC Portal Renewal" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Khách hàng<input name="customerName" maxLength={160} defaultValue={mode === "edit" ? project?.customerName ?? "" : ""} placeholder="株式会社みらい" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label></div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} {mode === "create" ? "Tạo project" : "Lưu thay đổi"}</Button></div></form></div>;
}

function Editor({ mode, requirement, saving, onClose, onSubmit }: { mode: "create" | "revision"; requirement: Requirement | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><h2 className="text-lg font-bold">{mode === "create" ? "Thêm requirement" : `Tạo revision mới · ${requirement?.displayKey}`}</h2><p className="mt-1 text-sm text-slate-500">Nhập nội dung song ngữ để nhóm Nhật–Việt cùng review.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4">{mode === "create" && <label className="block text-sm font-medium">Mã requirement<input name="displayKey" required maxLength={40} placeholder="REQ-018" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 font-mono outline-none focus:border-[#2878ad]" /></label>}<label className="block text-sm font-medium">Nội dung tiếng Nhật<textarea name="japaneseText" required defaultValue={mode === "revision" ? requirement?.latestRevision?.japaneseText : ""} rows={4} lang="ja" className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Bản dịch tiếng Việt<textarea name="vietnameseText" required defaultValue={mode === "revision" ? requirement?.latestRevision?.vietnameseText : ""} rows={4} className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label></div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} Lưu {mode === "create" ? "requirement" : "revision"}</Button></div></form></div>;
}
