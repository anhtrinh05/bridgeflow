"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, FileSearch, GitCompareArrows, LoaderCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import {
  bridgeFlowApi, ChangeImpact, Requirement, RequirementTraceability, Revision,
} from "@/lib/bridgeflow-api";

const impactStyle = {
  LOW: "border-emerald-200 bg-emerald-50 text-emerald-700",
  MEDIUM: "border-amber-200 bg-amber-50 text-amber-700",
  HIGH: "border-red-200 bg-red-50 text-red-700",
};
const typeLabel = {
  CLARIFICATION_QUESTION: "Q&A",
  ACCEPTANCE_CRITERION: "Criterion",
  TEST_CASE: "Test case",
};

export function RequirementTraceabilityWorkspace({ requirement, revision }: {
  requirement: Requirement; revision: Revision;
}) {
  const [traceability, setTraceability] = useState<RequirementTraceability | null>(null);
  const [impact, setImpact] = useState<ChangeImpact | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const [trace, report] = await Promise.all([
        bridgeFlowApi.getRequirementTraceability(requirement.id),
        bridgeFlowApi.getRequirementChangeImpact(requirement.id, revision.id),
      ]);
      setTraceability(trace); setImpact(report);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải traceability.");
    } finally { setLoading(false); }
  }, [requirement.id, revision.id]);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [load]);

  if (loading) return <div className="grid min-h-44 place-items-center text-sm text-slate-500"><LoaderCircle className="mr-2 inline size-4 animate-spin" />Đang phân tích impact…</div>;
  if (error) return <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mt-0.5 size-4 shrink-0" />{error}</div>;
  return <div className="space-y-5">
    {impact && <section className="rounded-xl border border-slate-200 p-4">
      <div className="flex flex-wrap items-center gap-2"><GitCompareArrows className="size-4 text-[#2878ad]" /><h3 className="font-semibold">Change impact · revision {revision.revisionNumber}</h3><Badge variant="outline" className={impactStyle[impact.impactLevel]}>{impact.impactLevel}</Badge></div>
      <ul className="mt-3 space-y-1 text-sm text-slate-600">{impact.reasons.map((reason) => <li key={reason}>• {reason}</li>)}</ul>
      {impact.affectedArtifacts.length > 0 && <div className="mt-4 overflow-hidden rounded-lg border border-slate-200"><div className="grid grid-cols-[1fr_auto_auto] gap-2 bg-slate-50 px-3 py-2 text-[11px] font-semibold uppercase text-slate-500"><span>Artifact</span><span>Trạng thái</span><span>Hành động</span></div>{impact.affectedArtifacts.map((artifact) => <div key={artifact.id} className="grid grid-cols-[1fr_auto_auto] items-center gap-2 border-t border-slate-100 px-3 py-2 text-xs"><span>{typeLabel[artifact.type]} · <span className="font-mono text-slate-400">{artifact.id.slice(0, 8)}</span></span><Badge variant="outline">{artifact.status}</Badge><span className="font-semibold text-amber-700">{artifact.recommendedAction}</span></div>)}</div>}
    </section>}
    <section><div className="mb-3 flex items-center gap-2"><FileSearch className="size-4 text-violet-600" /><h3 className="font-semibold">Traceability theo revision</h3></div><ol className="space-y-3">{traceability?.revisions.map((item) => <li key={item.revisionId} className="rounded-xl border border-slate-200 p-4"><div className="flex flex-wrap items-center gap-2"><span className="grid size-8 place-items-center rounded-full bg-slate-100 text-xs font-bold">v{item.revisionNumber}</span><Badge variant="outline">{item.changeType}</Badge><Badge variant="outline">{item.reviewStatus}</Badge>{traceability.currentRevisionId === item.revisionId && <Badge className="bg-emerald-600">Current</Badge>}<span className="ml-auto text-xs text-slate-400">{item.artifacts.length} artifacts</span></div>{item.sourceAnchor && <p className="mt-2 text-xs text-violet-700">Nguồn: {item.sourceAnchor}</p>}<div className="mt-3 flex flex-wrap gap-2">{item.artifacts.map((artifact) => <Badge key={artifact.id} variant="outline" className="font-normal">{typeLabel[artifact.type]} · {artifact.status}</Badge>)}{item.artifacts.length === 0 && <span className="text-xs text-slate-400">Chưa có artifact liên kết.</span>}</div></li>)}</ol></section>
  </div>;
}
