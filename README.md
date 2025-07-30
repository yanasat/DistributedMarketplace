# Distributed Marketplace System

A sophisticated distributed marketplace implementation featuring **SAGA pattern** transactions, **Docker containerization**, and **fault-tolerant microservices**.

## 🚀 Execution Guide

### Option A: With Docker (Recommended)

#### Prerequisites
- Docker & Docker Compose installed
- Java 17+ (for local development)

#### Step-by-Step Instructions
1. **Clone repository and navigate to directory**
   ```bash
   git clone <repository-url>
   cd DistributedMarketplace
   ```

2. **Remove old cache and container remnants (to guarantee a clean start)**
   ```bash
   # Stop and remove all running containers
   docker-compose down --volumes --remove-orphans
   
   # Clear Docker system cache
   docker system prune -f
   
   # Remove all images for complete restart (optional)
   docker image prune -a -f
   ```

3. **Build and start system**
   ```bash
   docker-compose up --build
   ```
   > The system automatically starts 5 Seller services and 2 Marketplace services

4. **View logs (in new terminal)**
   ```bash
   # All services
   docker-compose logs -f
   
   # Only specific service
   docker-compose logs -f marketplace1
   docker-compose logs -f seller1
   ```

5. **Stop system**
   ```bash
   # In terminal with Ctrl+C or in new terminal:
   docker-compose down
   ```

---

### Option B: With Maven (Local Development)

#### Prerequisites
- Java 17+ installed and in PATH
- Maven installed and in PATH

#### Step-by-Step Instructions
1. **Clone repository and navigate to directory**
   ```bash
   git clone <repository-url>
   cd DistributedMarketplace
   ```

2. **Remove old build cache (Clean Start)**
   ```bash
   # Clean Maven cache and target folder
   mvn clean
   
   # Stop all running Java processes
   pkill -f "java.*marketplace" || true
   pkill -f "java.*seller" || true
   
   # Completely remove target directory
   rm -rf target/
   ```

3. **Build project**
   ```bash
   # Build all JARs
   # On Linux and MacOS: 
   ./maven-build.sh
   # or on Windows:
   ./maven-build.bat
   
   # Alternative: Manual with Maven
   mvn clean package -DskipTests
   ```

4. **Start system**
   
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

5. **Stop system**
   ```bash
   # End all terminals with Ctrl+C in each terminal, or:
   # Automatic stop (if using maven-start scripts):
   ./maven-stop.sh       # Linux/MacOS
   ./maven-stop.bat      # Windows
   
   # Manual stop:
   pkill -f "java.*marketplace"
   pkill -f "java.*seller"
   ```

---

## 🧪 Testing the System

### Run Integration Tests
```bash
# With Docker (after system is running)
docker exec -it marketplace-alpha java -jar target/integration-test-jar-with-dependencies.jar

# With Maven (after building)
mvn exec:java -Dexec.mainClass="IntegrationTest"
```

### Health Check
```bash
# With Docker (after system is running)
docker exec -it marketplace-alpha java -jar target/health-check-jar-with-dependencies.jar

# With Maven
mvn exec:java -Phealth-check
```

---

## 📊 What You Should See

### Seller Services
Each seller shows:
```
✅ Seller listening on port 5555
📦 Inventory: laptop=50, smartphone=30, tablet=20
🔄 Processing SAGA transactions...
```

### Marketplace Services  
Marketplaces show:
```
🎯 Starting SAGA transaction for order: abc123
✅ Seller tcp://seller1:5555 CONFIRMED reservation
🎉 Order CONFIRMED by all sellers. Sending COMMIT...
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
- CI/CD pipeline with Maven profiles
- System monitoring and health checks
- Integration testing framework
- Process coordination scripts
- Build automation and deployment

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
