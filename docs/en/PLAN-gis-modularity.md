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

---

## Phase Y: Multi-Customer Deployment & Fleet Strategy Documentation

Follow-up phase, triggered by the confirmed real-world scenario of 3 concurrent customers (OCOP, KHCN, Nong Lam Nghiep) sharing the same core (135 phuong/xa) but requiring absolute data isolation from one another, plus the need to update all or a subset of deployed instances without manual per-instance editing.

### Task 4: Author Deployment & Fleet Strategy Documentation

- **Agent:** `project-planner` / `backend-specialist` / `devops-specialist`
- **Skills:** `architecture`, `clean-code`, `plan-writing`
- **Priority:** Critical
- **Dependencies:** Task 3 (Architecture Specification must exist first)
- **INPUT:** `docs/ARCHITECTURE SPECIFICATION.md`
- **OUTPUT:**
  - Updated `docs/ARCHITECTURE SPECIFICATION.md` with new **Section 6 (Multi-Customer Deployment & Isolation Strategy)** and **Section 7 (Fleet Management & Rollout Automation)**, including the current customer roster (Customer A/OCOP, Customer B/KHCN, Customer C/Nong Lam) and the geometry-type distinction (Point vs Polygon) for `nonglam`.
  - New `docs/DEPLOYMENT_AND_FLEET_STRATEGY.md` containing the fleet registry schema (`ops/fleet.yaml`), CI build pipeline description, the fleet rollout script (`ops/deploy.sh`), and standard runbooks (emergency fix to all, partial feature rollout, core data correction, new customer onboarding).
  - Updated `docs/Project Overview.md` Section 7 with a 4th bullet on deployment isolation, cross-referencing the two documents above.
- **VERIFY:** View all three files; confirm the database-per-customer isolation model is stated as the adopted decision (not shared multi-tenant), confirm `ops/fleet.yaml` and `ops/deploy.sh` examples are present and consistent with the Flyway/feature-flag mechanics already defined in Sections 2–5 of the Architecture Specification.

## Proposed File Structure (updated)

```
docs/
├── Tổng quan dự án.md (modified)
├── Project Overview.md (modified - section 7 updated)
├── ARCHITECTURE SPECIFICATION.md (modified - sections 6-7 added)
└── DEPLOYMENT_AND_FLEET_STRATEGY.md (new)
```

## Phase Y: Final Verification

- [ ] View `ARCHITECTURE SPECIFICATION.md` and confirm Sections 6-7 exist and reference the customer roster correctly.
- [ ] View `DEPLOYMENT_AND_FLEET_STRATEGY.md` and confirm it exists and is populated.
- [ ] Verify `Project Overview.md` Section 7 references both companion documents.
- [ ] Verify no contradiction with the database-per-customer decision (no shared multi-tenant / `tenant_id` pattern introduced anywhere).
