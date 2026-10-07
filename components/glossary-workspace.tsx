"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AlertCircle, BookMarked, Languages, LoaderCircle, Pencil, Plus, Search, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { bridgeFlowApi, GlossaryTerm, Project } from "@/lib/bridgeflow-api";

export function GlossaryWorkspace({ project }: { project: Project }) {
  const [terms, setTerms] = useState<GlossaryTerm[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editor, setEditor] = useState<GlossaryTerm | "create" | null>(null);
  const canManage = ["ADMIN", "BRSE"].includes(project.role);

  const load = useCallback(async (search = "") => {
    setLoading(true);
    setError(null);
    try {
      setTerms(await bridgeFlowApi.listGlossaryTerms(project.id, search));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải glossary.");
    } finally {
      setLoading(false);
    }
  }, [project.id]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await load(query);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = {
      japaneseTerm: String(form.get("japaneseTerm")),
      vietnameseTerm: String(form.get("vietnameseTerm")),
      notes: String(form.get("notes")),
    };
    setSaving(true);
    setError(null);
    try {
      if (editor === "create") await bridgeFlowApi.createGlossaryTerm(project.id, body);
      else if (editor) await bridgeFlowApi.updateGlossaryTerm(project.id, editor.id, body);
      setEditor(null);
      await load(query);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể lưu thuật ngữ.");
    } finally {
      setSaving(false);
    }
  }

  async function remove(term: GlossaryTerm) {
    if (!window.confirm(`Xóa thuật ngữ “${term.japaneseTerm}”?`)) return;
    setSaving(true);
    setError(null);
    try {
      await bridgeFlowApi.deleteGlossaryTerm(project.id, term.id);
      await load(query);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể xóa thuật ngữ.");
    } finally {
      setSaving(false);
    }
  }

  return <>
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center">
        <form onSubmit={search} className="flex min-w-0 flex-1 gap-2">
          <div className="relative min-w-0 flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm tiếng Nhật, tiếng Việt hoặc ghi chú…" className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm outline-none focus:border-[#2878ad] focus:bg-white" /></div>
          <Button type="submit" variant="outline">Tìm</Button>
        </form>
        {canManage && <Button onClick={() => setEditor("create")} className="bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Thêm thuật ngữ</Button>}
      </div>
      {error && <div className="m-4 flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="size-4 shrink-0" />{error}</div>}
      {loading ? <div className="grid min-h-72 place-items-center text-sm text-slate-500"><div className="text-center"><LoaderCircle className="mx-auto mb-3 size-6 animate-spin text-[#2878ad]" />Đang tải glossary…</div></div> : terms.length === 0 ? <div className="grid min-h-72 place-items-center p-8 text-center"><div><BookMarked className="mx-auto mb-3 size-9 text-slate-300" /><h2 className="font-semibold">Chưa có thuật ngữ</h2><p className="mt-1 text-sm text-slate-500">Thêm cặp thuật ngữ Nhật–Việt để cả team dùng nhất quán.</p></div></div> : <div className="divide-y divide-slate-100">{terms.map((term) => <article key={term.id} className="grid gap-3 p-4 transition hover:bg-slate-50/70 md:grid-cols-[minmax(180px,0.8fr)_minmax(220px,1fr)_minmax(200px,1.2fr)_auto] md:items-center"><div><p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-slate-400">日本語</p><p lang="ja" className="font-semibold text-slate-900">{term.japaneseTerm}</p></div><div><p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-slate-400">Tiếng Việt</p><p className="text-sm font-medium text-[#235b83]">{term.vietnameseTerm}</p></div><div><p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-slate-400">Ghi chú</p><p className="text-sm text-slate-500">{term.notes || "—"}</p></div>{canManage && <div className="flex justify-end gap-1"><Button variant="ghost" size="icon-sm" aria-label="Sửa thuật ngữ" onClick={() => setEditor(term)}><Pencil /></Button><Button variant="ghost" size="icon-sm" aria-label="Xóa thuật ngữ" disabled={saving} onClick={() => void remove(term)} className="text-red-600 hover:bg-red-50 hover:text-red-700"><Trash2 /></Button></div>}</article>)}</div>}
      <div className="border-t border-slate-100 px-4 py-3 text-xs text-slate-400">{terms.length} thuật ngữ trong kết quả hiện tại</div>
    </section>
    {editor && <GlossaryEditor term={editor === "create" ? null : editor} saving={saving} onClose={() => setEditor(null)} onSubmit={save} />}
  </>;
}

function GlossaryEditor({ term, saving, onClose, onSubmit }: { term: GlossaryTerm | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><div className="mb-2 flex size-9 items-center justify-center rounded-lg bg-[#e8f0f7] text-[#123a63]"><Languages className="size-5" /></div><h2 className="text-lg font-bold">{term ? "Chỉnh sửa thuật ngữ" : "Thêm thuật ngữ"}</h2><p className="mt-1 text-sm text-slate-500">Chuẩn hóa cách dùng từ giữa tài liệu tiếng Nhật và bản dịch.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4"><label className="block text-sm font-medium">Thuật ngữ tiếng Nhật<input name="japaneseTerm" required maxLength={160} lang="ja" defaultValue={term?.japaneseTerm ?? ""} placeholder="注文履歴" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Thuật ngữ tiếng Việt<input name="vietnameseTerm" required maxLength={240} defaultValue={term?.vietnameseTerm ?? ""} placeholder="Lịch sử đơn hàng" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Ghi chú<textarea name="notes" maxLength={2000} rows={3} defaultValue={term?.notes ?? ""} placeholder="Ngữ cảnh sử dụng hoặc thuật ngữ cần tránh…" className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label></div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} Lưu thuật ngữ</Button></div></form></div>;
}
