from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any


class ConfigError(ValueError):
    pass


ENV_PATTERN = re.compile(r"\$\{([A-Z_][A-Z0-9_]*)(?::-(.*?))?\}")


def load_config(path: Path) -> dict[str, Any]:
    try:
        config = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise ConfigError(f"unable to load config {path}: {exception}") from exception
    config = _expand(config)
    _validate(config)
    return config


def _expand(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: _expand(item) for key, item in value.items()}
    if isinstance(value, list):
        return [_expand(item) for item in value]
    if not isinstance(value, str):
        return value

    def replace(match: re.Match[str]) -> str:
        name, default = match.groups()
        if name in os.environ:
            return os.environ[name]
        if default is not None:
            return default
        raise ConfigError(f"required environment variable is missing: {name}")

    return ENV_PATTERN.sub(replace, value)


def _validate(config: dict[str, Any]) -> None:
    required = {
        "schema_version",
        "project",
        "dataset",
        "adapter",
        "execution_policy",
        "metrics",
        "gates",
        "report",
    }
    missing = sorted(required - config.keys())
    if missing:
        raise ConfigError(f"config is missing fields: {', '.join(missing)}")
    if config["schema_version"] != "1":
        raise ConfigError("only config schema_version 1 is supported")
    adapter = config["adapter"]
    if adapter.get("type") != "http":
        raise ConfigError("only the HTTP adapter is supported")
    if not adapter.get("base_url") or not adapter.get("endpoint"):
        raise ConfigError("adapter.base_url and adapter.endpoint are required")
    policy = config["execution_policy"]
    for field in ("min_completion_rate", "max_retryable_error_rate"):
        value = policy.get(field)
        if not isinstance(value, (int, float)) or not 0 <= value <= 1:
            raise ConfigError(f"execution_policy.{field} must be between 0 and 1")
    retries = policy.get("retries")
    if not isinstance(retries, int) or not 0 <= retries <= 5:
        raise ConfigError("execution_policy.retries must be between 0 and 5")
