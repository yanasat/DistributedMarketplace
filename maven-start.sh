#!/bin/bash
# Fixed Maven system startup script with correct working directory
echo "==============================================="
echo "  Starting Distributed Marketplace (Maven)"
echo "==============================================="

# Get absolute path of current directory
PROJECT_DIR=$(pwd)
echo "📁 Project directory: $PROJECT_DIR"

echo "Step 1: Starting 5 seller processes..."

# Mac-specific terminal commands with correct working directory
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected Mac OS - using Terminal.app"
    
    # Fixed osascript commands that properly set working directory
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Seller 1 (Port 5555) - Working in: \$(pwd)' && mvn exec:java -Pseller -Dexec.args='tcp://localhost:5555'\""
    sleep 1
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Seller 2 (Port 5556) - Working in: \$(pwd)' && mvn exec:java -Pseller -Dexec.args='tcp://localhost:5556'\""
    sleep 1
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Seller 3 (Port 5557) - Working in: \$(pwd)' && mvn exec:java -Pseller -Dexec.args='tcp://localhost:5557'\""
    sleep 1
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Seller 4 (Port 5558) - Working in: \$(pwd)' && mvn exec:java -Pseller -Dexec.args='tcp://localhost:5558'\""
    sleep 1
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Seller 5 (Port 5559) - Working in: \$(pwd)' && mvn exec:java -Pseller -Dexec.args='tcp://localhost:5559'\""
    
else
    # Linux fallback (background processes)
    echo "Detected Linux - using background processes"
    mvn exec:java -Pseller -Dexec.args="tcp://localhost:5555" &
    mvn exec:java -Pseller -Dexec.args="tcp://localhost:5556" &
    mvn exec:java -Pseller -Dexec.args="tcp://localhost:5557" &
    mvn exec:java -Pseller -Dexec.args="tcp://localhost:5558" &
    mvn exec:java -Pseller -Dexec.args="tcp://localhost:5559" &
fi

echo "All 5 sellers started!"

echo ""
echo "Step 2: Waiting for sellers to initialize..."
sleep 10

echo "Step 3: Starting 2 marketplace processes..."

# Mac-specific marketplace commands with correct working directory
if [[ "$OSTYPE" == "darwin"* ]]; then
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Marketplace 1 (Port 7777) - Working in: \$(pwd)' && mvn exec:java -Pmarketplace -Dexec.args='7777'\""
    sleep 1
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && echo '🏪 Marketplace 2 (Port 7778) - Working in: \$(pwd)' && mvn exec:java -Pmarketplace -Dexec.args='7778'\""
else
    # Linux fallback
    mvn exec:java -Pmarketplace -Dexec.args="7777" &
    mvn exec:java -Pmarketplace -Dexec.args="7778" &
fi

echo "All 2 marketplaces started!"

echo ""
echo "===== MAVEN SYSTEM STARTUP COMPLETE ====="
echo ""
echo "✅ You should now see 7 Terminal windows/tabs:"
echo "- 5 Seller terminals (ports 5555-5559)"
echo "- 2 Marketplace terminals (ports 7777-7778)"
echo ""
echo "Each terminal should show: 'Working in: $PROJECT_DIR'"
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