# Vendored EasyWeather

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

Only change from upstream: `android.support.annotation.NonNull` was migrated to
`androidx.annotation.NonNull` (this app is AndroidX). The upstream `res/` and
`AndroidManifest.xml` were intentionally not vendored (the code references
neither, and the library's `app_name` string would collide with the app's).

## License

Copyright 2016 Vatsal Bajpai. Licensed under the Apache License, Version 2.0.
See http://www.apache.org/licenses/LICENSE-2.0
