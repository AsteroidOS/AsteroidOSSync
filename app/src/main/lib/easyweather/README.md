# Vendored EasyWeather

This directory is taken from Florent Revest's vendoring change in PR #229
(commit `7a3a385`), so the build-only fix uses the same source layout and code.
The runtime dependencies retain EasyWeather 1.3's versions (Retrofit 2.1.0 and
OkHttp logging interceptor 3.4.2), with Gson provided by the application's
existing dependency, rather than adopting the broader dependency updates.

These sources are vendored from the EasyWeather library:
https://github.com/MagneFire/EasyWeather (fork of
https://github.com/code-crusher/EasyWeather).

They were previously consumed as the Gradle dependency
`com.github.MagneFire:EasyWeather:1.3` via JitPack. JitPack can no longer
(re)build that artifact on demand — every version reports an `Error` build
status and the published files have been evicted — so the dependency became
unresolvable. The sources are vendored here instead, and the library's runtime
dependencies (Retrofit, Gson, OkHttp logging interceptor) are declared directly
in `app/build.gradle.kts` from Maven Central.

Changes from upstream: `android.support.annotation.NonNull` was migrated to
`androidx.annotation.NonNull` (this app is AndroidX), and `ApiClient` only
enables OkHttp request logging in debuggable builds. The upstream `res/` and
`AndroidManifest.xml` were intentionally not vendored (the code references
neither, and the library's `app_name` string would collide with the app's).

## License

Copyright 2016 Vatsal Bajpai. Licensed under the Apache License, Version 2.0.
See the LICENSE file in this directory or http://www.apache.org/licenses/LICENSE-2.0
