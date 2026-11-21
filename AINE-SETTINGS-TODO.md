# GitHub and account settings still to apply

The `aine-readiness` branch adds everything that can live in the repo. These remaining items are
GitHub repository settings or provider-account settings. They were deliberately **not** changed by
the agent that made the branch; the repo owner runs them. State below was read on 2026-11-20 with
`gh api repos/adityamparikh/customer-support-agent`.

Run them in this order. Steps 1 and 2 have to happen first, because GitHub only offers `build` as a
required check after the workflow has run once.

## 1. Push the branch and open a pull request (so CI runs once)

```bash
git push -u origin aine-readiness
gh pr create -R adityamparikh/customer-support-agent --base main --head aine-readiness \
  --title "AINE readiness: opt-in paid tests, CI, lint, docs" --fill
gh pr checks -R adityamparikh/customer-support-agent --watch   # the "build" job must pass
```

## 2. Protect `main`: pull requests only, `build` must pass, no force pushes

Today: `gh api repos/adityamparikh/customer-support-agent/branches/main/protection` returns
`Branch not protected`, so a `git push origin main` lands with no check. AINE items 39, 41 and 42 depend on this.

```bash
gh api -X PUT repos/adityamparikh/customer-support-agent/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": { "strict": true, "checks": [ { "context": "build" } ] },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": true
  },
  "required_conversation_resolution": true,
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
```

`required_approving_review_count` is 0 because this is a solo repo and GitHub does not let an
author approve their own pull request; the gate is "every change goes through a PR with a green
`build`". Raise it to 1 once there is a second maintainer. `enforce_admins: true` makes the rule
apply to the owner too, which is what stops an agent with the owner's `gh` token from pushing to `main`.

Check: `gh api repos/adityamparikh/customer-support-agent/branches/main/protection --jq '{checks: .required_status_checks.checks, force: .allow_force_pushes.enabled, admins: .enforce_admins.enabled}'`

## 3. `live-api` environment with a required reviewer, holding the API keys

The CI `live-it` job (manual dispatch with `run_live_it`) uses `environment: live-api`. Until the
environment exists with a reviewer, GitHub creates it on first use with no protection.

```bash
USER_ID=$(gh api users/adityamparikh --jq .id)
gh api -X PUT repos/adityamparikh/customer-support-agent/environments/live-api --input - <<JSON
{
  "reviewers": [ { "type": "User", "id": ${USER_ID} } ],
  "prevent_self_review": false,
  "deployment_branch_policy": null
}
JSON

# Prompts for each value; use project-scoped keys with a spend limit (step 5).
gh secret set OPENAI_API_KEY    --env live-api -R adityamparikh/customer-support-agent
gh secret set ANTHROPIC_API_KEY --env live-api -R adityamparikh/customer-support-agent
```

Check: `gh api repos/adityamparikh/customer-support-agent/environments/live-api --jq '.protection_rules'`

## 4. Secret scanning for non-provider patterns (optional)

Secret scanning and push protection are already enabled. Generic patterns (private keys,
connection strings, passwords in URLs) are not:

```bash
gh api -X PATCH repos/adityamparikh/customer-support-agent --input - <<'JSON'
{ "security_and_analysis": { "secret_scanning_non_provider_patterns": { "status": "enabled" } } }
JSON
```

Check: `gh api repos/adityamparikh/customer-support-agent --jq .security_and_analysis`

## 5. Outside GitHub: spend limits and scoped keys (no `gh` command exists)

- OpenAI: create a project for this repo, set a monthly budget on it, and use that project's key
  as `OPENAI_API_KEY` (platform.openai.com, Settings, Projects, Limits).
- Anthropic: create a workspace for this repo with a spend limit and use its key as
  `ANTHROPIC_API_KEY` (console.anthropic.com, Settings, Workspaces, Limits).
- Keep those keys in this repo's gitignored `.env` instead of exporting them from the global
  shell profile, so they are only in reach while working here.

## 6. Local agent permissions (your machine, not the repo)

`/Users/adityaparikh/IdeaProjects/customer-support-agent/.claude/settings.local.json` pre-allows
`Bash(git push:*)`, `Bash(git filter-branch:*)` and `Bash(gh repo create:*)`, which lets an agent
skip the "Ask first" list in `CLAUDE.md`. Remove those three entries by hand, or with `/permissions`
in Claude Code.
