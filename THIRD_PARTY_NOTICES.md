# Third-party notices

WaveBalance's own code is under the [MIT License](LICENSE). The app is built with the open-source
libraries below, which are compiled into the APK. All of them are under the
**Apache License 2.0**; its full text is in [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt).

| Component | Used for | License |
| --- | --- | --- |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) (runtime, UI, foundation, animation) and [Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3), including the adaptive layout and navigation-suite libraries | Drawing the whole interface and adapting it to the window size | Apache-2.0 |
| [Material icons](https://fonts.google.com/icons) (compose-material-icons-extended) | Icons | Apache-2.0 |
| [AndroidX](https://developer.android.com/jetpack/androidx) Core, Activity, Lifecycle, Window, Collection, Annotation, SavedState, Startup, Emoji2, Graphics, ProfileInstaller, Tracing and their dependencies | The Android app framework | Apache-2.0 |
| [OkHttp](https://github.com/square/okhttp) and [Okio](https://github.com/square/okio) | The speed test's connection to M-Lab | Apache-2.0 |
| [Kotlin standard library](https://github.com/JetBrains/kotlin) | The language runtime | Apache-2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Background work | Apache-2.0 |
| [Guava ListenableFuture](https://github.com/google/guava) | AndroidX's async results | Apache-2.0 |
| [JetBrains Java Annotations](https://github.com/JetBrains/java-annotations) | Nullability annotations | Apache-2.0 |

**Vendor names** come from the [IEEE MA-L registry](https://standards-oui.ieee.org/), the public list
of which company each network-card address prefix belongs to.
[`tools/generate_oui_registry.py`](tools/generate_oui_registry.py) turns it into
`app/src/main/assets/oui_registry.tsv`.

**Speed tests** run on [Measurement Lab](https://www.measurementlab.net/) servers, under M-Lab's
[privacy policy](https://www.measurementlab.net/privacy/). M-Lab isn't part of WaveBalance and
doesn't endorse it.
