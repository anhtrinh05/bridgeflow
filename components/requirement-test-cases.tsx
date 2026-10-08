"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, Check, FlaskConical, LoaderCircle, Sparkles, ThumbsDown } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ArtifactReviewStatus, bridgeFlowApi, Project, Requirement, Revision,
  TestCaseWorkspace, VerificationTestCase,
} from "@/lib/bridgeflow-api";

const statusLabel: Record<ArtifactReviewStatus, string> = {
  DRAFT: "Chờ review", APPROVED: "Đã duyệt", REJECTED: "Từ chối",
};
const priorityStyle = {
  HIGH: "border-red-200 bg-red-50 text-red-700",
  MEDIUM: "border-amber-200 bg-amber-50 text-amber-700",
  LOW: "border-slate-200 bg-slate-50 text-slate-600",
};

export function RequirementTestCases({ project, requirement, revision }: {
  project: Project; requirement: Requirement; revision: Revision;
}) {
  const [workspace, setWorkspace] = useState<TestCaseWorkspace | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canReview = ["ADMIN", "BRSE"].includes(project.role);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try { setWorkspace(await bridgeFlowApi.getTestCases(requirement.id, revision.id)); }
    catch (cause) { setError(message(cause, "Không thể tải test cases.")); }
    finally { setLoading(false); }
  }, [requirement.id, revision.id]);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [load]);

  async function generate() {
    setSaving(true); setError(null);
    try { setWorkspace(await bridgeFlowApi.generateTestCases(requirement.id, revision.id)); }
    catch (cause) { setError(message(cause, "Không thể tạo test cases.")); }
    finally { setSaving(false); }
  }

  async function review(testCaseId: string, decision: "APPROVED" | "REJECTED") {
    setSaving(true); setError(null);
    try {
      await bridgeFlowApi.reviewTestCase(requirement.id, revision.id, testCaseId, decision);
      await load();
    } catch (cause) { setError(message(cause, "Không thể review test case.")); }
    finally { setSaving(false); }
  }

  if (loading) return <div className="grid min-h-44 place-items-center text-sm text-slate-500"><LoaderCircle className="mr-2 inline size-4 animate-spin" />Đang tải test cases…</div>;
  return <div className="space-y-4">
    {error && <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mt-0.5 size-4 shrink-0" />{error}</div>}
    <div className="flex flex-wrap items-start justify-between gap-3 rounded-xl border border-cyan-200 bg-cyan-50 p-4">
      <div><p className="flex items-center gap-2 font-semibold text-cyan-950"><FlaskConical className="size-4" />Test case song ngữ có truy vết</p><p className="mt-1 text-xs leading-5 text-cyan-800">Chỉ dùng acceptance criteria đã duyệt. Mỗi test case giữ liên kết về criterion nguồn và cần Admin/BrSE review.</p>{workspace?.job && <p className="mt-2 font-mono text-[11px] text-cyan-700">{workspace.job.provider} · {workspace.job.model} · {workspace.job.correlationId.slice(0, 8)}</p>}</div>
      {canReview && <Button size="sm" disabled={saving || !project.aiEnabled || workspace?.job?.status === "COMPLETED"} onClick={() => void generate()} className="bg-cyan-700 hover:bg-cyan-800">{saving ? <LoaderCircle className="animate-spin" /> : <Sparkles />}Tạo test cases</Button>}
    </div>
    {!workspace?.job && <div className="rounded-xl border border-dashed border-slate-300 p-6 text-center"><FlaskConical className="mx-auto size-7 text-slate-300" /><p className="mt-2 text-sm font-semibold">Chưa có test case cho revision này</p><p className="mt-1 text-xs text-slate-500">Duyệt ít nhất một acceptance criterion ở tab Q&amp;A &amp; tiêu chí trước khi tạo.</p></div>}
    {workspace?.job?.status === "FAILED" && <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">AI job thất bại: {workspace.job.errorMessage ?? workspace.job.errorCode}</p>}
    <div className="space-y-3">{workspace?.testCases.map((testCase) => <TestCaseCard key={testCase.id} testCase={testCase} saving={saving} canReview={canReview} onReview={(decision) => void review(testCase.id, decision)} />)}</div>
  </div>;
}

function TestCaseCard({ testCase, saving, canReview, onReview }: {
  testCase: VerificationTestCase; saving: boolean; canReview: boolean;
  onReview: (decision: "APPROVED" | "REJECTED") => void;
}) {
  return <article className="rounded-xl border border-slate-200 p-4">
    <div className="flex flex-wrap items-center gap-2"><Badge variant="outline">{statusLabel[testCase.status]}</Badge><Badge variant="outline" className={priorityStyle[testCase.priority]}>{testCase.priority}</Badge><span className="ml-auto font-mono text-[10px] text-slate-400">criterion {testCase.acceptanceCriterionId.slice(0, 8)}</span></div>
    <h3 lang="ja" className="mt-3 text-sm font-semibold">{testCase.titleJapanese}</h3><p className="mt-1 text-sm text-slate-600">{testCase.titleVietnamese}</p>
    <CaseBlock title="前提条件 · Điều kiện" japanese={testCase.preconditionsJapanese} vietnamese={testCase.preconditionsVietnamese} />
    <CaseBlock title="手順 · Các bước" japanese={testCase.stepsJapanese} vietnamese={testCase.stepsVietnamese} />
    <CaseBlock title="期待結果 · Kết quả mong đợi" japanese={testCase.expectedResultJapanese} vietnamese={testCase.expectedResultVietnamese} />
    {canReview && <div className="mt-3 flex gap-2"><Button size="xs" variant="outline" disabled={saving} onClick={() => onReview("APPROVED")} className="text-emerald-700"><Check />Duyệt</Button><Button size="xs" variant="outline" disabled={saving} onClick={() => onReview("REJECTED")} className="text-red-600"><ThumbsDown />Từ chối</Button></div>}
  </article>;
}

function CaseBlock({ title, japanese, vietnamese }: { title: string; japanese: string; vietnamese: string }) {
  return <div className="mt-3 rounded-lg bg-slate-50 p-3"><p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">{title}</p><p lang="ja" className="mt-1 whitespace-pre-line text-sm">{japanese}</p><p className="mt-1 whitespace-pre-line text-sm text-slate-600">{vietnamese}</p></div>;
}

function message(cause: unknown, fallback: string) { return cause instanceof Error ? cause.message : fallback; }
