# Tasks: Mejoramiento Iterativo — NaN Honesty Fix + Test Hardening

Main-only; every commit green. Strict TDD: NaN tests RED-first; transcripts → `output/progress` (never commit red).

## Review Workload Forecast

Highest-risk-first: C1 mutates shared `normaInfinito(double[])` (callers only `MejoramientoIterativo:78/:108`; finite results proven invariant). ~50/~190 lines per commit, both <400.

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

## Phase 1: C1 RED — prove the lie

- [x] 1.1 `test/NormasTest.java` + `verificaNormaInfinitoPropagaNoFinitos`: {3,-4,NaN}→NaN, {NaN,NaN,NaN}→NaN, {±Inf}→+Inf, {Inf,NaN}→NaN, {}→0.0.
- [x] 1.2 `test/MejoramientoIterativoTest.java` + `verificaEntradaNaNNoDeclaraConvergencia`: A={{1,2},{3,5}}, b={1,NaN} → assertFalse(convergio()), x=[NaN,NaN] unsanitized (R2.1).
- [x] 1.3 Evidence: `sh ./mvnw -o clean test` → `Tests run: 69, Failures: 2`; transcript `expected: <false> but was: <true>` + norm NaN mismatch → output/progress. No GREEN before this.

## Phase 2: C1 GREEN + commit

- [x] 2.1 `Normas.java` loop → `max = Math.max(max, Math.abs(valor));` + javadoc NaN note. Zero solver edits; no `Double.is*` tokens in model/.
- [x] 2.2 Gate: `sh ./mvnw -o clean test` → `Tests run: 69, Failures: 0, Errors: 0`.
- [x] 2.3 Stage exactly (NEVER `git add -A`; openspec/ untracked): `src/unsa/sistemas/model/matrix/Normas.java test/NormasTest.java test/MejoramientoIterativoTest.java`; `git commit -m "fix: propagar NaN en la norma infinito para no declarar convergencia falsa"`

## Phase 3: C2 R6 test-quality hardening

- [x] 3.1 Canonical test: trivial `assertTrue(getIteraciones() >= 0)` → `assertEquals(0, getIteraciones())` + pin errorFinal 8.881784197001252e-16 (delta 1e-15).
- [x] 3.2 3x3 test: assert x vs (47/23, −133/69, 95/69) ±1e-6, convergio true (R4.1); delete scratch lines 55–56 ("Wait:") + `Ax≈b` proxy.

## Phase 4: C2 new coverage (zero production edits)

- [x] 4.1 R1.3: same instance, `resolver()` twice → iterations/error/flag identical.
- [x] 4.2 R2.2 overflow: A={{10,0},{0,1}}, x0={MAX_VALUE,0}, b={0,0} → false/1000/NaN (pre-fix lie true/1/0.0).
- [x] 4.3 R4.2: A={{1,2},{3,4}}, b={2,5} → (1,0.5) ±1e-6, converges; inline-Jacobi contrast → false/1000/finite.
- [x] 4.4 R5.1: assertThrows(ArithmeticException), A={{1,2},{2,4}}, b={3,6}; post-throw convergio() not true.
- [x] 4.5 R5.2 cap on NaN-cap run: iterations==1000, !convergio(), last iterate returned; no finite-cap fixture hunt (proven unreachable).
- [x] 4.6 R3.2/R3.4, `test/SistemaControllerTest.java`: MEJORAMIENTO b={1,NaN} → NO_FINITO, sol null, copied Convergencia(1000,NaN,false) via instanceof branch (:122), no "Resuelto con éxito.".
- [x] 4.7 R3.3: singular → ERROR, message names failed execution. Existing controller tests untouched (R7.2).

## Phase 5: C2 GREEN + commit

- [x] 5.1 Gate: `sh ./mvnw -o clean test` → `Tests run: 76, Failures: 0, Errors: 0` (MejoramientoIterativoTest 9, NormasTest 6, SistemaControllerTest 24; 12 classes).
- [x] 5.2 Stage exactly: `test/MejoramientoIterativoTest.java test/SistemaControllerTest.java`; `git commit -m "test: endurecer oraculos de verdad y cubrir NaN, desborde, tope e integracion en mejoramiento iterativo"`

## Phase 6: Verification + mutation gates

- [x] 6.1 R7 firewall: 76/0/0; `git diff --name-only HEAD~2` = Normas.java + 3 test files; Jacobi/GaussSeidel/SOR/RelajacionSimple zero-modified.
- [x] 6.2 R7.1: NormasTest's 5 existing tests pass unmodified; finite results bit-identical (4.0, 0.0 empty/−0.0, 1e308, ±Inf).
- [x] 6.3 Greps no-match: model/ `Double.is*`; MejoramientoIterativoTest `getIteraciones() >= 0`; `Wait:`.
- [x] 6.4 Mutations (edit→run→restore→re-gate 76/0/0; evidence→output/progress): revert skip-comparison → ≥2 NaN tests red, rest green (4 post-C2); `TOLERANCIA`→1e-16 → canonical/perturbed red; `MAX_ITERACIONES` tweak → cap test red. These are local, reverted, never committed.

## Phase 7: Handoff + rollback

- [x] 7.1 Sibling `trace-label-fixes` applies AFTER: report k_s=9, c_s=0, M′=76/12 → their final 82/12; they own R5 delta text. Rollback: `git revert` C2 then C1; C2 alone keeps fix at 69.

## Coverage & Out of Scope

R1: 1.2, 3.1, 4.1 | R2: 1.2–2.1 (RED-first), 4.2 | R3: 4.6, 4.7 (R3.1 existing) | R4: 3.2, 4.3 | R5: 4.4, 4.5, 4.7 | R6: Phase 3, 6.4 | R7: 6.1, 6.2.

Confirmed out-of-scope: no UI/FXML; no `Normas` beyond `normaInfinito(double[])`; zero `MejoramientoIterativo` production edits; trace-label/R5-text = sibling; no finite-cap hunt.
