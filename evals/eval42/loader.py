from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

from .models import DatasetCase


class DatasetError(ValueError):
    pass


EXPECTED_ONLY_FIELDS = {
    "relevant_ids",
    "forbidden_ids",
    "constraints",
    "facts",
    "accepted_domains",
    "required_evidence_matchers",
    "expected_outcome",
}

SUPPORTED_CONSTRAINTS = {
    "max_price",
    "min_price",
    "excluded_brands",
    "allowed_brands",
    "require_sellable",
}


def load_dataset(path: Path) -> tuple[list[DatasetCase], str]:
    try:
        raw = path.read_bytes()
    except OSError as exception:
        raise DatasetError(f"unable to read dataset {path}: {exception}") from exception

    cases: list[DatasetCase] = []
    seen: set[str] = set()
    for line_number, line in enumerate(raw.decode("utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            payload = json.loads(line)
        except json.JSONDecodeError as exception:
            raise DatasetError(f"{path}:{line_number}: invalid JSON: {exception}") from exception
        case = _parse_case(payload, path, line_number)
        if case.case_id in seen:
            raise DatasetError(f"{path}:{line_number}: duplicate case id {case.case_id}")
        seen.add(case.case_id)
        cases.append(case)
    if not cases:
        raise DatasetError(f"dataset is empty: {path}")
    canonical_hashes = json.dumps(
        [case.case_hash for case in cases],
        separators=(",", ":"),
    ).encode("utf-8")
    return cases, "sha256:" + hashlib.sha256(canonical_hashes).hexdigest()


def _parse_case(payload: dict[str, Any], path: Path, line_number: int) -> DatasetCase:
    required = {"schema_version", "id", "input", "expected", "tags", "metadata"}
    missing = sorted(required - payload.keys())
    prefix = f"{path}:{line_number}"
    if missing:
        raise DatasetError(f"{prefix}: missing fields: {', '.join(missing)}")
    if payload["schema_version"] != "1":
        raise DatasetError(f"{prefix}: only schema_version 1 is supported")
    if not isinstance(payload["input"], dict) or not payload["input"]:
        raise DatasetError(f"{prefix}: input must be a non-empty object")
    leaked = sorted(EXPECTED_ONLY_FIELDS & payload["input"].keys())
    if leaked:
        raise DatasetError(
            f"{prefix}: scoring-only fields leaked into input: {', '.join(leaked)}"
        )
    expected = payload["expected"]
    if not isinstance(expected, dict):
        raise DatasetError(f"{prefix}: expected must be an object")
    constraints = expected.get("constraints", {})
    unknown = sorted(set(constraints) - SUPPORTED_CONSTRAINTS)
    if unknown:
        raise DatasetError(
            f"{prefix}: unsupported hard constraints: {', '.join(unknown)}"
        )
    metadata = payload["metadata"]
    if not isinstance(metadata, dict) or not metadata.get("reviewed_by"):
        raise DatasetError(f"{prefix}: metadata.reviewed_by is required")
    canonical = json.dumps(
        payload,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return DatasetCase(
        case_id=str(payload["id"]),
        input=payload["input"],
        expected=expected,
        tags=tuple(payload["tags"]),
        metadata=metadata,
        case_hash="sha256:" + hashlib.sha256(canonical).hexdigest(),
    )
