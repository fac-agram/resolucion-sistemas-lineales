# result-honesty

<!--
Provenance
- Capability: result-honesty (new capability)
- Origin change: honest-results
- Source: openspec/changes/archive/2026-11-17-honest-results/specs/result-honesty/spec.md
- Synced: 2026-11-17
- Origin verdict: pass_with_warnings (zero CRITICAL; commits 8e0f854 + 6ab9fb9, squashed, on base
  3f2fd7e; archived at HEAD 96f0b7d; see archive-report.md and verify-report.md in the archived change)
- Requirements R1-R7 and their scenarios (R1.1-R7.1, 14 scenarios) are preserved verbatim from the
  delta spec; ids unchanged. No scenario contains a test-count pin (grep-verified at merge): the
  suite at HEAD is 82 tests / 12 classes; the +23 tests / +1 class over this change's own tip
  (59 / 11 at 6ab9fb9) are sibling-attributed (mejoramiento-iterativo, trace-label-fixes).
- Carried WARNING (verify WARNING-2): the 8-row manual GUI checklist (tasks 3.7) was never signed
  off. R6 rendering copy is proven by the byte-exact pure-mapper test (verificaTextosDeEstado),
  3 mutation reproductions and view source inspection — not by a live GUI run.
- Carried WARNING (verify test-layer note): no TestFX/Monocle oracle exists for the R6
  view-application half ("the view MUST apply it"). By design (design.md: TestFX absent) that half
  is carried statically: single exhaustive switch on getEstado(), null-guard before formatting,
  zero success literals in the view (grep-clean). GUI sign-off and a scripted GUI pass remain
  backlog items — see archive-report.md.
-->

## Purpose

Controller-view outcome contract for honest results. Every `Resultado` carries one of four states
(`OK`, `NO_CONVERGIO`, `NO_FINITO`, `ERROR`); classification precedence, state-derived messages,
per-state solution exposure, backward-compatible accessors, state-driven rendering through one pure
JavaFX-free state-to-text mapper, and the rule that non-finite classification stays in the
controller while solvers keep returning raw `NaN`/`±Infinity`.

## Requirements

### Requirement: R1 Outcome State Model

`Resultado` MUST expose `getEstado()`: exactly one of `OK`, `NO_CONVERGIO`, `NO_FINITO`, `ERROR`. Outcome MUST NOT be a boolean alone.

#### Scenario: R1.1 One accessor, four states

- GIVEN any `resolver()` result
- WHEN its state is read
- THEN it is one of four enum values

### Requirement: R2 Classification Precedence

Classification order MUST be: structural failure → `ERROR`; non-finite solution element → `NO_FINITO`; iterative not converged → `NO_CONVERGIO`; else → `OK`. Non-finite detection MUST precede convergence.

#### Scenario: R2.1 Non-finite beats non-convergence

- GIVEN an iterative run overflowing to `NaN`/`±Infinity`, `convergio() == false`
- WHEN resolved
- THEN the state is `NO_FINITO`

#### Scenario: R2.2 Finite, not converged

- GIVEN a finite iterate at the iteration cap with `convergio() == false`
- WHEN resolved
- THEN the state is `NO_CONVERGIO`

#### Scenario: R2.3 Finite, converged or direct

- GIVEN a converging iterative run or a solvable direct method
- WHEN resolved
- THEN the state is `OK`

#### Scenario: R2.4 Structural failure

- GIVEN an unknown/null method, bad dimensions, or a throwing/null solver
- WHEN resolved
- THEN the state is `ERROR`

### Requirement: R3 State-Derived Messages

Messages MUST derive from the state, be non-empty, and name the condition. `"Resuelto con éxito."` MUST appear only for `OK`.

#### Scenario: R3.1 Success text gated by state

- GIVEN one result per state
- WHEN messages are read
- THEN only `OK` messages contain `"Resuelto con éxito."`

### Requirement: R4 Solution Exposure

`NO_FINITO`/`ERROR` MUST expose null `getSolucion()`. `NO_CONVERGIO` MUST expose the last finite iterate labelled not a solution. `OK` MUST expose a finite solution.

#### Scenario: R4.1 No solution on failure

- GIVEN `NO_FINITO` and `ERROR` results
- WHEN `getSolucion()` is read
- THEN both are null

#### Scenario: R4.2 Last iterate labelled

- GIVEN a `NO_CONVERGIO` result
- WHEN solution and message are read
- THEN the solution is non-null and finite
- AND the message contains `"no es una solución del sistema"`

### Requirement: R5 Accessor Compatibility

`isExitoso()` MUST be true iff the state is `OK`. `isAdvertencia()` MUST equal `estado == NO_CONVERGIO`, never a flag. Direct-method success MUST be unchanged.

#### Scenario: R5.1 isExitoso only for OK

- GIVEN results in `NO_CONVERGIO`, `NO_FINITO`, `ERROR`
- WHEN `isExitoso()` is read
- THEN it is false for all, true only for `OK`

#### Scenario: R5.2 Direct success unchanged

- GIVEN the canonical 3x3 system, each direct method
- WHEN resolved
- THEN `isExitoso()` is true, the solution matches within `1e-6`, convergence is null

### Requirement: R6 State-Driven Rendering

Views MUST branch on `getEstado()`, not `isExitoso()`/`isAdvertencia()`. Exactly three renderings — ok, warning, error — MUST exist with distinct headings and per-state status text. The state-to-text mapping MUST be pure, JavaFX-free and headlessly unit-testable; the view MUST apply it.

#### Scenario: R6.1 OK rendering

- GIVEN an `OK` result
- WHEN mapped
- THEN heading `"SOLUCIÓN DEL SISTEMA"`, status `"Sistema resuelto correctamente."`

#### Scenario: R6.2 Warning rendering

- GIVEN a `NO_CONVERGIO` result
- WHEN mapped
- THEN heading `"ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)"`, status `"El método no convergió. Se muestra la última aproximación."`
- AND no `"SOLUCIÓN DEL SISTEMA"` heading appears

#### Scenario: R6.3 Error rendering

- GIVEN `NO_FINITO` and `ERROR` results
- WHEN mapped
- THEN both use heading `"NO SE PUDO RESOLVER EL SISTEMA"`
- AND statuses are `"Error: el resultado no es finito."` and `"Error durante la resolución."`
- AND no solution or success text rendered

### Requirement: R7 Classification Stays in the Controller

Solvers MUST NOT gain finiteness guards and MUST keep returning computed `NaN`/`±Infinity`. Non-finite classification MUST live only in the controller.

#### Scenario: R7.1 No solver changes

- GIVEN the change applied
- WHEN `git diff --stat` and a `Double.isFinite`/`Double.isNaN` search in `src/unsa/sistemas/model/` run
- THEN no model file changed and no solver `resolver()` gained a finiteness guard
