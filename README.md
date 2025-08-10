# Distributed Marketplace System

A sophisticated distributed marketplace implementation featuring **SAGA pattern** transactions, **Docker containerization**, and **fault-tolerant microservices**.

## 🚀 Execution Guide

### Option A: With Docker (Recommended)

#### Prerequisites
- Docker & Docker Compose installed
- Java 17+ (for local development)

#### Step-by-Step Instructions
1. **Remove old cache and container remnants (to guarantee a clean start)**
   ```bash
   # Stop and remove all running containers
   docker-compose down --volumes --remove-orphans
   
   # Clear Docker system cache
   docker system prune -f
   
   # Remove all images for complete restart (optional)
   docker image prune -a -f
   ```

2. **Build and start system**
   Option 3A: Using convenience scripts (Recommended)
   ./docker-build.sh    # Build Docker images
   ./docker-start.sh    # Start all services

   Option 3B: Manual Docker Compose
   ```bash
      docker-compose up --build
      ```
   > The system automatically starts 5 Seller services and 2 Marketplace services

3. **View logs (in new terminal)**
   ```bash
   # All services
   docker-compose logs -f
   
   # Only specific service
   docker-compose logs -f seller1
   ```
4. **Run integration tests(optional)**
   ./docker-test.sh

5. **Stop system**
   ```bash
   # In terminal with Ctrl+C or in new terminal:
   docker-compose down
   ```

#### Windows-Specific Docker Instructions

For Windows users, dedicated batch scripts are available for easier Docker management:

**Available Windows Scripts:**
- `docker-build.bat` - Build Docker images
- `docker-start.bat` - Start all services
- `docker-stop.bat` - Stop all services  
- `docker-test.bat` - Run integration tests

**Windows Quick Start:**
```batch
# Build Docker images
docker-build.bat

# Start services
docker-start.bat

# Check status
docker-compose ps

# Run tests (optional)
docker-test.bat

# Stop services
docker-stop.bat
```

**Windows Service Overview:**
After starting with `docker-start.bat`, the following services are available:
- **Seller Services**: localhost:5555-5559 (5 sellers)
- **Marketplace Services**: localhost:7777-7778 (2 marketplaces)

**Windows Log Viewing:**
```batch
# All logs
docker-compose logs -f

# Specific service logs
docker-compose logs -f seller1
docker-compose logs -f marketplace1
```

**Windows Troubleshooting:**
- Ensure Docker Desktop is running before executing scripts
- Run Command Prompt as Administrator if permission issues occur
- Check for port conflicts if services fail to start

---

### Option B: With Maven (Local Development)

#### Prerequisites
- Java 17+ installed and in PATH
- Maven installed and in PATH

#### Step-by-Step Instructions
1. **Remove old build cache (Clean Start)**
   ```bash
   # Clean Maven cache and target folder
   mvn clean
   
   # Stop all running Java processes (Linux/macOS)
   pkill -f "java.*marketplace" || true
   pkill -f "java.*seller" || true
   
   # Completely remove target directory (Linux/macOS)
   rm -rf target/
   ```
   
   **Windows equivalent:**
   ```batch
   # Clean Maven cache and target folder
   mvn clean
   
   # Stop all Java processes (Windows)
   taskkill /F /IM java.exe 2>nul || echo No Java processes found
   
   # Remove target directory (Windows)
   if exist target rmdir /s /q target
   ```

2. **Build project**
   ```bash
   # Build all JARs
   # On Linux and MacOS: 
   ./maven-build.sh
   # or on Windows:
   ./maven-build.bat
   
   # Alternative: Manual with Maven
   mvn clean package -DskipTests
   ```

3. **Start system**
   
   **Option 4A: Automatic (Recommended) - Opens all terminals automatically**
   ```bash
   # On Linux and MacOS:
   ./maven-start.sh
   
   # On Windows:
   ./maven-start.bat
   ```
   > This automatically opens 7 separate terminals: 5 for sellers + 2 for marketplaces
   
   **Option 4B: Manual - Start services individually (in separate terminals)**
   
   **Terminal 1-5: Start Sellers**
   ```bash
   # Seller 1
   java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5555 src/main/resources/seller1.yaml
   
   # Seller 2 (new terminal)
   java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5556 src/main/resources/seller2.yaml
   
   # Seller 3 (new terminal)
   java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5557 src/main/resources/seller3.yaml
   
   # Seller 4 (new terminal)
   java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5558 src/main/resources/seller4.yaml
   
   # Seller 5 (new terminal)
   java -jar target/seller-jar-with-dependencies.jar tcp://localhost:5559 src/main/resources/seller5.yaml
   ```
   
   **Terminal 6-7: Start Marketplaces** (after sellers)
   ```bash
   # Marketplace 1
   java -jar target/marketplace.jar src/main/resources/marketplace1.yaml
   
   # Marketplace 2 (new terminal)
   java -jar target/marketplace.jar src/main/resources/marketplace2.yaml
   ```

4. **Stop system**
   ```bash
   # End all terminals with Ctrl+C in each terminal, or:
   # Automatic stop (if using maven-start scripts):
   ./maven-stop.sh       # Linux/MacOS
   ./maven-stop.bat      # Windows
   
   # Manual stop (Linux/macOS):
   pkill -f "java.*marketplace"
   pkill -f "java.*seller"
   ```
   
   **Windows manual stop:**
   ```batch
   # Stop all Java processes (Windows)
   taskkill /F /IM java.exe 2>nul || echo No Java processes found
   ```
---

## � Troubleshooting

### Common Issues & Solutions

#### Port Already in Use
```bash
# Stop all containers
docker-compose down

# Remove orphaned containers
docker-compose down --remove-orphans
```

#### Container Connection Issues
```bash
# Check network connectivity
docker network ls
docker network inspect marketplace-network

# Restart with fresh network
docker-compose down
docker-compose up --build
```

#### Memory/Performance Issues
```bash
# Check resource usage
docker stats

# Limit container resources (add to docker-compose.yml)
# deploy:
#   resources:
#     limits:
#       memory: 512M
#       cpus: 0.5
```

#### Debug Container Issues
```bash
# Access container shell
docker exec -it marketplace-seller1 /bin/bash

# View detailed logs
docker logs --details marketplace-seller1
```

## System Architecture

### Containerized Microservices
```
┌────────────────────────────────────────────────────────┐
│                Docker Network                          │
│  ┌─────────────┐    ┌─────────────┐                    │
│  │Marketplace 1│◄──►│Marketplace 2│                    │
│  │ (port 7777) │    │ (port 7778) │                    │
│  └─────────────┘    └─────────────┘                    │
│         │                   │                          │
│         ▼                   ▼                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Seller Network                     │   │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐    │   │
│  │  │Seller 1│ │Seller 2│ │Seller 3│ │Seller 4│    │   │
│  │  │  :5555 │ │  :5556 │ │  :5557 │ │  :5558 │    │   │
│  │  └────────┘ └────────┘ └────────┘ └────────┘    │   │
│  │                        ┌────────┐               │   │
│  │                        │Seller 5│               │   │
│  │                        │  :5559 │               │   │
│  │                        └────────┘               │   │
│  └─────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

### SAGA Transaction Flow
```
Marketplace → RESERVE → All Sellers
     ↓
 Collect Responses
     ↓
┌─────────────┬─────────────┐
│All CONFIRM  │Some REJECT  │
│     ↓       │     ↓       │
│  COMMIT     │  ROLLBACK   │
│  to All     │  to All     │
└─────────────┴─────────────┘
```

## Features Implemented

### Core Features
- **SAGA Pattern**: 2-Phase Commit (RESERVE → COMMIT/ROLLBACK)
- **Distributed Architecture**: 5 Sellers + 2 Marketplaces
- **ZeroMQ Messaging**: Asynchronous inter-service communication
- **Docker Containerization**: Full orchestration with docker-compose
- **Fault Tolerance**: Network failures, timeouts, crash simulation
- **Inventory Management**: Thread-safe product reservations
- **Health Monitoring**: System status and performance tracking

### Advanced Features
- **Concurrent Processing**: Multi-threaded order handling
- **Configuration Management**: YAML-based service configuration
- **Integration Testing**: Comprehensive test suite
- **Process Monitoring**: Real-time system observability
- **Maven Build Pipeline**: Multi-JAR builds with profiles

---

## Development Team Contributions

### **Antonia** - Integration & Testing
- Maven build profiles and automation scripts
- System monitoring and health checks
- Integration testing framework
- Process coordination scripts
- Build automation and deployment
- .bat files for windows

### **Yana** - Marketplace Logic & Docker Infrastructure
- SAGA pattern implementation
- Marketplace coordination logic
- Transaction state management
- Error handling and rollback mechanisms
- Docker containerization and orchestration
- Docker-compose configuration and networking

### **Noah** - Seller Architecture & Inventory
- Seller service implementation
- Product inventory management system  
- Thread-safe reservation mechanisms
- Seller configuration and YAML integration
- Network fault simulation
- README documentation and improvements
