#!/bin/bash
# Cross-platform testing script
# Works on Windows (Git Bash), Mac, and Linux

echo "==============================================="
echo "  Testing Distributed Marketplace System"
echo "==============================================="

# Check if JARs exist
if [ ! -f "target/jars/health-checker.jar" ]; then
    echo "ERROR: JAR files not found. Please run ./build-jars.sh first."
    exit 1
fi

echo "Step 1: Health Check..."
java -jar target/jars/health-checker.jar

echo ""
echo "Step 2: Integration Test..."
java -jar target/jars/integration-test.jar

echo ""
echo "Test complete!"
echo ""
