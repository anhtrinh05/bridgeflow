"use client";

import { useRef, useState } from "react";
import { ArrowRight, BookOpenText, Check, CircleCheck, FileText, Languages, ListFilter, Search, Sparkles, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Project, Requirement } from "@/lib/bridgeflow-api";
import type { WorkspaceView } from "@/lib/workspace-navigation";

type Filter = "all" | "pending" | "confirmed";
const statusLabels: Record<string, string> = { DRAFT: "Bản nháp", REVIEWING: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", REJECTED: "Cần chỉnh sửa", ARCHIVED: "Đã lưu trữ" };
const pendingStatuses = new Set(["DRAFT", "REVIEWING", "REJECTED"]);

export function OverviewDashboard({ project, requirements, documentCount, glossaryCount, onNavigate, onOpenRequirement }: {
  project: Project; requirements: Requirement[]; documentCount: number; glossaryCount: number;
  onNavigate: (view: WorkspaceView) => void; onOpenRequirement: (item: Requirement) => void;
}) {
  const [filter, setFilter] = useState<Filter>("all");
  const [search, setSearch] = useState("");
  const [visibleCount, setVisibleCount] = useState(6);
  const listRef = useRef<HTMLElement>(null);
  const active = requirements.filter(item => item.status !== "ARCHIVED");
  const confirmed = active.filter(item => item.status === "CONFIRMED").length;
  const pending = active.filter(item => pendingStatuses.has(item.status)).length;
  const progress = active.length ? Math.round(confirmed / active.length * 100) : 0;
  const normalized = search.trim().toLocaleLowerCase("vi");
  const matches = active.filter(item => (
    filter === "all" || (filter === "confirmed" ? item.status === "CONFIRMED" : pendingStatuses.has(item.status))
  ) && [item.displayKey, item.latestRevision?.vietnameseText, item.latestRevision?.japaneseText].some(value => value?.toLocaleLowerCase("vi").includes(normalized)))
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  function changeFilter(value: Filter, scroll = false) {
    setFilter(value);
    setVisibleCount(6);
    if (scroll) listRef.current?.scrollIntoView({ behavior: "auto", block: "start" });
  }
  const nextStep = project.status === "ARCHIVED"
    ? { title: "Dự án đang được lưu trữ", description: "Bạn vẫn có thể xem yêu cầu và lịch sử của dự án.", action: "Xem yêu cầu", view: "requirements" as const }
    : pending > 0
    ? { title: `${pending} yêu cầu cần bạn xem lại`, description: "Đối chiếu bản dịch, làm rõ câu hỏi rồi xác nhận nội dung.", action: "Xem danh sách", view: null }
    : !active.length
    ? { title: "Bắt đầu từ tài liệu đầu tiên", description: "Thêm đặc tả Nhật–Việt để cùng nhóm phân tích yêu cầu.", action: "Mở tài liệu", view: "documents" as const }
    : { title: "Các yêu cầu đã được xác nhận", description: "Tiếp tục làm rõ tiêu chí và chuẩn bị kịch bản kiểm thử.", action: "Mở kiểm thử", view: "test-cases" as const };
  const steps = [
    { icon: FileText, label: "Tài liệu nguồn", description: "Tải lên và quản lý phiên bản", value: `${documentCount} tài liệu`, view: "documents" },
    { icon: Languages, label: "Thuật ngữ chung", description: "Thống nhất cách dịch Nhật–Việt", value: `${glossaryCount} thuật ngữ`, view: "glossary" },
    { icon: BookOpenText, label: "Phân tích yêu cầu", description: "Đối chiếu, làm rõ và xác nhận", value: `${active.length} yêu cầu`, view: "requirements" },
    { icon: CircleCheck, label: "Kiểm thử & truy vết", description: "Liên kết tiêu chí với kiểm thử", value: "Mở kiểm thử", view: "test-cases" },
  ] as const;
  return <div className="bf-overview">
    <section className="bf-next-step" aria-label="Gợi ý bước tiếp theo">
      <span className="bf-next-icon"><Sparkles size={21} /></span>
      <div><span className="bf-eyebrow">BƯỚC TIẾP THEO</span><h2>{nextStep.title}</h2><p>{nextStep.description}</p></div>
      <Button onClick={() => nextStep.view ? onNavigate(nextStep.view) : changeFilter("pending", true)}>{nextStep.action}<ArrowRight /></Button>
    </section>

    <div className="bf-metric-grid">
      <button className="bf-metric bf-metric-total" onClick={() => changeFilter("all", true)}><span><BookOpenText size={17} />Tổng yêu cầu</span><strong>{active.length}</strong><small>Đang theo dõi <ArrowRight size={14} /></small></button>
      <button className="bf-metric bf-metric-pending" onClick={() => changeFilter("pending", true)}><span><ListFilter size={17} />Cần xem lại</span><strong className="bf-amber">{pending}</strong><small>Nháp, chờ duyệt hoặc cần sửa <ArrowRight size={14} /></small></button>
      <button className="bf-metric bf-metric-confirmed" onClick={() => changeFilter("confirmed", true)}><span><CircleCheck size={17} />Đã xác nhận</span><strong className="bf-green">{confirmed}</strong><small>Nội dung đã được xác nhận <ArrowRight size={14} /></small></button>
      <button className="bf-metric bf-metric-docs" onClick={() => onNavigate("documents")}><span><FileText size={17} />Tài liệu nguồn</span><strong>{documentCount}</strong><small>Xem tài liệu và phiên bản <ArrowRight size={14} /></small></button>
    </div>

    <div className="bf-overview-columns">
      <section ref={listRef} className="bf-task-panel" aria-labelledby="bf-task-heading">
        <div className="bf-panel-heading"><div><h2 id="bf-task-heading">Yêu cầu của dự án</h2><p>Chọn một yêu cầu để mở nội dung song ngữ.</p></div><span className="bf-count">{active.length}</span></div>
        <div className="bf-task-tools">
          <div className="bf-filter-group" aria-label="Lọc yêu cầu">{([["all", "Tất cả"], ["pending", "Cần xem lại"], ["confirmed", "Đã xác nhận"]] as const).map(([value, label]) => <button key={value} aria-pressed={filter === value} onClick={() => changeFilter(value)}>{label}</button>)}</div>
          <label className="bf-task-search"><Search size={16} /><span className="sr-only">Tìm yêu cầu trong tổng quan</span><input value={search} onChange={event => { setSearch(event.target.value); setVisibleCount(6); }} placeholder="Tìm mã, nội dung Nhật–Việt…" />{search && <button type="button" aria-label="Xóa tìm kiếm" onClick={() => { setSearch(""); setVisibleCount(6); }}><X size={15} /></button>}</label>
        </div>
        <div className="bf-task-list">
          {matches.slice(0, visibleCount).map(item => <button className="bf-task-row" key={item.id} onClick={() => onOpenRequirement(item)}>
            <span className="bf-task-symbol"><BookOpenText size={18} /></span>
            <span className="bf-task-copy"><span className="bf-task-meta"><span className="bf-task-key">{item.displayKey}</span><span className={`bf-status bf-status-${item.status.toLowerCase()}`}>{statusLabels[item.status] ?? item.status}</span></span><strong>{item.latestRevision?.vietnameseText || item.latestRevision?.japaneseText || "Chưa có nội dung"}</strong><span className="bf-task-sub">Phiên bản {item.latestRevision?.revisionNumber ?? 0} · Cập nhật {new Date(item.updatedAt).toLocaleDateString("vi-VN")}</span></span>
            <ArrowRight size={16} className="bf-row-arrow" />
          </button>)}
          {!matches.length && <div className="bf-list-empty"><Search size={24} /><h3>{active.length ? "Không có yêu cầu phù hợp" : "Chưa có yêu cầu nào"}</h3><p>{active.length ? "Thử từ khóa khác hoặc đổi bộ lọc." : "Mở tài liệu để bắt đầu, hoặc thêm yêu cầu từ nút phía trên."}</p>{active.length > 0 && <Button variant="outline" onClick={() => { changeFilter("all"); setSearch(""); }}>Đặt lại bộ lọc</Button>}</div>}
        </div>
        <div className="bf-list-footer"><span aria-live="polite">Hiển thị {Math.min(matches.length, visibleCount)} / {matches.length} yêu cầu</span>{matches.length > visibleCount ? <Button size="sm" variant="ghost" onClick={() => setVisibleCount(count => count + 6)}>Xem thêm<ArrowRight /></Button> : <Button size="sm" variant="ghost" onClick={() => onNavigate("requirements")}>Mở trang yêu cầu<ArrowRight /></Button>}</div>
      </section>

      <aside className="bf-overview-rail" aria-label="Tiến độ và hướng dẫn dự án">
        <section className="bf-progress-panel"><div className="bf-panel-heading"><h2>Tiến độ xác nhận</h2><CircleCheck size={18} /></div><div className="bf-progress-summary"><strong>{progress}<span>%</span></strong><span>{confirmed} / {active.length} yêu cầu</span></div><div className="bf-progress-bar" role="progressbar" aria-label="Tỷ lệ yêu cầu đã xác nhận" aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress}><span style={{width: `${progress}%`}} /></div><p><span className="bf-dot" />Đã xác nhận <span className="bf-dot bf-dot-muted" />Chưa xác nhận</p></section>
        <section className="bf-workflow-panel"><div className="bf-panel-heading"><h2>Quy trình làm việc</h2><span className="bf-count">4 bước</span></div><div className="bf-step-list">{steps.map(({icon: Icon, label, description, value, view}, index) => <button key={view} onClick={() => onNavigate(view)}><span className="bf-step-index">{index + 1}</span><span><strong>{label}</strong><small>{description}</small><em><Icon size={12} />{value}</em></span><ArrowRight size={14} /></button>)}</div></section>
        <div className="bf-review-note"><span><Check size={16} /></span><p><strong>Con người quyết định.</strong> Kết quả AI là bản nháp; hãy kiểm tra trước khi xác nhận.</p></div>
      </aside>
    </div>
  </div>;
}

