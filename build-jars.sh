#!/bin/bash
# Cross-platform JAR building script
# Works on Windows (Git Bash), Mac, and Linux

echo "==============================================="
echo "  Building Distributed Marketplace JARs"
echo "==============================================="

# Compile first
echo "Step 1: Compiling source code..."
./compile.sh

# Create JARs directory
echo "Step 2: Creating JAR output directory..."
mkdir -p target/jars

# Extract ZeroMQ library
echo "Step 3: Extracting ZeroMQ library..."
mkdir -p target/temp_lib
cd target/temp_lib
jar xf ../../lib/jeromq-0.5.2.jar
cd ../..

# Build JAR files
echo "Step 4: Building JAR files..."

# Health Checker JAR
echo "Main-Class: HealthChecker" > target/temp_manifest.txt
echo "" >> target/temp_manifest.txt
jar cfm target/jars/health-checker.jar target/temp_manifest.txt -C target/classes . -C target/temp_lib .

# Integration Test JAR
echo "Main-Class: IntegrationTest" > target/temp_manifest.txt
echo "" >> target/temp_manifest.txt
jar cfm target/jars/integration-test.jar target/temp_manifest.txt -C target/classes . -C target/temp_lib .

# Seller JAR
echo "Main-Class: SellerProcess" > target/temp_manifest.txt
echo "" >> target/temp_manifest.txt
jar cfm target/jars/seller.jar target/temp_manifest.txt -C target/classes . -C target/temp_lib .

# Marketplace JAR
echo "Main-Class: MarketplaceProcess" > target/temp_manifest.txt
echo "" >> target/temp_manifest.txt
jar cfm target/jars/marketplace.jar target/temp_manifest.txt -C target/classes . -C target/temp_lib .

# Cleanup
echo "Step 5: Cleaning up..."
rm -rf target/temp_lib
rm -f target/temp_manifest.txt

echo ""
echo "===== JAR BUILD COMPLETE ====="
echo ""
echo "Created JAR files in target/jars/:"
echo "- health-checker.jar"
echo "- integration-test.jar"
echo "- seller.jar"
echo "- marketplace.jar"
echo ""
echo "Usage:"
echo "  java -jar target/jars/health-checker.jar"
echo "  java -jar target/jars/integration-test.jar"
echo "  java -jar target/jars/seller.jar [endpoint]"
echo "  java -jar target/jars/marketplace.jar [port]"
echo ""
