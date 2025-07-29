#!/bin/bash
# Build script for creating executable JAR files

echo "================================================"
echo "  Building Distributed Marketplace JAR Files"
echo "================================================"

echo "Step 1: Clean previous builds..."
mvn clean

echo "Step 2: Compile sources..."
mvn compile

echo "Step 3: Run tests..."
mvn test

echo "Step 4: Package JAR files..."
mvn package

echo ""
echo "===== BUILD COMPLETE ====="
echo ""
echo "Generated JAR files:"
echo "  target/marketplace.jar              - Marketplace process"
echo "  target/seller-jar-with-dependencies.jar - Seller process"
echo "  target/integration-test-jar-with-dependencies.jar - Integration test"
echo "  target/health-check-jar-with-dependencies.jar - Health checker"
echo ""
echo "Usage examples:"
echo "  java -jar target/marketplace.jar [config.yaml]"
echo "  java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5555 [config.yaml]"
echo "  java -jar target/integration-test-jar-with-dependencies.jar"
echo "  java -jar target/health-check-jar-with-dependencies.jar"
echo ""