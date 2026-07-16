from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def write_report(report: dict[str, Any], output_dir: Path, formats: list[str]) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    if "json" in formats:
        (output_dir / "report.json").write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    if "markdown" in formats:
        (output_dir / "report.md").write_text(_markdown(report), encoding="utf-8")


def _markdown(report: dict[str, Any]) -> str:
    summary = report["summary"]
    lines = [
        "# PhoneMall evaluation report",
        "",
        f"- Gate: **{summary['gate']}**",
        f"- Cases: {summary['completed_cases']}/{summary['total_cases']} completed",
        f"- Completion rate: {summary['completion_rate']:.3f}",
        f"- Retryable error rate: {summary['retryable_error_rate']:.3f}",
        f"- Budget stop: {summary['budget_stop_reason'] or 'none'}",
        "",
        "## Metrics",
        "",
        "| Metric | Value |",
        "|---|---:|",
    ]
    for name, value in sorted(summary["metrics"].items()):
        lines.append(f"| `{name}` | {value:.4f} |")
    lines.extend([
        "",
        "## Gates",
        "",
        "| Metric | Current | Baseline | Delta | Status |",
        "|---|---:|---:|---:|---|",
    ])
    for gate in report["gates"]:
        value = "n/a" if gate["value"] is None else f"{gate['value']:.4f}"
        baseline = "n/a" if gate["baseline"] is None else f"{gate['baseline']:.4f}"
        delta = "n/a" if gate["delta"] is None else f"{gate['delta']:+.4f}"
        lines.append(
            f"| `{gate['metric']}` | {value} | {baseline} | {delta} | "
            f"{gate['status']} |"
        )
    baseline_diff = summary.get("baseline_comparison")
    if baseline_diff:
        lines.extend([
            "",
            "## Baseline comparison",
            "",
            f"- Status: {baseline_diff['status']}",
            f"- Baseline revision: {baseline_diff['baseline_revision']}",
            f"- Comparable cases: {len(baseline_diff['comparable_cases'])}",
            f"- Changed cases: {', '.join(baseline_diff['changed_cases']) or 'none'}",
            f"- New cases: {', '.join(baseline_diff['new_cases']) or 'none'}",
            f"- Removed cases: {', '.join(baseline_diff['removed_cases']) or 'none'}",
            f"- New failures: {', '.join(baseline_diff['new_failures']) or 'none'}",
            f"- Fixed cases: {', '.join(baseline_diff['fixed_cases']) or 'none'}",
            "",
            "| Metric | Delta |",
            "|---|---:|",
        ])
        for name, delta_value in baseline_diff["metric_deltas"].items():
            lines.append(f"| `{name}` | {delta_value:+.4f} |")
    lines.extend(["", "## Cases", ""])
    for case in report["cases"]:
        marker = "PASS" if not case["failures"] and case["status"] == "completed" else "FAIL"
        lines.append(f"### {case['case_id']} — {marker}")
        lines.append("")
        lines.append(f"- Status: {case['status']}")
        for name, value in sorted(case["metrics"].items()):
            lines.append(f"- `{name}`: {value:.4f}")
        for failure in case["failures"]:
            lines.append(f"- Failure: {failure}")
        if case.get("error"):
            lines.append(f"- Error: {case['error']['message']}")
        lines.append("")
    return "\n".join(lines)
