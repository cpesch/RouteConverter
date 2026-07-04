# Project overview + docs frontmatter — prompt template

Reusable across projects. Fill the `[bracketed]` placeholders per project.
Referenced from `AGENTS.md` so it applies whenever a docs file is touched or a
status overview is requested.

---

## Part 1 — Normalize frontmatter on every docs file

For every `specs/*.md` file, ensure a YAML frontmatter block exists at the very top.
If one already exists, fill in any missing fields; do not clobber existing values.
Block shape:

```yaml
---
name: <slug derived from filename, without .md>
status: <one of: draft | planned | finalized | implemented | superseded>
phases_done: [<phases already completed, e.g. clarify, refine-1, finalize>]
phases_next: [<phases not yet done, e.g. implement>]
last_touched: <YYYY-MM-DD — the file's last git commit date: `git log -1 --format=%cs -- <file>`>
---
```

Rules:
- Derive `name` from the filename (e.g. `00035-foo-bar.md` → `00035-foo-bar`).
- Infer `status` from the document body: a finished/shipped doc is `implemented`,
  a reviewed-but-unbuilt one is `finalized`, an early sketch is `draft`. When unsure,
  use `draft` and say so.
- Infer `phases_done` / `phases_next` from any process the doc describes; if there is
  no phase model, use `phases_done: []` and `phases_next: []`.
- Do not rewrite document content — only add/complete the frontmatter.
- List every file you changed and the status you assigned, so mis-guesses can be corrected.

## Part 2 — Print a project / migration status overview

Produce a single Markdown overview table for **[PROJECT NAME]**, tracking the move
from **[OLD APPROACH / current state]** to **[NEW APPROACH / target state]**.

Lead with a dot legend (reword the states to fit the project's axis):

> 🔴 **Not started** — still on the old approach ([OLD APPROACH])
> 🟡 **In progress** — some parts already on the new approach
> 🟢 **Done** — fully on the new approach ([NEW APPROACH]); old parts removed

Then the table:

| Consumer | Repo dir | Git URL | Status | Live? |
|---|---|---|---|---|
| [name] | [repo path] | [git url] | 🔴 Not started | [live status] |
| … one row per consumer … |

Derive the rows from the **actual repository** — enumerate the real consumers/modules,
their directories, and remotes (`git remote -v`, submodule paths). Do not invent
entries; grep/inspect to confirm each one's status before assigning a dot.

Below the table add a **footer note** that:
- lists 🟢 items already complete (no action needed), and
- calls out special cases needing a separate proposal or discussion.

Keep the tone technical and concise. Use the colored-dot indicators for status.
