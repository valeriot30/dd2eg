FROM mongo:7.0

# Install dependencies if needed
RUN apt-get update && apt-get install -y procps && rm -rf /var/lib/apt/lists/*

# Create log directory
RUN mkdir -p /var/log/mongodb && chown -R mongodb:mongodb /var/log/mongodb

COPY scripts/start-services.sh /usr/local/bin/start-services.sh
RUN chmod +x /usr/local/bin/start-services.sh

CMD ["/usr/local/bin/start-services.sh"]
