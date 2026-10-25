# BridgeFlow AI Agent Guidelines & Capabilities

## Project Overview
BridgeFlow is an AI-assisted requirements analysis and traceability platform for Japanese–Vietnamese software teams.
- **Frontend Stack**: Next.js 16 (App Router), React 19, Tailwind CSS v4, ThreeUI Glassmorphism, Lucide React, Lenis smooth scrolling.
- **Backend Stack**: Java 21, Spring Boot 3.4, PostgreSQL 17, Caddy TLS Gateway, Docker Compose.
- **Design Language**: ThreeUI Glassmorphism with Dual-Theme (Aurora Light / Midnight Dark), frosted glass cards, 3D specular bevels, and ambient gemstone particles.

## Agent Skills Available
- `clone-website` (`.agents/skills/clone-website/SKILL.md`): Rebuild external websites, sections, or UI animations into clean Next.js + Tailwind components matching real typography, assets, responsive layouts, and interactive motion.
  - Command: `/clone-website <url>`
  - References:
    - [Inspection Guide](.agents/skills/clone-website/references/inspection-guide.md)
    - [Framer & Motion Guide](.agents/skills/clone-website/references/framer-and-motion.md)

## Development & Quality Gate Rules
1. Never break existing APIs, data models, or form submission structures in `requirement-traceability.tsx`.
2. All 6 automated frontend tests (`npm run test:frontend`) must pass at 100%.
3. Zero TypeScript compiler errors (`tsc --noEmit`) and zero ESLint errors (`npm run lint`).
4. Support both Light Mode and Dark Mode (`[data-theme="dark"]`, `.dark`) for any newly cloned or authored components.
