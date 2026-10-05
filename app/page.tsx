"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertCircle, Bell, BookOpenText, Check, ChevronDown, FileText,
  FolderKanban, GitCompareArrows, Languages, LayoutDashboard, LoaderCircle,
  MessageSquareText, MoreHorizontal, PanelLeftClose, Plus, RefreshCw, Search,
  Settings, TestTube2, Users, X,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { bridgeFlowApi, Project, Requirement } from "@/lib/bridgeflow-api";

const REVIEWER_ID = "40000000-0000-4000-8000-000000000001";
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
  const [project, setProject] = useState<Project | null>(null);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [selected, setSelected] = useState<Requirement | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [editor, setEditor] = useState<"create" | "revision" | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const projects = await bridgeFlowApi.listProjects();
      const activeProject = projects[0] ?? null;
      setProject(activeProject);
      if (!activeProject) { setRequirements([]); setSelected(null); return; }
      const items = await bridgeFlowApi.listRequirements(activeProject.id);
      setRequirements(items);
      setSelected(items[0] ? await bridgeFlowApi.getRequirement(items[0].id) : null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể kết nối backend.");
    } finally { setLoading(false); }
  }, []);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initialLoad);
  }, [load]);

  const filtered = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase();
    if (!keyword) return requirements;
    return requirements.filter((item) =>
      `${item.displayKey} ${item.latestRevision?.japaneseText} ${item.latestRevision?.vietnameseText}`
        .toLocaleLowerCase().includes(keyword),
    );
  }, [query, requirements]);

  async function selectRequirement(item: Requirement) {
    setError(null);
    try { setSelected(await bridgeFlowApi.getRequirement(item.id)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể tải requirement."); }
  }

  async function refreshRequirement(requirement: Requirement) {
    setSelected(requirement);
    if (project) setRequirements(await bridgeFlowApi.listRequirements(project.id));
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
      const result = await bridgeFlowApi.confirmRevision(selected.id, selected.latestRevision.id, REVIEWER_ID);
      await refreshRequirement(result);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể xác nhận revision.");
    } finally { setSaving(false); }
  }

  return (
    <main className="min-h-screen bg-[#f4f6f8] text-slate-950">
      <header className="sticky top-0 z-30 flex h-16 items-center border-b border-slate-200 bg-white px-4 lg:px-6">
        <div className="flex w-64 items-center gap-3"><div className="grid size-9 place-items-center rounded-xl bg-[#123a63] text-white shadow-sm"><Languages className="size-5" /></div><div><p className="text-base font-bold tracking-tight">BridgeFlow</p><p className="text-[11px] font-medium tracking-wide text-slate-500">JP × VN REQUIREMENTS</p></div></div>
        <div className="ml-auto flex items-center gap-2"><Badge variant="outline" className="hidden border-emerald-200 bg-emerald-50 text-emerald-700 sm:flex"><span className="size-1.5 rounded-full bg-emerald-500" />LIVE API</Badge><Button variant="ghost" size="icon-sm" aria-label="Thông báo"><Bell /></Button><div className="ml-1 grid size-8 place-items-center rounded-full bg-[#e8eef5] text-xs font-bold text-[#123a63]">TN</div></div>
      </header>

      <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-[1800px]">
        <aside className="hidden w-64 shrink-0 border-r border-slate-200 bg-[#f8fafc] p-4 lg:flex lg:flex-col">
          <div className="mb-5 flex w-full items-center gap-3 rounded-xl border border-slate-200 bg-white p-3 text-left shadow-sm"><div className="grid size-9 place-items-center rounded-lg bg-[#e8f0f7] text-sm font-bold text-[#123a63]">EC</div><div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold">{project?.name ?? "Workspace"}</p><p className="truncate text-xs text-slate-500">{project?.customerName ?? "Chưa chọn project"}</p></div><ChevronDown className="size-4 text-slate-400" /></div>
          <nav className="space-y-1">{nav.map(([Icon, label]) => <button key={label} className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${label === "Requirements" ? "bg-[#e7eff7] text-[#123a63]" : "text-slate-600 hover:bg-white"}`}><Icon className="size-[18px]" />{label}{label === "Requirements" && <span className="ml-auto rounded-md bg-white/80 px-1.5 py-0.5 text-[11px] text-slate-500">{requirements.length}</span>}</button>)}</nav>
          <div className="mt-6 border-t border-slate-200 pt-5"><p className="mb-2 px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">Không gian làm việc</p><button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600"><Users className="size-[18px]" /> Thành viên</button><button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600"><Settings className="size-[18px]" /> Cài đặt</button></div>
        </aside>

        <section className="min-w-0 flex-1 p-4 md:p-6 xl:p-8">
          <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><div className="mb-2 flex items-center gap-2 text-xs font-medium text-slate-500"><FolderKanban className="size-3.5" /> {project?.name ?? "Project"} <span>/</span> Requirements</div><h1 className="text-2xl font-bold tracking-tight md:text-3xl">Phân tích yêu cầu</h1><p className="mt-1 text-sm text-slate-500">Dữ liệu song ngữ được đọc trực tiếp từ PostgreSQL qua Spring Boot API.</p></div><div className="flex gap-2"><Button variant="outline" className="border-slate-200 bg-white"><PanelLeftClose /> Traceability</Button><Button disabled={!project} onClick={() => setEditor("create")} className="bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Thêm yêu cầu</Button></div></div>
          {error && <div className="mb-4 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="size-4 shrink-0" /><span className="flex-1">{error}</span><Button variant="ghost" size="sm" onClick={() => void load()}><RefreshCw /> Thử lại</Button></div>}
          {loading ? <div className="grid min-h-96 place-items-center rounded-2xl border border-slate-200 bg-white"><div className="text-center text-sm text-slate-500"><LoaderCircle className="mx-auto mb-3 size-6 animate-spin text-[#2878ad]" />Đang tải workspace…</div></div> : !project ? <EmptyState title="Chưa có project" description="Tạo project qua API để bắt đầu quản lý requirement." /> : (
            <div className="grid gap-4 xl:grid-cols-[minmax(350px,0.92fr)_minmax(520px,1.45fr)]">
              <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center gap-2 border-b border-slate-200 p-3"><div className="relative min-w-0 flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm requirement..." className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm outline-none focus:border-[#2878ad]" /></div></div><div className="divide-y divide-slate-100">{filtered.length === 0 ? <p className="p-8 text-center text-sm text-slate-500">Không tìm thấy requirement.</p> : filtered.map((item) => <button key={item.id} onClick={() => void selectRequirement(item)} className={`w-full border-l-[3px] p-4 pl-[13px] text-left transition hover:bg-slate-50 ${selected?.id === item.id ? "border-[#2878ad] bg-[#f2f7fb]" : "border-transparent"}`}><div className="mb-2 flex items-center justify-between gap-2"><span className="font-mono text-xs font-semibold text-[#2878ad]">{item.displayKey}</span><StatusBadge status={item.status} /></div><p lang="ja" className="line-clamp-1 text-sm font-semibold text-slate-900">{excerpt(item.latestRevision?.japaneseText)}</p><p className="mt-1 line-clamp-1 text-sm text-slate-500">{excerpt(item.latestRevision?.vietnameseText)}</p><p className="mt-2 text-xs text-slate-400">Revision {item.latestRevision?.revisionNumber ?? 0}</p></button>)}</div></section>
              {selected ? <RequirementDetail requirement={selected} saving={saving} onEdit={() => setEditor("revision")} onConfirm={() => void confirmLatest()} /> : <EmptyState title="Chưa có requirement" description="Thêm requirement đầu tiên cho project này." />}
            </div>
          )}
        </section>
      </div>
      {editor && <Editor mode={editor} requirement={selected} saving={saving} onClose={() => setEditor(null)} onSubmit={submitEditor} />}
    </main>
  );
}

function RequirementDetail({ requirement, saving, onEdit, onConfirm }: { requirement: Requirement; saving: boolean; onEdit: () => void; onConfirm: () => void }) {
  const revision = requirement.latestRevision;
  const canConfirm = revision && revision.reviewStatus !== "CONFIRMED";
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4"><span className="font-mono text-sm font-bold text-[#2878ad]">{requirement.displayKey}</span><StatusBadge status={requirement.status} /><span className="text-xs text-slate-400">Stable ID · {requirement.id.slice(0, 8)}</span><Button variant="ghost" size="icon-sm" className="ml-auto"><MoreHorizontal /></Button></div><Tabs defaultValue="analysis" className="gap-0"><TabsList variant="line" className="h-12 w-full justify-start gap-5 border-b border-slate-200 px-5"><TabsTrigger value="analysis" className="px-0">Phân tích song ngữ</TabsTrigger><TabsTrigger value="history" className="px-0">Lịch sử ({requirement.revisions.length})</TabsTrigger></TabsList><TabsContent value="analysis" className="p-5 md:p-6"><div className="grid gap-4 md:grid-cols-2"><LanguageCard label="原文 · Tiếng Nhật" lang="ja" text={revision?.japaneseText} /><LanguageCard label="Bản dịch · Tiếng Việt" lang="vi" text={revision?.vietnameseText} translated /></div><div className="mt-5 rounded-xl border border-blue-200 bg-blue-50/70 p-4 text-sm text-blue-950"><p className="font-semibold">Revision {revision?.revisionNumber ?? 0} · {revision?.changeType ?? "—"}</p><p className="mt-1 text-blue-800">Requirement ID được giữ ổn định; mỗi lần sửa tạo một revision mới để truy vết thay đổi.</p></div><div className="mt-5 flex flex-wrap gap-2 border-t border-slate-100 pt-5"><Button variant="outline" onClick={onEdit}>Tạo revision mới</Button>{canConfirm && <Button disabled={saving} onClick={onConfirm} className="bg-[#2878ad] hover:bg-[#226994]">{saving ? <LoaderCircle className="animate-spin" /> : <Check />} BrSE xác nhận revision</Button>}</div></TabsContent><TabsContent value="history" className="p-6"><ol className="space-y-3">{[...requirement.revisions].reverse().map((item) => <li key={item.id} className="flex gap-3 rounded-xl border border-slate-200 p-4"><div className="grid size-8 shrink-0 place-items-center rounded-full bg-slate-100 text-xs font-bold">v{item.revisionNumber}</div><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><p className="text-sm font-semibold">{item.changeType}</p><StatusBadge status={item.reviewStatus} /></div><p lang="ja" className="mt-1 truncate text-sm text-slate-600">{item.japaneseText}</p><p className="mt-1 text-xs text-slate-400">{new Date(item.createdAt).toLocaleString("vi-VN")}</p></div></li>)}</ol></TabsContent></Tabs></section>;
}

function LanguageCard({ label, lang, text, translated = false }: { label: string; lang: string; text?: string; translated?: boolean }) {
  return <article className={`rounded-xl border p-4 ${translated ? "border-[#dbe7f1] bg-[#f4f8fb]" : "border-slate-200 bg-slate-50/70"}`}><span className={`text-xs font-bold uppercase tracking-wider ${translated ? "text-[#436987]" : "text-slate-500"}`}>{label}</span><p lang={lang} className="mt-3 text-[15px] font-medium leading-7 text-slate-800">{text ?? "Chưa có nội dung"}</p></article>;
}

function EmptyState({ title, description }: { title: string; description: string }) {
  return <div className="grid min-h-80 place-items-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center"><div><BookOpenText className="mx-auto mb-3 size-8 text-slate-300" /><h2 className="font-semibold">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p></div></div>;
}

function Editor({ mode, requirement, saving, onClose, onSubmit }: { mode: "create" | "revision"; requirement: Requirement | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><h2 className="text-lg font-bold">{mode === "create" ? "Thêm requirement" : `Tạo revision mới · ${requirement?.displayKey}`}</h2><p className="mt-1 text-sm text-slate-500">Nhập nội dung song ngữ để nhóm Nhật–Việt cùng review.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4">{mode === "create" && <label className="block text-sm font-medium">Mã requirement<input name="displayKey" required maxLength={40} placeholder="REQ-018" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 font-mono outline-none focus:border-[#2878ad]" /></label>}<label className="block text-sm font-medium">Nội dung tiếng Nhật<textarea name="japaneseText" required defaultValue={mode === "revision" ? requirement?.latestRevision?.japaneseText : ""} rows={4} lang="ja" className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Bản dịch tiếng Việt<textarea name="vietnameseText" required defaultValue={mode === "revision" ? requirement?.latestRevision?.vietnameseText : ""} rows={4} className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label></div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} Lưu {mode === "create" ? "requirement" : "revision"}</Button></div></form></div>;
}
