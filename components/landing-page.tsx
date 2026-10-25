"use client";

import Link from "next/link";

import { useEffect, useRef, useState } from "react";
import { ArrowDown, ArrowRight, BookOpenText, Check, FileText, GitCompareArrows, Languages, Menu, MessageSquareText, ShieldCheck, Sparkles, TestTube2, X } from "lucide-react";
import { SmoothScrollProvider } from "@/components/smooth-scroll-provider";
import "./landing-page.css";

/* ═══════════════════════════════════════════════════════════════
   Flow Network Canvas — Natural Ambient Gemstone Constellation
   Particles float freely, randomly, and peacefully across space
   like glowing stardust with gentle breathing pulses and delicate
   interconnecting filaments.
   ═══════════════════════════════════════════════════════════════ */
function FlowCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animRef = useRef<number>(0);
  const mouseRef = useRef<{ x: number; y: number; active: boolean }>({ x: -1000, y: -1000, active: false });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (prefersReduced) return;

    let width = 0;
    let height = 0;

    function resize() {
      const dpr = window.devicePixelRatio || 1;
      width = window.innerWidth;
      height = window.innerHeight;
      canvas!.width = width * dpr;
      canvas!.height = height * dpr;
      ctx!.scale(dpr, dpr);
    }
    resize();
    window.addEventListener("resize", resize);

    function onMouseMove(e: MouseEvent) {
      mouseRef.current = { x: e.clientX, y: e.clientY, active: true };
    }
    function onMouseLeave() {
      mouseRef.current.active = false;
    }
    window.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseleave", onMouseLeave);

    // ── 1. Ambient Background Flow Curves ──────────────────────
    const numPaths = Math.min(36, Math.max(18, Math.floor(width / 40)));
    interface FlowPath {
      isLeft: boolean;
      startY: number;
      particles: { t: number; speed: number }[];
    }
    const paths: FlowPath[] = [];
    for (let i = 0; i < numPaths; i++) {
      paths.push({
        isLeft: i % 2 === 0,
        startY: (i / numPaths) * height * 1.3 - height * 0.15,
        particles: [{
          t: Math.random(),
          speed: 0.0006 + Math.random() * 0.0008,
        }],
      });
    }

    // ── 2. Natural Random Gemstone Particles ───────────────────
    const numGems = Math.min(70, Math.max(35, Math.floor((width * height) / 16000)));

    interface NaturalGem {
      x: number;
      y: number;
      vx: number;
      vy: number;
      radius: number;
      glowRadius: number;
      phase: number;
      wanderSpeed: number;
      r: number;
      g: number;
      b: number;
    }

    const palette = [
      { r: 167, g: 243, b: 207 }, // Mint
      { r: 85,  g: 214, b: 194 }, // Soft Cyan
      { r: 45,  g: 212, b: 191 }, // Teal
      { r: 190, g: 248, b: 238 }, // Aqua Ice
    ];

    const gems: NaturalGem[] = [];
    for (let i = 0; i < numGems; i++) {
      const col = palette[i % palette.length];
      const speed = 0.2 + Math.random() * 0.4;
      const angle = Math.random() * Math.PI * 2;
      gems.push({
        x: Math.random() * width,
        y: Math.random() * height,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        radius: 1.4 + Math.random() * 2.0, // 1.4px - 3.4px
        glowRadius: 6 + Math.random() * 10,
        phase: Math.random() * Math.PI * 2,
        wanderSpeed: 0.008 + Math.random() * 0.012,
        r: col.r,
        g: col.g,
        b: col.b,
      });
    }

    function getBezierPoint(t: number, p0: { x: number; y: number }, p1: { x: number; y: number }, p2: { x: number; y: number }, p3: { x: number; y: number }) {
      const u = 1 - t;
      return {
        x: u ** 3 * p0.x + 3 * u ** 2 * t * p1.x + 3 * u * t ** 2 * p2.x + t ** 3 * p3.x,
        y: u ** 3 * p0.y + 3 * u ** 2 * t * p1.y + 3 * u * t ** 2 * p2.y + t ** 3 * p3.y,
      };
    }

    let time = 0;

    function render() {
      time += 0.012;
      ctx!.clearRect(0, 0, width, height);

      const mx = mouseRef.current.x;
      const my = mouseRef.current.y;
      const hasMouse = mouseRef.current.active;

      // ── Soft Ambient Cursor Halo ──────────────────────
      if (hasMouse) {
        const aura = ctx!.createRadialGradient(mx, my, 0, mx, my, 120);
        aura.addColorStop(0, "rgba(85, 214, 194, 0.06)");
        aura.addColorStop(0.5, "rgba(85, 214, 194, 0.015)");
        aura.addColorStop(1, "transparent");
        ctx!.fillStyle = aura;
        ctx!.fillRect(mx - 120, my - 120, 240, 240);
      }

      // ── Ambient Background Flow Paths ─────────────────
      const centerX = width * 0.5;
      const centerY = height * 0.42;
      paths.forEach((path) => {
        const p0 = { x: path.isLeft ? -10 : width + 10, y: path.startY };
        const p1 = { x: path.isLeft ? centerX * 0.4 : width - centerX * 0.4, y: path.startY };
        const p2 = { x: path.isLeft ? centerX * 0.75 : width - centerX * 0.75, y: centerY };
        const p3 = { x: centerX, y: centerY };

        ctx!.beginPath();
        ctx!.moveTo(p0.x, p0.y);
        ctx!.bezierCurveTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
        ctx!.strokeStyle = "rgba(85, 214, 194, 0.02)";
        ctx!.lineWidth = 0.5;
        ctx!.setLineDash([1, 8]);
        ctx!.stroke();
        ctx!.setLineDash([]);

        path.particles.forEach((p) => {
          p.t += p.speed;
          if (p.t > 1) {
            p.t = 0;
            path.startY += (Math.random() - 0.5) * 6;
          }
          const pos = getBezierPoint(p.t, p0, p1, p2, p3);
          ctx!.fillStyle = "rgba(167, 243, 207, 0.18)";
          ctx!.fillRect(pos.x - 0.75, pos.y - 0.75, 1.5, 1.5);
        });
      });

      // ── Update and Render Natural Gemstones ────────────
      gems.forEach((gem) => {
        // Natural curved wander (gentle sine wave drift)
        gem.x += gem.vx + Math.sin(time * 0.8 + gem.phase) * 0.25;
        gem.y += gem.vy + Math.cos(time * 0.7 + gem.phase) * 0.25;

        // Subtle feather-light mouse interaction (part gently when cursor is close)
        if (hasMouse) {
          const dmx = gem.x - mx;
          const dmy = gem.y - my;
          const dDist = Math.hypot(dmx, dmy);
          if (dDist < 140 && dDist > 0) {
            const force = (1 - dDist / 140) * 0.008;
            gem.x += (dmx / dDist) * force * 15;
            gem.y += (dmy / dDist) * force * 15;
          }
        }

        // Soft bounce / wrap at boundaries
        const margin = 20;
        if (gem.x < -margin) gem.x = width + margin;
        if (gem.x > width + margin) gem.x = -margin;
        if (gem.y < -margin) gem.y = height + margin;
        if (gem.y > height + margin) gem.y = -margin;

        // Breathing pulse
        const pulse = 0.75 + Math.sin(time * 1.5 + gem.phase) * 0.25;

        // Soft multi-stop glow
        const glow = ctx!.createRadialGradient(gem.x, gem.y, 0, gem.x, gem.y, gem.glowRadius * pulse);
        glow.addColorStop(0, `rgba(${gem.r}, ${gem.g}, ${gem.b}, ${0.75 * pulse})`);
        glow.addColorStop(0.3, `rgba(${gem.r}, ${gem.g}, ${gem.b}, ${0.2 * pulse})`);
        glow.addColorStop(0.7, `rgba(${gem.r}, ${gem.g}, ${gem.b}, ${0.03 * pulse})`);
        glow.addColorStop(1, "transparent");

        ctx!.fillStyle = glow;
        ctx!.beginPath();
        ctx!.arc(gem.x, gem.y, gem.glowRadius * pulse, 0, Math.PI * 2);
        ctx!.fill();

        // Pearl core
        ctx!.beginPath();
        ctx!.arc(gem.x, gem.y, gem.radius * 0.7, 0, Math.PI * 2);
        ctx!.fillStyle = `rgba(255, 255, 255, ${0.8 * pulse})`;
        ctx!.fill();
      });

      // ── Delicate Constellation Links Between Nearby Gems ─
      const connectDist = 110;
      for (let i = 0; i < gems.length; i++) {
        for (let j = i + 1; j < gems.length; j++) {
          const d = Math.hypot(gems[i].x - gems[j].x, gems[i].y - gems[j].y);
          if (d < connectDist) {
            const alpha = Math.pow(1 - d / connectDist, 2) * 0.12;
            ctx!.beginPath();
            ctx!.moveTo(gems[i].x, gems[i].y);
            ctx!.lineTo(gems[j].x, gems[j].y);
            ctx!.strokeStyle = `rgba(85, 214, 194, ${alpha})`;
            ctx!.lineWidth = 0.5;
            ctx!.stroke();
          }
        }
      }

      animRef.current = requestAnimationFrame(render);
    }

    render();

    return () => {
      cancelAnimationFrame(animRef.current);
      window.removeEventListener("resize", resize);
      window.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseleave", onMouseLeave);
    };
  }, []);

  return <canvas ref={canvasRef} className="bf-flow-canvas" aria-hidden="true" />;
}

/* ═══════════════════════════════════════════════════════════════
   Content Data
   ═══════════════════════════════════════════════════════════════ */
const examples = [
  { label: "Yêu cầu", icon: BookOpenText, heading: "Một ý nghĩa. Hai ngôn ngữ.", ja: "ユーザーは注文履歴を確認できること。", vi: "Người dùng có thể xem lịch sử đơn hàng.", note: "Thuật ngữ chung: 注文履歴 → Lịch sử đơn hàng", status: "Bản nháp · Chờ review" },
  { label: "Làm rõ", icon: MessageSquareText, heading: "Hỏi rõ trước khi bắt đầu xây.", ja: "注文履歴はどの期間まで表示しますか？", vi: "Lịch sử đơn hàng cần hiển thị trong khoảng thời gian nào?", note: "Câu trả lời của người phụ trách giúp thống nhất phạm vi triển khai.", status: "Câu hỏi · Chờ phản hồi" },
  { label: "Kiểm thử", icon: TestTube2, heading: "Từ yêu cầu đến bằng chứng.", ja: "ログインしたユーザーに、自分の注文履歴のみが表示されること。", vi: "Người dùng đã đăng nhập chỉ nhìn thấy lịch sử đơn hàng của chính mình.", note: "Kịch bản kiểm thử được liên kết với phiên bản yêu cầu tương ứng.", status: "Test case · Chờ duyệt" },
];

const steps = [
  { icon: FileText, title: "Tập hợp tài liệu", body: "Lưu tài liệu nguồn và từng phiên bản. Chuẩn hóa thuật ngữ Nhật–Việt ngay từ đầu." },
  { icon: Languages, title: "Thống nhất yêu cầu", body: "Đọc song song nguyên văn và bản dịch. AI hỗ trợ tạo bản nháp để người phụ trách kiểm tra." },
  { icon: MessageSquareText, title: "Làm rõ & phê duyệt", body: "Ghi câu hỏi, câu trả lời và tiêu chí nghiệm thu cùng yêu cầu mà chúng giải thích." },
  { icon: GitCompareArrows, title: "Kiểm thử & truy vết", body: "Liên kết test case, xem lịch sử thay đổi và xuất nội dung đã được review cho cả nhóm." },
];

/* ═══════════════════════════════════════════════════════════════
   Landing Page Component
   ═══════════════════════════════════════════════════════════════ */
export function LandingPage() {
  const [example, setExample] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  // Preserve bookmarks from the original workspace at the root URL.
  useEffect(() => {
    function restoreWorkspaceLink() {
      if (new URLSearchParams(window.location.hash.slice(1)).has("view")) {
        window.location.replace(`/workspace${window.location.hash}`);
      }
    }
    restoreWorkspaceLink();
    window.addEventListener("hashchange", restoreWorkspaceLink);
    return () => window.removeEventListener("hashchange", restoreWorkspaceLink);
  }, []);
  const current = examples[example];
  return (
    <SmoothScrollProvider>
      <div className="bf-landing">
        {/* ── Canvas Background ────────────────────────────── */}
        <FlowCanvas />

    <a className="bf-skip" href="#noi-dung">Đi tới nội dung</a>
    <header className="bf-landing-header">
      <Link href="/" className="bf-wordmark" aria-label="BridgeFlow — Trang chủ"><span className="bridgeflow-brand-mark"><Languages size={20} /></span>BridgeFlow<span className="bf-brand-caption">NHẬT × VIỆT</span></Link>
      <nav className="bf-desktop-nav" aria-label="Điều hướng giới thiệu"><a href="#cach-hoat-dong">Cách hoạt động</a><a href="#tinh-nang">Tính năng</a><a href="#cau-hoi">Câu hỏi</a></nav>
      <Link className="bf-link-login" href="/workspace">Vào workspace <ArrowRight size={16} /></Link>
      <button className="bf-menu-toggle" aria-label={menuOpen ? "Đóng menu" : "Mở menu"} aria-expanded={menuOpen} aria-controls="landing-mobile-menu" onClick={() => setMenuOpen(!menuOpen)}>{menuOpen ? <X /> : <Menu />}</button>
      {menuOpen && <nav id="landing-mobile-menu" className="bf-mobile-nav" aria-label="Điều hướng trên điện thoại">{[["#cach-hoat-dong", "Cách hoạt động"], ["#tinh-nang", "Tính năng"], ["#cau-hoi", "Câu hỏi"], ["/workspace", "Vào workspace"]].map(([href, label]) => <a key={href} href={href} onClick={() => setMenuOpen(false)}>{label}<ArrowRight size={16} /></a>)}</nav>}
    </header>

    <main id="noi-dung">
      {/* ── Hero ────────────────────────────────────────── */}
      <section className="bf-hero bf-section">
        <div className="bf-hero-copy">
          <p className="bf-eyebrow"><span /> KẾT NỐI NHÓM PHÁT TRIỂN NHẬT–VIỆT</p>
          <h1>Hiểu cùng một yêu cầu.<br /><em>Xây cùng một sản phẩm.</em></h1>
          <p className="bf-hero-description">Khoảng cách ngôn ngữ không nên trở thành khoảng cách trong sản phẩm. Đưa tài liệu, bản dịch và mọi quyết định về cùng một không gian làm việc.</p>
          <div className="bf-hero-actions"><Link href="/workspace" className="bf-cta">Khám phá workspace <ArrowRight size={18} /></Link><a href="#trai-nghiem" className="bf-secondary-link">Xem luồng minh họa <ArrowDown size={16} /></a></div>
          <p className="bf-hero-footnote"><ShieldCheck size={16} /> AI hỗ trợ. Người phụ trách quyết định.</p>
        </div>
        <div className="bf-hero-art" aria-label="Minh họa kết nối yêu cầu tiếng Nhật và tiếng Việt">
          <div className="bf-orbit bf-orbit-one" aria-hidden="true" /><div className="bf-orbit bf-orbit-two" aria-hidden="true" />
          <div className="bf-language-sheet bf-sheet-ja"><span className="bf-sheet-label">01 / NGUYÊN VĂN</span><span className="bf-language-symbol" lang="ja">要件</span><p lang="ja">ユーザーは注文履歴を<br />確認できること。</p><span className="bf-sheet-tag">JP · Tài liệu nguồn</span></div>
          <div className="bf-language-sheet bf-sheet-vi"><span className="bf-sheet-label">02 / CÙNG CÁCH HIỂU</span><span className="bf-language-symbol">Yêu cầu</span><p>Người dùng có thể xem<br />lịch sử đơn hàng.</p><span className="bf-sheet-tag"><Check size={12} /> VN · Bản dịch minh họa</span></div>
          <div className="bf-bridge-node" aria-hidden="true"><Languages size={28} /></div>
          <p className="bf-art-caption">HAI NGÔN NGỮ <span /> MỘT NGUỒN THÔNG TIN CHUNG</p>
        </div>
      </section>

      {/* ── Value Strip ─────────────────────────────────── */}
      <div className="bf-value-strip"><div><Languages size={18} />Song ngữ từ đầu</div><div><ShieldCheck size={18} />Review bởi con người</div><div><GitCompareArrows size={18} />Truy vết từng phiên bản</div></div>

      {/* ── Process ─────────────────────────────────────── */}
      <section id="cach-hoat-dong" className="bf-section bf-process">
        <div className="bf-section-heading"><p className="bf-eyebrow">01 / MỘT LUỒNG LÀM VIỆC LIỀN MẠCH</p><h2>Từ tài liệu đầu tiên<br />đến yêu cầu sẵn sàng triển khai.</h2><p>Mỗi bước giữ lại ngữ cảnh cho bước tiếp theo. Cả nhóm biết nội dung đến từ đâu, đã được làm rõ thế nào và ai cần review.</p></div>
        <div className="bf-steps">{steps.map(({ icon: Icon, title, body }, i) => <article key={title}><div className="bf-step-top"><Icon size={22} /><span>0{i + 1}</span></div><h3>{title}</h3><p>{body}</p></article>)}</div>
      </section>

      {/* ── Demo ────────────────────────────────────────── */}
      <section id="trai-nghiem" className="bf-demo-section">
        <div className="bf-section bf-demo-inner">
          <div className="bf-demo-intro"><p className="bf-eyebrow">02 / TỪ MƠ HỒ ĐẾN RÕ RÀNG</p><h2>Một yêu cầu.<br />Đủ ngữ cảnh để bắt đầu.</h2><p>Chọn từng bước để xem cách nhóm cùng đọc, đặt câu hỏi và xác định điều cần kiểm thử.</p><p className="bf-demo-disclaimer">Ví dụ minh họa cố định. Không chạy AI hoặc thay đổi dữ liệu dự án.</p></div>
          <div className="bf-demo-card">
            <div className="bf-demo-toolbar"><span><span className="bf-dot" /> BRIDGEFLOW / MINH HỌA</span><span>REQ-001</span></div>
            <div className="bf-demo-tabs" aria-label="Chọn bước minh họa">{examples.map(({ label, icon: Icon }, i) => <button key={label} aria-pressed={i === example} onClick={() => setExample(i)}><Icon size={16} />{label}</button>)}</div>
            <div className="bf-demo-body" aria-live="polite"><p className="bf-demo-status">{current.status}</p><h3>{current.heading}</h3><div className="bf-demo-language"><span>日本語 <small>NGUYÊN VĂN</small></span><p lang="ja">{current.ja}</p></div><div className="bf-demo-language bf-demo-translated"><span>Tiếng Việt <small>BẢN DỊCH</small></span><p>{current.vi}</p></div><p className="bf-demo-note"><BookOpenText size={16} />{current.note}</p></div>
          </div>
        </div>
      </section>

      {/* ── Features ────────────────────────────────────── */}
      <section id="tinh-nang" className="bf-section bf-features">
        <div className="bf-section-heading"><p className="bf-eyebrow">03 / NGỮ CẢNH KHÔNG BỊ THẤT LẠC</p><h2>Mọi quyết định đều có<br />một nơi để quay lại.</h2></div>
        <div className="bf-feature-grid">
          <article className="bf-feature-main"><div className="bf-feature-icon"><GitCompareArrows /></div><h3>Thay đổi có thể truy vết.</h3><p>Mỗi lần sửa tạo một phiên bản yêu cầu. Liên kết tài liệu nguồn, câu hỏi, tiêu chí và test case giúp bạn theo dõi những gì đã thay đổi.</p><div className="bf-revision-example" aria-label="Ví dụ lịch sử phiên bản"><div><span>v1</span><p>Yêu cầu từ tài liệu<small>Bản nháp đầu tiên</small></p></div><div><span>v2</span><p>Bản dịch đã review<small>Giữ nguyên mã yêu cầu</small></p><Check size={18} /></div></div></article>
          <article><div className="bf-feature-icon"><Languages /></div><h3>Một bộ thuật ngữ chung.</h3><p>Chuẩn hóa cách dịch theo từng dự án, để cùng một khái niệm giữ cùng một cách hiểu.</p><div className="bf-term"><span lang="ja">注文履歴</span><ArrowRight size={16} /><span>Lịch sử đơn hàng</span></div></article>
          <article><div className="bf-feature-icon"><Sparkles /></div><h3>AI đề xuất. Nhóm phê duyệt.</h3><p>Hỗ trợ trích xuất yêu cầu, đặt câu hỏi và tạo test case. Các bản nháp cần được người phụ trách kiểm tra trước khi sử dụng.</p><span className="bf-feature-tag">Có lịch sử xử lý AI</span></article>
          <article><div className="bf-feature-icon"><ShieldCheck /></div><h3>Đúng người, đúng dự án.</h3><p>Vai trò Admin, BrSE, Developer và Viewer xác định quyền truy cập và các thao tác trong workspace.</p></article>
          <article><div className="bf-feature-icon"><FileText /></div><h3>Đưa kết quả đến cả nhóm.</h3><p>Xuất yêu cầu song ngữ bằng CSV hoặc Markdown để chia sẻ trong quá trình review và bàn giao.</p></article>
        </div>
      </section>

      {/* ── FAQ ─────────────────────────────────────────── */}
      <section id="cau-hoi" className="bf-section bf-faq"><div className="bf-section-heading"><p className="bf-eyebrow">TRƯỚC KHI BẮT ĐẦU</p><h2>Hiểu rõ cách<br />BridgeFlow hoạt động.</h2></div><div className="bf-faq-items">
        {[
          ["BridgeFlow dành cho ai?", "Dành cho nhóm phát triển phần mềm có thành viên Nhật–Việt, đặc biệt là BrSE, người phân tích yêu cầu, lập trình viên và người kiểm thử cần chia sẻ cùng một cách hiểu."],
          ["AI có tự xác nhận yêu cầu không?", "Không. AI tạo đề xuất và bản nháp. Người có quyền review quyết định phê duyệt. Project phải cho phép xử lý AI; chất lượng kết quả phụ thuộc provider được cấu hình. Chế độ stub chỉ mô phỏng luồng, không phải mô hình AI thực."],
          ["Làm thế nào để vào workspace?", "Nhấn Vào workspace và đăng nhập bằng tài khoản đã được cấp. BridgeFlow hiện chưa hỗ trợ tự đăng ký tài khoản trên landing page."],
          ["Có thể xem lại bản dịch đã thay đổi không?", "Có. Yêu cầu giữ mã định danh ổn định và lưu lịch sử phiên bản. Bạn có thể xem lại nội dung từng phiên bản cùng các liên kết truy vết."],
        ].map(([question, answer]) => <details key={question}><summary>{question}<span aria-hidden="true">+</span></summary><p>{answer}</p></details>)}
      </div></section>

      {/* ── Final CTA ───────────────────────────────────── */}
      <section className="bf-section bf-final-cta"><p className="bf-eyebrow">CÙNG HIỂU. CÙNG XÂY.</p><h2>Bắt đầu từ một yêu cầu rõ ràng.</h2><p>Mở workspace và kết nối tài liệu với những quyết định của nhóm.</p><Link href="/workspace" className="bf-cta">Vào BridgeFlow <ArrowRight size={18} /></Link><small>Dành cho tài khoản đã được cấp quyền.</small></section>
    </main>
    <footer className="bf-landing-footer"><Link className="bf-wordmark" href="/"> <Languages size={21} />BridgeFlow</Link><p>日本語 × Tiếng Việt · Cùng một cách hiểu.</p><a href="#noi-dung">Về đầu trang ↑</a></footer>
  </div>
</SmoothScrollProvider>
);
}
