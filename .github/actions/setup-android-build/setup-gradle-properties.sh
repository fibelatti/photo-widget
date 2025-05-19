#! /usr/bin/env bash

set -e

readonly workdir="$GITHUB_WORKSPACE"
readonly properties_file="$workdir/gradle.properties"

write_property() {
    echo "$1" >>$properties_file
}

write_common_properties() {
    echo "Writing common Gradle properties for the GHA runner"

    # Gradle properties common to all build environments
    write_property "org.gradle.caching=true"
    write_property "org.gradle.configuration-cache=true"
    write_property "org.gradle.configuration-cache.parallel=true"
    write_property "org.gradle.configureondemand=true"
    write_property "org.gradle.daemon=true"
    write_property "org.gradle.parallel=true"
    write_property "org.gradle.logging.stacktrace=all"

    # Kotlin properties common to all build environments
    write_property "kotlin.code.style=official"
    write_property "ksp.useKSP2=true"

    # Android properties common to all build environments
    write_property "android.useAndroidX=true"
}

write_macos_properties() {
    echo "Fine tuning Gradle properties for MacOS GHA runner"

    write_property "org.gradle.jvmargs=-Xmx8g -Xms512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC -Dfile.encoding=UTF-8"
    write_property "kotlin.daemon.jvmargs=-Xmx2g -Xms512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC -Dfile.encoding=UTF-8"
}

write_linux_properties() {
    echo "Fine tuning Gradle properties for Linux GHA runner"

    write_property "org.gradle.jvmargs=-Xmx4g -Xms512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC -Dfile.encoding=UTF-8"
    write_property "kotlin.daemon.jvmargs=-Xmx2g -Xms512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC -Dfile.encoding=UTF-8"
}

rm "$properties_file" && touch "$properties_file"

echo
write_common_properties

case "$RUNNER_OS" in
"macOS")
    write_macos_properties
    ;;
*)
    write_linux_properties
    ;;
esac

echo
cat $properties_file
echo
