from __future__ import annotations

import argparse
from pathlib import Path

from .runner import run


def main() -> int:
    parser = argparse.ArgumentParser(description="Run deterministic PhoneMall evaluation")
    parser.add_argument("--config", required=True, type=Path)
    parser.add_argument(
        "--write-baseline",
        type=Path,
        help="write a trusted Baseline v1 document after the run",
    )
    arguments = parser.parse_args()
    return run(arguments.config, arguments.write_baseline)


if __name__ == "__main__":
    raise SystemExit(main())
