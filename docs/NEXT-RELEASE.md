# Next release

- User confirmed on 2026-10-13 that the Windowed Glass retreat is fixed in 3.5.5-alpha.2.
- Glass strength defaults to 40% for new setups and Reset glass strength. Existing saved strengths remain selected. UI, renderer, screenshot path, report fallback and initial setup agree.

Remaining priorities:
1. Quick-close unanimated cover flash: still unconfirmed fixed; alpha.1 recorded 104–506 ms from app remap observation to first glass transaction commit.
2. Custom wallpaper can briefly reveal Samsung wallpaper on unlock.
3. Mid-fold physical display remap continuity: separate from the successful full-close fix; software OFF samples are not optical timing.
4. Intermittent rotation release verification failure appeared in a 3.5.4 report, followed by successful verification; reproduction needed.
5. Screenshot mode remains experimental.
6. Direct root backend needs testing on a rooted Galaxy, including restart/reboot and wallpaper setup.
7. Capture performance varies; target FPS is not delivered capture FPS.
