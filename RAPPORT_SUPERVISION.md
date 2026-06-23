# Rapport de Supervision & Monitoring — DIGITRANS-CM

**Date :** 21 mai 2026  
**Version :** 1.0

---

## 1. Architecture de Supervision

```
┌─────────────────────────────────────────────────────────────────┐
│                        Prometheus                                │
│              Collecte de métriques (pull)                        │
└─────┬───────────────┬──────────────────┬────────────────────────┘
      │               │                  │
┌─────▼──────┐ ┌──────▼───────┐  ┌──────▼──────────┐
│  Grafana   │ │  Loki        │  │  AlertManager   │
│ Dashboard  │ │  Centralis.  │  │  Alertes         │
│ Métriques  │ │  Logs        │  │  Slack/Email     │
└────────────┘ └──────────────┘  └─────────────────┘
```

---

## 2. Métriques Clés (KPIs)

### 2.1 Performance applicative

| Métrique | Seuil d'alerte | Description |
|----------|----------------|-------------|
| `http_request_duration_ms` | P95 > 500 ms | Latence des requêtes API |
| `http_requests_total` | — | Volume de requêtes par service |
| `http_errors_total` | > 1 % | Taux d'erreur (4xx/5xx) |
| `jwt_verify_duration_ms` | P95 > 100 ms | Temps de validation JWT |
| `db_query_duration_ms` | P95 > 200 ms | Temps des requêtes JPA |

### 2.2 Infrastructure

| Métrique | Seuil d'alerte | Description |
|----------|----------------|-------------|
| `jvm_memory_used_bytes` | > 80 % heap | Utilisation mémoire JVM |
| `jvm_cpu_load` | > 70 % | Charge CPU |
| `jvm_thread_count` | — | Nombre de threads actifs |
| `hikari_connections_active` | > 80 % pool | Connexions DB actives |
| `disk_space_bytes` | > 85 % | Espace disque |

### 2.3 Métier

| Métrique | Seuil d'alerte | Description |
|----------|----------------|-------------|
| `batches_created_total` | — | Nombre de lots de récolte créés |
| `orders_processed_total` | — | Commandes traitées |
| `payroll_processed_total` | — | Paies traitées |
| `traceability_verifications_total` | — | Vérifications d'intégrité |

---

## 3. Exposition des Métriques avec Spring Actuator + Micrometer

### 3.1 Dépendance Maven (à ajouter à chaque service)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 3.2 Configuration application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
        step: 15s
```

### 3.3 Endpoints exposés

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check (liveness + readiness) |
| `/actuator/metrics` | Liste des métriques disponibles |
| `/actuator/prometheus` | Format Prometheus (scraping) |
| `/actuator/info` | Informations service |

### 3.4 Métriques Prometheus disponibles

```
# JVM
jvm_memory_used_bytes{area="heap",...}
jvm_gc_pause_seconds{...}
jvm_threads_live_threads{...}
jvm_cpu_load{...}

# HTTP
http_server_requests_seconds_count{method,status,uri,...}
http_server_requests_seconds_sum{...}

# DataSource
hikaricp_connections_active{pool="HikariPool-1"}
hikaricp_connections_idle{...}
hikaricp_connections_pending{...}

# Business (custom)
batches_created_total{service="supply-chain-service"}
orders_processed_total{service="crm-service"}
payroll_processed_total{service="erp-service"}
```

---

## 4. Configuration Prometheus

### Fichier `prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']
        labels:
          service: 'api-gateway'

  - job_name: 'erp-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['erp-service:8081']
        labels:
          service: 'erp-service'

  - job_name: 'crm-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['crm-service:8082']
        labels:
          service: 'crm-service'

  - job_name: 'supply-chain-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['supply-chain-service:8083']
        labels:
          service: 'supply-chain-service'
```

---

## 5. Docker Compose — Supervision (à ajouter dans `docker-compose.yml`)

```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: digitrans-prometheus
  volumes:
    - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus-data:/prometheus
  ports:
    - "9090:9090"
  depends_on:
    - api-gateway
    - erp-service
    - crm-service
    - supply-chain-service

grafana:
  image: grafana/grafana:latest
  container_name: digitrans-grafana
  ports:
    - "3000:3000"
  volumes:
    - grafana-data:/var/lib/grafana
  environment:
    GF_SECURITY_ADMIN_PASSWORD: admin123
  depends_on:
    - prometheus

loki:
  image: grafana/loki:latest
  container_name: digitrans-loki
  ports:
    - "3100:3100"
  volumes:
    - loki-data:/loki
```

---

## 6. Dashboard Grafana — Métriques Essentielles

### Panneau 1 : Latence API (P50 / P95 / P99)

```promql
# P95 par service
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service)
)
```

### Panneau 2 : Taux d'erreur

```promql
# Ratio 5xx par service
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
/
sum(rate(http_server_requests_seconds_count[5m])) by (service)
```

### Panneau 3 : Mémoire JVM

```promql
# Utilisation heap par service
jvm_memory_used_bytes{area="heap"} / 1024 / 1024
```

### Panneau 4 : Connexions DB

```promql
hikaricp_connections_active / hikaricp_connections_max * 100
```

### Panneau 5 : Requêtes par minute

```promql
sum(rate(http_server_requests_seconds_count[1m])) by (service, method, status)
```

---

## 7. Alertes Recommandées (AlertManager)

### Fichier `alertmanager.yml`

```yaml
groups:
  - name: digitrans-alerts
    rules:
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Latence élevée sur {{ $labels.service }}"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Taux d'erreur > 1% sur {{ $labels.service }}"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Mémoire > 80% sur {{ $labels.service }}"

      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.instance }} injoignable"
```

---

## 8. Métriques Métier Personnalisées

Ajouter dans chaque service un `@Component` ou utiliser `MeterRegistry` :

### Supply Chain

```java
@Component
public class BusinessMetrics {
    private final Counter batchesCreated;
    private final Counter verificationsPerformed;

    public BusinessMetrics(MeterRegistry registry) {
        this.batchesCreated = registry.counter("batches_created_total");
        this.verificationsPerformed = registry.counter("traceability_verifications_total");
    }

    public void recordBatchCreated() { batchesCreated.increment(); }
    public void recordVerification() { verificationsPerformed.increment(); }
}
```

### CRM

```java
private final Counter ordersProcessed;
// À injecter dans OrderService et incrémenter après chaque création de commande
```

### ERP

```java
private final Counter payrollProcessed;
// À injecter dans EmployeeService.processPayroll()
```

---

## 9. Logs Centralisés (Loki + Promtail)

Chaque service doit écrire ses logs au format JSON :

```yaml
# application.yml
logging:
  pattern:
    json:
      include: application,level,logger,thread,message,exception
  level:
    com.digitrans: INFO
```

Promtail collecte les logs Docker et les envoie à Loki :

```yaml
scrape_configs:
  - job_name: docker
    docker_sd_configs:
      - host: "unix:///var/run/docker.sock"
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: 'digitrans-(.*)'
        target_label: 'service'
```

---

## 10. Health Checks API

### Endpoint standard

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

### Health checks personnalisés

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection c = dataSource.getConnection()) {
            return Health.up().withDetail("database", c.getMetaData().getURL()).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

---

## 11. Dépendances Maven à Ajouter

Dans chaque `pom.xml` de service :

```xml
<!-- Actuator + Prometheus -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Logs JSON (optionnel, pour Loki) -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

---

## 12. URLs de Supervision

| Service | URL | Port |
|---------|-----|------|
| Prometheus | `http://localhost:9090` | 9090 |
| Grafana | `http://localhost:3000` (admin/admin123) | 3000 |
| Loki | `http://localhost:3100` | 3100 |
| AlertManager | `http://localhost:9093` | 9093 |
| Actuator (api-gateway) | `http://localhost:8080/actuator` | 8080 |
| Actuator (erp) | `http://localhost:8081/actuator` | 8081 |
| Actuator (crm) | `http://localhost:8082/actuator` | 8082 |
| Actuator (supply) | `http://localhost:8083/actuator` | 8083 |
