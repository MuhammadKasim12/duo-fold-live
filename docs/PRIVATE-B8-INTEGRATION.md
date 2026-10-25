# B8 integration checkpoint

The user authorized public pre-release delivery after this branch was created.
See `3.5.9-alpha.2-CHANGELOG.md` for the shipped scope and instructions.

The B8 0.5.11 manual test suite is now embedded under Developer settings, off by
default. Its observer is a separate Shizuku process. This is not yet the automatic
hinge-driven animation replacement. Timings, mirrors and brightness behavior are
preserved. The only core adaptation is the built-in task's package identity.

`B8-BASELINE-SHA256.json` records original engine hashes. Run
`python3 tools/check_b8_baseline.py` to verify these allowed relocations and the
portable guard/recovery tests. Set ANDROID_HOME for the core Android compile.
The existing GitHub signed workflow builds and publishes the pre-release; no
signing secrets are copied into source or exposed through logs/artifacts.

The source also retains the previously published alpha.1 stale-angle fix.
The standalone Lab 0.5.11 checkpoint remains untouched. Occasional flash tuning
and the historical mirror layer-cycle failure are not claimed fixed.
