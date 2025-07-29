#!/bin/bash
# Startup script for JAR-based distributed marketplace

echo "================================================"
echo "  Starting Distributed Marketplace (JAR Mode)"
echo "================================================"

# Check if JAR files exist
if [ ! -f "target/marketplace.jar" ]; then
    echo "ERROR: JAR files not found. Please run ./build-jars.sh first"
    exit 1
fi

echo "Step 1: Starting 5 seller processes..."

# Function to open terminal with JAR
open_terminal_jar() {
    local cmd="$1"
    local title="$2"
    
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command -v gnome-terminal &> /dev/null; then
            gnome-terminal --tab --title="$title" -- bash -c "cd \"$(pwd)\" && $cmd; exec bash" &
        elif command -v xfce4-terminal &> /dev/null; then
            xfce4-terminal --tab --title="$title" --hold -e "bash -c 'cd \"$(pwd)\" && $cmd; exec bash'" &
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        osascript -e "tell app \"Terminal\" to do script \"cd \\\"$(pwd)\\\" && $cmd\"" &
    fi
}

# Start sellers with their specific configurations
open_terminal_jar "java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5555 src/main/resources/seller1.yaml" "Seller-5555"
sleep 1
open_terminal_jar "java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5556 src/main/resources/seller2.yaml" "Seller-5556"
sleep 1
open_terminal_jar "java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5557 src/main/resources/seller3.yaml" "Seller-5557"
sleep 1
open_terminal_jar "java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5558 src/main/resources/seller4.yaml" "Seller-5558"
sleep 1
open_terminal_jar "java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5559 src/main/resources/seller5.yaml" "Seller-5559"

echo "All 5 sellers started!"

echo ""
echo "Step 2: Waiting for sellers to initialize..."
sleep 8

echo "Step 3: Starting 2 marketplace processes..."
open_terminal_jar "java -jar target/marketplace.jar src/main/resources/marketplace1.yaml" "Marketplace-7777"
sleep 1
open_terminal_jar "java -jar target/marketplace.jar src/main/resources/marketplace2.yaml" "Marketplace-7778"

echo "All 2 marketplaces started!"
echo ""
echo "===== JAR SYSTEM STARTUP COMPLETE ====="
echo ""
echo "To test the system:"
echo "  java -jar target/integration-test-jar-with-dependencies.jar"
echo ""
echo "To check system health:"
echo "  java -jar target/health-check-jar-with-dependencies.jar"
echo ""
echo "To stop all processes: Close the terminal windows or use Ctrl+C"
echo ""