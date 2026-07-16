from __future__ import annotations

import copy
import hashlib
import json
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .adapters import HttpAdapter
from .baseline import (
    BaselineError,
    compare_baseline,
    create_baseline,
    load_baseline,
    write_baseline,
)
from .config import ConfigError, load_config
from .loader import DatasetError, load_dataset
from .metrics import aggregate, evaluate_case, evaluate_gates
from .models import CaseResult
from .report import write_report


def run(config_path: Path, baseline_output: Path | None = None) -> int:
    root = Path.cwd().resolve()
    try:
        config = load_config(config_path)
        dataset_path = (root / config["dataset"]["path"]).resolve()
        cases, dataset_hash = load_dataset(dataset_path)
        adapter = HttpAdapter(config["adapter"], root)
        config_hash = _hash(_comparison_config(config))
        metric_hash = _hash(config["metrics"])
        baseline = _load_configured_baseline(config, root, baseline_output)
    except (
            BaselineError,
            ConfigError,
            DatasetError,
            ValueError,
            RuntimeError,
    ) as exception:
        print(f"configuration error: {exception}")
        return 1

    started_at = datetime.now(timezone.utc)
    execution_started_at = time.perf_counter()
    retries = config["execution_policy"]["retries"]
    budget = config.get("run_budget", {})
    results: list[CaseResult] = []
    cases_with_retryable_errors = 0
    estimated_cost = 0.0
    budget_reason = None
    for index, case in enumerate(cases):
        budget_reason = _budget_reason(
            budget,
            index,
            execution_started_at,
            estimated_cost,
        )
        if budget_reason:
            results.extend(
                _budget_error(remaining.case_id, budget_reason)
                for remaining in cases[index:]
            )
            break
        result = adapter.execute(case)
        attempt = 0
        had_retryable_error = bool(
            result.status == "error"
            and result.error
            and result.error.get("retryable")
        )
        while (
                result.status == "error"
                and result.error
                and result.error.get("retryable")
                and attempt < retries
        ):
            attempt += 1
            result = adapter.execute(case)
            had_retryable_error = had_retryable_error or bool(
                result.status == "error"
                and result.error
                and result.error.get("retryable")
            )
        if had_retryable_error:
            cases_with_retryable_errors += 1
        results.append(result)
        cost = result.usage.get("estimated_cost")
        if isinstance(cost, (int, float)):
            estimated_cost += float(cost)

    recall_k = _recall_k(config["metrics"])
    evaluations = [
        evaluate_case(case, result, recall_k)
        for case, result in zip(cases, results, strict=True)
    ]
    completed = sum(result.status == "completed" for result in results)
    completion_rate = completed / len(cases)
    aggregate_metrics = aggregate(
        [
            evaluation.metrics
            for result, evaluation in zip(results, evaluations, strict=True)
            if result.status == "completed"
        ]
    )
    baseline_diff = None
    baseline_metrics = {}
    if baseline is not None:
        baseline_diff = compare_baseline(
            baseline,
            config,
            dataset_hash,
            config_hash,
            metric_hash,
            cases,
            results,
            evaluations,
            aggregate_metrics,
            config["gates"],
        )
        if baseline_diff["summary_comparable"]:
            baseline_metrics = baseline.get("summary", {})
    gates = evaluate_gates(
        aggregate_metrics,
        config["gates"],
        baseline_metrics,
    )
    policy = config["execution_policy"]
    retryable_error_rate = cases_with_retryable_errors / len(cases)
    untrusted = (
        completion_rate < policy["min_completion_rate"]
        or retryable_error_rate > policy["max_retryable_error_rate"]
        or completed != len(cases)
    )
    quality_failed = any(
        gate["status"] == "fail" and gate["severity"] == "error"
        for gate in gates
    )
    gate_status = "untrusted" if untrusted else ("fail" if quality_failed else "pass")

    report = {
        "schema_version": "1",
        "run": {
            "project_name": config["project"]["name"],
            "revision": config["project"]["revision"],
            "dataset_hash": dataset_hash,
            "config_hash": config_hash,
            "metric_hash": metric_hash,
            "started_at": started_at.isoformat(),
            "completed_at": datetime.now(timezone.utc).isoformat(),
        },
        "summary": {
            "gate": gate_status,
            "total_cases": len(cases),
            "completed_cases": completed,
            "completion_rate": completion_rate,
            "retryable_error_rate": retryable_error_rate,
            "estimated_cost": estimated_cost,
            "budget_stop_reason": budget_reason,
            "metrics": aggregate_metrics,
            "baseline_comparison": baseline_diff,
        },
        "gates": gates,
        "cases": [
            {
                "case_id": case.case_id,
                "case_hash": case.case_hash,
                "status": result.status,
                "metrics": evaluation.metrics,
                "failures": list(evaluation.failures),
                "error": result.error,
            }
            for case, result, evaluation in zip(cases, results, evaluations, strict=True)
        ],
    }
    output_dir = (root / config["report"]["output_dir"]).resolve()
    write_report(report, output_dir, config["report"]["formats"])
    if baseline_output is not None and not untrusted:
        baseline_document = create_baseline(
            config,
            dataset_hash,
            config_hash,
            metric_hash,
            cases,
            results,
            evaluations,
            aggregate_metrics,
        )
        write_baseline((root / baseline_output).resolve(), baseline_document)
    print(
        f"Eval42 {gate_status}: {completed}/{len(cases)} cases completed; "
        f"reports written to {output_dir}"
    )
    if untrusted:
        return 3
    if quality_failed:
        return 2
    return 0


def _recall_k(metrics: list[dict[str, Any]]) -> int:
    for metric in metrics:
        if metric.get("type") == "recall_at_k":
            return int(metric.get("k", 5))
    return 5


def _hash(value: Any) -> str:
    canonical = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return "sha256:" + hashlib.sha256(canonical).hexdigest()


def _comparison_config(config: dict[str, Any]) -> dict[str, Any]:
    comparable = copy.deepcopy(config)
    comparable["project"]["revision"] = "<target-revision>"
    comparable.pop("baseline", None)
    comparable.pop("report", None)
    if str(comparable["adapter"]["base_url"]).startswith("mock://"):
        comparable["adapter"]["base_url"] = "mock://<fixture>"
    headers = comparable.get("adapter", {}).get("headers", {})
    comparable["adapter"]["headers"] = {
        name: "<redacted>"
        for name in headers
    }
    return comparable


def _load_configured_baseline(
        config: dict[str, Any],
        root: Path,
        baseline_output: Path | None,
) -> dict[str, Any] | None:
    baseline_config = config.get("baseline")
    if not baseline_config:
        return None
    path = (root / baseline_config["path"]).resolve()
    if not path.exists():
        if baseline_output is not None:
            return None
        raise BaselineError(f"configured baseline does not exist: {path}")
    return load_baseline(path)


def _budget_reason(
        budget: dict[str, Any],
        executed_cases: int,
        started_at: float,
        estimated_cost: float,
) -> str | None:
    if budget.get("max_cases") is not None and executed_cases >= budget["max_cases"]:
        return "max_cases"
    if (
            budget.get("max_duration_seconds") is not None
            and time.perf_counter() - started_at >= budget["max_duration_seconds"]
    ):
        return "max_duration_seconds"
    if (
            budget.get("max_estimated_cost") is not None
            and estimated_cost >= budget["max_estimated_cost"]
    ):
        return "max_estimated_cost"
    return None


def _budget_error(case_id: str, reason: str) -> CaseResult:
    return CaseResult(
        case_id=case_id,
        status="error",
        error={
            "type": "budget_exhausted",
            "message": f"case was not run because {reason} was reached",
            "retryable": False,
        },
    )
