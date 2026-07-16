from __future__ import annotations

from statistics import mean
from typing import Any

from .models import CaseEvaluation, CaseResult, DatasetCase, RetrievedItem


def evaluate_case(case: DatasetCase, result: CaseResult, recall_k: int) -> CaseEvaluation:
    if result.status != "completed":
        return CaseEvaluation(metrics={}, failures=("execution error",))

    expected = case.expected
    relevant = {str(item) for item in expected.get("relevant_ids", [])}
    forbidden = {str(item) for item in expected.get("forbidden_ids", [])}
    eligible_ids = [item.item_id for item in result.eligible_items]
    top_ids = eligible_ids[:recall_k]
    metrics: dict[str, float] = {}
    failures: list[str] = []

    if relevant:
        recalled = relevant & set(top_ids)
        metrics[f"recall_at_{recall_k}"] = len(recalled) / len(relevant)
        reciprocal_rank = next(
            (1 / rank for rank, item_id in enumerate(eligible_ids, start=1)
             if item_id in relevant),
            0.0,
        )
        metrics["mrr"] = reciprocal_rank
        missing = sorted(relevant - set(eligible_ids))
        if missing:
            failures.append("missing relevant products: " + ", ".join(missing))

    forbidden_hits = [item_id for item_id in eligible_ids if item_id in forbidden]
    if eligible_ids:
        metrics["forbidden_item_rate"] = len(forbidden_hits) / len(eligible_ids)
    if forbidden_hits:
        failures.append("returned forbidden products: " + ", ".join(forbidden_hits))

    expected_empty = bool(expected.get("expected_empty_result", False))
    unexpected_empty = bool(not expected_empty and relevant and not eligible_ids)
    metrics["unexpected_empty_result_rate"] = 1.0 if unexpected_empty else 0.0
    if unexpected_empty:
        failures.append("eligible result was unexpectedly empty")
    if expected_empty and eligible_ids:
        failures.append("expected an empty result but products were returned")

    constraints = expected.get("constraints", {})
    if result.eligible_items:
        passed = sum(
            _passes_constraints(item, constraints)
            for item in result.eligible_items
        )
        metrics["constraint_pass_rate"] = passed / len(result.eligible_items)
        if passed != len(result.eligible_items):
            failures.append("one or more eligible products violated hard constraints")

    return CaseEvaluation(metrics=metrics, failures=tuple(failures))


def aggregate(case_metrics: list[dict[str, float]]) -> dict[str, float]:
    names = sorted({name for metrics in case_metrics for name in metrics})
    return {
        name: mean(metrics[name] for metrics in case_metrics if name in metrics)
        for name in names
    }


def evaluate_gates(
        metrics: dict[str, float],
        gates: list[dict[str, Any]],
        baseline_metrics: dict[str, float] | None = None,
) -> list[dict[str, Any]]:
    baseline_metrics = baseline_metrics or {}
    results = []
    for gate in gates:
        name = gate["metric"]
        value = metrics.get(name)
        baseline = baseline_metrics.get(name)
        status = "not_applicable"
        regression = None
        if value is not None:
            passed = True
            if "min" in gate:
                passed = passed and value >= float(gate["min"])
            if "max" in gate:
                passed = passed and value <= float(gate["max"])
            if baseline is not None and (
                    "max_regression" in gate
                    or "max_relative_regression" in gate
            ):
                regression = (
                    baseline - value
                    if "min" in gate or "max" not in gate
                    else value - baseline
                )
                if "max_regression" in gate:
                    passed = passed and regression <= float(gate["max_regression"])
                if "max_relative_regression" in gate:
                    relative = (
                        regression / abs(baseline)
                        if baseline != 0
                        else (float("inf") if regression > 0 else 0.0)
                    )
                    passed = passed and relative <= float(
                        gate["max_relative_regression"]
                    )
            status = "pass" if passed else "fail"
        results.append(
            {
                "metric": name,
                "value": value,
                "baseline": baseline,
                "delta": (
                    value - baseline
                    if value is not None and baseline is not None
                    else None
                ),
                "regression": regression,
                "status": status,
                "severity": gate.get("severity", "error"),
            }
        )
    return results


def _passes_constraints(item: RetrievedItem, constraints: dict[str, Any]) -> bool:
    attributes = item.attributes
    price = attributes.get("price")
    brand = attributes.get("brand")
    if "max_price" in constraints and (price is None or float(price) > constraints["max_price"]):
        return False
    if "min_price" in constraints and (price is None or float(price) < constraints["min_price"]):
        return False
    if brand in constraints.get("excluded_brands", []):
        return False
    allowed = constraints.get("allowed_brands")
    if allowed and brand not in allowed:
        return False
    if constraints.get("require_sellable", True) and attributes.get("sellable") is not True:
        return False
    return True
