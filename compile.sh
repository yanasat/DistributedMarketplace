#!/bin/bash
# Cross-platform compilation script
# Works on Windows (Git Bash), Mac, and Linux

echo "==============================================="
echo "  Compiling Distributed Marketplace System"
echo "==============================================="

# Create target directory
mkdir -p target/classes

# Find Java source files
echo "Compiling Java source files..."

# Compile with proper classpath (works on all platforms)
if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows (Git Bash)
    SEPARATOR=";"
    JEROMQ_PATH="lib/jeromq-0.5.2.jar"
else
    # Mac/Linux
    SEPARATOR=":"
    JEROMQ_PATH="lib/jeromq-0.5.2.jar"
fi

# Compile core classes first
echo "Compiling core classes..."
javac -cp "$JEROMQ_PATH" -d target/classes src/main/java/messaging/MessageUtils.java
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/model/Order.java
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/ProcessMonitor.java

# Compile seller classes
echo "Compiling seller classes..."
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/seller/SellerStub.java

# Compile marketplace classes
echo "Compiling marketplace classes..."
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/marketplace/Marketplace.java

# Compile main entry points
echo "Compiling main classes..."
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/HealthChecker.java
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/IntegrationTest.java
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/SellerProcess.java
javac -cp "$JEROMQ_PATH${SEPARATOR}target/classes" -d target/classes src/main/java/MarketplaceProcess.java

echo "All files compiled successfully!"
echo "Compiled classes are in target/classes/"
