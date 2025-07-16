#!/bin/bash
# Maven system startup script for Mac/Linux
echo "==============================================="
echo "  Starting Distributed Marketplace (Maven)"
echo "==============================================="

echo "Step 1: Starting 5 seller processes..."
mvn exec:java -Pseller -Dexec.args="tcp://localhost:5555" &
mvn exec:java -Pseller -Dexec.args="tcp://localhost:5556" &
mvn exec:java -Pseller -Dexec.args="tcp://localhost:5557" &
mvn exec:java -Pseller -Dexec.args="tcp://localhost:5558" &
mvn exec:java -Pseller -Dexec.args="tcp://localhost:5559" &

echo "All 5 sellers started!"

echo ""
echo "Step 2: Waiting for sellers to initialize..."
sleep 8

echo "Step 3: Starting 2 marketplace processes..."
mvn exec:java -Pmarketplace -Dexec.args="7777" &
mvn exec:java -Pmarketplace -Dexec.args="7778" &

echo "All 2 marketplaces started!"

echo ""
echo "===== MAVEN SYSTEM STARTUP COMPLETE ====="
echo ""
echo "Background processes started:"
echo "- 5 Seller processes (Maven profiles)"
echo "- 2 Marketplace processes (Maven profiles)"
echo ""
echo "To test the system:"
echo "  mvn exec:java -Pintegration-test"
echo ""
echo "To check system health:"
echo "  mvn exec:java -Phealth-check"
echo ""
echo "To stop all processes:"
echo "  ./maven-stop.sh"
echo ""
