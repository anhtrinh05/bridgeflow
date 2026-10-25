import type { Metadata } from "next";
import { LandingPage } from "@/components/landing-page";

export const metadata: Metadata = {
  title: "BridgeFlow — Hiểu cùng một yêu cầu. Xây cùng một sản phẩm.",
  description: "Không gian làm việc cho nhóm Nhật–Việt: kết nối tài liệu, thuật ngữ, yêu cầu, câu hỏi và kiểm thử với quy trình AI có người phê duyệt.",
};

export default function Home() {
  return <LandingPage />;
}
