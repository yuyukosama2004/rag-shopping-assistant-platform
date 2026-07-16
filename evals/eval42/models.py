from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class DatasetCase:
    case_id: str
    input: dict[str, Any]
    expected: dict[str, Any]
    tags: tuple[str, ...]
    metadata: dict[str, Any]
    case_hash: str


@dataclass(frozen=True)
class RetrievedItem:
    item_id: str
    score: float | None
    rank: int
    attributes: dict[str, Any]


@dataclass
class CaseResult:
    case_id: str
    status: str
    retrieved_items: list[RetrievedItem] = field(default_factory=list)
    eligible_items: list[RetrievedItem] = field(default_factory=list)
    recommended_ids: list[str] = field(default_factory=list)
    answer: str | None = None
    usage: dict[str, Any] = field(default_factory=dict)
    system: dict[str, Any] = field(default_factory=dict)
    error: dict[str, Any] | None = None


@dataclass(frozen=True)
class CaseEvaluation:
    metrics: dict[str, float]
    failures: tuple[str, ...]
