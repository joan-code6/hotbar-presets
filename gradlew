#!/bin/sh
# (C) Copyright (c) 2015-2018 Gradle Inc.
#
# All rights reserved.
#
# gradle-wrapper.sh

[ -z "$JAVA_HOME" ] && echo "Warning: JAVA_HOME environment variable is not set." && echo "Trying to find java on PATH"

# Determine OS and architecture
case "`uname`" in
  CYGWIN* | MINGW* | MSYS*)
    WRAPPER_OS="windows"
    ;;
  Darwin* )
    WRAPPER_OS="mac"
    ;;
  *)
    WRAPPER_OS="linux"
    ;;
esac

WRAPPER_FILE=`basename "$0"`
WRAPPER_DIR=`dirname "$0"`
WRAPPER_PROPERTIES="$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.properties"

if [ -r "$WRAPPER_PROPERTIES" ]; then
    . "$WRAPPER_PROPERTIES"
fi

if [ -z "$distributionUrl" ]; then
    distributionUrl="https://services.gradle.org/distributions/gradle-8.10.2-bin.zip"
fi

if [ ! -f "$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Downloading gradle-wrapper.jar..."
    mkdir -p "$WRAPPER_DIR/gradle/wrapper"

    # Use curl or wget to download
    if command -v curl > /dev/null 2>&1; then
        curl -L -o "$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.jar" "https://github.com/gradle/gradle/raw/v8.10.2/gradle/wrapper/gradle-wrapper.jar"
    elif command -v wget > /dev/null 2>&1; then
        wget -O "$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.jar" "https://github.com/gradle/gradle/raw/v8.10.2/gradle/wrapper/gradle-wrapper.jar"
    else
        echo "Error: neither curl nor wget is available."
        exit 1
    fi
fi

exec java -cp "$WRAPPER_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
