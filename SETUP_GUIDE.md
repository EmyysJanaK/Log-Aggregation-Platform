# Log Aggregation Platform - Setup & Usage Guide

## 🚀 Quick Setup Guide

### Prerequisites
Before running the Log Aggregation Platform, ensure you have the following installed:

- **Java 17+** (OpenJDK or Oracle JDK)
- **Apache Maven 3.6+**
- **Apache Kafka 2.8+**
- **Elasticsearch 8.x**

### Infrastructure Setup

#### 1. Start Elasticsearch
```bash
# Download and start Elasticsearch
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.13.2-linux-x86_64.tar.gz
tar -xzf elasticsearch-8.13.2-linux-x86_64.tar.gz
cd elasticsearch-8.13.2/
./bin/elasticsearch
```

#### 2. Start Apache Kafka
```bash
# Download and start Kafka
wget https://downloads.apache.org/kafka/2.13-3.6.1/kafka_2.13-3.6.1.tgz
tar -xzf kafka_2.13-3.6.1.tgz
cd kafka_2.13-3.6.1/

# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka (in another terminal)
bin/kafka-server-start.sh config/server.properties

# Create required topics
bin/kafka-topics.sh --create --topic raw-logs --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic processed-logs --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### Application Setup

#### 1. Build the Project
```bash
git clone https://github.com/EmyysJanaK/Log-Aggregation-Platform.git
cd Log-Aggregation-Platform
mvn clean package -DskipTests
```

#### 2. Start Components (in order)

**Terminal 1 - Log Receiver**
```bash
cd log-receiver
mvn spring-boot:run
# Runs on http://localhost:8082
```

**Terminal 2 - Log Processor**
```bash
cd log-processor
mvn spring-boot:run
# Runs on http://localhost:8083
```

**Terminal 3 - Log Agent**
```bash
cd log-agent
mvn spring-boot:run
# Runs on http://localhost:8081
```

**Terminal 4 - Dashboard**
```bash
cd log-dashboard
mvn spring-boot:run
# Runs on http://localhost:8080
```

## 📊 Usage Examples

### 1. Monitoring Agent Status
```bash
# Check agent health
curl http://localhost:8081/api/agent/health

# Get agent status
curl http://localhost:8081/api/agent/status

# Get performance metrics
curl http://localhost:8081/api/agent/metrics
```

### 2. Sending Logs via API
```bash
# Send a log entry directly to the receiver
curl -X POST http://localhost:8082/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "my-app",
    "level": "ERROR",
    "message": "Database connection failed",
    "hostname": "web-server-01",
    "application": "user-service"
  }'
```

### 3. Searching Logs via Dashboard API
```bash
# Get recent logs
curl "http://localhost:8080/dashboard/api/logs?page=0&size=10"

# Search logs by level
curl "http://localhost:8080/dashboard/search" \
  -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "level=ERROR&page=0&size=20"

# Get log statistics
curl "http://localhost:8080/dashboard/api/stats"
```

### 4. File-based Log Collection
Create log files in monitored directories:

```bash
# Create a logs directory
mkdir -p ./logs

# Generate some sample logs
echo "$(date) INFO [main] Application started successfully" >> ./logs/app.log
echo "$(date) ERROR [db-pool] Connection timeout after 30s" >> ./logs/app.log
echo "$(date) WARN [security] Failed login attempt from 192.168.1.100" >> ./logs/app.log
```

The Log Agent will automatically detect and process these files.

## 🔧 Configuration Examples

### Log Agent Configuration
Create `log-agent/src/main/resources/application.properties`:

```properties
# Agent Identity
log.agent.agent-id=production-agent-01
log.agent.hostname=prod-server-01

# File Monitoring
log.agent.watch-directories=/var/log/myapp,/opt/applications/logs
log.agent.file-patterns=*.log,*.out,*.err
log.agent.scan-interval-seconds=10
log.agent.enable-file-watcher=true

# Kafka Configuration
spring.kafka.producer.bootstrap-servers=kafka-cluster:9092
log.kafka.topic.name=application-logs

# Performance Settings
log.agent.batch-size=500
log.agent.max-file-size-bytes=52428800  # 50MB
log.agent.max-retries=5
```

### Dashboard Configuration
Create `log-dashboard/src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=80
spring.application.name=log-dashboard

# Elasticsearch Configuration
spring.elasticsearch.uris=http://elasticsearch-cluster:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=changeme

# Pagination Settings
logging.dashboard.default-page-size=50
logging.dashboard.max-page-size=1000
```

### Log Processor Configuration
Update `log-processor/src/main/resources/application.properties`:

```properties
# Kafka Consumer Settings
spring.kafka.consumer.group-id=log-processor-cluster
spring.kafka.consumer.max-poll-records=1000
spring.kafka.consumer.fetch-max-wait=5000

# Processing Rules
log.processor.error-code-pattern=ERROR_\\d{4}
log.processor.enable-filtering=true
log.processor.min-log-level=DEBUG
```

## 🌐 Web Dashboard Usage

### Accessing the Dashboard
1. Open your browser and navigate to `http://localhost:8080`
2. You'll see the main dashboard with recent logs and statistics

### Dashboard Features

#### 1. Log Search
- **Full-text search**: Search across all log messages
- **Filter by level**: ERROR, WARN, INFO, DEBUG
- **Filter by source**: Application name or log source
- **Time range filtering**: Select date/time ranges
- **Pagination**: Navigate through large result sets

#### 2. Statistics View
- **Total log count**: Overall number of logs in the system
- **Error percentage**: Ratio of error logs to total logs
- **Warning percentage**: Ratio of warning logs to total logs
- **Real-time updates**: Statistics update as new logs arrive

#### 3. Log Details
- Click on any log entry to see detailed information
- View metadata, tags, and processing information
- See original message vs processed message

## 🔍 Monitoring and Troubleshooting

### Health Checks
Each component provides health check endpoints:

```bash
# Check all components
curl http://localhost:8081/api/agent/health
curl http://localhost:8082/actuator/health  # If Spring Actuator is enabled
curl http://localhost:8083/actuator/health
curl http://localhost:8080/actuator/health
```

### Log Levels
Adjust logging levels for debugging:

```properties
# In application.properties
logging.level.com.logaggregator=DEBUG
logging.level.org.springframework.kafka=INFO
logging.level.org.elasticsearch.client=WARN
```

### Common Issues

#### 1. Kafka Connection Issues
```bash
# Verify Kafka is running and topics exist
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

#### 2. Elasticsearch Connection Issues
```bash
# Check Elasticsearch status
curl http://localhost:9200/_cluster/health
```

#### 3. File Permission Issues
```bash
# Ensure log directories are readable
chmod -R 755 /path/to/log/directories
```

## 📊 Performance Tuning

### Kafka Optimization
```properties
# High-throughput settings
spring.kafka.producer.batch-size=65536
spring.kafka.producer.linger-ms=50
spring.kafka.producer.compression-type=snappy
spring.kafka.consumer.max-poll-records=2000
```

### Elasticsearch Optimization
```properties
# Bulk indexing settings
spring.elasticsearch.bulk.actions=1000
spring.elasticsearch.bulk.size=5MB
spring.elasticsearch.bulk.flush-interval=10s
```

### JVM Tuning
```bash
# Set JVM options for better performance
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
mvn spring-boot:run
```

## 🔒 Security Considerations

### Elasticsearch Security
```properties
# Enable Elasticsearch authentication
spring.elasticsearch.username=loguser
spring.elasticsearch.password=securepassword
spring.elasticsearch.ssl.enabled=true
```

### Kafka Security
```properties
# Enable SASL authentication
spring.kafka.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="password";
```

## 📈 Scaling the Platform

### Horizontal Scaling
1. **Multiple Log Agents**: Deploy agents on different servers
2. **Kafka Partitioning**: Increase partitions for parallel processing
3. **Elasticsearch Clustering**: Set up Elasticsearch cluster
4. **Load Balancing**: Use reverse proxy for dashboard

### Vertical Scaling
1. **Increase JVM heap size**: Allocate more memory
2. **Optimize batch sizes**: Tune Kafka batch settings
3. **Storage optimization**: Use SSD storage for Elasticsearch

This guide should help you get started with the Log Aggregation Platform and customize it for your specific needs!