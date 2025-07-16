#!/bin/bash
# Cross-platform system stop script
# Works on Windows (Git Bash), Mac, and Linux

echo "==============================================="
echo "  Stopping Distributed Marketplace System"
echo "==============================================="

# Detect OS and stop Java processes accordingly
if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows (Git Bash)
    echo "Stopping Java processes on Windows..."
    taskkill //F //IM java.exe 2>/dev/null || echo "No Java processes found"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac
    echo "Stopping Java processes on Mac..."
    pkill -f "java -jar target/jars" 2>/dev/null || echo "No Java processes found"
else
    # Linux
    echo "Stopping Java processes on Linux..."
    pkill -f "java -jar target/jars" 2>/dev/null || echo "No Java processes found"
fi

echo "All distributed marketplace processes stopped."
echo ""
