BUILD (needs JDK 21 + Gradle 8.x installed, or copy gradlew/gradle/ from a template at fabricmc.net/develop):
  gradle build
Jar appears in build/libs/coinflipbot-1.0.0.jar  (use the one WITHOUT "-sources")
Put it in .minecraft/mods with Fabric Loader + Fabric API for 1.21.4.
Press J in-game to toggle.
Different MC version? Edit gradle.properties using values from fabricmc.net/develop.
