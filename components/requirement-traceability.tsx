"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AlertCircle, FileSearch, GitCompareArrows, Link2, LoaderCircle, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  bridgeFlowApi, ChangeImpact, Requirement, RequirementRelationType,
  RequirementTraceability, Revision,
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
const relationTypes: RequirementRelationType[] = [
  "DEPENDS_ON", "SUPERSEDES", "SPLIT_INTO", "MERGED_INTO", "DUPLICATES",
];

export function RequirementTraceabilityWorkspace({ requirement, revision }: {
  requirement: Requirement; revision: Revision;
}) {
  const [traceability, setTraceability] = useState<RequirementTraceability | null>(null);
  const [impact, setImpact] = useState<ChangeImpact | null>(null);
  const [candidates, setCandidates] = useState<Requirement[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [canManage, setCanManage] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const [trace, report, requirementPage, projects] = await Promise.all([
        bridgeFlowApi.getRequirementTraceability(requirement.id),
        bridgeFlowApi.getRequirementChangeImpact(requirement.id, revision.id),
        bridgeFlowApi.listRequirements(requirement.projectId, { size: 100 }),
        bridgeFlowApi.listProjects(),
      ]);
      setTraceability(trace); setImpact(report);
      setCandidates(requirementPage.items.filter((item) => item.id !== requirement.id));
      setCanManage(["ADMIN", "BRSE"].includes(
        projects.find((item) => item.id === requirement.projectId)?.role ?? "VIEWER",
      ));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải traceability.");
    } finally { setLoading(false); }
  }, [requirement.id, requirement.projectId, revision.id]);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [load]);

  async function createRelation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true); setError(null);
    try {
      await bridgeFlowApi.createRequirementRelation(
        requirement.id, String(form.get("targetRequirementId")),
        String(form.get("relationType")) as RequirementRelationType,
      );
      event.currentTarget.reset();
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tạo relation.");
    } finally { setSaving(false); }
  }

  async function deleteRelation(relationId: string) {
    setSaving(true); setError(null);
    try { await bridgeFlowApi.deleteRequirementRelation(requirement.id, relationId); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể xóa relation."); }
    finally { setSaving(false); }
  }

  if (loading) return <div className="grid min-h-44 place-items-center text-sm text-slate-500"><LoaderCircle className="mr-2 inline size-4 animate-spin" />Đang phân tích impact…</div>;
  return <div className="space-y-5">
    {error && <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mt-0.5 size-4 shrink-0" />{error}</div>}
    <section className="rounded-xl border border-slate-200 p-4">
      <div className="flex items-center gap-2"><Link2 className="size-4 text-violet-600" /><h3 className="font-semibold">Quan hệ với requirements khác</h3><Badge variant="outline">{traceability?.relations.length ?? 0}</Badge></div>
      {canManage && candidates.length > 0 && <form onSubmit={createRelation} className="mt-3 grid gap-2 sm:grid-cols-[1fr_1fr_auto]"><select required name="targetRequirementId" defaultValue="" className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"><option value="" disabled>Chọn requirement</option>{candidates.map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.displayKey}</option>)}</select><select required name="relationType" defaultValue="DEPENDS_ON" className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">{relationTypes.map((type) => <option key={type} value={type}>{type}</option>)}</select><Button disabled={saving} className="bg-violet-700 hover:bg-violet-800">{saving && <LoaderCircle className="animate-spin" />}Thêm relation</Button></form>}
      <div className="mt-3 space-y-2">{traceability?.relations.map((relation) => <div key={relation.relationId} className="flex items-center gap-2 rounded-lg bg-slate-50 px-3 py-2 text-sm"><Badge variant="outline">{relation.direction}</Badge><span className="font-semibold">{relation.relationType}</span><span className="text-slate-500">→ {relation.relatedDisplayKey}</span>{canManage && <Button aria-label="Xóa relation" variant="ghost" size="icon-xs" disabled={saving} onClick={() => void deleteRelation(relation.relationId)} className="ml-auto text-red-600"><Trash2 /></Button>}</div>)}{traceability?.relations.length === 0 && <p className="text-sm text-slate-400">Chưa có relation nào.</p>}</div>
    </section>
    {impact && <section className="rounded-xl border border-slate-200 p-4"><div className="flex flex-wrap items-center gap-2"><GitCompareArrows className="size-4 text-[#2878ad]" /><h3 className="font-semibold">Change impact · revision {revision.revisionNumber}</h3><Badge variant="outline" className={impactStyle[impact.impactLevel]}>{impact.impactLevel}</Badge></div><ul className="mt-3 space-y-1 text-sm text-slate-600">{impact.reasons.map((reason) => <li key={reason}>• {reason}</li>)}</ul>{impact.affectedArtifacts.length > 0 && <div className="mt-4 overflow-hidden rounded-lg border border-slate-200"><div className="grid grid-cols-[1fr_auto_auto] gap-2 bg-slate-50 px-3 py-2 text-[11px] font-semibold uppercase text-slate-500"><span>Artifact</span><span>Trạng thái</span><span>Hành động</span></div>{impact.affectedArtifacts.map((artifact) => <div key={artifact.id} className="grid grid-cols-[1fr_auto_auto] items-center gap-2 border-t border-slate-100 px-3 py-2 text-xs"><span>{typeLabel[artifact.type]} · <span className="font-mono text-slate-400">{artifact.id.slice(0, 8)}</span></span><Badge variant="outline">{artifact.status}</Badge><span className="font-semibold text-amber-700">{artifact.recommendedAction}</span></div>)}</div>}</section>}
    <section><div className="mb-3 flex items-center gap-2"><FileSearch className="size-4 text-violet-600" /><h3 className="font-semibold">Traceability theo revision</h3></div><ol className="space-y-3">{traceability?.revisions.map((item) => <li key={item.revisionId} className="rounded-xl border border-slate-200 p-4"><div className="flex flex-wrap items-center gap-2"><span className="grid size-8 place-items-center rounded-full bg-slate-100 text-xs font-bold">v{item.revisionNumber}</span><Badge variant="outline">{item.changeType}</Badge><Badge variant="outline">{item.reviewStatus}</Badge>{traceability.currentRevisionId === item.revisionId && <Badge className="bg-emerald-600">Current</Badge>}<span className="ml-auto text-xs text-slate-400">{item.artifacts.length} artifacts</span></div>{item.sourceAnchor && <p className="mt-2 text-xs text-violet-700">Nguồn: {item.sourceAnchor}</p>}<div className="mt-3 flex flex-wrap gap-2">{item.artifacts.map((artifact) => <Badge key={artifact.id} variant="outline" className="font-normal">{typeLabel[artifact.type]} · {artifact.status}</Badge>)}{item.artifacts.length === 0 && <span className="text-xs text-slate-400">Chưa có artifact liên kết.</span>}</div></li>)}</ol></section>
  </div>;
}
