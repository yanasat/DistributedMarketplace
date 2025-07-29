# Distributed Marketplace System

A sophisticated distributed marketplace implementation featuring **SAGA pattern** transactions, **Docker containerization**, and **fault-tolerant microservices**.

## Quick Start with Docker

### Prerequisites
- Docker & Docker Compose installed
- Java 17+ (for local development)

### Running with Docker (Recommended)

#### Start the Complete System
```bash
docker-compose up --build
```

#### Start Individual Components
```bash
# Start only sellers
docker-compose up seller1 seller2 seller3 seller4 seller5

# Start marketplaces (requires sellers to be running)
docker-compose up marketplace1 marketplace2
```

#### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f marketplace1
```

#### Stop the System
```bash
docker-compose down
```

### Alternative: Maven Build & Run

#### Build JARs
```bash
./maven-build.sh
# or on Windows:
maven-build.bat
```

#### Start with Scripts
```bash
# Start all components
./docker-start.sh

# Or start individually
./start-jars.sh
```

## Testing the System

### Run Integration Tests
```bash
# With Docker
docker run --network marketplace-network distributed-marketplace java -jar integration-test-jar-with-dependencies.jar

# With Maven
mvn exec:java -Dexec.mainClass="IntegrationTest"
```

### Health Check
```bash
# Check system health
docker run --network marketplace-network distributed-marketplace java -jar health-check-jar-with-dependencies.jar

# Or with Maven
mvn exec:java -Phealth-check
```

## System Monitoring

### View Container Status
```bash
docker ps
```

### Monitor Resource Usage
```bash
docker stats
```

### Access Container Logs
```bash
# Real-time logs
docker-compose logs -f marketplace1

# Last 100 lines
docker-compose logs --tail=100 seller1
```

## What You Should See

### Seller Services
Each seller container will display:
```
✅ Seller listening on port 5555
📦 Inventory: laptop=50, smartphone=30, tablet=20
🔄 Processing SAGA transactions...
```

### Marketplace Services  
Marketplace containers will show:
```
Starting SAGA transaction for order: abc123
Seller tcp://seller1:5555 CONFIRMED reservation
Order CONFIRMED by all sellers. Sending COMMIT...
```

### Integration Tests
Test output will display:
```
=== Integration Test: Multi-Process Communication ===
--- Test 1: Single Order ---
✅ Order processed successfully
--- Test 2: Concurrent Orders ---
✅ All concurrent orders handled correctly
```

## Troubleshooting

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
┌─────────────────────────────────────────────────────────┐
│                Docker Network                           │
│  ┌─────────────┐    ┌─────────────┐                    │
│  │Marketplace 1│◄──►│Marketplace 2│                    │
│  │ (port 7777) │    │ (port 7778) │                    │
│  └─────────────┘    └─────────────┘                    │
│         │                   │                          │
│         ▼                   ▼                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Seller Network                     │   │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │   │
│  │  │Seller 1│ │Seller 2│ │Seller 3│ │Seller 4│   │   │
│  │  │  :5555 │ │  :5556 │ │  :5557 │ │  :5558 │   │   │
│  │  └────────┘ └────────┘ └────────┘ └────────┘   │   │
│  │                        ┌────────┐              │   │
│  │                        │Seller 5│              │   │
│  │                        │  :5559 │              │   │
│  │                        └────────┘              │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
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

### **Toni** - Integration & Testing
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
