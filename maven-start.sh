#!/bin/bash
# Maven system startup script for Mac/Linux
echo "==============================================="
echo "  Starting Distributed Marketplace (Maven)"
echo "==============================================="

echo "Step 1: Starting 5 seller processes..."
open_terminal() {
    local cmd="$1"
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command -v gnome-terminal &> /dev/null; then
            gnome-terminal -- bash -c "$cmd; exec bash" &
        elif command -v xfce4-terminal &> /dev/null; then
            xfce4-terminal --hold -e "$cmd" &
        elif command -v xterm &> /dev/null; then
            xterm -hold -e "$cmd" &
        else
            echo "Kein unterstütztes Terminal gefunden!"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        osascript -e "tell application \"Terminal\" to do script \"$cmd\"" &
    elif [[ "$OSTYPE" == "cygwin"* || "$OSTYPE" == "msys"* || "$OSTYPE" == "win32" ]]; then
        start cmd /k "$cmd"
    else
        echo "Unbekanntes Betriebssystem: $OSTYPE"
    fi
}

echo "Step 1: Starting 5 seller processes..."
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5555'"
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5556'"
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5557'"
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5558'"
open_terminal "mvn exec:java -Pseller -Dexec.args='tcp://localhost:5559'"

echo "All 5 sellers started!"

echo ""
echo "Step 2: Waiting for sellers to initialize..."
sleep 8

echo "Step 3: Starting 2 marketplace processes..."
gnome-terminal -- bash -c "mvn exec:java -Pmarketplace -Dexec.args='7777'; exec bash" &
gnome-terminal -- bash -c "mvn exec:java -Pmarketplace -Dexec.args='7778'; exec bash" &

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
