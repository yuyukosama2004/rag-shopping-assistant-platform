# PhoneMall deterministic evaluation

This directory contains the minimum Eval42 Core required to validate PhoneMall before the generic
implementation is migrated into the Eval42 repository.

It uses only the Python standard library and supports:

- versioned JSONL cases with stable case hashes;
- the protected PhoneMall HTTP endpoint;
- a fully offline `mock://` HTTP fixture;
- Recall@5, MRR, forbidden-product rate, hard-constraint pass rate, and unexpected-empty checks;
- JSON and Markdown reports;
- Baseline v1 creation, run fingerprints, case-hash comparability, and regression gates;
- execution budgets and explicit untrusted partial-run handling;
- exit code `0` for pass, `2` for quality-gate failure, `3` for untrusted partial execution, and
  `1` for configuration or dataset errors.

## Offline run

From the repository root:

```bash
python -m unittest discover -s evals/tests -v
python -m evals.eval42 --config evals/config/phonemall.mock.json
```

The offline command runs all 20 candidate cases without an API key or live service.
It compares the result with `evals/baselines/phonemall-main.json`. CI uploads the JSON and
Markdown reports as a 14-day artifact.

To intentionally refresh a trusted fixture baseline:

```bash
python -m evals.eval42 \
  --config evals/config/phonemall.mock.json \
  --write-baseline evals/baselines/phonemall-main.json
```

Baseline review must accompany any dataset, metric, adapter, or fixture change. A changed
`case_hash` is listed as changed rather than silently compared with an old annotation.
The committed fixture baseline verifies deterministic evaluation and regression plumbing; it is
not evidence of live recommendation quality while the candidate labels remain pending review.

## Live protected run

Start PhoneMall with `AI_EVAL_ENABLED=true`, obtain a normal merchant token, then run:

```bash
PHONEMALL_BASE_URL=http://127.0.0.1:8080 \
PHONEMALL_MERCHANT_TOKEN=replace-me \
GIT_SHA="$(git rev-parse HEAD)" \
python -m evals.eval42 --config evals/config/phonemall.live.json
```

PowerShell users can set the same environment variables through `$env:NAME = "value"`.

The live config expects `evals/baselines/phonemall-live-main.json`. Create and review that
baseline from a trusted live run before enabling nightly comparison.

## Nightly live evaluation

`.github/workflows/eval-nightly.yml` is inert until the repository variable
`PHONEMALL_NIGHTLY_ENABLED=true` is set. It also requires:

- secret `PHONEMALL_BASE_URL`;
- secret `PHONEMALL_MERCHANT_TOKEN`;
- a protected deployment with `AI_EVAL_ENABLED=true`;
- the reviewed live baseline described above.

Nightly reports are retained for 30 days. Authorization values are never written to reports or
baseline documents.

## Dataset status

`shopping_queries.pending.jsonl` is deliberately marked `pending-human-review`. Its queries and
candidate labels were derived from the frozen mock product snapshot, but they are not a Gold Set
until a maintainer reviews every relevant-product label and annotation note. CI uses the dataset
only to validate the deterministic runner and fixture path.
