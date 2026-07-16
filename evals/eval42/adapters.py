from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

from .models import CaseResult, DatasetCase, RetrievedItem


class AdapterError(RuntimeError):
    def __init__(self, message: str, retryable: bool) -> None:
        super().__init__(message)
        self.retryable = retryable


class HttpAdapter:
    def __init__(self, config: dict[str, Any], root: Path) -> None:
        self.config = config
        self.root = root
        self.base_url = config["base_url"].rstrip("/")
        self.endpoint = config["endpoint"]
        self.timeout = float(config.get("timeout_seconds", 60))
        self.headers = dict(config.get("headers", {}))
        self._fixture = self._load_fixture() if self.base_url.startswith("mock://") else None

    def execute(self, case: DatasetCase) -> CaseResult:
        started_at = time.perf_counter()
        try:
            response = self._mock_response(case) if self._fixture else self._http_response(case)
            return self._normalize(case, response, started_at)
        except AdapterError as exception:
            return CaseResult(
                case_id=case.case_id,
                status="error",
                usage={"total_latency_ms": _elapsed_ms(started_at)},
                error={
                    "type": "adapter_error",
                    "message": str(exception)[:500],
                    "retryable": exception.retryable,
                },
            )

    def _load_fixture(self) -> dict[str, Any]:
        relative = self.base_url.removeprefix("mock://")
        path = (self.root / relative).resolve()
        if not path.is_relative_to(self.root.resolve()):
            raise AdapterError("mock fixture must stay inside the repository", False)
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exception:
            raise AdapterError(f"unable to load mock fixture: {exception}", False) from exception

    def _mock_response(self, case: DatasetCase) -> dict[str, Any]:
        case_fixture = self._fixture.get("cases", {}).get(case.case_id)
        if case_fixture is None:
            raise AdapterError(f"mock response is missing case {case.case_id}", False)
        products = self._fixture.get("products", {})
        retrieved = []
        for rank, reference in enumerate(case_fixture.get("retrieved", []), start=1):
            product = products.get(str(reference["id"]))
            if product is None:
                raise AdapterError(
                    f"mock product is missing id {reference['id']}",
                    False,
                )
            retrieved.append(
                {
                    **product,
                    "score": reference.get("score"),
                    "rank": rank,
                }
            )
        eligible_ids = {
            str(item)
            for item in case_fixture.get(
                "eligible_ids",
                [item["id"] for item in retrieved if item.get("sellable", True)],
            )
        }
        eligible = [item for item in retrieved if str(item["id"]) in eligible_ids]
        return {
            "retrieved_products": retrieved,
            "eligible_products": eligible,
            "recommended_ids": [],
            "answer": None,
            "usage": {
                "retrieval_latency_ms": 1,
                "total_latency_ms": 1,
                "token_count_kind": "unavailable",
            },
            "versions": {
                "retrieval_mode": "fixture",
                "index_ready": True,
                "index_fingerprint": self._fixture.get("index_fingerprint"),
            },
        }

    def _http_response(self, case: DatasetCase) -> dict[str, Any]:
        body = json.dumps(
            {
                "query": case.input["query"],
                "generate_answer": False,
            }
        ).encode("utf-8")
        request = urllib.request.Request(
            self.base_url + self.endpoint,
            data=body,
            method="POST",
            headers={"Content-Type": "application/json", **self.headers},
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exception:
            retryable = exception.code == 429 or exception.code >= 500
            raise AdapterError(f"target returned HTTP {exception.code}", retryable) from exception
        except (urllib.error.URLError, TimeoutError) as exception:
            raise AdapterError(f"target request failed: {exception.reason}", True) from exception
        except json.JSONDecodeError as exception:
            raise AdapterError("target returned invalid JSON", False) from exception

    def _normalize(
            self,
            case: DatasetCase,
            response: dict[str, Any],
        started_at: float,
    ) -> CaseResult:
        try:
            retrieved = _deduplicate(
                [_item(item) for item in response["retrieved_products"]]
            )
            eligible = _deduplicate(
                [_item(item) for item in response["eligible_products"]]
            )
        except (KeyError, TypeError, ValueError) as exception:
            raise AdapterError(f"target response shape is invalid: {exception}", False) from exception
        usage = dict(response.get("usage", {}))
        usage.setdefault("total_latency_ms", _elapsed_ms(started_at))
        return CaseResult(
            case_id=case.case_id,
            status="completed",
            retrieved_items=retrieved,
            eligible_items=eligible,
            recommended_ids=[str(item) for item in response.get("recommended_ids", [])],
            answer=response.get("answer"),
            usage=usage,
            system=dict(response.get("versions", {})),
        )


def _item(payload: dict[str, Any]) -> RetrievedItem:
    attributes = {
        key: value
        for key, value in payload.items()
        if key not in {"id", "score", "rank"}
    }
    return RetrievedItem(
        item_id=str(payload["id"]),
        score=float(payload["score"]) if payload.get("score") is not None else None,
        rank=int(payload["rank"]),
        attributes=attributes,
    )


def _deduplicate(items: list[RetrievedItem]) -> list[RetrievedItem]:
    seen: set[str] = set()
    result = []
    for item in items:
        if item.item_id not in seen:
            seen.add(item.item_id)
            result.append(item)
    return result


def _elapsed_ms(started_at: float) -> int:
    return max(0, int((time.perf_counter() - started_at) * 1000))
