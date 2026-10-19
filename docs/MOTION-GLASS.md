# Motion glass experiment — 3.5.7-alpha.3

Developer settings → Motion glass → Motion-driven glass. Off by default. Live Screen only; screenshot mode and static settings previews retain their previous behavior.

Alpha.3 replaces alpha.2’s small texture translation with an inverse geometry warp before projection, stretch, blur, and shading. Sideways tilt expands/contracts the projection from the fixed left edge; the other tilt axis skews it progressively away from that edge. On the primary inner display, both the left edge and clean right half/seam remain fixed. The reflected cover preview compensates for its canvas flip when choosing its anchor. Existing neutral geometry is unchanged, including with motion disabled or response zero.

Controls: enable, reverse direction, projection response (0–200%, default 100%), smoothing (0–300 ms, default 90 ms), recenter, reset tuning. The new projection-response preference deliberately does not reuse alpha.2’s small image-shift value. At 100% response, a 30-degree tilt gives a 2x/0.5x cover geometry scale; 200% permits 4x/0.25x. Inner geometry redistributes stretch between pinned boundaries without moving the clean reference half.

Sensor sampling requests approximately 60 Hz. The first fresh rotation-vector sample after leaving a fold endpoint establishes neutral; the reference persists through primary-display handoff unless the service stops the effect. A sensor gap over 500 ms re-establishes neutral. Recenter is available during a partial fold.

Uses the non-wakeup game rotation vector, with rotation vector fallback. This is simulated viewpoint motion, not gaze tracking or translation measurement. No system rotation settings, hinge values, handoff thresholds, captures, or permissions are changed. Sensor updates redraw existing captures. Sensors stop at fold endpoints, with feature/animation off, on stale hinge input, screen off/lock, or service destruction. Unsupported sensors leave the original effect intact.

Validation: tools/check_motion_projection.py compiles the actual shared shader warp and checks zero-tilt identity, the pinned edge, clean inner half, all display rotations and reflected canvas, bounded monotonic inner geometry, and opposite tilt directions. Existing inner-stretch and shading checks remain in CI.

Handset acceptance: use a detailed wallpaper or grid, enable motion, partially open and hold the hinge still, then tilt left/right and up/down. Watch the cover left edge and the primary inner left edge for drift. Check the clean right preview stays still, including through handoff. Repeat while closing, in each display rotation, with reflected preview enabled. Test reverse direction, response zero/max, smoothing zero/max, recenter, full endpoints, and lock/stop. Share the usage report. Automated checks do not establish physical sensor direction, visual quality, or handset handoff latency.

Existing Repair auto-rotate resolved the owner's rotation issue. No rotation-policy patch is included. Main/stable and Play remain unchanged. The separate stock-handoff diagnostic app remains deferred until this experiment is tested.
