from __future__ import annotations

import argparse
from pathlib import Path

from .runner import run


def main() -> int:
    parser = argparse.ArgumentParser(description="Run deterministic PhoneMall evaluation")
    parser.add_argument("--config", required=True, type=Path)
    arguments = parser.parse_args()
    return run(arguments.config)


if __name__ == "__main__":
    raise SystemExit(main())
