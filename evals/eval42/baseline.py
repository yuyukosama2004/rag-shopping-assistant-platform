from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .models import CaseEvaluation, CaseResult, DatasetCase


class BaselineError(ValueError):
    pass


ADAPTER_VERSION = "phonemall-http-v1"
EVAL42_VERSION = "phonemall-phase2"


def load_baseline(path: Path) -> dict[str, Any]:
    try:
        baseline = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise BaselineError(f"unable to load baseline {path}: {exception}") from exception
    if baseline.get("schema_version") != "1":
        raise BaselineError("only baseline schema_version 1 is supported")
    required = {
        "project_name",
        "dataset",
        "run_fingerprint",
        "summary",
        "cases",
    }
    missing = sorted(required - baseline.keys())
    if missing:
        raise BaselineError(
            "baseline is missing fields: " + ", ".join(missing)
        )
    if not isinstance(baseline["cases"], list):
        raise BaselineError("baseline.cases must be an array")
    for index, case in enumerate(baseline["cases"]):
        if not isinstance(case, dict):
            raise BaselineError(f"baseline.cases[{index}] must be an object")
        case_missing = sorted(
            {"case_id", "case_hash", "status", "metrics"} - case.keys()
        )
        if case_missing:
            raise BaselineError(
                f"baseline.cases[{index}] is missing fields: "
                + ", ".join(case_missing)
            )
    return baseline


def create_baseline(
        config: dict[str, Any],
        dataset_hash: str,
        config_hash: str,
        metric_hash: str,
        cases: list[DatasetCase],
        results: list[CaseResult],
        evaluations: list[CaseEvaluation],
        summary_metrics: dict[str, float],
) -> dict[str, Any]:
    versions = {
        str(case.metadata.get("dataset_version", "unknown"))
        for case in cases
    }
    dataset_version = versions.pop() if len(versions) == 1 else "mixed"
    return {
        "schema_version": "1",
        "project_name": config["project"]["name"],
        "created_at": datetime.now(timezone.utc).isoformat(),
        "dataset": {
            "version": dataset_version,
            "hash": dataset_hash,
        },
        "run_fingerprint": {
            "target_revision": config["project"]["revision"],
            "adapter_version": ADAPTER_VERSION,
            "eval42_version": EVAL42_VERSION,
            "config_hash": config_hash,
            "metric_hash": metric_hash,
            "environment": _environment(config),
        },
        "summary": summary_metrics,
        "cases": [
            {
                "case_id": case.case_id,
                "case_hash": case.case_hash,
                "status": result.status,
                "metrics": evaluation.metrics,
            }
            for case, result, evaluation in zip(
                cases,
                results,
                evaluations,
                strict=True,
            )
        ],
    }


def compare_baseline(
        baseline: dict[str, Any],
        config: dict[str, Any],
        dataset_hash: str,
        config_hash: str,
        metric_hash: str,
        cases: list[DatasetCase],
        results: list[CaseResult],
        evaluations: list[CaseEvaluation],
        summary_metrics: dict[str, float],
        gates: list[dict[str, Any]],
) -> dict[str, Any]:
    baseline_cases = {
        item["case_id"]: item
        for item in baseline.get("cases", [])
    }
    current_cases = {case.case_id: case for case in cases}
    comparable = sorted(
        case_id
        for case_id in current_cases.keys() & baseline_cases.keys()
        if current_cases[case_id].case_hash == baseline_cases[case_id]["case_hash"]
    )
    changed = sorted(
        case_id
        for case_id in current_cases.keys() & baseline_cases.keys()
        if current_cases[case_id].case_hash != baseline_cases[case_id]["case_hash"]
    )
    new_cases = sorted(current_cases.keys() - baseline_cases.keys())
    removed_cases = sorted(baseline_cases.keys() - current_cases.keys())

    fingerprint = baseline.get("run_fingerprint", {})
    fingerprint_mismatches = []
    expected_fingerprint = {
        "adapter_version": ADAPTER_VERSION,
        "eval42_version": EVAL42_VERSION,
        "config_hash": config_hash,
        "metric_hash": metric_hash,
        "environment": _environment(config),
    }
    for field, expected in expected_fingerprint.items():
        if fingerprint.get(field) != expected:
            fingerprint_mismatches.append(field)
    if baseline.get("project_name") != config["project"]["name"]:
        fingerprint_mismatches.append("project_name")

    summary_comparable = (
        baseline.get("dataset", {}).get("hash") == dataset_hash
        and not fingerprint_mismatches
    )
    metric_deltas = {}
    if summary_comparable:
        for name in sorted(set(summary_metrics) | set(baseline.get("summary", {}))):
            current = summary_metrics.get(name)
            previous = baseline.get("summary", {}).get(name)
            if current is not None and previous is not None:
                metric_deltas[name] = current - previous

    current_by_id = {
        case.case_id: (result, evaluation)
        for case, result, evaluation in zip(
            cases,
            results,
            evaluations,
            strict=True,
        )
    }
    new_failures = []
    fixed = []
    for case_id in comparable:
        current_result, current_evaluation = current_by_id[case_id]
        current_failed = _case_failed(
            current_result.status,
            current_evaluation.metrics,
            gates,
        )
        previous = baseline_cases[case_id]
        previous_failed = _case_failed(
            previous["status"],
            previous.get("metrics", {}),
            gates,
        )
        if current_failed and not previous_failed:
            new_failures.append(case_id)
        if previous_failed and not current_failed:
            fixed.append(case_id)

    dataset_changed = baseline.get("dataset", {}).get("hash") != dataset_hash
    if fingerprint_mismatches:
        status = "incomparable"
    elif dataset_changed or changed or new_cases or removed_cases:
        status = "partially_comparable"
    else:
        status = "comparable"
    return {
        "status": status,
        "baseline_revision": fingerprint.get("target_revision"),
        "summary_comparable": summary_comparable,
        "fingerprint_mismatches": sorted(set(fingerprint_mismatches)),
        "comparable_cases": comparable,
        "changed_cases": changed,
        "new_cases": new_cases,
        "removed_cases": removed_cases,
        "metric_deltas": metric_deltas,
        "new_failures": new_failures,
        "fixed_cases": fixed,
    }


def write_baseline(path: Path, baseline: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(baseline, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def _case_failed(
        status: str,
        metrics: dict[str, float],
        gates: list[dict[str, Any]],
) -> bool:
    if status != "completed":
        return True
    for gate in gates:
        value = metrics.get(gate["metric"])
        if value is None:
            continue
        if "min" in gate and value < float(gate["min"]):
            return True
        if "max" in gate and value > float(gate["max"]):
            return True
    return False


def _environment(config: dict[str, Any]) -> str:
    return (
        "fixture"
        if str(config["adapter"]["base_url"]).startswith("mock://")
        else "live"
    )
