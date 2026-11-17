# Bundle analysis report

Date: 2026-09-09

## Scope and method

- Project: Vite 8.1.x, Rolldown, React 19.
- Analyzer: `vite-bundle-visualizer` / `rollup-plugin-visualizer`.
- Production build analyzed with the repository's current `vite.config.ts`.
- Outputs:
  - `stats.json`: raw module/chunk data with rendered, gzip, and Brotli estimates.
  - `treemap.html`: interactive treemap.
  - `stats-no-manual.json`: counterfactual build with `manualChunks` removed.
- Vite's final emitted gzip sizes are used for transfer-size comparisons. The
  analyzer's per-module sizes are used for composition because, without source
  maps, they describe transformed module code before final bundle minification
  and therefore do not add up exactly to the emitted files.

## Executive finding

There is no single catastrophic dependency and no duplicate module was found.
The main defect is *when* code is loaded: the current `manualChunks` policy
creates cross-chunk dependencies that force the map stack (Leaflet) into the
initial HTML preload graph, including on `/login`.

The current production HTML preloads five JavaScript chunks totaling **161.25
kB gzip**. A production counterfactual build without `manualChunks` preloads one
JavaScript chunk totaling **99.82 kB gzip**: **61.43 kB / 38.1% less initial
JavaScript**. Total application JavaScript remains approximately the same
(about 200 kB gzip), so this is a route-loading/order problem rather than a
general dependency-bloat problem.

## Current output summary

| Chunk | Final minified | Final gzip | Analyzer composition |
|---|---:|---:|---|
| `vendor-react` | 253.88 kB | 82.15 kB | React DOM, React Router, part of TanStack Query, Lucide, React compiler runtime |
| `vendor-leaflet` | 198.19 kB | 56.72 kB | Leaflet, markercluster, react-leaflet, and unexpectedly the React/ReactDOM core entry modules |
| `Home` | 59.00 kB | 16.31 kB | Main map view and eagerly imported map UI components |
| `vendor-query` | 44.72 kB | 17.02 kB | In practice, Axios; TanStack Query did not land here as intended |
| Feature/admin panels | 74.79 kB | 18.52 kB | Correctly lazy-loaded and not part of initial route load |

The complete emitted application is about **200.96 kB gzip** across 17 JS
chunks. The initial HTML preloads roughly **80%** of all compressed JS before
authentication/route resolution.

Largest module families in analyzer raw data:

| Package/family | Analyzer rendered estimate | Analyzer gzip estimate | Assessment |
|---|---:|---:|---|
| `react-dom` | 448.23 kB | 85.09 kB | Largest dependency, but expected framework runtime |
| `leaflet` | 236.54 kB | 52.73 kB | Expected for the map route; wasteful on login |
| `leaflet.markercluster` | 54.19 kB | 10.86 kB | Expected for clustered POIs |
| `react-router` | 88.50 kB | 21.90 kB | Moderate; replacing it is not justified yet |
| TanStack Query core + React adapter | 67.34 kB | 20.74 kB | Useful on the authenticated map, avoidable on login with provider refactoring |
| `axios` | 94.15 kB | 36.12 kB | Analyzer estimate; final `vendor-query` is only 17.02 kB gzip |
| `lucide-react` | 19.29 kB | 12.39 kB | Tree-shaking works; only referenced icons are present |

## Root-cause evidence

1. `App.tsx` correctly makes `Home` and `Login` dynamic imports, but the current
   generated `index.html` still preloads `vendor-leaflet`.
2. The emitted `vendor-react` chunk imports values from `vendor-leaflet`, making
   the map vendor chunk an initial dependency of React.
3. The `manualChunks` classifier uses broad substring tests. The `react` rule
   also matches `lucide-react` and `@tanstack/react-query`, so:
   - the configured `vendor-icons` chunk is never emitted;
   - `vendor-query` contains Axios rather than the intended Query stack;
   - package boundaries become cyclic/brittle under Rolldown's CommonJS and
     shared-module linking.
4. A second counterfactual using exact package-name classification still
   preloaded the map chunk and only reduced initial JS from 161.25 kB to about
   157.09 kB gzip. Reordering or tightening the substring rules alone does not
   solve the route leakage.
5. Git history shows this policy and two extra font preloads were introduced in
   commit `13ca3e0` as a performance optimization. The route-level `React.lazy`
   change is useful; the manual vendor split and blanket font preloads are the
   regressions.

## Recommended solution, in order

### P0 — Remove the current `manualChunks` policy

Keep the existing route and panel `React.lazy` boundaries and let Vite/Rolldown
form shared chunks automatically. This is the safest tested change and gives the
largest measured improvement for unauthenticated/login loading:

| Build | Initial JS requests | Initial JS gzip | Change |
|---|---:|---:|---:|
| Current manual chunks | 5 | 161.25 kB | baseline |
| Exact package-name manual chunks | 5 | 157.09 kB | -2.6% |
| No manual chunks | 1 | 99.82 kB | **-38.1%** |

For the authenticated map, total JS remains close to the current total; the map
code is simply loaded after route/auth resolution instead of for every visitor.
If authenticated map startup is the only priority, measure the added post-auth
network phase before rollout and optionally prefetch the map route based on an
explicit product decision.

### P1 — Make map CSS route-owned

`src/main.tsx` globally imports Leaflet and markercluster CSS, forcing **16.74 kB
minified / 6.62 kB gzip** onto the login route. Move these styles behind a true
async map-runtime boundary and verify that `index.html` no longer links the map
stylesheet. With multiple dynamic consumers (`GisMap` and `MapPicker`), merely
moving three static import lines may still cause CSS consolidation; validate the
emitted HTML/chunks rather than assuming it split.

### P1 — Reduce font preload pressure

`index.html` preloads regular, 600, and 700 WOFF2 files totaling **86.48 KiB**.
WOFF2 is already compressed, so these bytes are real network cost. The 700 file
alone is 29.57 kB and is not essential to first-paint login content. Keep only
the weights proven critical above the fold (likely regular + 600), rely on the
existing `font-display: swap`, and measure CLS/LCP before and after.

### P2 — Keep TanStack Query off the public shell

`QueryClientProvider` wraps the entire app and `AuthContext` imports
`useQueryClient` mainly to clear cache on logout. Move the query provider into
the authenticated application shell, or inject a small cache-reset callback,
so login does not pay for the Query runtime. This requires a focused refactor
and should follow P0 because P0 already fixes the larger Leaflet leakage.

### P2 — Add enforceable bundle budgets

- Initial/public route JS: <= 110 kB gzip.
- Map route total JS: <= 190 kB gzip.
- No map JS or map CSS in `/login`'s preload graph.
- No single new dependency > 25 kB gzip without review.
- Generate analyzer raw data in CI and compare against the baseline.
- Restore Vite's default warning threshold or lower it; `600 kB` suppresses
  useful growth warnings.

### P3 — Optional follow-ups

- Feature flags currently hide functionality at runtime but all three feature
  panel chunks are still built. Compile-time conditional imports can reduce the
  per-customer artifact, but these chunks are already lazy and do not affect
  initial load.
- Replacing Axios with native `fetch` could save part of its 17.02 kB final
  gzip chunk, but it has higher refactoring risk and is not the first move.
- Verify immutable browser caching for hashed `/assets/*` files. Internal Spring
  resource-chain caching is not a substitute for explicit long-lived client
  cache headers.

## Important boundary of this report

Bundle analysis does not measure API payloads, tile downloads, JavaScript
execution, or map rendering. `Home.tsx` immediately requests province GeoJSON
and Leaflet requests map tiles. If the authenticated map remains heavy after P0,
the next investigation should capture browser Network and Performance traces;
GeoJSON size/parse and vector-layer rendering are more likely than another
hidden JS dependency.
