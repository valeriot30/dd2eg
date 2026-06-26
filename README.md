# Dev2Enterprise 🚀
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-6.x+-green.svg?style=flat-square&logo=mongodb)](https://www.mongodb.com/)
[![Neo4j](https://img.shields.io/badge/Neo4j-5.x+-blue.svg?style=flat-square&logo=neo4j)](https://neo4j.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](LICENSE)
**Dev2Enterprise** is an innovative micro-sponsorship platform designed to bridge the gap between open-source developers and corporate sponsors. The system enables enterprises to fund specific tasks, bug fixes, or features in open-source projects via targeted bounties. Developers contribute code via targeted commits, collaborate with the community, and earn rewards while building their reputations.
Dev2Enterprise is powered by a high-performance **polyglot database architecture** using **MongoDB** (as the system of record) and **Neo4j** (as the graph analytical engine) to deliver real-time developer recommendations and robust cross-enterprise fraud detection.
---
## 📖 Table of Contents
- [Core Features](#-core-features)
- [System Architecture](#-system-architecture)
    - [Polyglot Persistence](#polyglot-persistence)
    - [Transactional Outbox & Eventual Consistency](#transactional-outbox--eventual-consistency)
    - [Explicit Transaction Control & Routing](#explicit-transaction-control--routing)
- [Data Models & Graph Topology](#-data-models--graph-topology)
    - [MongoDB Collections](#mongodb-collections)
    - [Neo4j Graph Schema](#neo4j-graph-schema)
- [Analytical & Recommendation Queries](#-analytical--recommendation-queries)
    - [Recommendations](#recommendations)
    - [Anomaly & Fraud Detection](#anomaly--fraud-detection)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
    - [Prerequisites](#prerequisites)
    - [Configuration](#configuration)
    - [Installation & Run](#installation--run)
- [Authors & License](#-authors--license)
---
## 🌟 Core Features
- **Micro-Sponsorship Model**: Enterprises directly fund individual tasks with custom budgets.
- **Commit-based Contribution Tracking**: Developers submit commits to claim bounties, which are verified and recorded.
- **Graph-Based Recommendations**: Matches developers to projects based on skill-sets and thematic affinities.
- **Cold-Start Mitigation Engine**: Suggests skills to developers based on task co-requirements.
- **Real-Time Fraud & Anomaly Detection**:
    - **Symmetric Wash Trading**: Detects collusive funding loops between pairs of enterprises.
    - **Shell Projects**: Flags one-person projects created by developers solely to drain corporate funds under complicit enterprises.
- **State-of-the-Art Security**: Stateless JWT authentication, strict Role-Based Access Control (RBAC), and IDOR prevention through secure context identity extraction.
---
## 🏗️ System Architecture
```mermaid
graph TD
    Client[Web/Mobile Client] <--> API[REST API / Spring Boot]
    
    subgraph Storage Layer
        API <-->|Write Path / Read Queries| MongoDB[(MongoDB - Source of Truth)]
        API -.->|Direct Neo4j Java Driver| Neo4j[(Neo4j - Analytics/Graph)]
    end
    subgraph Synchronization (Eventual Consistency)
        MongoDB -->|Outbox Pattern| Events[events collection]
        Scheduler[Scheduled Worker 30s] -->|Polls PENDING| Events
        Scheduler -->|Idempotent MERGE Writes| Neo4j
        Scheduler -->|Failure / Max Retries| DLQ[Dead Letter Queue / FAILED]
    end
```
### Polyglot Persistence
- **MongoDB** is the primary system of record, storing rich documents (user profiles, project metadata, task details, and granular commits). It utilizes MongoDB's Aggregation Framework to compute real-time statistics (e.g., top contributors, enterprise budget charts).
- **Neo4j** serves exclusively as the analytical graph engine, storing a minimal, index-free adjacency representation of the network (IDs and status flags only). This separation of concerns avoids data duplication while maximizing query speeds for multi-hop graph traversals.
### Transactional Outbox & Eventual Consistency
To maintain consistency between MongoDB and Neo4j without resorting to slow two-phase commits:
1. **Synchronous Write Path**: Business operations write to MongoDB and append an `Event` document (e.g., `ADD_PROJECT`, `FUNDING`) to the `events` collection inside the same local database transaction.
2. **Asynchronous Read/Sync Path**: A scheduled worker (`GraphSyncService`) polls pending events, executes idempotent `MERGE` statements on Neo4j using the **Neo4j Java Driver**, and updates the status to `COMPLETED`.
3. **Fault Tolerance**: Failed sync events are retried up to 5 times. If they continue to fail, they are moved to a dead-letter queue (status `FAILED`) for manual administrative intervention via dedicated endpoints.
### Explicit Transaction Control & Routing
Rather than using the repository abstraction of Spring Data Neo4j (SDN), the application directly uses the **Neo4j Java Driver** (`org.neo4j.driver`) for maximum flexibility:
- **CQRS-aligned Routing**: Uses `session.executeRead()` to automatically route recommendation queries to read-replicas, offloading analytical workloads from the main leader node. Writes are directed to the leader via `session.executeWrite()`.
- **Cypher Injection Prevention**: All queries pass parameters securely via `Map.of("paramName", value)` to protect the database against manipulation.
---
## 🗄️ Data Models & Graph Topology
### MongoDB Collections
- **`users`**: Email, username, hashed password, userType (`DEVELOPER` or `ENTERPRISE`), skills, and role-specific nested details.
- **`projects`**: Name, description, budget, creatorId, tags, and contributors.
- **`tasks`**: Title, description, status (`OPEN`, `IN_PROGRESS`, `COMPLETED`), priority, budget, requiredSkills, and comments.
- **`commits`**: Git hash, commit message, lines changed, author, and associated taskId.
- **`events`**: Transactional outbox event records including type, payload, status (`PENDING`, `COMPLETED`, `FAILED`), and retry metrics.
### Neo4j Graph Schema
#### Node Labels & Properties
|
Label
|
Properties
|
Description
|
|
:---
|
:---
|
:---
|
|
`:Developer`
|
`id`
(String)
|
Maps to MongoDB User (
`userType = DEVELOPER`
)
|
|
`:Enterprise`
|
`id`
(String)
|
Maps to MongoDB User (
`userType = ENTERPRISE`
)
|
|
`:Project`
|
`id`
(String),
`status`
(String)
|
Shared project node; replicates status for fast Cypher filters
|
|
`:Task`
|
`id`
(String),
`status`
(String)
|
Unique task node; replicates status
|
|
`:Skill`
|
`name`
(String)
|
Shared skill nodes (e.g., "Java", "Docker")
|
|
`:Tag`
|
`name`
(String)
|
Shared categorizations (e.g., "web", "AI")
|
#### Relationship Types & Topology
```
(:Developer)-[:HAS_SKILL]->(:Skill)<-[:REQUIRES_SKILL]-(:Task)
(:Developer)-[:WORK_ON]->(:Task)-[:BELONGS_TO]->(:Project)
(:Developer | :Enterprise)-[:CREATED]->(:Project)-[:CATEGORIZED_BY]->(:Tag)
(:Enterprise)-[:FINANCED]->(:Task)-[:BELONGS_TO]->(:Project)
```
---
## ⚡ Analytical & Recommendation Queries
The application leverages Cypher's pattern matching to run high-value business intelligence.
### Recommendations
#### 1. Project Recommendations for Developers (Q1)
Suggests open projects based on the developer's work history (tags of projects they previously worked on) and matching skill requirements.
```cypher
MATCH (dev:Developer {id: $devId})-[:WORK_ON]->(:Task)-[:BELONGS_TO]->(:Project)-[:CATEGORIZED_BY]->(tag:Tag)
MATCH (tag)<-[:CATEGORIZED_BY]-(recProj:Project {status: 'open'})<-[:BELONGS_TO]-(task:Task {status: 'open'})
WHERE NOT (dev)-[:WORK_ON]->(task) AND NOT (dev)-[:CREATED]->(recProj)
MATCH (dev)-[:HAS_SKILL]->(skill:Skill)<-[:REQUIRES_SKILL]-(task)
RETURN recProj.id AS RecommendedProject,
       count(DISTINCT tag) AS SharedTagCount,
       count(DISTINCT task) AS OpenMatchingTasks,
       collect(DISTINCT skill.name) AS MatchingSkills
ORDER BY SharedTagCount DESC, OpenMatchingTasks DESC
LIMIT 10
```
#### 2. Skill Recommendations / Cold Start Mitigation (Q2)
Suggests skills a developer should learn, analyzing the co-requirements of tasks related to their current skills. Uses `CALL` subqueries to prevent graph traversal fan-out.
```cypher
MATCH (dev:Developer {id: $devId})-[:HAS_SKILL]->(knownSkill:Skill)
CALL (knownSkill) {
    MATCH (knownSkill)<-[:REQUIRES_SKILL]-(t:Task {status: 'open'})
    ORDER BY t.created_at DESC
    RETURN t LIMIT 100
}
MATCH (t)-[:REQUIRES_SKILL]->(recommended:Skill)
WHERE NOT (dev)-[:HAS_SKILL]->(recommended)
RETURN recommended.name AS RecommendedSkill, count(DISTINCT t) AS Frequency
ORDER BY Frequency DESC
LIMIT 5
```
#### 3. Task Ranking within a Project (Q3)
Ranks open tasks within a target project based on how many skills the developer matches. Using `OPTIONAL MATCH` ensures tasks with zero matching skills are still visible (scored 0).
```cypher
MATCH (proj:Project {id: $projId})<-[:BELONGS_TO]-(task:Task {status: 'open'})
MATCH (dev:Developer {id: $devId})
OPTIONAL MATCH (task)-[:REQUIRES_SKILL]->(reqSkill:Skill)<-[:HAS_SKILL]-(dev)
RETURN task.id AS Task,
       task.priority AS Priority,
       collect(reqSkill.name) AS MatchedSkills,
       count(reqSkill) AS MatchScore
ORDER BY MatchScore DESC, Priority DESC
```
#### 4. Enterprise Financing Recommendations (Q4)
Recommends projects to an enterprise based on their past project investment tags, prioritizing portfolio diversification (excluding projects already funded).
```cypher
MATCH (ent:Enterprise {id: $entId})-[:FINANCED]->(:Task)-[:BELONGS_TO]->(:Project)-[:CATEGORIZED_BY]->(tag:Tag)
MATCH (tag)<-[:CATEGORIZED_BY]-(recProj:Project {status: 'open'})
WHERE NOT (ent)-[:FINANCED]->(:Task)-[:BELONGS_TO]->(recProj)
OPTIONAL MATCH (recProj)<-[:BELONGS_TO]-(openTask:Task {status: 'open'})
RETURN recProj.id AS RecommendedProject,
       count(DISTINCT tag) AS ShareTagCount,
       collect(DISTINCT tag.name) AS MatchingTags,
       count(DISTINCT openTask) AS AvailableTask
ORDER BY ShareTagCount DESC, AvailableTask DESC
LIMIT 10
```
---
### Anomaly & Fraud Detection
#### 5. Cross-Enterprise Wash Trading (Q5 / Q6)
Detects collusion where Enterprise A funds projects created by Enterprise B, and Enterprise B simultaneously funds projects created by Enterprise A.
```cypher
MATCH (e1:Enterprise)-[:FINANCED]->(t1:Task)-[:BELONGS_TO]->(p1:Project)<-[:CREATED]-(e2:Enterprise)
MATCH (e2)-[:FINANCED]->(t2:Task)-[:BELONGS_TO]->(p2:Project)<-[:CREATED]-(e1)
WHERE e1.id < e2.id  // Lexicographical ordering prevents duplicate pairs
RETURN e1.id AS EnterpriseA, count(DISTINCT t1) AS TasksFinancedByA_in_B,
       e2.id AS EnterpriseB, count(DISTINCT t2) AS TasksFinancedByB_in_A
ORDER BY TasksFinancedByA_in_B + TasksFinancedByB_in_A DESC
```
#### 6. Developer-Enterprise Shell Projects (Q7 / Q8)
Identifies situations where a developer sets up a project and task, receives funding from a complicit enterprise, and completes it themself with zero collaboration from external developers.
```cypher
MATCH (ent:Enterprise)-[:FINANCED]->(t:Task)<-[:WORK_ON]-(dev:Developer)
MATCH (dev)-[:CREATED]->(p:Project)<-[:BELONGS_TO]-(t)
WHERE NOT EXISTS {
    MATCH (otherDev:Developer)-[:WORK_ON]->(:Task)-[:BELONGS_TO]->(p)
    WHERE otherDev <> dev
}
RETURN dev.id AS FraudsterDeveloper,
       ent.id AS ComplicitEnterprise,
       p.id AS ShellProject,
       count(DISTINCT t) AS FakeTasksCompleted,
       collect(t.id) AS CompromisedTaskIds
ORDER BY FakeTasksCompleted DESC
```
---
## 🔌 API Endpoints
### Authentication & Profiles
* `POST /api/auth/register` - Create a new Developer or Enterprise account.
* `POST /api/auth/login` - Authenticate credentials and receive stateless JWT token.
* `GET /api/user/profile` - Retrieve personal profile information and statistics.
* `PUT /api/user/skills` - Update developer's technical skill-set registry.
### Project & Task Operations
* `GET /api/projects` - Browse all projects (supports full-text search filters via MongoDB `TextCriteria`).
* `POST /api/projects` - Create a new project (Enterprise or Developer).
* `GET /api/projects/{projectId}/tasks` - List tasks associated with a specific project.
* `POST /api/tasks/{taskId}/funding` - Fund a specific task (Enterprise only).
* `POST /api/tasks/{taskId}/commits` - Submit a commit contribution to resolve a task (Developer only).
### Graph Recommendations
* `GET /api/recommendations/projects` - Get personalized project recommendations (Q1).
* `GET /api/recommendations/skills` - Get recommended skills to acquire (Q2).
* `GET /api/recommendations/tasks?projId={id}` - Rank tasks in a project for the developer (Q3).
* `GET /api/recommendations/financing` - Get tailored investment opportunities (Q4) (Enterprise only).
### Admin Diagnostics & Recoveries
* `GET /api/admin/anomaly/scan-all` - Run batch global wash-trading and shell-project detection (Q5 + Q7).
* `GET /api/admin/anomaly/scan/{enterpriseId}` - Targeted real-time anomaly sweep (Q6 + Q8).
* `GET /api/admin/sync/stats` - Display statistics on Outbox synchronization statuses.
* `POST /api/admin/sync/retry-all` - Manually trigger reprocessing for all `FAILED` outbox events.
---
## ⚙️ Getting Started
### Prerequisites
- **Java JDK**: Version 17 or higher
- **Maven**: Version 3.8+
- **MongoDB**: Version 6.0+ (running locally on port `27017` or configured via URI)
- **Neo4j Database**: Version 5.0+ (with BOLT protocol enabled, running on port `7687`)
### Configuration
Configure database connection settings in `src/main/resources/application.properties`:
```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/dev2enterprise
# Neo4j Driver Connection Configuration
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=your_secure_password
# Spring Boot Bean Overrides
spring.main.allow-bean-definition-overriding=true
```
### Installation & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/valeriot30/dd2eg.git
   cd dd2eg
   ```
2. Compile and package the application:
   ```bash
   mvn clean package
   ```
3. Launch the Spring Boot server:
   ```bash
   mvn spring-boot:run
   ```
4. Initialize Recommended Database Constraints (Neo4j Console / Cypher Shell):
   ```cypher
   CREATE CONSTRAINT FOR (d:Developer) REQUIRE d.id IS UNIQUE;
   CREATE CONSTRAINT FOR (e:Enterprise) REQUIRE e.id IS UNIQUE;
   CREATE CONSTRAINT FOR (p:Project) REQUIRE p.id IS UNIQUE;
   CREATE CONSTRAINT FOR (t:Task) REQUIRE t.id IS UNIQUE;
   CREATE CONSTRAINT FOR (s:Skill) REQUIRE s.name IS UNIQUE;
   CREATE CONSTRAINT FOR (tag:Tag) REQUIRE tag.name IS UNIQUE;
   CREATE INDEX FOR (t:Task) ON (t.status);
   CREATE INDEX FOR (p:Project) ON (p.status);
   ```
---
## 👥 Authors & License
Developed as part of the **Large Scale and Multi-Structured Databases** curriculum at **Università di Pisa**, Master's Degree in Computer Engineering (June 2026).
- **Valerio Triolo**
- **Silvia Festa**
- **Savino Gorgoglione**


This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.