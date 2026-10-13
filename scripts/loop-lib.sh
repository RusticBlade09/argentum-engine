# Shared by the headless loop launchers (set-loop, commander-loop, assay-loop). Sourced, not run.
#
# A loop PR is reviewed *before* it is opened, so it needs exactly one CI run, and the session
# that merges it goes straight on to build the next unit — no session is spent on review alone or
# on merging alone.

# The prompt block every launcher appends: how a PR moves from build to merge.
LOOP_PR_PROTOCOL="PR LIFECYCLE — every PR is reviewed BEFORE it is opened, so it needs exactly one CI run:
- Gate with \`just loop-gate\` from the worktree, plus whatever unit-specific gates the step names (rebless,
  printing checks, assay gates). Never run \`just build\` or \`just test\`: loop-gate runs the tests your diff can
  break and CI runs the full suite on the PR — running the full suite twice is the waste this rule exists to stop.
- Then review the branch (\`origin/main...HEAD\`) with the review-changes skill in SPLIT MODE: you wrote the code, so
  you only dispatch — one TRIAGE subagent, one subagent per area it names, one WEIGH subagent — each a fresh context,
  with the review dir \`<worktree>/.claude/loop-runs/review-<branch>/\`. Tell each that \`just loop-gate\` already
  passed on this head (CI runs the full suite), so nobody re-runs tests. WEIGH returns the verdict and writes the
  findings to the review dir instead of posting them. Fix the findings it calls blocking or important; you may decline
  one only with a stated reason. Re-run \`just loop-gate\` if anything changed. Without subagents (e.g. Codex), run
  the skill's sequential mode and say in the PR body that the review was not fresh-context.
- Then push, open the PR (its body lists the review's fixes and declines), and post a PR comment whose first line
  is exactly \`loop-reviewed <full sha of the pushed HEAD>\`. Record the unit as in review, with its PR number, in the
  ledger. Then stop: never wait on or poll CI — the launcher waits for it between sessions.
- Any later push to an open PR (a CI fix, a rebase) ends with a new \`loop-reviewed <new head sha>\` comment saying
  what changed since the review.
- Our PR is open:
  a. CI failed -> read only the failing log tail (\`gh run view <id> --log-failed | tail -200\`), fix it, re-run
     \`just loop-gate\`, push, post the new marker. A known-flaky test: \`gh run rerun --failed\` instead.
  b. Merge conflicts -> rebase on origin/main (re-bless snapshot goldens / re-bake generated files instead of
     hand-merging), \`just loop-gate\`, push, post the new marker.
  c. No loop-reviewed marker naming its current head (an earlier session died before finishing) -> review it in
     split mode as above, fix, gate, push, post the marker.
  d. CI green, merges cleanly, and the newest loop-reviewed comment names its head -> \`gh pr merge --squash
     --delete-branch\` (from the main checkout, after \`git worktree remove\` of its worktree), then do the merge
     bookkeeping below. Then DON'T stop: \`git fetch\` and carry on in this same session with the build step the
     updated state calls for, so the next PR is already open by the time you finish.
  e. CI still pending or running -> answer WAITING_CI."

# Our open PR, by branch prefix.
loop_find_pr() {
  gh pr list --author @me --state open --json number,headRefName \
    --jq "[.[] | select(.headRefName | startswith(\"$1\"))][0].number // empty"
}

# Waits out CI on PR $1 for free, so no session is spent watching it. Callers define `log`.
loop_wait_ci() {
  local pr=$1
  log "waiting for CI on PR #$pr"
  sleep 60 # let CI register the latest push before watching it
  gh pr checks "$pr" --watch >/dev/null 2>&1
  log "CI on PR #$pr: $(gh pr checks "$pr" --json bucket --jq '[.[].bucket] | group_by(.) | map("\(.[0])=\(length)") | join(" ")' 2>/dev/null)"
}
