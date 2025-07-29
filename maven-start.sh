#!/bin/bash
# Maven system startup script for Mac/Linux
echo "==============================================="
echo "  Starting Distributed Marketplace (Maven)"
echo "==============================================="

echo "Step 1: Starting 5 seller processes..."

# Funktion zum Öffnen von Terminal-Tabs
open_terminal() {
    local cmd="$1"
    local title="$2"
    
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command -v gnome-terminal &> /dev/null; then
            gnome-terminal --tab --title="$title" -- bash -c "cd \"$(pwd)\" && $cmd; exec bash" &
        elif command -v xfce4-terminal &> /dev/null; then
            xfce4-terminal --tab --title="$title" --hold -e "bash -c 'cd \"$(pwd)\" && $cmd; exec bash'" &
        elif command -v xterm &> /dev/null; then
            xterm -title "$title" -hold -e "bash -c 'cd \"$(pwd)\" && $cmd; exec bash'" &
        else
            echo "Kein unterstütztes Terminal gefunden!"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS - mit richtig escaptem Pfad
        osascript -e "tell app \"Terminal\" to do script \"cd \\\"$(pwd)\\\" && $cmd\"" &
    fi
}

# Seller starten
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5555'" "Seller-5555"
sleep 1
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5556'" "Seller-5556"
sleep 1
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5557'" "Seller-5557"
sleep 1
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5558'" "Seller-5558"
sleep 1
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5559'" "Seller-5559"

echo "All 5 sellers started!"

echo ""
echo "Step 2: Waiting for sellers to initialize..."
sleep 8

echo "Step 3: Starting 2 marketplace processes..."
open_terminal "mvn exec:java -Pmarketplace -Dexec.args='7777'" "Marketplace-7777"
sleep 1
open_terminal "mvn exec:java -Pmarketplace -Dexec.args='7778'" "Marketplace-7778"

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