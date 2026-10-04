"use client";

import { useState } from "react";
import {
  Bell, BookOpenText, Check, ChevronDown, CircleHelp, FileText,
  FolderKanban, GitCompareArrows, Languages, LayoutDashboard,
  MessageSquareText, MoreHorizontal, PanelLeftClose, Plus, Search,
  Settings, Sparkles, TestTube2, Users,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const requirements = [
  {
    id: "REQ-014", title: "承認済み申請の編集制限", vi: "Giới hạn chỉnh sửa đơn đã được phê duyệt", status: "Cần xác nhận", tone: "amber", module: "Đơn đăng ký",
    page: 24, confidence: 96,
    jpLead: "承認済みの申請については、管理者のみ編集可能とする。",
    jpNote: "ただし、申請者による取消の場合の動作については別途協議とする。",
    viLead: "Đối với đơn đã được phê duyệt, chỉ quản trị viên mới có quyền chỉnh sửa.",
    viNote: "Tuy nhiên, hành vi của hệ thống khi người nộp đơn thực hiện hủy sẽ được trao đổi riêng.",
    ambiguity: "Khi người nộp đơn hủy một đơn đã được phê duyệt, trạng thái cuối cùng và quyền chỉnh sửa của quản trị viên chưa được xác định.",
    clarificationJa: "承認済み申請を申請者が取り消した場合、ステータスおよび管理者の編集権限はどのようになりますでしょうか。",
    priority: "Ưu tiên cao", testCases: 2,
    criteria: ["Chỉ người dùng có vai trò Admin được chỉnh sửa đơn đã phê duyệt.", "Mọi chỉnh sửa phải được lưu trong lịch sử thay đổi.", "Người dùng không có quyền nhận thông báo rõ ràng khi thao tác."],
    history: "Requirement được cập nhật từ đặc tả v2.4 vào hôm nay lúc 09:42.",
  },
  {
    id: "REQ-015", title: "申請履歴のCSV出力", vi: "Xuất lịch sử đăng ký dưới dạng CSV", status: "Đã xác nhận", tone: "emerald", module: "Báo cáo",
    page: 31, confidence: 98,
    jpLead: "管理者は、指定した期間の申請履歴をCSV形式で出力できるものとする。",
    jpNote: "出力項目は申請番号、申請者、申請日時、現在のステータスとする。",
    viLead: "Quản trị viên có thể xuất lịch sử đăng ký trong khoảng thời gian chỉ định dưới dạng CSV.",
    viNote: "Các trường xuất gồm mã đơn, người nộp, thời gian nộp và trạng thái hiện tại.",
    ambiguity: "Quy tắc mã hóa ký tự của file CSV chưa được ghi rõ, có thể ảnh hưởng khi mở bằng Excel phiên bản tiếng Nhật.",
    clarificationJa: "CSVファイルの文字コードはUTF-8（BOM付き）でよろしいでしょうか。",
    priority: "Ưu tiên vừa", testCases: 3,
    criteria: ["Chỉ Admin nhìn thấy chức năng xuất CSV.", "Khoảng thời gian xuất là bắt buộc và không vượt quá 12 tháng.", "File CSV chứa đúng các trường đã được xác nhận."],
    history: "Requirement đã được khách hàng xác nhận trong Q&A-009 vào hôm qua lúc 16:18.",
  },
  {
    id: "REQ-016", title: "権限別の表示項目制御", vi: "Kiểm soát trường hiển thị theo quyền", status: "Đang phân tích", tone: "blue", module: "Phân quyền",
    page: 18, confidence: 91,
    jpLead: "ユーザーの権限に応じて、申請詳細画面に表示する項目を制御する。",
    jpNote: "権限と表示項目の対応は権限マトリクスを参照すること。",
    viLead: "Kiểm soát các trường hiển thị trên màn hình chi tiết đơn theo quyền của người dùng.",
    viNote: "Tham chiếu ma trận phân quyền để xác định quan hệ giữa vai trò và trường hiển thị.",
    ambiguity: "Tài liệu đang tham chiếu ma trận phân quyền nhưng chưa có phiên bản hoặc đường dẫn tới tài liệu nguồn.",
    clarificationJa: "参照対象となる権限マトリクスのファイル名および最新版をご共有いただけますでしょうか。",
    priority: "Ưu tiên cao", testCases: 0,
    criteria: ["Mỗi vai trò chỉ nhìn thấy các trường được cấp phép.", "API không trả về dữ liệu của trường bị hạn chế.", "Thay đổi vai trò được áp dụng sau lần đăng nhập tiếp theo."],
    history: "AI phát hiện liên kết tài liệu còn thiếu vào hôm nay lúc 09:44.",
  },
  {
    id: "REQ-017", title: "通知メールの送信条件", vi: "Điều kiện gửi email thông báo", status: "Đã xác nhận", tone: "emerald", module: "Thông báo",
    page: 37, confidence: 97,
    jpLead: "申請のステータスが承認または却下に変更された場合、申請者へ通知メールを送信する。",
    jpNote: "同一申請に対する重複送信を防止すること。",
    viLead: "Gửi email cho người nộp khi trạng thái đơn chuyển thành đã duyệt hoặc bị từ chối.",
    viNote: "Hệ thống phải ngăn việc gửi trùng thông báo cho cùng một đơn.",
    ambiguity: "Cơ chế gửi lại khi dịch vụ email tạm thời thất bại cần được xác nhận để tránh mất thông báo.",
    clarificationJa: "メール送信に失敗した場合、再送回数および再送間隔に指定はございますでしょうか。",
    priority: "Ưu tiên vừa", testCases: 4,
    criteria: ["Email được gửi khi trạng thái chuyển sang Approved hoặc Rejected.", "Một lần chuyển trạng thái chỉ tạo tối đa một email.", "Lỗi gửi email được ghi nhận để có thể xử lý lại."],
    history: "Requirement đã được xác nhận và liên kết với Q&A-012 vào hôm nay lúc 08:55.",
  },
];

const nav = [
  [LayoutDashboard, "Tổng quan"], [FileText, "Tài liệu", "12"],
  [BookOpenText, "Requirements", "34"], [MessageSquareText, "Q&A", "7"],
  [TestTube2, "Test cases"], [GitCompareArrows, "Thay đổi"],
] as const;

function StatusBadge({ tone, children }: { tone: string; children: React.ReactNode }) {
  const colors: Record<string, string> = {
    amber: "border-amber-200 bg-amber-50 text-amber-700",
    emerald: "border-emerald-200 bg-emerald-50 text-emerald-700",
    blue: "border-blue-200 bg-blue-50 text-blue-700",
  };
  return <Badge variant="outline" className={`font-medium ${colors[tone]}`}><span className="size-1.5 rounded-full bg-current" />{children}</Badge>;
}

export default function Home() {
  const [selected, setSelected] = useState(requirements[0]);

  return (
    <main className="min-h-screen bg-[#f4f6f8] text-slate-950">
      <header className="sticky top-0 z-30 flex h-16 items-center border-b border-slate-200 bg-white px-4 lg:px-6">
        <div className="flex w-64 items-center gap-3">
          <div className="grid size-9 place-items-center rounded-xl bg-[#123a63] text-white shadow-sm"><Languages className="size-5" /></div>
          <div><p className="text-base font-bold tracking-tight">BridgeFlow</p><p className="text-[11px] font-medium tracking-wide text-slate-500">JP × VN REQUIREMENTS</p></div>
        </div>
        <div className="ml-auto flex items-center gap-2">
          <Button variant="outline" size="sm" className="hidden border-slate-200 sm:flex"><Search /> Tìm kiếm <kbd className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-500">⌘K</kbd></Button>
          <Button variant="ghost" size="icon-sm" aria-label="Thông báo"><Bell /></Button>
          <div className="ml-1 grid size-8 place-items-center rounded-full bg-[#e8eef5] text-xs font-bold text-[#123a63]">TN</div>
        </div>
      </header>

      <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-[1800px]">
        <aside className="hidden w-64 shrink-0 border-r border-slate-200 bg-[#f8fafc] p-4 lg:flex lg:flex-col">
          <button className="mb-5 flex w-full items-center gap-3 rounded-xl border border-slate-200 bg-white p-3 text-left shadow-sm transition hover:border-slate-300">
            <div className="grid size-9 place-items-center rounded-lg bg-[#e8f0f7] text-sm font-bold text-[#123a63]">EC</div>
            <div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold">EC Portal Renewal</p><p className="text-xs text-slate-500">株式会社みらい</p></div>
            <ChevronDown className="size-4 text-slate-400" />
          </button>
          <nav className="space-y-1" aria-label="Điều hướng dự án">
            {nav.map(([Icon, label, count]) => (
              <button key={label} className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${label === "Requirements" ? "bg-[#e7eff7] text-[#123a63]" : "text-slate-600 hover:bg-white hover:text-slate-950"}`}>
                <Icon className="size-[18px]" /><span>{label}</span>{count && <span className="ml-auto rounded-md bg-white/80 px-1.5 py-0.5 text-[11px] text-slate-500">{count}</span>}
              </button>
            ))}
          </nav>
          <div className="mt-6 border-t border-slate-200 pt-5">
            <p className="mb-2 px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">Không gian làm việc</p>
            <button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-white"><Users className="size-[18px]" /> Thành viên</button>
            <button className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-white"><Settings className="size-[18px]" /> Cài đặt</button>
          </div>
          <div className="mt-auto rounded-xl border border-[#dce7f1] bg-[#edf4fa] p-3.5">
            <div className="mb-2 flex items-center gap-2 text-[#123a63]"><Sparkles className="size-4" /><span className="text-xs font-bold">AI processing</span><span className="ml-auto text-xs">78%</span></div>
            <div className="h-1.5 overflow-hidden rounded-full bg-white"><div className="h-full w-[78%] rounded-full bg-[#2878ad]" /></div>
            <p className="mt-2 text-[11px] leading-4 text-slate-500">Đang phân tích đặc tả v2.4</p>
          </div>
        </aside>

        <section className="min-w-0 flex-1 p-4 md:p-6 xl:p-8">
          <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div className="mb-2 flex items-center gap-2 text-xs font-medium text-slate-500"><FolderKanban className="size-3.5" /> EC Portal Renewal <span>/</span> Requirements</div>
              <h1 className="text-2xl font-bold tracking-tight md:text-3xl">Phân tích yêu cầu</h1>
              <p className="mt-1 text-sm text-slate-500">Đồng bộ lần cuối từ EC基本設計書_v2.4.pdf · 10 phút trước</p>
            </div>
            <div className="flex gap-2"><Button variant="outline" className="border-slate-200 bg-white"><PanelLeftClose /> Traceability</Button><Button className="bg-[#123a63] hover:bg-[#0d2e50]"><Plus /> Thêm yêu cầu</Button></div>
          </div>

          <div className="grid gap-4 xl:grid-cols-[minmax(350px,0.92fr)_minmax(520px,1.45fr)]">
            <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_1px_2px_rgba(15,23,42,.03)]">
              <div className="flex items-center gap-2 border-b border-slate-200 p-3">
                <div className="relative min-w-0 flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><input aria-label="Tìm requirement" placeholder="Tìm requirement..." className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm outline-none transition focus:border-[#2878ad] focus:bg-white focus:ring-2 focus:ring-[#2878ad]/10" /></div>
                <Button variant="outline" size="sm" className="border-slate-200">Trạng thái <ChevronDown /></Button>
              </div>
              <div className="divide-y divide-slate-100">
                {requirements.map((item) => (
                  <button key={item.id} onClick={() => setSelected(item)} className={`w-full p-4 text-left transition hover:bg-slate-50 ${selected.id === item.id ? "border-l-[3px] border-[#2878ad] bg-[#f2f7fb] pl-[13px]" : "border-l-[3px] border-transparent pl-[13px]"}`}>
                    <div className="mb-2 flex items-center justify-between gap-2"><span className="font-mono text-xs font-semibold text-[#2878ad]">{item.id}</span><StatusBadge tone={item.tone}>{item.status}</StatusBadge></div>
                    <p lang="ja" className="line-clamp-1 text-sm font-semibold text-slate-900">{item.title}</p><p className="mt-1 line-clamp-1 text-sm text-slate-500">{item.vi}</p><p className="mt-2 text-xs text-slate-400">{item.module}</p>
                  </button>
                ))}
              </div>
            </section>

            <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_1px_2px_rgba(15,23,42,.03)]">
              <div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4"><span className="font-mono text-sm font-bold text-[#2878ad]">{selected.id}</span><StatusBadge tone={selected.tone}>{selected.status}</StatusBadge><Button variant="ghost" size="icon-sm" className="ml-auto" aria-label="Thêm thao tác"><MoreHorizontal /></Button></div>
              <Tabs defaultValue="analysis" className="gap-0">
                <TabsList variant="line" className="h-12 w-full justify-start gap-5 overflow-x-auto border-b border-slate-200 px-5"><TabsTrigger value="analysis" className="flex-none px-0">Phân tích song ngữ</TabsTrigger><TabsTrigger value="acceptance" className="flex-none px-0">Acceptance criteria</TabsTrigger><TabsTrigger value="history" className="flex-none px-0">Lịch sử</TabsTrigger></TabsList>
                <TabsContent value="analysis" className="p-5 md:p-6">
                  <div className="grid gap-4 md:grid-cols-2">
                    <article className="rounded-xl border border-slate-200 bg-slate-50/70 p-4"><div className="mb-3 flex items-center justify-between"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">原文 · Tiếng Nhật</span><Badge variant="outline" className="border-slate-200 bg-white text-slate-500">Trang {selected.page}</Badge></div><p lang="ja" className="text-[15px] font-semibold leading-7 text-slate-900">{selected.jpLead}</p><p lang="ja" className="mt-2 text-sm leading-6 text-slate-600">{selected.jpNote}</p></article>
                    <article className="rounded-xl border border-[#dbe7f1] bg-[#f4f8fb] p-4"><div className="mb-3 flex items-center justify-between"><span className="text-xs font-bold uppercase tracking-wider text-[#436987]">Bản dịch · Tiếng Việt</span><Badge variant="outline" className="border-[#cfdfec] bg-white text-[#436987]"><Sparkles /> AI {selected.confidence}%</Badge></div><p className="text-[15px] font-semibold leading-7 text-slate-900">{selected.viLead}</p><p className="mt-2 text-sm leading-6 text-slate-600">{selected.viNote}</p></article>
                  </div>
                  <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50/70 p-4"><div className="flex items-start gap-3"><div className="mt-0.5 grid size-8 shrink-0 place-items-center rounded-lg bg-amber-100 text-amber-700"><CircleHelp className="size-4" /></div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="text-sm font-bold text-amber-950">Điểm cần xác nhận với khách hàng</h2><Badge className="bg-amber-200 text-amber-900 hover:bg-amber-200">{selected.priority}</Badge></div><p className="mt-2 text-sm leading-6 text-amber-950/80">{selected.ambiguity}</p><div className="mt-3 rounded-lg border border-amber-200 bg-white p-3"><p className="mb-1 text-[11px] font-bold uppercase tracking-wider text-amber-700">確認事項案</p><p lang="ja" className="text-sm leading-6 text-slate-800">{selected.clarificationJa}</p></div></div></div></div>
                  <div className="mt-5 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:items-center"><div className="flex items-center gap-2 text-sm text-slate-500"><Check className="size-4 text-emerald-600" /> Đã liên kết với {selected.testCases} test cases</div><div className="ml-auto flex gap-2"><Button variant="outline" className="border-slate-200">Chỉnh sửa</Button><Button className="bg-[#2878ad] hover:bg-[#226994]">Tạo Q&A tiếng Nhật</Button></div></div>
                </TabsContent>
                <TabsContent value="acceptance" className="p-6"><h2 className="text-sm font-bold">Acceptance criteria</h2><ul className="mt-4 space-y-3 text-sm text-slate-600">{selected.criteria.map((text) => <li key={text} className="flex gap-3 rounded-lg border border-slate-200 p-3"><Check className="mt-0.5 size-4 shrink-0 text-emerald-600" />{text}</li>)}</ul></TabsContent>
                <TabsContent value="history" className="p-6 text-sm text-slate-500">{selected.history}</TabsContent>
              </Tabs>
            </section>
          </div>
        </section>
      </div>
    </main>
  );
}
