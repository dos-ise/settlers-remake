# Local build on a corporate network

This documents how to build the project locally behind a corporate proxy that
requires authentication. The Gradle wrapper cannot download its distribution
through the proxy by itself, and a few other environment quirks need workarounds.

## 1. Gradle distribution

The wrapper (`gradlew.bat`) downloads the distribution with the JVM's HTTP
client, which does **not** use the `HTTP_PROXY`/`HTTPS_PROXY` environment
variables and fails with `407 Proxy Authentication Required`.

Download the distribution manually with `curl` (which handles the proxy auth)
and place it where the wrapper expects it:

```powershell
# credentials are usually stored in the OS credential store (e.g. Windows Credential Manager)
$dest = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14.3-bin"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
curl.exe -x "http://<user>%40<domain>:<password>@<proxy-host>:<proxy-port>" `
  -L -o "$dest\gradle-8.14.3-bin.zip" `
  "https://services.gradle.org/distributions/gradle-8.14.3-bin.zip"
```

Notes:
- The `@` in the user name must be URL-encoded as `%40`, otherwise `curl`
  misparses the proxy URL.
- The hash subdirectory under `dists\gradle-8.14.3-bin\` is created by the
  wrapper on first run; move the zip there if the wrapper already created it.
- Check the current version in `gradle/wrapper/gradle-wrapper.properties`.

## 2. Proxy for dependency downloads

Gradle's dependency resolution also needs the proxy. Pass the credentials as
JVM system properties (the `@` in the user name is fine here):

```powershell
$env:JAVA_OPTS = '-Dhttp.proxyHost=<proxy-host> -Dhttp.proxyPort=<proxy-port> ' +
  '-Dhttps.proxyHost=<proxy-host> -Dhttps.proxyPort=<proxy-port> ' +
  '-Dhttp.proxyUser=<user>@<domain> -Dhttp.proxyPassword=<password> ' +
  '-Dhttps.proxyUser=<user>@<domain> -Dhttps.proxyPassword=<password>'
```

## 3. Linked git worktrees: JGit `findGitRevision` task

In a linked worktree the `findGitRevision` task in `jsettlers.common/build.gradle`
fails during configuration with `Cannot get property 'name' on null object`
(JGit's `exactRef("HEAD")` returns a ref without an object id in a worktree).

The `git checkout --detach HEAD` workaround from `CLAUDE.md` is not sufficient.
Make the task null-safe instead (local-only change, do not commit):

```groovy
def localRev = headRef != null && headRef.objectId != null ? headRef.objectId.name : "unknown"
```

and in the `doLast` block:

```groovy
localRev.length() >= 8 ? localRev.substring(0, 8) : localRev
```

## 4. Android modules

The Android modules are only included when `ANDROID_HOME` (or a
`local.properties` with `sdk.dir`) is present. The project targets
`compileSdk 30`, which is no longer available in current SDK repositories
(only 35/36 are installable), so the Android modules cannot be built locally.

To build the desktop/logic modules only, unset the SDK variables:

```powershell
Remove-Item Env:ANDROID_HOME -ErrorAction SilentlyContinue
Remove-Item Env:ANDROID_SDK_ROOT -ErrorAction SilentlyContinue
```

`settings.gradle` then excludes `:go.graphics.android` and
`:jsettlers.main.android` automatically.

## 5. Build commands

```powershell
# compile a single module (fast feedback)
.\gradlew.bat :jsettlers.common:compileJava --no-daemon
.\gradlew.bat :jsettlers.logic:compileJava --no-daemon

# run a single test
.\gradlew.bat :jsettlers.logic:test --tests 'jsettlers.integration.ai.ColonizationIT' --no-daemon --info
```

Always pass `--no-daemon` (see `CLAUDE.md`). The CI pipeline
(`.github/workflows/CI.yml`) is the authoritative verification: it runs
`release` and `test --info` on `ubuntu-latest` with a full Android SDK.