#!/bin/bash
# Maven system stop script for Mac/Linux
echo "==============================================="
echo "  Stopping Distributed Marketplace (Maven)"
echo "==============================================="

# Stop Java processes
if [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac
    echo "Stopping Maven processes on Mac..."
    pkill -f "mvn exec:java" 2>/dev/null || echo "No Maven processes found"
else
    # Linux
    echo "Stopping Maven processes on Linux..."
    pkill -f "mvn exec:java" 2>/dev/null || echo "No Maven processes found"
fi

echo "All Maven processes stopped."
echo ""
