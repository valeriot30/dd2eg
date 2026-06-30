#!/bin/bash

# Initialize Config Server Replica Set
echo "Initializing Config Server Replica Set..."
docker exec node1 mongosh --port 27019 --eval '
rs.initiate({
  _id: "configRS",
  configsvr: true,
  members: [
    { _id: 0, host: "node1:27019" },
    { _id: 1, host: "node2:27019" },
    { _id: 2, host: "node3:27019" }
  ]
})
'

echo "Waiting for Config Server to elect a primary..."
sleep 15

# Initialize Shard Replica Set
echo "Initializing Shard Server Replica Set..."
docker exec node1 mongosh --port 27018 --eval '
rs.initiate({
  _id: "shardRS",
  members: [
    { _id: 0, host: "node1:27018", priority: 3 },
    { _id: 1, host: "node2:27018", priority: 2 },
    { _id: 2, host: "node3:27018", priority: 1 }
  ]
})
'

echo "Waiting for Shard Server to elect a primary..."
sleep 15

# Add Shard to the cluster via Mongos
echo "Adding shard to the cluster..."
docker exec node1 mongosh --port 27017 --eval '
sh.addShard("shardRS/node1:27018,node2:27018,node3:27018")
'

echo "Cluster initialization complete. You can verify sharding status by connecting to mongos:"
echo "docker exec -it node1 mongosh --port 27017"
echo "sh.status()"
