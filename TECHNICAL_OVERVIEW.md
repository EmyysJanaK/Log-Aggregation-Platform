# Log Aggregation Platform - Technical Overview

## 🔍 Repository Explanation

This is a **distributed log aggregation platform** built with Java and Spring Boot that provides real-time log collection, processing, and visualization capabilities. The platform follows a microservices architecture designed to handle large volumes of log data from multiple sources.

## 🏗️ Architecture Overview

The platform consists of five core components working together in a distributed manner:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│             │    │             │    │             │    │             │
│ Log Sources │───▶│ Log Agent   │───▶│   Kafka     │───▶│Log Processor│
│             │    │             │    │  (Message   │    │             │
│(Apps, Files)│    │ (Collector) │    │   Queue)    │    │ (Enricher)  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                                                  │
┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │
│             │    │             │    │             │             │
│   Users     │◀───│   Web       │◀───│Elasticsearch│◀────────────┘
│             │    │ Dashboard   │    │  (Storage)  │
│             │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
                          ▲
                          │
                   ┌─────────────┐
                   │             │
                   │Log Receiver │
                   │   (API)     │
                   │             │
                   └─────────────┘
```

## 📦 Components Breakdown

### 1. **Common Module** (`common/`)
**Purpose**: Shared data models and utilities used across all components

**Key Classes**:
- `LogEntry.java` - Core data model representing a log entry
- `LogLevel.java` - Enumeration for log severity levels (DEBUG, INFO, WARN, ERROR, FATAL)
- `LogParser.java`, `LogValidator.java` - Utilities for log processing

### 2. **Log Agent** (`log-agent/`)
**Purpose**: Collects logs from various sources (files, system logs) and sends them to Kafka

**Key Features**:
- File monitoring with configurable patterns
- System log collection
- Health monitoring and metrics
- Kafka integration for message publishing
- RESTful endpoints for monitoring agent status

**Key Classes**:
- `LogAgentApplication.java` - Main Spring Boot application
- `LogAgentService.java` - Core orchestration service
- `FileMonitorService.java` - Monitors log files for changes
- `HealthMonitorService.java` - Tracks system health and performance
- `LogProducerService.java` - Sends logs to Kafka

### 3. **Log Receiver** (`log-receiver/`)
**Purpose**: Receives logs via HTTP API and stores them in Elasticsearch

**Key Features**:
- RESTful API for receiving logs
- Elasticsearch integration for storage
- Kafka consumer for processed logs
- Direct API for external log submissions

**Key Classes**:
- `LogReceiverApplication.java` - Main Spring Boot application
- `LogController.java` - REST endpoints for receiving logs
- `LogRepository.java` - Elasticsearch repository interface
- `LogConsumer.java` - Kafka message consumer

### 4. **Log Processor** (`log-processor/`)
**Purpose**: Processes and enriches raw logs from Kafka before storage

**Key Features**:
- Real-time log processing via Kafka streams
- Log enrichment (error code extraction, severity categorization)
- Message filtering and transformation
- Metadata addition

**Key Classes**:
- `LogProcessorApplication.java` - Main Spring Boot application
- `LogProcessor.java` - Core processing logic with Kafka listeners

### 5. **Log Dashboard** (`log-dashboard/`)
**Purpose**: Web-based UI for searching, viewing, and analyzing logs

**Key Features**:
- Web dashboard with Thymeleaf templates
- Advanced log search and filtering
- Real-time log statistics
- REST API for log retrieval
- Pagination support

**Key Classes**:
- `LogDashboardApplication.java` - Main Spring Boot application
- `DashboardController.java` - Web and API controllers
- `LogSearchService.java` - Business logic for searching logs
- `LogStats.java` - Statistics aggregation model

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.2.5 |
| **Messaging** | Apache Kafka | 3.6.1 |
| **Search & Storage** | Elasticsearch | 8.13.2 |
| **Web Framework** | Spring Web MVC | - |
| **Template Engine** | Thymeleaf | - |
| **Build Tool** | Maven | 3.x |
| **Data Binding** | Jackson | 2.15.4 |

## 🔄 Data Flow

1. **Collection**: Log Agent monitors files/systems and collects log entries
2. **Queuing**: Raw logs are sent to Kafka topic (`raw-logs`)
3. **Processing**: Log Processor consumes from Kafka, enriches logs, and publishes to `processed-logs` topic
4. **Storage**: Log Receiver consumes processed logs and stores them in Elasticsearch
5. **Visualization**: Dashboard queries Elasticsearch and presents data to users

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- Apache Kafka
- Elasticsearch 8.x

### Building the Project
```bash
# Clone the repository
git clone https://github.com/EmyysJanaK/Log-Aggregation-Platform.git
cd Log-Aggregation-Platform

# Build all modules
mvn clean compile

# Package all modules
mvn clean package
```

### Running Individual Components

```bash
# Start Log Agent
cd log-agent
mvn spring-boot:run

# Start Log Receiver  
cd log-receiver
mvn spring-boot:run

# Start Log Processor
cd log-processor  
mvn spring-boot:run

# Start Dashboard
cd log-dashboard
mvn spring-boot:run
```

### Default Ports
- **Log Agent**: 8081 (monitoring endpoints)
- **Log Receiver**: 8082 (log ingestion API)
- **Log Processor**: 8083 (processing service)
- **Log Dashboard**: 8080 (web interface)

## 📊 Key Features

### Log Collection
- **File Monitoring**: Real-time monitoring of log files with pattern matching
- **System Integration**: Collection of system logs and metrics
- **Multiple Sources**: Support for various log sources and formats

### Processing Capabilities
- **Real-time Processing**: Stream processing via Kafka
- **Log Enrichment**: Automatic extraction of error codes and metadata
- **Severity Classification**: Intelligent categorization of log levels
- **Filtering**: Configurable filtering rules

### Search & Analytics
- **Full-text Search**: Powered by Elasticsearch
- **Advanced Filtering**: Filter by level, source, time range, etc.
- **Real-time Statistics**: Live metrics and dashboards
- **Pagination**: Efficient handling of large result sets

### Monitoring & Health
- **Health Checks**: Built-in health monitoring for all components
- **Performance Metrics**: JVM and system performance tracking
- **Agent Status**: Real-time agent status and configuration monitoring

## 🔧 Configuration

Each component can be configured via `application.properties`:

```properties
# Example Log Agent Configuration
log.agent.agent-id=my-agent
log.agent.hostname=localhost
log.agent.watch-directories=/var/log,/app/logs
log.agent.file-patterns=*.log,*.txt
log.agent.scan-interval-seconds=30
log.agent.kafka-topic-name=raw-logs
```

## 🧪 Testing

The project includes comprehensive tests:

```bash
# Run all tests
mvn test

# Run tests for specific module
cd log-agent
mvn test
```

## 📈 Scalability Considerations

- **Horizontal Scaling**: Each component can be scaled independently
- **Kafka Partitioning**: Supports multiple partitions for parallel processing
- **Elasticsearch Clustering**: Can work with clustered Elasticsearch deployments
- **Load Balancing**: Components can be load balanced behind reverse proxies

## 🔒 Security Features

- **Input Validation**: Comprehensive validation of log entries
- **Health Monitoring**: Built-in monitoring for security and performance
- **Error Handling**: Robust error handling and logging

## 🤝 Contributing

This is an open-source project following best practices:
- Modular architecture for easy maintenance
- Comprehensive documentation
- Test coverage
- Configuration flexibility

## 📝 License

This project is licensed under the GNU General Public License - see the LICENSE file for details.