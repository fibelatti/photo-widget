#! /usr/bin/env bash

set -e

./gradlew app:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/res/raw/ -PaboutLibraries.exportVariant=release
