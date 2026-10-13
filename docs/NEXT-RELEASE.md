# Next release

- User confirmed on 2026-10-13 that the Windowed Glass retreat is fixed in 3.5.5-alpha.2.
- Glass strength defaults to 40% for new setups and Reset glass strength. Existing saved strengths remain selected. UI, renderer, screenshot path, report fallback and initial setup agree.
- Custom wallpaper unlock flash: partial success / largely resolved in 3.5.5-alpha.5. User reports no flashes after the off/on test, with possibly one on the first unlock, versus every unlock before preparation was implemented. Software logs also show a completed atomic Home handoff after re-enabling. Keep wake preparation ON by default in future builds, with the three-second-hold developer override. Defer residual first-unlock / missing-Home-eligibility investigation until after the main priorities; do not claim every edge case is fixed.

Remaining main priorities (owner order, 2026-10-13):
1. Screenshot mode first (owner update 2026-10-13), using its findings to improve standard mid-fold display handoff. Alpha.6 tests live incoming animation captures after exact app verification; Home routing and native endpoint switching remain unresolved.
2. Performance consistency.
3. Quick-close unanimated cover flash, last of the main priorities.

After the main priorities:
4. Intermittent rotation recovery.
5. On-device validation of direct root support.
6. Revisit residual custom wallpaper unlock flash edge cases.
