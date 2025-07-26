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
    echo "Stopping Marketplace processes on Mac..."
    pkill -f "MarketplaceProcess" 2>/dev/null || echo "No Marketplace processes found"
else
    # Linux
    echo "Stopping Maven processes on Linux..."
    pkill -f "mvn exec:java" 2>/dev/null || echo "No Maven processes found"
    echo "Stopping Marketplace processes on Linux..."
    pkill -f "MarketplaceProcess" 2>/dev/null || echo "No Marketplace processes found"
fi

# Forcefully terminate lingering threads or processes
echo "Checking for lingering Marketplace threads..."
jps | grep MarketplaceProcess | awk '{print $1}' | xargs -r kill -9 2>/dev/null || echo "No lingering Marketplace threads found"

echo "All Maven and Marketplace processes stopped."
echo ""
