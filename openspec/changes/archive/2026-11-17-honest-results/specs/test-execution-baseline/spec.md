# Delta for test-execution-baseline

## MODIFIED Requirements

### Requirement: R2 Test Execution

`sh ./mvnw -o clean test` MUST execute the full discovered JUnit 5 test set, exit 0, and report `Failures: 0, Errors: 0, Skipped: 0`. It MUST write one `TEST-*.xml` per test class and leave no `*.dump`/`*.dumpstream`. Surefire MUST use its JUnit Platform provider; the `JUnit3Provider` fallback with `Tests run: 0` MUST NOT occur. The expected total test count MUST be maintained in `openspec/config.yaml`; this requirement SHALL NOT pin a literal count.
(Previously: pinned the literal `Tests run: 52`.)

> Why the literal pin is removed: adding tests is legitimate behavior, and a literal pin turns the anti-false-green check into a change-detector that fails on every valid test addition. The count remains enforced, but against the single source in `openspec/config.yaml`.

#### Scenario: R2.1 Green baseline

- GIVEN the change is fully applied
- WHEN `sh ./mvnw -o clean test` runs
- THEN the output contains `Tests run: <expected>, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`
- AND `<expected>` equals the test total declared in `openspec/config.yaml`

#### Scenario: R2.2 Real runner, per-class reports

- GIVEN a clean `target/`
- WHEN the same command runs
- THEN `target/surefire-reports/` holds 11 `TEST-*.xml` reports with no `Tests run: 0`, `JUnit3Provider`, or fork dump
