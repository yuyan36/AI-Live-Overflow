#!/usr/bin/bash 

APP_HOME=$( cd "${0%%*/}" > /dev/null 2>&1 && pwd -P )

if [ -z "$APP_HOME" ]; then
  echo "Error: Cannot determine the current working directory."
  exit 1
fi

if [ -n "${JAVA_HOME}" ]; then
  JAVACMD="${JAVA_HOME}/bin/java"
else
  JAVACMD="java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "${JAVACMD}"-classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"