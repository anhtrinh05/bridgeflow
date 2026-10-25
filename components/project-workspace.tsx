"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, LoaderCircle, Pencil, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { AiJob, AuthUser, bridgeFlowApi, Project, ProjectMember, Requirement } from "@/lib/bridgeflow-api";
import { WorkspaceView } from "@/lib/workspace-navigation";

import { OverviewDashboard } from "@/components/overview-dashboard";

const panel = "bridgeflow-panel rounded-2xl border p-5";

function useProjectData<T>(fetchData: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    void fetchData().then((result) => { if (active) setData(result); })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "Không thể tải dữ liệu."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [fetchData, attempt]);
  function retry() { setError(null); setLoading(true); setAttempt((value) => value + 1); }
  return { data, loading, error, retry };
}

function DataStatus({ loading, error, retry }: { loading: boolean; error: string | null; retry: () => void }) {
  if (loading) return <div role="status" className={`${panel} grid min-h-48 place-items-center text-sm text-slate-500`}><span><LoaderCircle className="mr-2 inline size-4 animate-spin" />Đang tải dữ liệu…</span></div>;
  if (error) return <div role="alert" className={`${panel} flex items-center gap-3 text-sm text-red-700`}><AlertCircle className="size-5 shrink-0" /><span className="flex-1">{error}</span><Button variant="outline" onClick={retry}><RefreshCw />Thử lại</Button></div>;
  return null;
}

export function ProjectOverview({ project, onNavigate, onOpenRequirement }: { project: Project; onNavigate: (view: WorkspaceView) => void; onOpenRequirement: (item: Requirement) => void }) {
  const fetchOverview = useCallback(async () => {
    const [requirements, documents, glossary] = await Promise.all([
      bridgeFlowApi.listAllRequirements(project.id), bridgeFlowApi.listDocuments(project.id),
      bridgeFlowApi.listGlossaryTerms(project.id),
    ]);
    return { requirements, documents, glossary };
  }, [project.id]);
  const { data, ...status } = useProjectData(fetchOverview);
  if (status.loading || status.error || !data) return <DataStatus {...status} />;
  return <OverviewDashboard project={project} requirements={data.requirements} documentCount={data.documents.length} glossaryCount={data.glossary.length} onNavigate={onNavigate} onOpenRequirement={onOpenRequirement} />;
}

const roleDescriptions: Record<string, string> = {
  ADMIN: "Quản trị project, chỉnh sửa và duyệt yêu cầu.",
  BRSE: "Chỉnh sửa song ngữ, trả lời và duyệt các bản nháp.",
  DEVELOPER: "Chỉnh sửa yêu cầu và trả lời câu hỏi làm rõ.",
  VIEWER: "Xem dữ liệu được chia sẻ trong project.",
};

export function ProjectMembers({ project, user }: { project: Project; user: AuthUser }) {
  const fetchMembers = useCallback(() => bridgeFlowApi.listProjectMembers(project.id), [project.id]);
  const { data, ...status } = useProjectData<ProjectMember[]>(fetchMembers);
  if (status.loading || status.error || !data) return <DataStatus {...status} />;
  return <section className={panel}><h2 className="font-semibold">{data.length} thành viên</h2><p className="mt-1 text-sm text-slate-500">Danh sách hiện có trong project. Màn hình này hỗ trợ xem thành viên và quyền truy cập.</p><div className="mt-4 divide-y divide-slate-100">{data.map((member) => <article key={member.userId} className="flex flex-wrap items-center gap-4 py-4"><div className="min-w-0 flex-1"><p className="font-medium">{member.displayName}{member.userId === user.id && <span className="ml-2 text-xs text-slate-400">Bạn</span>}</p><p className="break-all text-sm text-slate-500">{member.email}</p><p className="mt-1 text-xs text-slate-500">{roleDescriptions[member.role] ?? member.role}</p></div><Badge variant="outline">{member.role}</Badge></article>)}</div></section>;
}

export function ProjectSettings({ project, user, onEdit }: { project: Project; user: AuthUser; onEdit: () => void }) {
  return <div className="space-y-5"><section className={panel}><div className="flex items-center justify-between gap-3"><h2 className="font-semibold">Thông tin project</h2>{project.role === "ADMIN" && <Button variant="outline" disabled={project.status === "ARCHIVED"} onClick={onEdit}><Pencil />Chỉnh sửa project</Button>}</div><dl className="mt-4 grid gap-4 text-sm sm:grid-cols-2">{[["Mã project", project.code], ["Tên project", project.name], ["Khách hàng", project.customerName || "Chưa thiết lập"], ["Trạng thái", project.status], ["Vai trò của bạn", project.role], ["Xử lý AI", project.aiEnabled ? "Đã bật" : "Đang tắt"]].map(([label, value]) => <div key={label}><dt className="text-slate-500">{label}</dt><dd className="mt-1 font-medium">{value}</dd></div>)}</dl><p className="mt-5 rounded-xl bg-blue-50 p-3 text-sm text-blue-800">Admin có thể bật AI trong phần chỉnh sửa project sau khi đồng ý chính sách xử lý dữ liệu. Mọi kết quả AI đều cần người phụ trách review.</p></section><section className={panel}><h2 className="font-semibold">Tài khoản đang đăng nhập</h2><p className="mt-3 text-sm">{user.displayName}</p><p className="mt-1 text-sm text-slate-500">{user.email}</p><p className="mt-3 text-xs text-slate-500">{roleDescriptions[project.role] ?? project.role}</p></section></div>;
}

export function ProjectAiActivity({ project }: { project: Project }) {
  const fetchJobs = useCallback(() => bridgeFlowApi.listAiJobs(project.id), [project.id]);
  const { data, ...status } = useProjectData<AiJob[]>(fetchJobs);
  if (status.loading || status.error || !data) return <DataStatus {...status} />;
  return <section className={panel}><div className="flex items-center justify-between gap-3"><h2 className="font-semibold">{data.length} lần xử lý AI</h2><Button variant="outline" size="sm" onClick={status.retry}><RefreshCw />Làm mới</Button></div><div className="mt-4 space-y-3">{data.map((job) => <article key={job.id} className="rounded-xl border border-slate-200 p-4"><div className="flex flex-wrap items-center gap-2"><p className="flex-1 text-sm font-medium">{job.purpose}</p><Badge variant="outline">{job.status}</Badge></div><p className="mt-2 text-sm text-slate-500">{job.provider === "stub" ? "Mô phỏng AI" : job.provider} · {job.model}</p><p className="mt-1 break-all font-mono text-xs text-slate-400">Mã tra cứu: {job.correlationId}</p>{job.errorMessage && <p className="mt-2 text-sm text-red-700">{job.errorMessage}</p>}</article>)}{!data.length && <p className="py-5 text-center text-sm text-slate-500">Chưa có lần xử lý AI nào. Các kết quả trích xuất, phân tích và tạo test case sẽ xuất hiện tại đây.</p>}</div></section>;
}
