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
        "",
        "## Metrics",
        "",
        "| Metric | Value |",
        "|---|---:|",
    ]
    for name, value in sorted(summary["metrics"].items()):
        lines.append(f"| `{name}` | {value:.4f} |")
    lines.extend(["", "## Gates", "", "| Metric | Value | Status |", "|---|---:|---|"])
    for gate in report["gates"]:
        value = "n/a" if gate["value"] is None else f"{gate['value']:.4f}"
        lines.append(f"| `{gate['metric']}` | {value} | {gate['status']} |")
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
