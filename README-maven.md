# Distributed Marketplace System (Maven)

A cross-platform distributed marketplace system with ZeroMQ messaging, supporting Windows, Mac, and Linux using Maven.

## System Architecture

- **5 Seller Processes** (ports 5555-5559)
- **2 Marketplace Processes** (ports 7777-7778)
- **ZeroMQ Messaging** for inter-process communication
- **Health Monitoring** and **Performance Tracking**
- **Integration Testing** suite

## Requirements

- **Java 11+** (required on all platforms)
- **Maven 3.6+** (required on all platforms)

## Installation

### Windows
```powershell
# Install Maven using Chocolatey
choco install maven -y
# Restart your terminal
```

### Mac
```bash
# Install Maven using Homebrew
brew install maven
```

### Linux
```bash
# Ubuntu/Debian
sudo apt-get install maven

# CentOS/RHEL
sudo yum install maven
```

## Quick Start

### 1. Build the System

**Windows:**
```batch
maven-build.bat
```

**Mac/Linux:**
```bash
chmod +x *.sh
./maven-build.sh
```

### 2. Start the System

**Windows:**
```batch
maven-start.bat
```

**Mac/Linux:**
```bash
./maven-start.sh
```

### 3. Test the System

**All Platforms:**
```bash
mvn exec:java -Pintegration-test
mvn exec:java -Phealth-check
```

### 4. Stop the System

**Mac/Linux:**
```bash
./maven-stop.sh
```

**Windows:**
```batch
# Close the terminal windows or use Task Manager
```

## Maven Commands (Cross-Platform)

### Development Commands
```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Build JAR packages
mvn package

# Clean build artifacts
mvn clean
```

### Individual Component Commands
```bash
# Health checker
mvn exec:java -Phealth-check

# Integration tests
mvn exec:java -Pintegration-test

# Individual seller (with custom port)
mvn exec:java -Pseller -Dexec.args="tcp://localhost:5555"

# Individual marketplace (with custom port)
mvn exec:java -Pmarketplace -Dexec.args="7777"
```

### Advanced Usage
```bash
# Run seller on different port
mvn exec:java -Pseller -Dexec.args="tcp://localhost:6666"

# Run marketplace on different port
mvn exec:java -Pmarketplace -Dexec.args="8888"

# Run with different seller endpoints
mvn exec:java -Pmarketplace -Dexec.args="7777,tcp://localhost:5555,tcp://localhost:5556"
```

## Project Structure (Maven Standard)
```
DistributedMarketplace/
├── pom.xml                 # Maven configuration
├── src/main/java/          # Java source files
├── target/                 # Maven build output
├── lib/                    # External libraries (ZeroMQ)
├── maven-*.bat            # Windows scripts
├── maven-*.sh             # Mac/Linux scripts
└── README-maven.md        # This file
```

## Team Development

### Cross-Platform Commands
All team members can use the same Maven commands:

**Windows (PowerShell/CMD):**
```batch
mvn clean compile
mvn exec:java -Phealth-check
```

**Mac (Terminal):**
```bash
mvn clean compile
mvn exec:java -Phealth-check
```

**Linux (Terminal):**
```bash
mvn clean compile
mvn exec:java -Phealth-check
```

### IDE Integration
- **IntelliJ IDEA**: Import as Maven project
- **Eclipse**: Import as Maven project
- **VS Code**: Install Java Extension Pack, auto-detects Maven

## Migrating from Batch Files

Your existing `.bat` files are replaced with Maven profiles:

| Old Command | New Maven Command |
|-------------|-------------------|
| `compile.bat` | `mvn clean compile` |
| `build_jars.bat` | `mvn package` |
| `start_all_auto.bat` | `./maven-start.sh` (Mac/Linux) or `maven-start.bat` (Windows) |
| `health_check.bat` | `mvn exec:java -Phealth-check` |
| `run_integration_test.bat` | `mvn exec:java -Pintegration-test` |

## Benefits of Maven Migration

- ✅ **Cross-platform compatibility** (Windows, Mac, Linux)
- ✅ **Automatic dependency management** (no need for lib folder)
- ✅ **Standard project structure**
- ✅ **IDE integration**
- ✅ **Build lifecycle management**
- ✅ **Plugin ecosystem**
- ✅ **Team collaboration**

## Troubleshooting

### Maven Not Found
```bash
# Check Maven installation
mvn --version

# Windows: Restart terminal after installation
# Mac/Linux: Check PATH environment variable
```

### Permission Issues (Mac/Linux)
```bash
chmod +x *.sh
```

### Port Already in Use
```bash
# Stop existing processes
./maven-stop.sh  # Mac/Linux
# or close terminal windows (Windows)
```

### Java Version Issues
```bash
# Check Java version
java -version

# Ensure Java 11+ is installed
```

## Key Features

- ✅ **Cross-platform Maven build**
- ✅ **Automatic dependency management**
- ✅ **Health monitoring system**
- ✅ **Integration testing suite**
- ✅ **Performance tracking**
- ✅ **Multi-process orchestration**
- ✅ **ZeroMQ messaging**
- ✅ **Professional development workflow**
