from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .adapters import HttpAdapter
from .config import ConfigError, load_config
from .loader import DatasetError, load_dataset
from .metrics import aggregate, evaluate_case, evaluate_gates
from .report import write_report


def run(config_path: Path) -> int:
    root = Path.cwd().resolve()
    try:
        config = load_config(config_path)
        dataset_path = (root / config["dataset"]["path"]).resolve()
        cases, dataset_hash = load_dataset(dataset_path)
        adapter = HttpAdapter(config["adapter"], root)
    except (ConfigError, DatasetError, ValueError, RuntimeError) as exception:
        print(f"configuration error: {exception}")
        return 1

    started_at = datetime.now(timezone.utc)
    retries = config["execution_policy"]["retries"]
    results = []
    cases_with_retryable_errors = 0
    for case in cases:
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
    gates = evaluate_gates(aggregate_metrics, config["gates"])
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
            "config_hash": _hash(config),
            "started_at": started_at.isoformat(),
            "completed_at": datetime.now(timezone.utc).isoformat(),
        },
        "summary": {
            "gate": gate_status,
            "total_cases": len(cases),
            "completed_cases": completed,
            "completion_rate": completion_rate,
            "retryable_error_rate": retryable_error_rate,
            "metrics": aggregate_metrics,
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
