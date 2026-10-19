# Motion glass experiment — 3.5.7-alpha.2

Developer settings → Motion glass → Motion-driven glass. Off by default. Live Screen only; screenshot mode and static settings previews retain their previous behavior.

Controls: enable, reverse direction, maximum source-image shift (0–10%, default 3.5%), smoothing (0–300 ms, default 90 ms), recenter, reset tuning. Sensor sampling requests approximately 60 Hz. The first fresh rotation-vector sample after leaving a fold endpoint establishes the neutral orientation; the reference persists through primary-display handoff. The first sample after a sensor gap over 500 ms re-establishes neutral. Recenter can be used during a partial fold.

Uses the non-wakeup game rotation vector, with rotation vector fallback. Relative pitch/roll move only the refraction sampling position, bounded at 30 degrees of tilt and faded near zero effect. This is simulated parallax, not eye tracking or translation measurement. Display rotation remaps the offset axes; no system rotation settings, hinge values, handoff thresholds, captures, or permissions are changed. Motion updates request a vsync-coalesced draw of the existing capture. Sensors stop at fold endpoints, with the feature/animation off, on stale hinge input, screen off/lock, or service destruction. Unsupported sensors leave the original effect intact.

Handset acceptance: compare feature off/on at a fixed partial fold on both panels, tilt in both axes, check all display rotations, reverse direction, zero/max strength, zero/max smoothing, and recenter. Verify full-open/full-close clean endpoints, Stop all and screen lock. Repeat auto-rotate tests after fold cycles. Share the usage report's Motion glass line. Automated checks do not establish sensor availability, physical direction, visual quality, or handoff latency on the phone.

Rotation troubleshooting before this build: owner reports turning animation off, using Repair auto-rotate, then re-enabling resolved the issue. No rotation-policy patch is included. Separate stock-handoff diagnostic app remains the next task after this experiment.
