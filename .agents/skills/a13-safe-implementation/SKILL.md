---
name: a13-safe-implementation
description: Implement one A13 change on tomthenpc/customiuizer-a13. Read AGENTS.md first.
argument-hint: <task>
triggers: ["user"]
---

# A13 Safe Implementation

Follow root `AGENTS.md`. Work on a branch created from an exact SHA.

Do not force-push, rewrite public history, or change PrefMap / ResourceHooks unless the task explicitly requires it.

Run `python tools/verify.py fast --changed` after production edits, and `python tools/verify.py full` before claiming the task is done.
