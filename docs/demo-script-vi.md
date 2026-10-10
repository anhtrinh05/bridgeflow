# Kịch Bản Trình Bày Demo BridgeFlow (2–3 Phút)

> **Mục tiêu**: Hướng dẫn BrSE hoặc Kỹ sư trình bày trực tiếp sản phẩm BridgeFlow cho khách hàng, đối tác hoặc nhà tuyển dụng kỹ thuật bằng tiếng Việt.  
> **Thời lượng**: Khoảng 2 phút 30 giây đến 3 phút.  
> **Môi trường**: Trình duyệt truy cập demo qua HTTPS (Cloudflare Quick Tunnel hoặc Local Caddy TLS).

---

## Chuẩn Bị Trước Khi Demo (1 phút chuẩn bị)

1. Khởi động demo stack và lấy URL HTTPS tạm thời:
   ```powershell
   .\scripts\start-demo-tunnel.ps1
   ```
2. Mở trình duyệt tại đường dẫn `https://<random>.trycloudflare.com` được script in ra.
3. Đăng nhập bằng tài khoản operator đã bootstrap:
   - Email: `operator@bridgeflow.local`
   - Mật khẩu: (Mật khẩu được sinh ngẫu nhiên khi chạy script bootstrap).

---

## Kịch Bản Chi Tiết Từng Phút

### Phút 0:00 – 0:30 | Giới thiệu bài toán & Mục tiêu của BridgeFlow

- **Hành động**: Đứng tại màn hình Đăng nhập / Dashboard danh sách dự án.
- **Lời thoại (Pitching)**:
  > *"Xin chào anh/chị. Trong các dự án phát triển phần mềm giữa khách hàng Nhật Bản và đội ngũ kỹ sư Việt Nam, rào cản lớn nhất không chỉ là ngôn ngữ mà là **sự sai lệch về hiểu biết nghiệp vụ, thuật ngữ không đồng nhất, và mất dấu vết khi yêu cầu thay đổi**.  
  > BridgeFlow là không gian làm việc chuyên biệt cho BrSE và PM song ngữ, giải quyết triệt để vấn đề này bằng cách kết hợp AI trích xuất yêu cầu nhưng đặt dưới **sự kiểm soát tuyệt đối của con người (Human-in-the-Loop)**."*

---

### Phút 0:30 – 1:15 | Tải tài liệu tiếng Nhật & Trích xuất AI an toàn (Draft)

- **Hành động**:
  1. Chọn dự án `DEMO-01` (Dự án mẫu tiếng Nhật – tiếng Việt).
  2. Bấm vào tab **Documents** -> Upload tài liệu đặc tả nghiệp vụ tiếng Nhật (`Authentication_Specification_JP.txt`).
  3. Bấm nút **Trích xuất bằng AI (AI Extract)**.
- **Lời thoại**:
  > *"Tại đây, tôi tải lên một tài liệu đặc tả yêu cầu xác thực bằng tiếng Nhật. Hệ thống sẽ băm SHA-256 để đảm bảo tính bất biến của tệp gốc.  
  > Khi kích hoạt AI trích xuất, BridgeFlow tự động lọc bỏ các thông tin nhạy cảm (như email, API key) trước khi xử lý, đồng thời áp dụng nghiêm ngặt bộ từ điển thuật ngữ (Glossary) của dự án.  
  > Quan trọng nhất: **Tất cả các yêu cầu do AI sinh ra ban đầu đều có trạng thái DRAFT (Nháp)**. AI tuyệt đối không bao giờ được phép tự ý phê duyệt yêu cầu vào hệ thống."*

---

### Phút 1:15 – 1:45 | BrSE Review: Xác nhận, Q&A làm rõ, & Sinh Test Case

- **Hành động**:
  1. Mở yêu cầu vừa trích xuất (ví dụ: `REQ-001: ログイン認証 - Đăng nhập hệ thống`).
  2. Chỉnh sửa bản dịch song ngữ nếu cần, sau đó bấm nút **Confirm Revision (Xác nhận phiên bản)**.
  3. Bấm **Analyze & Clarify** -> Hiển thị các câu hỏi làm rõ (Q&A) và Tiêu chí chấp nhận (Acceptance Criteria).
  4. Bấm **Approve Criterion** -> Bấm **Generate Test Cases**.
- **Lời thoại**:
  > *"Là một BrSE, tôi sẽ xem xét bản dịch song ngữ và đối chiếu trực tiếp với vị trí dòng trong tài liệu gốc. Sau khi tôi bấm Xác nhận, yêu cầu mới chính thức được phê duyệt.  
  > Từ yêu cầu đã xác nhận, hệ thống hỗ trợ sinh các câu hỏi làm rõ để gửi khách hàng Nhật, cùng các Tiêu chí chấp nhận (Acceptance Criteria).  
  > Đặc biệt, Test Case chỉ được phép sinh ra từ những tiêu chí đã được con người duyệt (`APPROVED`), đảm bảo QA và Dev không lãng phí nguồn lực kiểm thử những tính năng chưa rõ ràng."*

---

### Phút 1:45 – 2:15 | Ma trận truy vết (Traceability) & Báo cáo tác động thay đổi

- **Hành động**:
  1. Chuyển sang tab **Traceability** của yêu cầu: Quan sát luồng liên kết từ `Tài liệu gốc (SHA-256)` -> `Phiên bản yêu cầu (Rev 1)` -> `Tiêu chí` -> `Test Cases`.
  2. Tạo phiên bản sửa đổi (Revision 2) -> Bấm **Change Impact Report**.
- **Lời thoại**:
  > *"Điểm mạnh vượt trội của BridgeFlow là khả năng **truy vết hai chiều (End-to-End Traceability)**. Mỗi dòng test case hay tiêu chí đều gắn chặt với đúng phiên bản yêu cầu và hash tài liệu nguồn.  
  > Khi khách hàng Nhật cập nhật spec sang bản mới, tính năng **Change Impact Report** sẽ phân tích chính xác những tiêu chí hay test case nào bị ảnh hưởng và cần kiểm thử lại, loại bỏ hoàn toàn nguy cơ sót lỗi do spec bị trôi."*

---

### Phút 2:15 – 2:45 | Độ tin cậy vận hành: Demo 0 đồng & Sao lưu phục hồi

- **Hành động**: Mở terminal hoặc hiển thị sơ đồ vận hành.
- **Lời thoại**:
  > *"Về mặt kỹ thuật và vận hành:  
  > 1. Toàn bộ demo trực tiếp hôm nay đang chạy qua **Cloudflare Quick Tunnel với chi phí 0 đồng**, không cần thuê VPS, không cần mua domain hay trả phí API định kỳ.  
  > 2. Hệ thống backend dùng Spring Boot 4 và PostgreSQL 17 với kiến trúc mạng cô lập hoàn toàn, có sẵn bộ script sao lưu (`backup-production.ps1`) và khôi phục thảm họa (`restore-production.ps1`) đã được kiểm chứng bằng kiểm thử tự động.  
  > 3. Bộ kiểm thử ngoại tuyến (Offline AI Evaluation) đạt 100% độ tuân thủ từ điển và an toàn dữ liệu trên 16 ca kiểm thử tổng hợp."*

---

### Phút 2:45 – 3:00 | Kết luận & Q&A

- **Lời thoại**:
  > *"Tóm lại, BridgeFlow mang đến một quy trình làm việc chuẩn mực, an toàn và minh bạch cho các dự án phần mềm Nhật - Việt. Cảm ơn anh/chị đã theo dõi và tôi rất sẵn lòng giải đáp mọi câu hỏi chuyên sâu về kiến trúc cũng như sản phẩm."*

---

## Dọn Dẹp Sau Khi Demo

Khi buổi demo kết thúc, tắt đường hầm và dọn dẹp dữ liệu tạm:
```powershell
.\scripts\stop-demo-tunnel.ps1 -RemoveData
```
Toàn bộ container, mạng và volume tạm sẽ được giải phóng an toàn.
