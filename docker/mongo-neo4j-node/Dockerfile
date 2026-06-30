FROM mongo:7.0

USER root

# Install dependencies, Java 17, and Neo4j
RUN apt-get update && \
    apt-get install -y gnupg curl procps openjdk-17-jre-headless && \
    mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://debian.neo4j.com/neotechnology.gpg.key | gpg --dearmor -o /etc/apt/keyrings/neotechnology.gpg && \
    echo 'deb [signed-by=/etc/apt/keyrings/neotechnology.gpg] https://debian.neo4j.com stable 5' | tee -a /etc/apt/sources.list.d/neo4j.list && \
    apt-get update && \
    apt-get install -y neo4j && \
    rm -rf /var/lib/apt/lists/*

# Fix Neo4j configuration to listen on all interfaces
RUN sed -i 's/#server.default_listen_address=0.0.0.0/server.default_listen_address=0.0.0.0/g' /etc/neo4j/neo4j.conf || true

# Create log directory and data directories for mongo
RUN mkdir -p /var/log/mongodb /data/configdb /data/db && \
    chown -R mongodb:mongodb /var/log/mongodb /data/configdb /data/db

# Copy start script
COPY scripts/start-services-neo4j.sh /usr/local/bin/start-services-neo4j.sh
RUN chmod +x /usr/local/bin/start-services-neo4j.sh

# Expose Neo4j ports and Mongo ports
EXPOSE 7474 7687 27017 27018 27019

CMD ["/usr/local/bin/start-services-neo4j.sh"]
