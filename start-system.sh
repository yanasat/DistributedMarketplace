#!/bin/bash
# Cross-platform system startup script
# Works on Windows (Git Bash), Mac, and Linux

echo "==============================================="
echo "  Starting Distributed Marketplace System"
echo "==============================================="

# Check if JARs exist
if [ ! -f "target/jars/seller.jar" ]; then
    echo "ERROR: JAR files not found. Please run ./build-jars.sh first."
    exit 1
fi

echo "Step 1: Starting all sellers..."
echo "Starting 5 seller processes..."

# Start sellers in background
java -jar target/jars/seller.jar tcp://localhost:5555 &
java -jar target/jars/seller.jar tcp://localhost:5556 &
java -jar target/jars/seller.jar tcp://localhost:5557 &
java -jar target/jars/seller.jar tcp://localhost:5558 &
java -jar target/jars/seller.jar tcp://localhost:5559 &

echo "All 5 sellers started!"

echo ""
echo "Step 2: Waiting for sellers to initialize..."
sleep 5

echo "Step 3: Starting all marketplaces..."
echo "Starting 2 marketplace processes..."

# Start marketplaces in background
java -jar target/jars/marketplace.jar 7777 &
java -jar target/jars/marketplace.jar 7778 &

echo "All 2 marketplaces started!"

echo ""
echo "===== SYSTEM STARTUP COMPLETE ====="
echo ""
echo "You should now have 7 processes running:"
echo "- 5 Seller processes (using seller.jar)"
echo "- 2 Marketplace processes (using marketplace.jar)"
echo ""
echo "To test the system:"
echo "  java -jar target/jars/integration-test.jar"
echo ""
echo "To check system health:"
echo "  java -jar target/jars/health-checker.jar"
echo ""
echo "To stop all processes:"
echo "  ./stop-system.sh"
echo ""
