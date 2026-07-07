# Plan - GIS Modularity and Specification Update

Detailed task list for updating the Vietnamese project overview, creating the English project overview, and specifying the modular architecture using Flyway, compile-time properties, and Vite.

## Project Type
WEB / BACKEND (Multi-tier GIS Application)

## Success Criteria
- [x] Updated `docs/Tổng quan dự án.md` without AI references and with correct phasing.
- [x] Created `docs/Project Overview.md` in English reflecting the updated specifications.
- [x] Completed `docs/ARCHITECTURE SPECIFICATION.md` detailing the modular, compile-time configurable GIS schema structure and dynamic routes.

## Tech Stack
- Frontend: Vite + React + TypeScript + Leaflet
- Backend: Spring Boot 3.x + Java 17 + PostgreSQL + PostGIS + Flyway migrations

## Proposed File Structure (for documentation updates)
```
docs/
├── Tổng quan dự án.md (modified)
├── Project Overview.md (new - English)
└── ARCHITECTURE SPECIFICATION.md (modified - filled)
```

## Task Breakdown

### Task 1: Update Vietnamese Project Overview
- **Agent:** `documentation-writer`
- **Skills:** `clean-code`, `plan-writing`
- **Priority:** High
- **Dependencies:** None
- **INPUT:** `docs/Tổng quan dự án.md`
- **OUTPUT:** Updated `docs/Tổng quan dự án.md` with:
  - Phase 4 (AI chatbot) removed.
  - Phase 1-3 descriptions updated to emphasize the pluggable/modular feature toggle concept.
- **VERIFY:** View the file to verify the sections.

### Task 2: Create English Project Overview
- **Agent:** `documentation-writer`
- **Skills:** `clean-code`, `plan-writing`
- **Priority:** High
- **Dependencies:** Task 1
- **INPUT:** Updated `docs/Tổng quan dự án.md`
- **OUTPUT:** `docs/Project Overview.md` containing the complete English translation.
- **VERIFY:** View `docs/Project Overview.md` and check translation completeness.

### Task 3: Author Architecture Specification
- **Agent:** `project-planner` / `backend-specialist` / `frontend-specialist`
- **Skills:** `architecture`, `clean-code`
- **Priority:** Critical
- **Dependencies:** Task 1
- **INPUT:** `docs/ARCHITECTURE SPECIFICATION.md`
- **OUTPUT:** Populated `docs/ARCHITECTURE SPECIFICATION.md` with:
  - **Concept Diagram & Core Architecture:** FE/BE/DB integration.
  - **Frontend Modularity:** Compile-time toggle pattern using Vite `.env` configuration.
  - **Backend Modularity:** Spring profiles and conditional configuration.
  - **Database Migration:** Dynamic Flyway configuration via locations filtering.
- **VERIFY:** View the completed specification.

---

## Phase X: Final Verification
- [x] Verification scripts executed and passed.
- [x] Verification file check: verify `Tổng quan dự án.md`, `Project Overview.md`, and `ARCHITECTURE SPECIFICATION.md` exist and are populated.
- [x] Verify that no references to AI Chatbot remain.
- [x] Verify all file links are correct.

## ✅ PHASE X COMPLETE
- Lint: ✅ Pass
- Security: ✅ No critical issues
- Build: ✅ Success
- Date: 2026-07-07
