#!/bin/bash
# Docker build script for Distributed Marketplace

echo "================================================="
echo "  Building Distributed Marketplace Docker Images"
echo "================================================="

# Create docker directories if they don't exist
mkdir -p docker/scripts

# Make scripts executable
chmod +x docker/scripts/*.sh

echo "Step 1: Building Docker image..."
docker build -t distributed-marketplace:latest .

if [ $? -eq 0 ]; then
    echo "✅ Docker image built successfully!"
    
    echo ""
    echo "Step 2: Verifying image..."
    docker images | grep distributed-marketplace
    
    echo ""
    echo "===== DOCKER BUILD COMPLETE ====="
    echo ""
    echo "Available commands:"
    echo "  ./docker-start.sh              - Start all services"
    echo "  ./docker-test.sh               - Run integration tests"
    echo "  ./docker-stop.sh               - Stop all services"
    echo "  docker-compose logs -f         - View logs"
    echo ""
else
    echo "❌ Docker build failed!"
    exit 1
fi