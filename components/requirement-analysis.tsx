"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AlertCircle, Check, LoaderCircle, MessageCircleQuestion, Sparkles, ThumbsDown } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  AcceptanceCriterion,
  ArtifactReviewStatus,
  bridgeFlowApi,
  ClarificationQuestion,
  Project,
  Requirement,
  RequirementAnalysis,
  Revision,
} from "@/lib/bridgeflow-api";

const reviewMeta: Record<ArtifactReviewStatus, { label: string; colors: string }> = {
  DRAFT: { label: "Chờ review", colors: "border-amber-200 bg-amber-50 text-amber-700" },
  APPROVED: { label: "Đã duyệt", colors: "border-emerald-200 bg-emerald-50 text-emerald-700" },
  REJECTED: { label: "Từ chối", colors: "border-red-200 bg-red-50 text-red-700" },
};

export function RequirementAnalysisWorkspace({
  project,
  requirement,
  revision,
}: {
  project: Project;
  requirement: Requirement;
  revision: Revision;
}) {
  const [analysis, setAnalysis] = useState<RequirementAnalysis | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [answering, setAnswering] = useState<ClarificationQuestion | null>(null);
  const canReview = ["ADMIN", "BRSE"].includes(project.role);
  const canAnswer = ["ADMIN", "BRSE", "DEVELOPER"].includes(project.role);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAnalysis(await bridgeFlowApi.getRequirementAnalysis(requirement.id, revision.id));
    } catch (cause) {
      setError(message(cause, "Không thể tải AI analysis."));
    } finally {
      setLoading(false);
    }
  }, [requirement.id, revision.id]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function generate() {
    setSaving(true);
    setError(null);
    try {
      setAnalysis(await bridgeFlowApi.generateRequirementAnalysis(requirement.id, revision.id));
    } catch (cause) {
      setError(message(cause, "Không thể tạo AI analysis."));
    } finally {
      setSaving(false);
    }
  }

  async function answer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!answering) return;
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setError(null);
    try {
      await bridgeFlowApi.answerClarificationQuestion(requirement.id, revision.id, answering.id, {
        japaneseText: String(form.get("japaneseText")),
        vietnameseText: String(form.get("vietnameseText")),
      });
      setAnswering(null);
      await load();
    } catch (cause) {
      setError(message(cause, "Không thể lưu câu trả lời."));
    } finally {
      setSaving(false);
    }
  }

  async function reviewQuestion(questionId: string, decision: "APPROVED" | "REJECTED") {
    await mutate(() => bridgeFlowApi.reviewClarificationQuestion(
      requirement.id, revision.id, questionId, decision,
    ));
  }

  async function reviewCriterion(criterionId: string, decision: "APPROVED" | "REJECTED") {
    await mutate(() => bridgeFlowApi.reviewAcceptanceCriterion(
      requirement.id, revision.id, criterionId, decision,
    ));
  }

  async function mutate(action: () => Promise<unknown>) {
    setSaving(true);
    setError(null);
    try {
      await action();
      await load();
    } catch (cause) {
      setError(message(cause, "Không thể cập nhật bản nháp."));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="grid min-h-44 place-items-center text-sm text-slate-500"><LoaderCircle className="mr-2 inline size-4 animate-spin" />Đang tải analysis…</div>;

  return <div className="space-y-5">
    {error && <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mt-0.5 size-4 shrink-0" />{error}</div>}
    <div className="flex flex-wrap items-start justify-between gap-3 rounded-xl border border-violet-200 bg-violet-50 p-4">
      <div><p className="flex items-center gap-2 font-semibold text-violet-950"><Sparkles className="size-4" />AI drafts có human review</p><p className="mt-1 text-xs leading-5 text-violet-700">AI chỉ đề xuất câu hỏi và tiêu chí. Admin/BrSE quyết định duyệt; mỗi job có correlation ID để audit.</p>{analysis?.job && <p className="mt-2 font-mono text-[11px] text-violet-600">{analysis.job.provider} · {analysis.job.model} · {analysis.job.correlationId.slice(0, 8)}</p>}</div>
      {canReview && <Button size="sm" disabled={saving || !project.aiEnabled || analysis?.job?.status === "COMPLETED"} onClick={() => void generate()} className="bg-violet-700 hover:bg-violet-800">{saving ? <LoaderCircle className="animate-spin" /> : <Sparkles />} Tạo AI analysis</Button>}
    </div>
    {!project.aiEnabled && <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">AI đang tắt cho project. Admin cần bật sau khi xác nhận chính sách dữ liệu.</p>}
    {!analysis?.job && <EmptyDraft />}
    {analysis?.job?.status === "FAILED" && <p className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">AI job thất bại: {analysis.job.errorMessage ?? analysis.job.errorCode}</p>}
    <section><div className="mb-3 flex items-center gap-2"><MessageCircleQuestion className="size-4 text-[#2878ad]" /><h3 className="font-semibold">Clarification Q&A</h3><Badge variant="outline">{analysis?.questions.length ?? 0}</Badge></div><div className="space-y-3">{analysis?.questions.map((question) => <QuestionCard key={question.id} question={question} canAnswer={canAnswer} canReview={canReview} saving={saving} onAnswer={() => setAnswering(question)} onReview={(decision) => void reviewQuestion(question.id, decision)} />)}{analysis?.job && analysis.questions.length === 0 && <p className="text-sm text-slate-500">AI không tìm thấy điểm cần làm rõ.</p>}</div></section>
    <section><div className="mb-3 flex items-center gap-2"><Check className="size-4 text-emerald-600" /><h3 className="font-semibold">Acceptance criteria</h3><Badge variant="outline">{analysis?.acceptanceCriteria.length ?? 0}</Badge></div><div className="space-y-3">{analysis?.acceptanceCriteria.map((criterion) => <CriterionCard key={criterion.id} criterion={criterion} canReview={canReview} saving={saving} onReview={(decision) => void reviewCriterion(criterion.id, decision)} />)}{analysis?.job && analysis.acceptanceCriteria.length === 0 && <p className="text-sm text-slate-500">AI chưa tạo acceptance criterion nào.</p>}</div></section>
    {answering && <AnswerDialog question={answering} saving={saving} onClose={() => setAnswering(null)} onSubmit={answer} />}
  </div>;
}

function QuestionCard({ question, canAnswer, canReview, saving, onAnswer, onReview }: { question: ClarificationQuestion; canAnswer: boolean; canReview: boolean; saving: boolean; onAnswer: () => void; onReview: (decision: "APPROVED" | "REJECTED") => void }) {
  return <article className="rounded-xl border border-slate-200 p-4"><div className="flex items-center justify-between gap-2"><ReviewBadge status={question.status} />{question.rationale && <span lang="ja" className="text-right text-[11px] text-slate-400">{question.rationale}</span>}</div><p lang="ja" className="mt-3 text-sm font-semibold leading-6">{question.japaneseText}</p><p className="mt-1 text-sm leading-6 text-slate-600">{question.vietnameseText}</p>{question.answerJapanese && <div className="mt-3 rounded-lg bg-blue-50 p-3 text-sm"><p lang="ja">{question.answerJapanese}</p><p className="mt-1 text-blue-800">{question.answerVietnamese}</p></div>}<div className="mt-3 flex flex-wrap gap-2">{canAnswer && <Button variant="outline" size="xs" disabled={saving} onClick={onAnswer}>{question.answeredAt ? "Sửa câu trả lời" : "Trả lời"}</Button>}{canReview && <ReviewButtons saving={saving} onReview={onReview} />}</div></article>;
}

function CriterionCard({ criterion, canReview, saving, onReview }: { criterion: AcceptanceCriterion; canReview: boolean; saving: boolean; onReview: (decision: "APPROVED" | "REJECTED") => void }) {
  return <article className="rounded-xl border border-slate-200 p-4"><ReviewBadge status={criterion.status} /><p lang="ja" className="mt-3 text-sm font-semibold leading-6">{criterion.japaneseText}</p><p className="mt-1 text-sm leading-6 text-slate-600">{criterion.vietnameseText}</p>{canReview && <div className="mt-3"><ReviewButtons saving={saving} onReview={onReview} /></div>}</article>;
}

function ReviewButtons({ saving, onReview }: { saving: boolean; onReview: (decision: "APPROVED" | "REJECTED") => void }) {
  return <div className="flex gap-2"><Button size="xs" variant="outline" disabled={saving} onClick={() => onReview("APPROVED")} className="text-emerald-700"><Check />Duyệt</Button><Button size="xs" variant="outline" disabled={saving} onClick={() => onReview("REJECTED")} className="text-red-600"><ThumbsDown />Từ chối</Button></div>;
}

function ReviewBadge({ status }: { status: ArtifactReviewStatus }) {
  const meta = reviewMeta[status];
  return <Badge variant="outline" className={meta.colors}>{meta.label}</Badge>;
}

function EmptyDraft() {
  return <div className="rounded-xl border border-dashed border-slate-300 p-6 text-center"><MessageCircleQuestion className="mx-auto size-7 text-slate-300" /><p className="mt-2 text-sm font-semibold">Chưa có AI analysis cho revision này</p><p className="mt-1 text-xs text-slate-500">Tạo một lần; gọi lại giữ nguyên job và không sinh artifact trùng.</p></div>;
}

function AnswerDialog({ question, saving, onClose, onSubmit }: { question: ClarificationQuestion; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><form onSubmit={onSubmit} className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl"><h2 className="text-lg font-bold">Trả lời clarification</h2><p className="mt-2 text-sm text-slate-600">{question.vietnameseText}</p><div className="mt-4 space-y-3"><label className="block text-sm font-medium">回答 · Tiếng Nhật<textarea name="japaneseText" required rows={3} lang="ja" defaultValue={question.answerJapanese ?? ""} className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label><label className="block text-sm font-medium">Câu trả lời tiếng Việt<textarea name="vietnameseText" required rows={3} defaultValue={question.answerVietnamese ?? ""} className="mt-1.5 w-full rounded-lg border border-slate-200 p-3 outline-none focus:border-[#2878ad]" /></label></div><div className="mt-5 flex justify-end gap-2"><Button type="button" variant="outline" onClick={onClose}>Hủy</Button><Button disabled={saving} className="bg-[#123a63] hover:bg-[#0d2e50]">{saving && <LoaderCircle className="animate-spin" />}Lưu câu trả lời</Button></div></form></div>;
}

function message(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback;
}
