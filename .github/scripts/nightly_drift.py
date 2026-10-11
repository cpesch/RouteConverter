"""Environment-drift comparison for the nightly failure-triage pipeline.

Compares two nightly environment snapshots (the ``drift-snapshot``
artifact's ``drift.json``) and renders the difference as a Markdown
comment for the rolling ``nightly: drift log`` ledger issue. Chunk 5/5 of
the pipeline: ``nightly_triage.py`` (chunk 4/5) downloads the artifacts and
calls these pure helpers; the workflow step that *writes* the snapshot is
maintainer work (tracker #453). No network access and no ``gh`` calls here.

Python 3 stdlib only.
"""

import json


def parse(text):
    """Parse the flat ``drift.json`` text into a ``{key: value}`` mapping.

    Only a flat JSON object whose every key and value is a string is valid;
    any other input (non-JSON, an array, a scalar, a non-string key or
    value) yields ``{}``.
    """
    if not isinstance(text, str):
        return {}
    try:
        data = json.loads(text)
    except (TypeError, ValueError):
        return {}
    if not isinstance(data, dict):
        return {}
    if not all(isinstance(key, str) and isinstance(value, str) for key, value in data.items()):
        return {}
    return dict(data)


def diff(old, new):
    """Return ``{key: [old_value, new_value]}`` for every key added, removed
    or changed between the two snapshots, sorted by key.

    The side that is missing a key is ``None``; keys unchanged between the
    two snapshots are omitted.
    """
    changed = {}
    for key in sorted(set(old) | set(new)):
        old_value = old.get(key)
        new_value = new.get(key)
        if old_value != new_value:
            changed[key] = [old_value, new_value]
    return changed


def _cell(value):
    if value is None:
        return ""
    return str(value).replace("|", "\\|")


def drift_comment(diff, run_url):
    """Render a drift diff as a Markdown comment for the drift ledger.

    Returns ``""`` when the diff is empty; otherwise a header line naming
    the run followed by a ``Key | Old | New`` table. A ``|`` in any value is
    escaped so it cannot break a cell, and a missing side renders as an
    empty cell.
    """
    if not diff:
        return ""
    lines = [
        f"Environment changed in {run_url}",
        "",
        "| Key | Old | New |",
        "| --- | --- | --- |",
    ]
    for key in sorted(diff):
        old, new = diff[key]
        lines.append(f"| {_cell(key)} | {_cell(old)} | {_cell(new)} |")
    return "\n".join(lines)
