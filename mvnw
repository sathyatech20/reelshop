#!/bin/sh
# Maven Wrapper Script

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

# If mvn is available on PATH, just use it
if command -v mvn > /dev/null 2>&1; then
    exec mvn "$@"
fi

echo "Maven not found. Please install Maven: brew install maven"
exit 1
