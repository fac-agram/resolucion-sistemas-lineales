# Design: Mejoramiento Iterativo — NaN Honesty Fix + Test Hardening

## Technical Approach

Two-commit, main-only, strict-TDD change. Commit 1 fixes the convergence lie at its root — `Normas.normaInfinito(double[])` silently skips NaN because `NaN > max` is false — by switching the comparison loop to `Math.max` accumulation, which propagates NaN naturally. No `Double.isFinite`/`isNaN` calls anywhere: the honest-results gate (`grep -rn "Double.isFinite|Double.isNaN|Double.isInfinite" src/unsa/sistemas/model/` → no matches) stays green. `MejoramientoIterativo` gets **zero production changes** — its stop test `normaR <= TOLERANCIA` already behaves correctly once the norm is honest (`NaN ≤ 1e-7` is false → loop runs to cap → `convergio` stays false). Commit 2 hardens tests per spec R6 and adds the missing coverage.

## Measured behavior (re-measured at HEAD 8b40d42 via /tmp probes; no repo mutation)

Post-fix, `A={{1,2},{3,5}}, b={1,NaN}`: **convergio=false, iteraciones=1000 exactly** (runs to cap, never early-stops), **errorFinal=NaN**, x=[NaN,NaN] returned unsanitized, wall time **3–8 ms** (n=2). Controller: `NO_FINITO`, sol=null, `Convergencia[1000, NaN, false]` preserved by the `instanceof ResolutorIterativo` branch. Overflow-mid-loop fixture (`A={{10,0},{0,1}}, x0={MAX,0}, b={0,0}`): pre-fix lied `true/1/0.0`; post-fix `false/1000/NaN`. Finite invariance (R7.1): 4.0/0.0(empty)/0.0(−0.0)/1e308/±Inf identical pre/post; only NaN-domain changes (`{1,NaN}`:1.0→NaN, `{NaN}`:0.0→0.0→NaN, `{Inf,NaN}`:Inf→NaN). Ground truths confirmed: 3x3 x=(2.043478…, −1.927536…, 1.376812…)=(47/23,−133/69,95/69) convergio=true iter=0; canonical default-x0 iter=0 err=8.881784197001252e-16; perturbed-x0 iter=1; R4.2 `(1,0.5)` while Jacobi same system: `false/1000/finite`. Singular: `ArithmeticException("…pivote válido…")` propagates, post-throw `convergio=false iter=0`; controller → ERROR.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Fix in `normaInfinito` via `Math.max` | Touches shared util, but ONLY callers are MejoramientoIterativo:78/:108; finite domain proven invariant | **Selected** |
| Solver-side `Double.isFinite(normaR)` guard | Violates honest-results model/ grep ban; leaves util lying | Rejected |
| Finite cap-exhaustion fixture | 9 near-singular candidates probed: all converge in ≤3 iters or throw (refinement correction solves `A·z=r` exactly → residual drops to backward-error scale unless arithmetic goes non-finite) | **Unreachable — R5.2 witnessed on the NaN-cap run** (`iter==1000 && !convergio && last iterate`); finite capped iterate already proven controller-side by existing `verificaJacobiNoConvergeConAproximacionFinita` |
| R1.3 reset fixture | `Resolutor` clones A/b, so "changed inputs" on one instance is impossible | Double-run equality: run `resolver()` twice, assert iter/error/flag identical (catches accumulation-style mutations) |

```java
// The fix (Normas.java:94-101 body):
double max = 0.0;
for (double valor : v) {
    max = Math.max(max, Math.abs(valor));   // Math.max propagates NaN
}
return max;
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/unsa/sistemas/model/matrix/Normas.java` | Modify (C1) | `normaInfinito(double[])` → Math.max loop + javadoc |
| `test/NormasTest.java` | Modify (C1) | +`verificaNormaInfinitoPropagaNoFinitos` (NaN×3, ±Inf, empty; RED first) |
| `test/MejoramientoIterativoTest.java` | Modify (C1,C2) | +`verificaEntradaNaNNoDeclaraConvergencia` (C1, RED first); rewrite trivial bound to `assertEquals(0, iter)` + err pin; rewrite 3x3 to ground truth ±1e-6, drop scratch lines 55–56 & `Ax≈b` proxy; +5 new: reset, overflow, R4.2-with-inline-Jacobi-contrast, singular-assertThrows, cap-1000 |
| `test/SistemaControllerTest.java` | Modify (C2) | +2: NaN→NO_FINITO with honest copied `Convergencia(false,1000)` & no éxito text (R3.2+R3.4); singular→ERROR naming MEJORAMIENTO (R3.3). Existing tests untouched (R7.2) |

## Test count math

`MejoramientoIterativoTest` 3→9 (+6), `NormasTest` 5→6 (+1), `SistemaControllerTest` 22→24 (+2): **k_s = 9, M′ = 67+9 = 76**, 12 classes (c_s=0). Sibling `trace-label-fixes` (67 + k_s + 6): **final total 82/12**. Apply ours first (their design recommends); theirs pins R5 counts by measurement.

## Commit plan (Spanish messages, all commits green)

1. `fix: propagar NaN en la norma infinito para no declarar convergencia falsa` — RED (record failing surefire output: `expected: <false> but was: <true>`) → fix → GREEN. Suite 69. ~50 lines.
2. `test: endurecer oraculos de verdad y cubrir NaN, desborde, tope e integracion en mejoramiento iterativo` — hardening + 7 new tests. Suite 76. ~190 lines. Both <400.

## Verification gates

- Each commit: `sh ./mvnw -o clean test` → 69/0/0, then 76/0/0; honest-results grep → no match.
- Mutation (C1): revert loop → NaN unit test + solver NaN test red (≥2), rest green. Mutation (C2): `MAX_ITERACIONES`-boundary tweak → cap test red; tolerance widened to 1e-16 → canonical/perturbed red; restore skip-comparison → NaN solver+controller tests red.
- Review grep: `getIteraciones() >= 0` and scratch marker (`Wait:`) in `MejoramientoIterativoTest.java` → no matches (re-adding either fails review, caught statically).

## Migration / Rollout

No migration. GUI unchanged: NaN runs already render through `NO_FINITO` textos (manual check: solve `b={1,NaN}` via MEJORAMIENTO in-app → "NO SE PUDO RESOLVER…", no success text).

## Rollback

Independent `git revert` per commit, reverse order (C2 then C1); reverting C2 alone keeps the honest fix with the old (69→) tests; C1 revert restores 67-test baseline. No build coupling.

## Risks & Open Questions

- `getErrorFinal()` exposes `NaN` on non-finite runs (was the `0.0` lie) — **accepted**: spec R2 forbids sanitization; controller masks UI; `Convergencia` is display-only.
- NaN runs now cost 1000 corrections (~3–8 ms at n=2, grows O(1000·n³)) — **accepted**, measured trivial vs existing Jacobi cap behavior.
- R5.2 has no finite-iterate witness — **accepted**; 9-candidate probe + argument in Decisions.
- Open questions: none blocking.
