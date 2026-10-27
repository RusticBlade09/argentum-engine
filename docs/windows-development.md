# Windows development and agent tooling

Observed while implementing and verifying beam me up on native Windows, using PowerShell,
Git for Windows, `just`, and Codex command tools. These are environment-specific findings,
not a claim that every Windows installation has the same failures. Workarounds below were
used in that session unless marked as a proposed fix.

## Shell and launcher boundaries

### `just ui-dev` needs `cygpath` on PATH

The shebang recipe failed before starting the app:

```text
could not find `cygpath` executable to translate recipe `ui-dev` shebang interpreter path
```

Git for Windows was installed, but its `usr\bin` directory was absent from the command's
PATH. Adding it for the current PowerShell process made the recipe work:

```powershell
# Adjust this path if Git is installed elsewhere. This does not change the system PATH.
$env:Path = 'C:\Program Files\Git\usr\bin;' + $env:Path
just ui-dev 8187 5187
```

Use free ports belonging to this checkout. After the browser check, verify that its server
and Vite processes have stopped; terminating a launcher alone can leave children running.
If cleanup needs explicit process termination, identify the exact task's command, PID and
parent first. A substring such as a port number can also occur inside an unrelated JVM's
dependency paths or hashes.

### `just test-class` crosses an unsupported native Python execution boundary

The Python wrapper found the test module, then failed with:

```text
OSError: [WinError 193] %1 is not a valid Win32 application
```

[`scripts/test-class`](../scripts/test-class) passes the extensionless Bash script
`scripts/gradle-locked` directly to `subprocess.call`. Native Windows Python does not execute
that script through its shebang. Starting the Python wrapper from Git Bash does not itself
change this subprocess behavior.

The working workaround was a temporary `just` recipe that invokes the same build wrapper
directly, without the Python test-discovery launcher. For example, create
`.windows-check.just` in the repository root:

```just
targeted-check:
    scripts/gradle-locked :rules-engine:test --tests '*MyMechanicTest'
```

Replace the module and test pattern with the actual test, then run:

```powershell
just --justfile .windows-check.just targeted-check
```

Remove the temporary file afterward. Heavy builds must still go through `just`; this is
not permission to run raw Gradle. A repository fix would make `test-class` explicitly launch
Bash on Windows; that fix has not been implemented here.

### Assay has a native Windows launcher

The Unix Assay launcher printed reports, but command completion was sometimes delayed.
The cause of that delay was not established. The generated native launcher successfully
returned an exit code:

```powershell
# Build through the existing recipe first.
just _assay-tool
& .\oracle-assay\build\install\oracle-assay\bin\oracle-assay.bat differential --set TRK
$LASTEXITCODE
```

This uses the CLI already built by `just`, not a second build path. Read its report as well
as its exit code: a differential command can exit successfully while reporting divergences,
and a grammar decline is not independent validation of a card.

## Sandbox access is separate from installation

- **Python aliases:** `python` resolved to a WindowsApps execution alias that could not
  start inside the sandbox. `py` also failed to start its installed Windows Store Python
  there. Running the edit script with `py` through the command tool's normal approved
  escalation succeeded. Inspect resolution with `Get-Command python, python3, py -All`;
  the existence of an alias does not prove the interpreter is runnable in that context.
- **HTTPS with `curl.exe`:** a Scryfall request failed inside the sandbox with
  `curl: (35) schannel: AcquireCredentialsHandle failed: SEC_E_NO_CREDENTIALS`.
  The same request succeeded with approved escalation. This was not evidence of a bad
  Scryfall URL or certificate. Do not disable certificate verification as a workaround.

For an agent, retry a required command through the command tool's documented approval
mechanism when access is denied. Do not switch runtimes or tools to bypass that boundary.

## Build coordination and command completion

[`scripts/gradle-locked`](../scripts/gradle-locked) warns and runs **unlocked** when `shlock`
is unavailable, as it was in this Git for Windows environment. Calling `just` is still
required, but does not guarantee that the machine-global two-slot semaphore is active.
Coordinate builds manually when this warning appears.

Never overlap test runs in the **same checkout**, even when they select different tests.
Running a focused `:ai:test` diagnostic while the full test gate was still using `:ai:test`
caused a collision in `ai/build/test-results/test/binary/in-progress-results-generic.bin`.
A serial rerun removed that infrastructure failure. A temporary recipe does not give a
test run its own output directory.

A command tool can yield a session while a command is still running. A finished-looking
report is not an exit status: poll that original session to completion before launching a
replacement or deleting its logs. Delayed completion was observed here, but was not proven
to be a Windows-specific defect in the command tool.

## Paths, text encoding and diffs

- **PowerShell is not Bash.** Bash-style path braces such as
  `engine/{handlers,mechanics}` caused a PowerShell parser error. Pass explicit paths.
  Likewise, `rg` was given a literal path containing `*.kt` and rejected it; use an existing
  directory plus `rg -g '*.kt'` to filter files instead.
- **Facade allowlists currently compare platform-dependent path strings.**
  `FacadeBoundaryTest` checks `rel.toString().endsWith(...)` against entries with `/`.
  On Windows, `\` separators caused 16 already-allowlisted card entries to be reported as
  violations. The test was left unchanged in the beam-me-up work, with user approval.
  A proposed fix is to normalize the relative path to `/` before matching the allowlist.
  Inspect the reported entries before treating this signature as a card regression; do not
  disable the test or silently mark the full gate as passed.
- **Choose text encoding explicitly.** A Python edit using its Windows default encoding
  corrupted existing UTF-8 punctuation in comments. Specify `encoding="utf-8"` for both
  reads and writes, and preserve existing line endings. Windows text writes also produced
  CRLF-only diff churn. For an intentional CRLF file,
  `git -c core.whitespace=cr-at-eol diff --check` distinguishes line-ending carriage returns
  from other trailing whitespace; it does not fix a noisy diff or justify rewriting
  unrelated files.

### Conflux cache (`con.json`) and coverage regeneration

During the October 27, 2026 TRK update, `scripts/gen-set-totals` dropped Conflux (`CON`)
and its 145 cards from `game-server/src/main/resources/coverage/set-totals.json`.
Both that generator and `scripts/card-status` use `~/.cache/scryfall/con.json` for
Conflux. The generator skips sets whose cache is not a regular file, then replaces the
entire output; a missing cache can therefore remove an already-published set.

The filename behavior depends on the API/runtime. On this Windows build (10.0.19045):

- Node successfully created, listed and read an ordinary `con.json` in a temporary folder.
- Native Python 3.13 reported `os.path.isreserved(...) == True`. A
  `Path.write_text(..., encoding="utf-8")` call returned, but `Path.is_file()` was false
  and the directory remained empty. A separate read attempt stalled and was interrupted.
  This reproduces how the Python cache writer can appear to succeed without leaving a
  cache file for the generator.

Thus, saying that Windows can never store a file named `con.json` is too broad; successfully
creating it with another tool also does not prove the Python scripts can use it.
[Microsoft's filename guidance](https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file)
advises avoiding reserved device names even when followed by an extension. The string
`"CON"` inside JSON is harmless and must remain the set's code.

When reviewing a regeneration:

1. Compare set codes and card data, not just line counts. `card-status --refresh` refreshes
   every selected set (all by default), and `gen-set-totals` rewrites all cached sets.
2. If Conflux disappeared, restore its complete object from the last known-good Git version
   into `set-totals.json`, preserving other intended updates and the release-date ordering.
   This restores the page's data but does not repair the cache writer.
3. Preserve UTF-8 and LF in both coverage JSON files. Python's default Windows text writes
   can convert the whole file to CRLF. Use `newline="\n"` when writing LF output.
   `git diff --ignore-space-at-eol --histogram` helps distinguish content changes from
   line-ending churn and misleading matches between repetitive JSON entries.

A durable fix would give cache files a safe prefix (for example `set-con.json`) consistently
across readers and writers, migrate existing caches, and prevent regeneration from silently
dropping existing sets when cache entries are missing. These script changes are not implemented
by this documentation update.

These notes document observed limitations. They do not waive the verification requirements
or the rule against changing someone else's in-flight work.
