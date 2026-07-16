from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from evals.eval42.loader import DatasetError, load_dataset
from evals.eval42.metrics import evaluate_case, evaluate_gates
from evals.eval42.models import CaseResult, DatasetCase, RetrievedItem
from evals.eval42.runner import run


ROOT = Path(__file__).resolve().parents[2]


class LoaderTest(unittest.TestCase):

    def test_rejects_scoring_answer_leakage(self) -> None:
        payload = {
            "schema_version": "1",
            "id": "leaky",
            "input": {"query": "phone", "relevant_ids": [1]},
            "expected": {"relevant_ids": [1]},
            "tags": [],
            "metadata": {
                "dataset_version": "test",
                "reviewed_by": "test",
            },
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "dataset.jsonl"
            path.write_text(json.dumps(payload) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(DatasetError, "leaked into input"):
                load_dataset(path)


class MetricsTest(unittest.TestCase):

    def test_reports_relevance_forbidden_and_constraint_failures(self) -> None:
        case = DatasetCase(
            case_id="case-1",
            input={"query": "phone"},
            expected={
                "relevant_ids": [1],
                "forbidden_ids": [2],
                "constraints": {
                    "max_price": 4000,
                    "excluded_brands": ["Apple"],
                },
            },
            tags=(),
            metadata={},
            case_hash="sha256:test",
        )
        result = CaseResult(
            case_id="case-1",
            status="completed",
            eligible_items=[
                RetrievedItem(
                    item_id="2",
                    score=0.9,
                    rank=1,
                    attributes={
                        "brand": "Apple",
                        "price": 5999,
                        "sellable": True,
                    },
                )
            ],
        )

        evaluation = evaluate_case(case, result, 5)

        self.assertEqual(0.0, evaluation.metrics["recall_at_5"])
        self.assertEqual(0.0, evaluation.metrics["mrr"])
        self.assertEqual(1.0, evaluation.metrics["forbidden_item_rate"])
        self.assertEqual(0.0, evaluation.metrics["constraint_pass_rate"])
        self.assertGreaterEqual(len(evaluation.failures), 3)

    def test_gate_engine_honors_minimum_and_maximum(self) -> None:
        gates = evaluate_gates(
            {"recall_at_5": 0.75, "forbidden_item_rate": 0.0},
            [
                {"metric": "recall_at_5", "min": 0.8},
                {"metric": "forbidden_item_rate", "max": 0.0},
            ],
        )
        self.assertEqual(["fail", "pass"], [gate["status"] for gate in gates])


class RunnerTest(unittest.TestCase):

    def test_offline_fixture_runs_twenty_cases_and_writes_reports(self) -> None:
        config_path = ROOT / "evals" / "config" / "phonemall.mock.json"
        report_dir = ROOT / "evals" / "reports"

        exit_code = run(config_path)

        self.assertEqual(0, exit_code)
        report = json.loads((report_dir / "report.json").read_text(encoding="utf-8"))
        self.assertEqual("pass", report["summary"]["gate"])
        self.assertEqual(20, report["summary"]["total_cases"])
        self.assertEqual(20, report["summary"]["completed_cases"])
        self.assertTrue((report_dir / "report.md").exists())

    def test_quality_gate_failure_returns_two(self) -> None:
        source = json.loads(
            (ROOT / "evals" / "config" / "phonemall.mock.json").read_text(
                encoding="utf-8"
            )
        )
        source["gates"] = [{"metric": "recall_at_5", "max": 0.5}]
        with tempfile.TemporaryDirectory() as directory:
            source["report"]["output_dir"] = directory
            path = Path(directory) / "config.json"
            path.write_text(json.dumps(source), encoding="utf-8")

            self.assertEqual(2, run(path))

    def test_incomplete_execution_returns_three(self) -> None:
        source = json.loads(
            (ROOT / "evals" / "config" / "phonemall.mock.json").read_text(
                encoding="utf-8"
            )
        )
        unknown_case = {
            "schema_version": "1",
            "id": "missing-fixture",
            "input": {"query": "phone"},
            "expected": {"relevant_ids": [1007], "constraints": {}},
            "tags": [],
            "metadata": {
                "dataset_version": "test",
                "reviewed_by": "test",
            },
        }
        with tempfile.TemporaryDirectory() as directory:
            directory_path = Path(directory)
            dataset_path = directory_path / "dataset.jsonl"
            dataset_path.write_text(
                json.dumps(unknown_case) + "\n",
                encoding="utf-8",
            )
            source["dataset"]["path"] = str(dataset_path)
            source["report"]["output_dir"] = directory
            path = directory_path / "config.json"
            path.write_text(json.dumps(source), encoding="utf-8")

            self.assertEqual(3, run(path))


if __name__ == "__main__":
    unittest.main()
