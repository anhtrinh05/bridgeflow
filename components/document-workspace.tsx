"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AlertCircle, Archive, Download, FileText, History, LoaderCircle, Plus, Upload, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { bridgeFlowApi, Project, ProjectDocument } from "@/lib/bridgeflow-api";

export function DocumentWorkspace({ project }: { project: Project }) {
  const [documents, setDocuments] = useState<ProjectDocument[]>([]);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [uploadTarget, setUploadTarget] = useState<ProjectDocument | "new" | null>(null);
  const canUpload = ["ADMIN", "BRSE", "DEVELOPER"].includes(project.role);
  const canArchive = ["ADMIN", "BRSE"].includes(project.role);

  const load = useCallback(async (archived = includeArchived) => {
    setLoading(true);
    setError(null);
    try {
      setDocuments(await bridgeFlowApi.listDocuments(project.id, archived));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải tài liệu.");
    } finally {
      setLoading(false);
    }
  }, [includeArchived, project.id]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const file = form.get("file");
    if (!(file instanceof File) || file.size === 0) return;
    if (file.size > 10 * 1024 * 1024) {
      setError("File vượt quá giới hạn 10 MB.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      if (uploadTarget === "new") {
        await bridgeFlowApi.uploadDocument(project.id, String(form.get("title")), file);
      } else if (uploadTarget) {
        await bridgeFlowApi.uploadDocumentVersion(uploadTarget.id, file);
      }
      setUploadTarget(null);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể upload tài liệu.");
    } finally {
      setSaving(false);
    }
  }

  async function download(document: ProjectDocument, versionId: string) {
    setError(null);
    try {
      const result = await bridgeFlowApi.downloadDocumentVersion(document.id, versionId);
      const url = URL.createObjectURL(result.blob);
      const anchor = window.document.createElement("a");
      anchor.href = url;
      anchor.download = result.filename;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải file.");
    }
  }

  async function archive(document: ProjectDocument) {
    if (!window.confirm(`Archive tài liệu “${document.title}”?`)) return;
    setSaving(true);
    setError(null);
    try {
      await bridgeFlowApi.archiveDocument(document.id);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể archive tài liệu.");
    } finally {
      setSaving(false);
    }
  }

  return <>
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between">
        <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={includeArchived} onChange={(event) => { setIncludeArchived(event.target.checked); void load(event.target.checked); }} className="size-4 rounded border-slate-300" />Hiện tài liệu đã archive</label>
        {canUpload && <Button onClick={() => setUploadTarget("new")} className="bg-[#123a63] hover:bg-[#0d2e50]"><Upload /> Upload tài liệu</Button>}
      </div>
      {error && <div className="m-4 flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="size-4 shrink-0" />{error}</div>}
      {loading ? <div className="grid min-h-72 place-items-center text-sm text-slate-500"><div className="text-center"><LoaderCircle className="mx-auto mb-3 size-6 animate-spin text-[#2878ad]" />Đang tải tài liệu…</div></div> : documents.length === 0 ? <div className="grid min-h-72 place-items-center p-8 text-center"><div><FileText className="mx-auto mb-3 size-9 text-slate-300" /><h2 className="font-semibold">Chưa có tài liệu</h2><p className="mt-1 text-sm text-slate-500">Upload PDF, DOCX, TXT hoặc Markdown để quản lý lịch sử version.</p></div></div> : <div className="space-y-3 bg-slate-50/60 p-4">{documents.map((document) => <article key={document.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><div className="flex flex-col gap-3 sm:flex-row sm:items-start"><div className="grid size-11 shrink-0 place-items-center rounded-xl bg-[#e8f0f7] text-[#123a63]"><FileText className="size-5" /></div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="font-semibold text-slate-900">{document.title}</h2><Badge variant="outline" className={document.status === "ARCHIVED" ? "bg-slate-100 text-slate-500" : "border-emerald-200 bg-emerald-50 text-emerald-700"}>{document.status}</Badge></div><p className="mt-1 text-xs text-slate-400">Stable ID · {document.id.slice(0, 8)} · {document.versions?.length ?? 0} version</p></div>{document.status !== "ARCHIVED" && <div className="flex gap-1">{canUpload && <Button variant="outline" size="sm" onClick={() => setUploadTarget(document)}><Plus /> Version mới</Button>}{canArchive && <Button variant="ghost" size="icon-sm" disabled={saving} onClick={() => void archive(document)} className="text-red-600 hover:bg-red-50 hover:text-red-700"><Archive /></Button>}</div>}</div><div className="mt-4 overflow-hidden rounded-xl border border-slate-100"><div className="flex items-center gap-2 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-500"><History className="size-3.5" /> Lịch sử version</div><div className="divide-y divide-slate-100">{document.versions?.map((version) => <div key={version.id} className="flex flex-col gap-2 px-3 py-3 sm:flex-row sm:items-center"><div className="grid size-8 shrink-0 place-items-center rounded-lg bg-slate-100 text-xs font-bold text-slate-600">v{version.versionNumber}</div><div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">{version.originalFilename}</p><p className="mt-0.5 text-xs text-slate-400">{formatBytes(version.sizeBytes)} · SHA-256 {version.sha256.slice(0, 12)}… · {new Date(version.createdAt).toLocaleString("vi-VN")}</p></div><Button variant="ghost" size="sm" onClick={() => void download(document, version.id)}><Download /> Tải xuống</Button></div>)}</div></div></article>)}</div>}
      <div className="border-t border-slate-100 px-4 py-3 text-xs text-slate-400">{documents.length} tài liệu trong kết quả hiện tại</div>
    </section>
    {uploadTarget && <UploadDialog document={uploadTarget === "new" ? null : uploadTarget} saving={saving} onClose={() => setUploadTarget(null)} onSubmit={upload} />}
  </>;
}

function UploadDialog({ document, saving, onClose, onSubmit }: { document: ProjectDocument | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-start justify-between"><div><div className="mb-2 grid size-9 place-items-center rounded-lg bg-[#e8f0f7] text-[#123a63]"><Upload className="size-5" /></div><h2 className="text-lg font-bold">{document ? `Thêm version · ${document.title}` : "Upload tài liệu"}</h2><p className="mt-1 text-sm text-slate-500">File tối đa 10 MB; hỗ trợ PDF, DOCX, TXT và Markdown.</p></div><Button type="button" variant="ghost" size="icon-sm" onClick={onClose}><X /></Button></div><div className="mt-5 space-y-4">{!document && <label className="block text-sm font-medium">Tên tài liệu<input name="title" required maxLength={200} placeholder="Đặc tả màn hình quản lý đơn hàng" className="mt-1.5 h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-[#2878ad]" /></label>}<label className="block text-sm font-medium">File<input name="file" type="file" required accept=".pdf,.docx,.txt,.md,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,text/markdown" className="mt-1.5 block w-full rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-[#e8f0f7] file:px-3 file:py-2 file:font-semibold file:text-[#123a63]" /></label></div><div className="mt-6 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />} Upload</Button></div></form></div>;
}

function formatBytes(value: number) {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}
