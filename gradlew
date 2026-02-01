#!/usr/bin/env sh

# Simplified Gradle wrapper script
DIR="$(cd "$(dirname "$0")" && pwd)"

JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
