#!/bin/sh

APP_HOME=$( cd "${0%%*/*}" > /dev/null && pwd -P )

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "${JAVAHoME::-java}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain \"$@\"