#!/bin/bash

### **Authored by Shivam Kumar on 2025-12-06 with open source community ideas and services
### and with the help of my project team Tushar, Saurabh, and Shreya. **

set -e

mkdir -p out

# Try to find javac
JAVAC_CMD="javac"
if ! command -v javac &> /dev/null; then
    echo "javac not found in PATH. Trying standard locations..."
    if [ -f "/usr/lib/jvm/java-21-openjdk-amd64/bin/javac" ]; then
        JAVAC_CMD="/usr/lib/jvm/java-21-openjdk-amd64/bin/javac"
    elif [ -n "$JAVA_HOME" ]; then
        JAVAC_CMD="$JAVA_HOME/bin/javac"
    else
        # Try to infer from java path
        JAVA_PATH=$(readlink -f $(which java))
        JAVAC_CANDIDATE=$(dirname "$JAVA_PATH")/javac
        if [ -f "$JAVAC_CANDIDATE" ]; then
            JAVAC_CMD="$JAVAC_CANDIDATE"
        else
            echo "Error: JDK not found. Please install a JDK."
            exit 1
        fi
    fi
fi

echo "Using compiler: $JAVAC_CMD"

# Compile all java files
echo "Compiling..."
"$JAVAC_CMD" -d out -sourcepath src $(find src -name "*.java")

if [ "$1" == "server" ]; then
    echo "Starting Discovery Server..."
    java -cp out p2p.net.DiscoveryServer
elif [ "$1" == "client" ]; then
    echo "Starting Client..."
    java -cp out p2p.App
else
    echo "Usage: ./build.sh [server|client]"
fi
