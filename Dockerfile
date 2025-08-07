# Dockerfile - Encoding Fix hinzufügen
FROM openjdk:17-jdk-slim

WORKDIR /app

# Install Maven and debugging tools + LOCALE FIX
RUN apt-get update && apt-get install -y \
    maven \
    netcat-traditional \
    curl \
    locales \
    && rm -rf /var/lib/apt/lists/*

# Configure UTF-8 locale
RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen

# Set environment variables for UTF-8
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en  
ENV LC_ALL=en_US.UTF-8
ENV JAVA_OPTS="-Dfile.encoding=UTF-8"

# Copy source code
COPY pom.xml .
COPY src ./src

# Build the application with UTF-8 encoding
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Rest bleibt gleich...
RUN ls -la target/
COPY src/main/resources/*.yaml ./config/

# Create startup script (unchanged)
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
    echo '        exec java $JAVA_OPTS -jar target/seller-jar-with-dependencies.jar "$SELLER_ENDPOINT" "$CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        ;;' >> ./scripts/start-component.sh && \
    echo '    "marketplace")' >> ./scripts/start-component.sh && \
    echo '        echo "Starting Marketplace Process..."' >> ./scripts/start-component.sh && \
    echo '        echo "Config: $CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        exec java $JAVA_OPTS -jar target/marketplace.jar "$CONFIG_FILE"' >> ./scripts/start-component.sh && \
    echo '        ;;' >> ./scripts/start-component.sh && \
    echo '    *)' >> ./scripts/start-component.sh && \
    echo '        echo "Unknown component type: $COMPONENT_TYPE"' >> ./scripts/start-component.sh && \
    echo '        echo "Available types: seller, marketplace"' >> ./scripts/start-component.sh && \
    echo '        echo "Starting debug shell..."' >> ./scripts/start-component.sh && \
    echo '        exec /bin/bash' >> ./scripts/start-component.sh && \
    echo '        ;;' >> ./scripts/start-component.sh && \
    echo 'esac' >> ./scripts/start-component.sh && \
    chmod +x ./scripts/start-component.sh

EXPOSE 5555 5556 5557 5558 5559 7777 7778

CMD ["sh", "-c", "echo 'Container starting...' && echo 'COMPONENT_TYPE=' $COMPONENT_TYPE && ./scripts/start-component.sh"]