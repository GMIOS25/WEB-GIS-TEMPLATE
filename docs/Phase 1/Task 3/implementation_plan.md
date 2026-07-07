# Implementation Plan - Task 3: Login Redesign (Penpot Layout & Light Mode Only)

This updated plan covers the redesign of the React frontend login screen to match the layout and styling guidelines extracted from the Penpot design file, and the removal of the dark mode theme.

## User Review Required

> [!IMPORTANT]
> We will align the interface with your Penpot design structure. We will default to a clean, minimalist light theme with the green `#10b981` accent, and remove dark mode logic completely.

## Open Questions

> [!IMPORTANT]
> Please review and clarify the following details for the final implementation:
> 1. **Font Sizes & Geometry**: The Penpot file specifies quite large text properties (e.g., 40px for headings, 27px for input text, 26px for buttons and footer). Should we scale these down slightly to standard web proportions (e.g. 14px for input text, 14px for labels, 32px for main title) for better balance, or match the exact large sizes?
> 2. **Button Text Color**: In the Penpot design, the "Đăng nhập" text is set to black (`#000000`) on top of the green `#10b981` button. Would you prefer us to use white (`#FFFFFF`) for better contrast and accessibility, or stick to the exact black color?
> 3. **Input Borders**: The password field in Penpot has a soft lilac border (`#bb97d8`, 2px solid). Should we keep this accent border or use a uniform neutral border for all inputs (with `#10b981` green border on active focus)?

---

## Proposed Changes

### Configuration & Theme Cleanup

#### [MODIFY] [index.html](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/index.html)
- Remove dark mode body class. Ensure default background is white/light slate.

#### [MODIFY] [index.css](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/src/index.css)
- Clean up `@theme` and remove dark-specific custom classes (e.g. `.dark .glass`, `.dark .bg-grid-pattern`).
- Set `#FFFFFF` as the main body background.

---

### Core Authentication & Layout

#### [MODIFY] [Login.tsx](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/src/pages/Login.tsx)
- Redesign the layout according to Penpot specifications:
  - Top header: Emerald green `#10b981` icon box (`Board [77x77]`) with a white map SVG icon.
  - Page title: "Hệ thống bản đồ địa giới GIS." (Font size: 40px, bold, black).
  - Main Login Card: Background `rgba(214, 214, 214, 0.3)` or equivalent light grey, centered.
    - Card Title: "Đăng nhập" (Font size: 40px, bold, black).
    - Username block: Label "Tên đăng nhập" (20px, bold) + input field with user icon and placeholder "Nhập tên đăng nhập".
    - Password block: Label "Mật khẩu" (20px, bold) + input field with lock icon, eye toggle, lilac border (`#bb97d8`), and placeholder "Nhập mật khẩu".
    - Submit Button: Background `#10b981`, height 59px, bold text "Đăng nhập" (26px).
  - Footer copyright text: "@2026 - GPHI" (26px, bold).
- Remove the theme toggle button and all dark mode state sync logic.

#### [MODIFY] [Home.tsx](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/src/pages/Home.tsx)
- Remove the theme toggle button, keeping the page purely in clean light mode.

---

## Verification Plan

### Automated Checks
- Run `pnpm run build` to make sure there are no TypeScript compiler errors.
- Ensure ESLint checks pass.

### Manual Verification
- Redirection from `/` to `/login` if unauthenticated.
- Verify the layout matches the Penpot structure.
- Verify dark mode is completely deactivated.
