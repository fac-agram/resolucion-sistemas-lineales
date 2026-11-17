# Apply progress: mejoramiento-iterativo — transcripts & mutation evidence

Baseline at HEAD 8b40d42: `sh ./mvnw -o clean test` → Tests run: 67, Failures: 0, Errors: 0 (12 classes).

## RED C1 (Phase 1.3) — verbatim, before any production change

```
[ERROR] NormasTest.verificaNormaInfinitoPropagaNoFinitos:90 ‖(3,-4,NaN)‖∞ debe ser NaN, no 4.0 ==> expected: <NaN> but was: <4.0>
[ERROR] MejoramientoIterativoTest.verificaEntradaNaNNoDeclaraConvergencia:61 Un residuo NaN jamas puede declarar convergencia ==> expected: <false> but was: <true>
[ERROR] Tests run: 69, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

Matches design expectation: `Tests run: 69, Failures: 2` with `expected: <false> but was: <true>` + norm NaN mismatch.

## GREEN C1 (Phase 2.2)
`sh ./mvnw -o clean test` → `Tests run: 69, Failures: 0, Errors: 0` — commit 901bd84 (52+/5−).

## GREEN C2 (Phase 5.1)
`Tests run: 76, Failures: 0, Errors: 0` — MejoramientoIterativoTest 9, NormasTest 6, SistemaControllerTest 24, 12 classes — commit 70a0602 (171+/11−).

## Mutation gates (Phase 6.4) — local, reverted, never committed

**A) Revert fix (skip-comparison) → 5 NaN tests red, 71 green** (≥2 required):
```
[ERROR]   NormasTest.verificaNormaInfinitoPropagaNoFinitos:90 expected: <NaN> but was: <4.0>
[ERROR]   SistemaControllerTest.verificaMejoramientoNoFinitoConBanderasHonestas:255 expected: <false> but was: <true>
[ERROR]   MejoramientoIterativoTest.verificaDesbordeIntermedioNoDeclaraConvergencia:132 expected: <false> but was: <true>
[ERROR]   MejoramientoIterativoTest.verificaEntradaNaNNoDeclaraConvergencia:66 expected: <false> but was: <true>
[ERROR]   MejoramientoIterativoTest.verificaTopeAgotadoDevuelveUltimoIterado:194 expected: <false> but was: <true>
[ERROR] Tests run: 76, Failures: 5
```
Restored → re-gate 76/0/0.

**B) TOLERANCIA → 1e-16 → 3 red** (canonical + perturbed as planned; controller happy path extra-protected):
```
[ERROR]   SistemaControllerTest.verificaMejoramientoIterativo:88 expected: <true> but was: <false>
[ERROR]   MejoramientoIterativoTest.verificaMejoramientoConX0InicialPerturbado:48 expected: <true> but was: <false>
[ERROR]   MejoramientoIterativoTest.verificaSolucionDirectaCanonica:30 expected: <true> but was: <false>
```
Restored → re-gate 76/0/0.

**C) MAX_ITERACIONES → 999 → 3 red** (cap witnesses):
```
[ERROR]   SistemaControllerTest.verificaMejoramientoNoFinitoConBanderasHonestas:256 expected: <1000> but was: <999>
[ERROR]   MejoramientoIterativoTest.verificaDesbordeIntermedioNoDeclaraConvergencia:133 expected: <1000> but was: <999>
[ERROR]   MejoramientoIterativoTest.verificaTopeAgotadoDevuelveUltimoIterado:195 expected: <1000> but was: <999>
```
Restored → re-gate 76/0/0.

## Phase 6 static gates
- `git diff --name-only HEAD~2` = Normas.java + 3 test files; Jacobi/GaussSeidel/SOR/RelajacionSimple + their tests zero-modified.
- `grep -rn "Double.isFinite|Double.isNaN|Double.isInfinite" src/unsa/sistemas/model/` → no matches. NOTE: the launch prompt's "anywhere in src/" scope is impossible by plan itself — pre-existing honest UI-boundary tokens live in controller/SistemaController.java:241 and view/SistemaFXController.java (both in the Do-NOT-touch list); design/tasks/spec define the gate at model/ scope, which is clean.
- `getIteraciones() >= 0` and `Wait:` greps in MejoramientoIterativoTest → no matches.

## Handoff (Phase 7)
k_s=9, c_s=0, M′=76/12 → sibling trace-label-fixes final 82/12. Rollback: git revert 70a0602 then 901bd84 (C2 alone keeps honest fix at 69).
