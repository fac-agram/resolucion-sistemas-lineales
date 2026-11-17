# Delta for result-honesty

## ADDED Requirements

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
