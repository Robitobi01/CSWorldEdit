# CSWorldEdit

Minecraft 1.12.2 · Cleanroom 0.7.12-alpha · Java 25 · Gradle 9.8.1

```sh
nix develop
./gradlew build
./gradlew runClient
```

Use Java 25 for the Gradle JVM; `runClient` picks up the Nix native libraries on NixOS.

Client-side WorldEdit-style commands and selections. Enable the mod in its config,
then use `/undo`, `/set`, `/diagline` and the other client commands.

Settings: `gradle.properties`. Install `build/libs/csworldedit-<version>.jar`.
