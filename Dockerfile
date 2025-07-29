# Simplified Dockerfile with better debugging
FROM openjdk:17-jdk-slim

WORKDIR /app

# Install Maven and debugging tools
RUN apt-get update && apt-get install -y \
    maven \
    netcat-traditional \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Verify JAR files were created
RUN ls -la target/

# Copy configuration files
COPY src/main/resources/*.yaml ./config/

# Create startup script directly in Dockerfile (more reliable)
RUN mkdir -p ./scripts && \
    echo '#!/bin/bash' > ./scripts/start-component.sh && \
    echo 'set -e' >> ./scripts/start-component.sh && \
    echo 'echo "Container starting with COMPONENT_TYPE: $COMPONENT_TYPE"' >> ./scripts/start-component.sh && \
    echo 'echo "Available files:"' >> ./scripts/start-component.sh && \
    echo 'ls -la /app/' >> ./scripts/start-component.sh && \
    echo 'echo "Available JARs:"' >> ./scripts/start-component.sh && \
    echo 'ls -la /app/target/' >> ./scripts/start-component.sh && \
    echo '' >> ./scripts/start-component.sh && \
    echo 'case "$COMPONENT_TYPE" in' >> ./scripts/start-component.sh && \
    echo '    "seller")' >> ./scripts/start-component.sh && \
    echo '        echo "Starting Seller Process..."' >> ./scripts/start-component.sh && \
    echo '        echo "Endpoint: $SELLER_ENDPOINT"' >> ./scripts/start-component.sh && \
    echo '        echo "Config: $CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        exec java -jar target/seller-jar-with-dependencies.jar "$SELLER_ENDPOINT" "$CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        ;;' >> ./scripts/start-component.sh && \
    echo '    "marketplace")' >> ./scripts/start-component.sh && \
    echo '        echo "Starting Marketplace Process..."' >> ./scripts/start-component.sh && \
    echo '        echo "Config: $CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        exec java -jar target/marketplace.jar "$CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        ;;' >> ./scripts/start-component.sh && \
    echo '    *)' >> ./scripts/start-component.sh && \
    echo '        echo "Unknown component type: $COMPONENT_TYPE"' >> ./scripts/start-component.sh && \
    echo '        echo "Available types: seller, marketplace"' >> ./scripts/start-component.sh && \
    echo '        echo "Starting debug shell..."' >> ./scripts/start-component.sh && \
    echo '        exec /bin/bash' >> ./scripts/start-component.sh && \
    echo '        ;;' >> ./scripts/start-component.sh && \
    echo 'esac' >> ./scripts/start-component.sh && \
    chmod +x ./scripts/start-component.sh

# Expose ports for sellers and marketplaces
EXPOSE 5555 5556 5557 5558 5559 7777 7778

# Default command with debug output
CMD ["sh", "-c", "echo 'Container starting...' && echo 'COMPONENT_TYPE=' $COMPONENT_TYPE && ./scripts/start-component.sh"]