package org.duofold.live;

/** Alpha-only baseline: normal live handoff never waits for visual preparation.
 * Keeps hinge thresholds, prewarm, ownership and lock/freshness checks unchanged.
 * Screenshot routing is unaffected. Remove this switch after the comparison.
 */
final class HandoffBaseline {
 static final boolean ENABLED=true;
 static final String REPORT="Handoff baseline: ON; preparation gates bypassed; black fade and expansion holds disabled; renderer catches up after switch";
 private HandoffBaseline(){}
}
