# Log Aggregation Platform - API Reference

## 📡 API Endpoints Overview

The Log Aggregation Platform provides several REST APIs across its components for monitoring, log submission, and data retrieval.

## 🔧 Log Agent API (Port 8081)

### Health and Monitoring

#### GET `/api/agent/health`
Get the health status of the log agent.

**Response:**
```json
{
  "overall_status": "HEALTHY",
  "file_monitoring": {
    "status": "ACTIVE",
    "monitored_files": 5,
    "last_scan": "2024-08-31T13:20:15Z"
  },
  "system_logs": {
    "status": "ACTIVE",
    "enabled": true
  },
  "kafka_producer": {
    "status": "CONNECTED",
    "messages_sent": 1250
  }
}
```

**Status Codes:**
- `200 OK` - Agent is healthy
- `200 OK` - Agent has warnings
- `503 Service Unavailable` - Agent is in critical state

#### GET `/api/agent/status`
Get comprehensive status information about the agent.

**Response:**
```json
{
  "agent_id": "agent-001",
  "hostname": "localhost",
  "uptime_ms": 3600000,
  "version": "0.0.1-SNAPSHOT",
  "file_monitoring": {
    "enabled": true,
    "watched_directories": ["/var/log", "/tmp/logs"],
    "file_patterns": ["*.log", "*.txt"],
    "monitored_files_count": 5,
    "scan_interval_seconds": 30
  },
  "system_logs": {
    "enabled": true,
    "collection_active": true
  },
  "health": {
    "overall_status": "HEALTHY",
    "last_health_check": "2024-08-31T13:20:15Z"
  }
}
```

#### GET `/api/agent/metrics`
Get detailed performance metrics.

**Response:**
```json
{
  "jvm_total_memory": 1073741824,
  "jvm_free_memory": 536870912,
  "jvm_used_memory": 536870912,
  "jvm_max_memory": 2147483648,
  "available_processors": 8,
  "system_load_average": 1.5,
  "process_cpu_load": 0.15,
  "agent_uptime_ms": 3600000,
  "files_processed": 125,
  "logs_sent": 1250,
  "errors_count": 2,
  "last_error": "2024-08-31T13:15:30Z"
}
```

## 📥 Log Receiver API (Port 8082)

### Log Submission

#### POST `/api/logs`
Submit a single log entry.

**Request Body:**
```json
{
  "source": "my-application",
  "level": "ERROR",
  "message": "Database connection failed",
  "hostname": "web-server-01",
  "application": "user-service",
  "thread": "main",
  "loggerName": "com.example.DatabaseService"
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RECEIVED",
  "timestamp": "2024-08-31T13:20:15Z"
}
```

**Status Codes:**
- `201 Created` - Log entry created successfully
- `400 Bad Request` - Invalid log entry data
- `500 Internal Server Error` - Processing error

#### POST `/api/logs/batch`
Submit multiple log entries in a single request.

**Request Body:**
```json
{
  "logs": [
    {
      "source": "app-1",
      "level": "INFO",
      "message": "User logged in"
    },
    {
      "source": "app-1",
      "level": "ERROR",
      "message": "Payment processing failed"
    }
  ]
}
```

**Response:**
```json
{
  "processed": 2,
  "failed": 0,
  "batch_id": "batch-550e8400-e29b-41d4-a716-446655440000"
}
```

#### GET `/api/logs/{id}`
Retrieve a specific log entry by ID.

**Path Parameters:**
- `id` - Log entry UUID

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "source": "my-application",
  "level": "ERROR",
  "message": "Database connection failed",
  "hostname": "web-server-01",
  "timestamp": "2024-08-31T13:20:15Z",
  "processedTimestamp": "2024-08-31T13:20:16Z",
  "metadata": {
    "severity": "HIGH",
    "errorCode": "ERROR_1001"
  }
}
```

**Status Codes:**
- `200 OK` - Log entry found
- `404 Not Found` - Log entry not found

### Search and Query

#### GET `/api/logs/search`
Search for log entries with filtering and pagination.

**Query Parameters:**
- `query` - Full-text search query
- `level` - Filter by log level (DEBUG, INFO, WARN, ERROR, FATAL)
- `source` - Filter by log source
- `from` - Start date (ISO 8601 format)
- `to` - End date (ISO 8601 format)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20, max: 100)

**Example:**
```
GET /api/logs/search?level=ERROR&source=user-service&page=0&size=50
```

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "source": "user-service",
      "level": "ERROR",
      "message": "Database connection failed",
      "timestamp": "2024-08-31T13:20:15Z"
    }
  ],
  "page": {
    "size": 50,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## 📊 Dashboard API (Port 8080)

### Web Dashboard

#### GET `/dashboard`
Main dashboard page (HTML).

#### GET `/dashboard/search`
Search page (HTML).

### Dashboard REST API

#### GET `/dashboard/api/logs/{id}`
Get detailed information about a specific log entry.

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "source": "user-service",
  "level": "ERROR",
  "message": "Database connection failed",
  "rawMessage": "2024-08-31 13:20:15 ERROR [main] DatabaseService - Connection timeout",
  "hostname": "web-server-01",
  "application": "user-service",
  "thread": "main",
  "loggerName": "com.example.DatabaseService",
  "timestamp": "2024-08-31T13:20:15Z",
  "processedTimestamp": "2024-08-31T13:20:16Z",
  "metadata": {
    "severity": "HIGH",
    "errorCode": "ERROR_1001",
    "processedBy": "log-processor-1"
  },
  "tags": {
    "environment": "production",
    "version": "1.2.3"
  }
}
```

#### POST `/dashboard/search`
Perform advanced log search.

**Request Body (form-encoded):**
- `query` - Search query
- `level` - Log level filter
- `source` - Source filter
- `page` - Page number
- `size` - Page size

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "source": "user-service",
      "level": "ERROR",
      "message": "Database connection failed",
      "timestamp": "2024-08-31T13:20:15Z",
      "formattedTimestamp": "2024-08-31 13:20:15"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

#### GET `/dashboard/api/stats`
Get aggregated statistics about logs.

**Response:**
```json
{
  "totalLogs": 10000,
  "errorLogs": 150,
  "warnLogs": 800,
  "infoLogs": 9050,
  "errorPercentage": 1.5,
  "warnPercentage": 8.0,
  "infoPercentage": 90.5
}
```

## 🔄 Log Processor (Port 8083)

The Log Processor primarily operates through Kafka consumers and doesn't expose REST APIs by default. However, monitoring endpoints can be added:

#### GET `/actuator/health` (if Spring Actuator is enabled)
Check processor health.

#### GET `/actuator/metrics` (if Spring Actuator is enabled)
Get processing metrics.

## 📝 Data Models

### LogEntry Model
```json
{
  "id": "string (UUID)",
  "source": "string (required)",
  "level": "string (DEBUG|INFO|WARN|ERROR|FATAL, required)",
  "message": "string (required)",
  "rawMessage": "string",
  "hostname": "string",
  "application": "string",
  "thread": "string",
  "loggerName": "string",
  "timestamp": "string (ISO 8601)",
  "processedTimestamp": "string (ISO 8601)",
  "metadata": {
    "key": "value"
  },
  "tags": {
    "key": "value"
  }
}
```

### LogStats Model
```json
{
  "totalLogs": "number",
  "errorLogs": "number",
  "warnLogs": "number",
  "infoLogs": "number",
  "errorPercentage": "number",
  "warnPercentage": "number",
  "infoPercentage": "number"
}
```

### Page Model
```json
{
  "content": "array",
  "page": {
    "size": "number",
    "number": "number",
    "totalElements": "number",
    "totalPages": "number"
  }
}
```

## 🔐 Authentication and Security

Currently, the platform operates without authentication for simplicity. For production use, consider implementing:

### API Key Authentication
Add to request headers:
```
X-API-Key: your-api-key-here
```

### JWT Authentication
Add to request headers:
```
Authorization: Bearer your-jwt-token-here
```

### Basic Authentication
Add to request headers:
```
Authorization: Basic base64(username:password)
```

## 📋 Error Responses

### Standard Error Format
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": "Additional error details",
    "timestamp": "2024-08-31T13:20:15Z"
  }
}
```

### Common Error Codes
- `INVALID_LOG_ENTRY` - Malformed log entry data
- `LOG_NOT_FOUND` - Requested log entry doesn't exist
- `INVALID_SEARCH_PARAMS` - Invalid search parameters
- `INTERNAL_ERROR` - Internal server error
- `KAFKA_UNAVAILABLE` - Kafka connection error
- `ELASTICSEARCH_UNAVAILABLE` - Elasticsearch connection error

## 🧪 Testing the APIs

### Using curl

```bash
# Submit a log
curl -X POST http://localhost:8082/api/logs \
  -H "Content-Type: application/json" \
  -d '{"source":"test-app","level":"INFO","message":"Test message"}'

# Search logs
curl "http://localhost:8082/api/logs/search?level=ERROR&page=0&size=10"

# Check agent health
curl http://localhost:8081/api/agent/health

# Get dashboard stats
curl http://localhost:8080/dashboard/api/stats
```

### Using JavaScript

```javascript
// Submit a log entry
fetch('http://localhost:8082/api/logs', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    source: 'web-app',
    level: 'ERROR',
    message: 'User authentication failed',
    hostname: 'web-01'
  })
})
.then(response => response.json())
.then(data => console.log('Log submitted:', data));

// Search logs
fetch('http://localhost:8082/api/logs/search?level=ERROR&size=5')
  .then(response => response.json())
  .then(data => console.log('Search results:', data));
```

This API reference provides comprehensive documentation for interacting with all components of the Log Aggregation Platform.