#!/bin/bash
# Maven build script for Mac/Linux
echo "==============================================="
echo "  Building Distributed Marketplace (Maven)"
echo "==============================================="

echo "Step 1: Compiling with Maven..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "ERROR: Maven compilation failed"
    exit 1
fi

echo "Step 2: Running tests..."
mvn test

echo "Step 3: Building JAR packages..."
mvn package

echo ""
echo "===== MAVEN BUILD COMPLETE ====="
echo ""
echo "Available commands:"
echo "  mvn exec:java                          - Run integration test"
echo "  mvn exec:java -Phealth-check          - Run health checker"
echo "  mvn exec:java -Pseller                 - Run seller process"
echo "  mvn exec:java -Pmarketplace            - Run marketplace process"
echo ""
