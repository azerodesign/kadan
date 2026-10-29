# Kadan v1 ProGuard rules.
# Room + Compose need no extra rules for a debug build. Release (minify on)
# keeps Room entities and DAOs via the standard Room rules bundled with the
# library; add rules here only if a release build actually strips something.
-keep class com.azerostudio.kadan.data.** { *; }
