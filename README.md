# Distributed Marketplace - Multi-Process Setup

## Quick Start Guide

This guide helps you run the distributed marketplace system with multiple processes.

### Prerequisites
- Java installed on your system
- ZeroMQ library (jeromq-0.5.2.jar) in the lib/ folder

### Running the System

#### Option 1: Start Everything at Once
```bash
# Run this to start all processes automatically
start_all.bat
```

#### Option 2: Start Components Manually

1. **Start Sellers First:**
   ```bash
   start_sellers.bat
   ```
   This starts 5 sellers on ports 5555-5559.

2. **Start Marketplaces:**
   ```bash
   start_marketplaces.bat
   ```
   This starts 2 marketplaces on ports 7777-7778.

#### Option 3: Start Individual Processes

**Start a single seller:**
```bash
java -cp "lib/*;src" SellerProcess tcp://localhost:5555
```

**Start a single marketplace:**
```bash
java -cp "lib/*;src" MarketplaceProcess 7777
```

### Testing the System

1. **Start all processes** using one of the methods above
2. **Run the integration test:**
   ```bash
   java -cp "lib/*;src" IntegrationTest
   ```

### What You Should See

- **Seller windows:** Each seller will show "Seller online at tcp://localhost:XXXX"
- **Marketplace windows:** Each marketplace will place orders and show responses
- **Integration test:** Will test various scenarios and show results

### Troubleshooting

- **"Address already in use"**: Make sure no other processes are using the same ports
- **"Connection refused"**: Make sure sellers are started before marketplaces
- **No response**: Check if all processes are still running

### Process Architecture

```
Marketplace 1 (port 7777) ──┐
                             ├─→ Seller 1 (port 5555)
Marketplace 2 (port 7778) ──┤   Seller 2 (port 5556)
                             │   Seller 3 (port 5557)
Integration Test ────────────┤   Seller 4 (port 5558)
                             └─→ Seller 5 (port 5559)
```

### Next Steps for Development

1. **Yana** will enhance MarketplaceProcess.java with SAGA coordination
2. **Noah** will enhance SellerProcess.java with inventory management
3. **Toni** will add Docker support, monitoring, and more tests

---

## Files Created by Integration Team (Toni)

- `MarketplaceProcess.java` - Main class for marketplace processes
- `SellerProcess.java` - Main class for seller processes
- `start_sellers.bat` - Script to start all sellers
- `start_marketplaces.bat` - Script to start all marketplaces
- `start_all.bat` - Script to start entire system
- `IntegrationTest.java` - Basic integration test
- `README.md` - This file
