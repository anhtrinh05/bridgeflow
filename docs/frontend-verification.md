# Frontend navigation and relation regression verification

Use a synthetic project in an isolated stack. Keep existing demonstration
volumes and credentials intact. The current release tag stays immutable; these
changes require independent verification before a new release is prepared.

## Automated checks

```powershell
.\scripts\test-backend.cmd
npm.cmd run lint
npm.cmd exec tsc -- --noEmit
npm.cmd run test:frontend
npm.cmd run build
```

Expected backend: 31 tests, zero failures/errors/skips, including
`listsProjectMembersWithoutExposingCredentialsOrOtherProjects`. Expected frontend:
6 tests, zero failures/skips. The frontend tests exercise the actual relation
component submit handler with a React-style event whose `currentTarget` becomes
null during the request. They are component/API regressions, not browser tests.

## Browser checks

1. Open `/`: the public landing page should render without login. Follow a
   workspace CTA to `/workspace`, then log in using a provisioned operator.
   Login fields should be empty initially.
2. Open every sidebar item: Overview, Documents, Glossary, Requirements, Q&A,
   Test cases, Changes, Members, and Settings. Each must change the heading and
   highlight the current destination. At widths below 1024px, use the project
   and workspace selectors instead of the desktop sidebar.
3. In a project with no requirements, Q&A/Test cases/Changes should offer the
   document workflow instead of an inactive or blank view.
4. Upload and extract a synthetic Japanese document. Navigate to Requirements:
   the new drafts must appear without a manual browser refresh.
5. Choose the second requirement, then open Q&A from the sidebar. The same
   requirement must remain selected and the Q&A & criteria tab must be active.
   Repeat with Test cases (test tab) and Changes (traceability tab).
6. Switch quickly between requirements/projects. Data from a slower previous
   request must not replace the latest selection. A new revision must load its
   own Q&A/test/traceability state.
7. Add a relation to another requirement. Expect one saved relation, a reset
   target selector, an updated relation count, and no null/reset error. Refresh
   and confirm persistence. Submit the same relation again: expect the server's
   duplicate error without a false success/reset. Delete only this drill's
   relation and confirm the count refreshes.
8. Use the top Traceability button; it must open Changes/traceability. Check
   browser Back/Forward and reload retain the destination and detail tab.
9. Open Overview: counts must match real project data. The requirement count
   in the sidebar must not equal only the current page size. Verify pagination
   and sorting/filtering continue to work, including archived requirements.
10. Open Members as ADMIN and VIEWER: show the actual roster/roles. A user
    outside the project must get 403 from `/api/v1/projects/{id}/members`; an
    unauthenticated request must get 401. No password hash or session token may
    be returned. Membership editing/invitations remain outside this patch.
11. Open Settings as ADMIN: edit project name/customer and AI permission, save,
    and verify persistence. VIEWER must not see an edit action. Archived
    projects must not allow editing from Settings.
12. Click the header activity icon: display actual AI jobs, status, and provider.
    The stub provider must be identified as a simulation. This is an AI job
    history view, not a general notification system.
13. Export CSV/Markdown and download the original document to check the
    established flows still work. End the drill preserving pre-existing data.

Record automated output and browser evidence in `work/antigravity-handoff.md`.
Do not mark browser or Docker checks passed using the component harness alone.

## Landing and route checks

- Direct-load and reload `/` and `/workspace` through the production gateway.
  Landing content must render even if the isolated backend is unavailable.
- Verify old `/#view=questions&tab=artifacts` bookmarks redirect to
  `/workspace#view=questions&tab=artifacts`; preserve the intended destination
  through login. Landing anchors such as `/#cau-hoi` must not redirect.
- Check landing/workspace round trips before and after login, session reuse,
  logout and browser Back/Forward. Never prefill credentials in the source.
- At 320, 390, 800 and 1440px, check overflow, typography, hero illustration,
  mobile menu, anchor offsets, visible keyboard focus and reduced motion.
- Switch all three illustrative example buttons and expand/collapse FAQ items
  using mouse and keyboard. Examples must not write project data or call AI.
- Check all CTA links and browser console. Keep the AI simulation disclaimer
  and provisioned-account explanation accurate; no fake signup flow.
