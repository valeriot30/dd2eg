#!/bin/bash
set -e

# Create directories
mkdir -p /data/configdb /data/db
chown -R mongodb:mongodb /data/configdb /data/db

# Start Config Server Replica Set node
echo "Starting Config Server..."
su mongodb -s /bin/bash -c "mongod --configsvr --replSet configRS --bind_ip_all --port 27019 --dbpath /data/configdb --fork --logpath /var/log/mongodb/configsvr.log"

# Start Shard Server Replica Set node
echo "Starting Shard Server..."
su mongodb -s /bin/bash -c "mongod --shardsvr --replSet shardRS --bind_ip_all --port 27018 --dbpath /data/db --fork --logpath /var/log/mongodb/shardsvr.log"

# Wait for config server to be up before starting mongos
sleep 5

# Start Mongos Router
echo "Starting Mongos Router..."
su mongodb -s /bin/bash -c "mongos --configdb configRS/node1:27019,node2:27019,node3:27019 --bind_ip_all --port 27017 --fork --logpath /var/log/mongodb/mongos.log"

# Keep container running
tail -f /var/log/mongodb/mongos.log /var/log/mongodb/shardsvr.log /var/log/mongodb/configsvr.log
