# mejoramiento-iterativo

<!--
Provenance
- Capability: mejoramiento-iterativo (live spec)
- Origin change: mejoramiento-iterativo (retroactive formalization of e9582aa + convergence-honesty fix)
- Source: openspec/changes/archive/2026-11-17-mejoramiento-iterativo/specs/mejoramiento-iterativo/spec.md
- Synced: 2026-11-17
- Verify verdict: pass_with_warnings (zero CRITICAL; commits 901bd84 + 70a0602; see archive-report.md in the archived change)
- Archive-time merges of verify findings: R1.3 reworded per WARNING-1 (the "changed inputs" premise
  is unsatisfiable on the public API — Resolutor clones A/b in the constructor — so the scenario now
  states the honest, observable contract: double-run equality on one instance); R1.2 annotated per
  WARNING-2 (the delivered test pins errorFinal bit-exact, a test-strength choice beyond the
  normative <= 1e-7 bound).
- Ground truth verified by probes at HEAD: NaN lie (convergio=true, iter=0, err=0.0, x=[NaN,NaN] on
  A={{1,2},{3,5}}, b={1,NaN}); 3x3 oracle (47/23, -133/69, 95/69); MEJORAMIENTO solves
  {{1,2},{3,4}}/b={2,5} -> (1, 0.5) where Jacobi hits cap with convergio=false; singular A ->
  ArithmeticException -> controller ERROR; default Gauss x0 converges immediately (iter=0).
- R5 ownership: this capability writes NO requirement against test-execution-baseline; the sibling
  change trace-label-fixes owns the R5 supersession text. Suite-count facts stay in their reports.
-->

## Purpose

Contract for the shipped iterative-refinement solver registered as `"MEJORAMIENTO"`, plus honesty requirements for its convergence flag on non-finite residuals.

## Requirements

### Requirement: R1 Algorithm Contract

`MejoramientoIterativo` MUST implement `ResolutorIterativo` for `Ax = b`: initial guess x0 (supplied, or computed with `EliminacionGaussiana` when absent); then, while the stop test fails, recompute residual r = b − A·x, solve `A·z = r` with `EliminacionGaussiana`, update x ← x + z. The stop tolerance MUST be ‖r‖∞ ≤ 1e-7 and the correction cap 1000 iterations. After `resolver()` returns, `getIteraciones()`, `getErrorFinal()` and `convergio()` MUST describe that run; each call MUST reset prior state. An initial residual already within tolerance MUST report `convergio() == true` with `getIteraciones() == 0`.

#### Scenario: R1.1 Refinement from perturbed x0

- GIVEN A = {{2,1},{1,3}}, b = {3,5}, x0 = {0,0}
- WHEN `resolver()` runs
- THEN x ≈ (0.8, 1.4) within 1e-6, `convergio()` is true, `getIteraciones() >= 1`, `getErrorFinal() <= 1e-7`

#### Scenario: R1.2 Default Gaussian x0 converges immediately

- GIVEN A = {{2,1},{1,3}}, b = {3,5}, no x0 supplied
- WHEN `resolver()` runs
- THEN x ≈ (0.8, 1.4) within 1e-6, `convergio()` is true, `getIteraciones() == 0`, `getErrorFinal() <= 1e-7`

> **Archive annotation (verify WARNING-2, 2026-11-17)**: the delivered test `verificaSolucionDirectaCanonica` additionally pins `getErrorFinal()` bit-exact to 8.881784197001252e-16 (±1e-15) — deliberately stronger than the normative `<= 1e-7` above, chosen for discriminating power (R6). It is a test-strength choice, not a tightening of this contract, and it couples the test to the exact float-operation order of `EliminacionGaussiana`: a future legitimate pivoting change may require relaxing that pin without changing this requirement.

#### Scenario: R1.3 State resets between runs

- GIVEN an instance whose previous `resolver()` converged
- WHEN `resolver()` runs again on that same instance (inputs are cloned at construction, so per-instance "changed inputs" is unreachable through the public API; reworded at archive per verify WARNING-1)
- THEN iteration/error/flag reflect only the new run: repeated runs on the same instance yield identical iteration count, error, and flag — no state leaks or accumulates across runs

### Requirement: R2 Convergence-Flag Honesty

A non-finite residual measurement MUST NOT yield `convergio() == true`: if the measured ‖r‖∞ is NaN or ±Infinity — from non-finite input data or intermediate overflow — the run MUST end with `convergio() == false`. This is a contract on the flag, mechanism-neutral: the fix MAY land in the solver's stop test or in `Normas.normaInfinito(double[])` (design decides). `resolver()` MUST keep returning the computed iterate unchanged, including non-finite entries; the solver MUST NOT sanitize output or classify finiteness (result-honesty R7 boundary preserved).

#### Scenario: R2.1 NaN input must not report convergence

- GIVEN A = {{1,2},{3,5}}, b = {1,NaN}
- WHEN `resolver()` runs
- THEN `convergio()` is false (the shipped defect reports true)
- AND the returned x is the computed vector, unmodified (contains NaN)

#### Scenario: R2.2 Intermediate overflow must not report convergence

- GIVEN a run whose residual becomes NaN or ±Infinity mid-loop
- WHEN the stop test evaluates that residual
- THEN the run ends with `convergio()` false

### Requirement: R3 Controller Integration

`resolver(..., SistemaController.MEJORAMIENTO)` MUST dispatch through the `ResolutorIterativo` `instanceof` branch and expose `Convergencia` copied from the solver, never hardcoded. `Estado` MUST follow result-honesty R2 precedence: non-finite solution → `NO_FINITO`; finite with `convergio() == false` → `NO_CONVERGIO`; otherwise `OK`.

#### Scenario: R3.1 Happy path metadata agreement

- GIVEN the canonical 2x2 system
- WHEN resolved via the controller as `"MEJORAMIENTO"`
- THEN estado is `OK`, `getConvergencia()` is non-null and equals the solver's own flag/count/error

#### Scenario: R3.2 Non-finite input with honest flag

- GIVEN b containing NaN
- WHEN resolved via the controller as `"MEJORAMIENTO"`
- THEN estado is `NO_FINITO`, `getSolucion()` is null
- AND `getConvergencia().convergio()` is false (post-R2; today it lies with true)

#### Scenario: R3.3 Singular input maps to ERROR

- GIVEN A = {{1,2},{2,4}}, b = {3,6}
- WHEN resolved via the controller as `"MEJORAMIENTO"`
- THEN estado is `ERROR` and the message names the failed method execution

#### Scenario: R3.4 No hardcoded success

- GIVEN any failing `MEJORAMIENTO` run
- WHEN its `Resultado` is read
- THEN `isExitoso()` is false and `"Resuelto con éxito.""` never appears

### Requirement: R4 Correctness Oracles

For systems with known solutions the solver MUST match hand-derived ground-truth values within tolerance, and MUST succeed where a plain stationary iteration diverges.

#### Scenario: R4.1 3x3 ground truth

- GIVEN A = {{4,1,2},{2,4,−1},{1,1,5}}, b = {9,−5,7} (det = 69)
- WHEN `resolver()` runs
- THEN x ≈ (47/23, −133/69, 95/69) each component within 1e-6 and `convergio()` is true

#### Scenario: R4.2 Refinement where plain Jacobi is NO_CONVERGIO

- GIVEN A = {{1,2},{3,4}}, b = {2,5} (not diagonally dominant; Jacobi exhausts its cap with `convergio() == false`)
- WHEN `resolver()` runs
- THEN x ≈ (1, 0.5) within 1e-6 and `convergio()` is true

### Requirement: R5 Edge Behaviors

Singular input MUST fail loudly: the underlying `ArithmeticException` from a missing pivot (initial approximation or correction solve) MUST propagate out of `resolver()` and the controller MUST map it to `ERROR`; the solver MUST NOT return a result claiming convergence for singular A. Cap exhaustion MUST be honest: after 1000 corrections without meeting tolerance, `convergio()` is false, `getIteraciones()` is 1000, and the last computed iterate is returned; a finite capped iterate surfaces as `NO_CONVERGIO` labelled not a solution (non-finite iterates keep `NO_FINITO` precedence).

#### Scenario: R5.1 Singular system raises and never claims convergence

- GIVEN A = {{1,2},{2,4}}, b = {3,6}
- WHEN `resolver()` runs
- THEN an `ArithmeticException` is thrown and `convergio()` is not true

#### Scenario: R5.2 Cap exhaustion reports non-convergence

- GIVEN a run whose residual never reaches ‖r‖∞ ≤ 1e-7 within the cap
- WHEN `resolver()` returns
- THEN `convergio()` is false, `getIteraciones() == 1000`, and the last iterate is returned

### Requirement: R6 Test Quality (normative for acceptance)

Delivered tests MUST name the break they catch: numerical assertions MUST compare hand-derived ground-truth values with explicit tolerance (1e-6 class), never only the `Ax ≈ b` proxy where the true solution is known; trivially-true assertions (e.g. `iteraciones >= 0` when 0 is the only passing value) MUST be replaced with discriminating bounds; scratch derivation comments MUST be removed; the honesty fix (R2.1) MUST be proven RED before any production change and remain RED under mutation-revert.

#### Scenario: R6.1 Ground truth over residual proxy

- GIVEN `verificaSistema3x3ConTraza`
- WHEN hardened
- THEN it asserts x against (47/23, −133/69, 95/69) within tolerance and contains no scratch comments

#### Scenario: R6.2 Assertions can fail

- GIVEN any test in the hardened `MejoramientoIterativoTest`
- WHEN the solver behavior it names is mutated (flag, count, or iterate)
- THEN at least one assertion fails

### Requirement: R7 Regression Firewall

Behavior on finite inputs MUST NOT change: Jacobi, GaussSeidel, SOR, RelajacionSimple and the direct methods keep identical verdicts on all existing tests (they compute their stop norms inline and are untouched by this change). If the R2 fix lands in `Normas.normaInfinito(double[])`, its result on finite vectors MUST remain `max|vᵢ|` (including 0.0 for empty); only the non-finite domain MAY change. `normaInfinito(Matriz)` and `AnalisisSensibilidad` MUST NOT change. Suite-count facts belong to test-execution-baseline R5, whose supersession delta is owned by the sibling change trace-label-fixes; this delta writes none.

#### Scenario: R7.1 Finite norm invariance

- GIVEN the finite-vector fixtures in `NormasTest`
- WHEN the suite runs after the fix
- THEN every existing assertion passes unmodified

#### Scenario: R7.2 Other solvers untouched

- GIVEN the finite systems covered by `JacobiTest`, `GaussSeidelTest`, `SORTest`, `RelajacionSimpleTest` and `SistemaControllerTest`
- WHEN `sh ./mvnw -o clean test` runs
- THEN all their existing tests pass with zero modifications to those files

## Test Mapping (historical, from the originating change)

Recorded for traceability. This section is not normative; the requirements above are the contract.

| Req | Proven by |
|---|---|
| R1.1 | `MejoramientoIterativoTest#verificaMejoramientoConX0InicialPerturbado` (existing) |
| R1.2 / R1.3 | `MejoramientoIterativoTest#verificaSolucionDirectaCanonica` (hardened) + new reset case |
| R2.1 | new `MejoramientoIterativoTest` NaN case (RED-first) |
| R2.2 | new `MejoramientoIterativoTest` overflow case (fixture selected in design) |
| R3.1 | `SistemaControllerTest#verificaMejoramientoIterativo` (existing) |
| R3.2–R3.4 | new `SistemaControllerTest` MEJORAMIENTO integration cases |
| R4.1 | `MejoramientoIterativoTest#verificaSistema3x3ConTraza` (hardened to ground truth) |
| R4.2 | new `MejoramientoIterativoTest` case + `JacobiTest`/controller NO_CONVERGIO counterpart |
| R5.1 | new `MejoramientoIterativoTest` singular case + `SistemaControllerTest` ERROR case |
| R5.2 | new `MejoramientoIterativoTest` cap case (fixture selected in design) |
| R6 | mutation check over the hardened `MejoramientoIterativoTest` |
| R7 | full-suite gate `sh ./mvnw -o clean test` |
