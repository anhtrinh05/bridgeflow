export const workspaceMeta = {
  overview: { label: "Tổng quan", title: "Tổng quan dự án", description: "Không gian làm việc chung cho tài liệu, yêu cầu và quyết định của nhóm." },
  documents: { label: "Tài liệu", title: "Tài liệu dự án", description: "Quản lý file nguồn và lịch sử version của từng tài liệu." },
  glossary: { label: "Thuật ngữ", title: "Thuật ngữ song ngữ", description: "Chuẩn hóa thuật ngữ Nhật–Việt dùng chung trong toàn bộ project." },
  requirements: { label: "Yêu cầu", title: "Phân tích yêu cầu", description: "Đối chiếu nội dung song ngữ, chỉnh sửa và xác nhận từng revision." },
  questions: { label: "Hỏi đáp", title: "Q&A & tiêu chí chấp nhận", description: "Chọn yêu cầu để trả lời câu hỏi và duyệt tiêu chí của phiên bản hiện tại." },
  "test-cases": { label: "Kiểm thử", title: "Kịch bản kiểm thử", description: "Chọn yêu cầu để tạo và duyệt kịch bản kiểm thử từ tiêu chí đã duyệt." },
  changes: { label: "Thay đổi", title: "Thay đổi & truy vết", description: "Xem tác động thay đổi, các revision và quan hệ giữa requirements." },
  members: { label: "Thành viên", title: "Thành viên dự án", description: "Danh sách thành viên và vai trò được cấp trong dự án này." },
  settings: { label: "Cài đặt", title: "Cài đặt dự án", description: "Thông tin project, quyền truy cập và lựa chọn xử lý AI." },
  activity: { label: "Hoạt động AI", title: "Lịch sử xử lý AI", description: "Theo dõi kết quả các lần xử lý AI và mã tra cứu của project." },
} as const;

export type WorkspaceView = keyof typeof workspaceMeta;
export type RequirementTab = "analysis" | "artifacts" | "test-cases" | "traceability" | "history";

export function defaultRequirementTab(view: WorkspaceView): RequirementTab {
  if (view === "questions") return "artifacts";
  if (view === "test-cases") return "test-cases";
  if (view === "changes") return "traceability";
  return "analysis";
}

export function readWorkspaceLocation(hash: string): { view: WorkspaceView; tab: RequirementTab } {
  const params = new URLSearchParams(hash.replace(/^#/, ""));
  const requestedView = params.get("view");
  const view: WorkspaceView = requestedView && Object.hasOwn(workspaceMeta, requestedView)
    ? requestedView as WorkspaceView : "overview";
  const requestedTab = params.get("tab");
  const tab = ["analysis", "artifacts", "test-cases", "traceability", "history"].includes(requestedTab ?? "")
    ? requestedTab as RequirementTab : defaultRequirementTab(view);
  return { view, tab };
}
