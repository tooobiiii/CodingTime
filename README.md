# CodingTime

[![Build](https://github.com/tooobiiii/CodingTime/actions/workflows/build.yml/badge.svg)](https://github.com/tooobiiii/CodingTime/actions/workflows/build.yml)

An IntelliJ Platform plugin that tracks how much time you actually spend coding — locally, with no
account and no network traffic.

## Features

- **Active time only.** A configurable idle timeout closes the current session when you stop working,
  so breaks and an IDE left open overnight are not counted.
- **Live status bar widget** showing today's total, scoped to the current project or across all of them.
- **Dashboard tool window** with a daily activity chart plus breakdowns by language and project, over
  the last 7 days, last 30 days, or all time.
- **Per-project tracking.** Several open projects each record their own time.
- **Fully local.** Statistics live in the IDE configuration directory and never leave the machine.

## Installation

**From the JetBrains Marketplace:** <kbd>Settings</kbd> → <kbd>Plugins</kbd> → <kbd>Marketplace</kbd>,
search for *CodingTime*, install and restart.

**From a release archive:** download the ZIP from the
[releases page](https://github.com/tooobiiii/CodingTime/releases) and install it via
<kbd>Settings</kbd> → <kbd>Plugins</kbd> → <kbd>⚙</kbd> → <kbd>Install Plugin from Disk…</kbd>.

Requires IntelliJ Platform 2026.1 (build 261) or newer, running on JDK 21.

## Usage

Tracking starts with the first keystroke — there is nothing to switch on. The status bar widget shows
today's total; click it to open the **CodingTime** tool window on the right, which holds the activity
chart and the language and project breakdowns.

## Settings

<kbd>Settings</kbd> → <kbd>Tools</kbd> → <kbd>CodingTime</kbd>:

| Setting | Default | Description |
| --- | --- | --- |
| Stop counting after *n* seconds without activity | 120 s | A longer pause ends the current session. Switch it off to count every gap, however long. |
| Count caret movement and file switches as activity | on | When disabled, only actual edits keep the timer running. |
| Keep daily statistics for *n* days | 365 | Older daily statistics are pruned on startup. |
| Status bar shows time for the current project only | on | Otherwise the widget totals every open project. |
| Delete all recorded time | — | Permanently drops all statistics. |

## How it works

An editor listener registers activity for the file currently open in each project. Activity is folded
into sessions by `ActivityAccumulator`, which closes a session once the idle timeout elapses; a closed
session is split at midnight and added to a per-day / per-project / per-language total. The dashboard
folds the still-running session into today's numbers, so the displayed time grows live.

Totals are persisted as `codingTime.xml` in the IDE configuration directory (roaming disabled), which
is the only place any of this data exists.

## Development

```bash
./gradlew test          # unit tests
./gradlew check         # tests + verification tasks
./gradlew runIde        # launch a sandbox IDE with the plugin
./gradlew buildPlugin   # distribution ZIP in build/distributions
./gradlew verifyPlugin  # IntelliJ Plugin Verifier against recommended IDEs
```

The source layout follows the flow of the data:

| Package | Responsibility |
| --- | --- |
| `core` | Pure logic: session accumulation, midnight splitting, chart buckets, time formatting |
| `tracking` | Editor listeners and the per-project tracker service |
| `storage` | Persistent per-day / project / language totals |
| `stats` | Read model that merges persisted totals with the running session |
| `ui` | Status bar widget, tool window dashboard, chart and breakdown panels |
| `settings` | Application settings and their configurable |

Everything under `core` is plain Kotlin with no platform dependencies and is covered by unit tests.

## Releasing

`pluginVersion` in `gradle.properties` is the single source of truth. Bump it, commit, and push a
matching `v<pluginVersion>` tag — the release workflow runs the checks, publishes to the JetBrains
Marketplace and creates a GitHub release with the signed distribution. Signing and publishing read
`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` and `PUBLISH_TOKEN` from the environment.