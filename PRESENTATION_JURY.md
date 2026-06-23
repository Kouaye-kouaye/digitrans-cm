# DIGITRANS-CM — Présentation au Jury
## Plateforme Microservices de Gestion Intégrée Agricole

**Date :** 23 juin 2026  
**Version :** 1.0

---

## 1. Contexte et Objectifs

### 1.1 Problème métier
DIGITRANS-CM adresse les besoins d'un écosystème agricole au Cameroun :
- **Traçabilité** : suivre les lots cacao/café de la récolte à l'export
- **Gestion RH/Paie** : administrer employés, paies, contrats
- **Restauration/Commandes** : gestion online/offline de commandes
- **Analyse décisionnelle** : BI pour rapports et insights

### 1.2 Objectifs techniques
- Scalabilité : architecture microservices déployable sur cloud
- Sécurité : authentification JWT, données protégées
- Résilience : services autonomes, base de données centralisée
- Observabilité : monitoring, logs, métriques centralisés
- Traçabilité immutable : intégration blockchain

---

## 2. Architecture Global

### 2.1 Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT / FRONTEND                        │
│           (Web, Mobile, ou Système Externe)                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS + Bearer Token (JWT)
                         ▼
         ┌──────────────────────────────────┐
         │     API GATEWAY (Port 8080)      │
         │  - Authentification JWT          │
         │  - Routage des requêtes          │
         │  - Validation des accès          │
         │  - Swagger/OpenAPI               │
         └──────┬─────────────┬─────────────┘
                │             │
        ┌───────▼──┐   ┌──────▼────────┐   ┌──────────────┐
        │    ERP   │   │      CRM      │   │   SUPPLY-    │
        │ Service  │   │   Service     │   │   CHAIN      │
        │(Port 81) │   │  (Port 8082)  │   │  (Port 8083) │
        │   RH/Paie│   │  Restauration │   │  Traçabilité │
        └────┬─────┘   └───────┬──────┘   └──────┬───────┘
             │                 │                  │
             └────────┬────────┴──────────────────┘
                      │
         ┌────────────▼────────────┐
         │  PostgreSQL (Port 5432) │
         │  Base de données        │
         │  centralisée            │
         └─────────────────────────┘
                      │
         ┌────────────▼────────────────────┐
         │   Monitoring & Observation      │
         │  ├─ Prometheus (9090)           │
         │  ├─ Grafana (3000)              │
         │  ├─ Loki (3100)                 │
         │  └─ AlertManager (9093)         │
         └────────────────────────────────┘
```

### 2.2 Technologies utilisées

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Java | OpenJDK | 21 |
| Framework | Spring Boot | 3.3.5 |
| Build | Maven | multi-module |
| DB | PostgreSQL | 16 |
| Conteneurisation | Docker | latest |
| Orchestration | Docker Compose | 3.9 |
| Sécurité | Spring Security + JWT | HMAC-SHA256 |
| Métriques | Micrometer + Prometheus | latest |
| Logs | SLF4J + Loki | latest |

---

## 3. Services Microservices

### 3.1 API Gateway (Port 8080)

**Responsabilité :** Point d'entrée unique de la plateforme

**Fonctionnalités :**
- Authentification et délivrance JWT
- Routage des requêtes vers les services métiers
- Validation d'accès et autorisation
- Exposition des endpoints publics (`/api/auth/**`)
- Documentation OpenAPI sur `/swagger-ui/**`

**Endpoints clés :**
```
POST   /api/auth/register    → Créer un utilisateur
POST   /api/auth/login       → Authentification, retour JWT
GET    /api/health           → Health check
GET    /swagger-ui/**        → Documentation interactive
```

**Données stockées :**
- Utilisateurs (en mémoire `ConcurrentHashMap`)
- Tokens JWT

---

### 3.2 ERP Service (Port 8081)

**Responsabilité :** Gestion des ressources humaines et paie

**Domaines métier :**
- Gestion des employés (création, modification, suppression)
- Gestion des contrats (durée, statut)
- Traitement de la paie (salaires, cotisations)
- Rapports RH (présence, performance)

**Endpoints clés :**
```
POST   /api/erp/employees              → Recruter un employé
GET    /api/erp/employees/{id}         → Détails employé
PUT    /api/erp/employees/{id}/salary  → Modifier salaire
POST   /api/erp/payroll/process        → Traiter la paie
GET    /api/erp/payroll/{month}        → Rapport paye
```

**Données métier :**
- Employés, contrats, historique de paie

---

### 3.3 CRM Service (Port 8082)

**Responsabilité :** Gestion de la restauration et des commandes

**Domaines métier :**
- Gestion des menus et produits disponibles
- Commandes en ligne et hors ligne
- Programme de fidélité clients
- Historique de consommation

**Endpoints clés :**
```
POST   /api/crm/orders                 → Créer une commande
GET    /api/crm/orders/{id}/status     → Statut de commande
POST   /api/crm/loyalty/points/add     → Ajouter points fidélité
GET    /api/crm/customers/{id}/history → Historique client
```

**Particularité :** Supporte le mode **offline-first** (synchronisation ultérieure).

---

### 3.4 Supply Chain Service (Port 8083)

**Responsabilité :** Traçabilité cacao/café de la récolte à l'export

**Domaines métier :**
- Gestion des plantations (zones, propriétaires)
- Gestion des lots de récolte avec qualité (A/B/C)
- Événements de traçabilité (récolte → transport → entrepôt → export)
- Vérification de l'intégrité via blockchain

**Flux métier :**
```
Plantation créée
    ↓
Récolte → Création HarvestBatch (événement HARVEST)
    ↓
Événements chainés (qualité check, transport, entrée entrepôt, export)
    ↓
Hash blockchain enregistré pour immuabilité
    ↓
Client vérifie intégrité : GET /batches/{code}/verify
```

**Endpoints clés :**
```
POST   /api/supply/plantations         → Créer plantation
POST   /api/supply/batches             → Créer lot récolte
GET    /api/supply/batches/{code}/trace    → Chaîne traçabilité
GET    /api/supply/batches/{code}/verify   → Vérifier intégrité
PUT    /api/supply/batches/{code}/status   → Changer statut
```

**Modèle de données :**
```
Plantation (1:N) HarvestBatch (1:N) TraceabilityEvent
                      ↓
                 WarehouseStock
                      ↓
                blockchainTxHash (intégrité)
```

---

### 3.5 BI Service (Port — squelette)

**Responsabilité :** Analyse décisionnelle (future)

**Prévisions :**
- Agrégation de données métier
- Tableaux de bord décisionnels
- Rapports analytiques
- Prédictions via ML

---

## 4. Architecture de Sécurité

### 4.1 Authentification JWT

**Flux d'authentification :**
```
1. Client → POST /api/auth/login (username, password)
2. Gateway valide les credentials
3. Gateway émet un JWT signé HMAC-SHA256
4. JWT contient : { username, role, issuedAt, expiresIn }
5. Client inclut JWT en header : Authorization: Bearer <token>
6. Chaque requête est validée par JwtAuthenticationFilter
7. Si valide → request passe, sinon → 401 Unauthorized
```

**Configuration :**
```yaml
JWT_SECRET: "digitrans-cm-secret-key-agrocam-2026-camtech-minimum-256bits"
JWT_EXPIRATION: 86400000 ms (24 heures)
Algorithm: HMAC-SHA256 (jjwt 0.12.3)
```

### 4.2 Contrôle d'accès

**Routes publiques :**
- `/api/auth/**` (register, login)
- `/swagger-ui/**`, `/v3/api-docs/**`

**Routes protégées :**
- Toutes les autres routes → Authenticated (JWT required)

**CORS :**
- Origines : toutes (`*`) en développement
- Méthodes : GET, POST, PUT, DELETE, PATCH, OPTIONS
- Headers : Authorization, Content-Type

### 4.3 Isolation des services

Chaque service embarque sa propre couche sécurité, permettant un fonctionnement autonome même en cas de défaillance du gateway.

---

## 5. Infrastructure et Déploiement

### 5.1 Docker Compose

**Services lancés :**
```yaml
postgres       # Base de données centrale
api-gateway    # Point d'entrée
erp-service    # RH/Paie
crm-service    # Restauration
supply-chain-service  # Traçabilité
prometheus     # Collecte métriques
grafana        # Visualisation
loki           # Logs centralisés
```

**Commande de lancement :**
```bash
docker-compose up -d
```

### 5.2 Volumes persistants

```
postgres-data/          # Données PostgreSQL
prometheus-data/        # Métriques Prometheus
grafana-data/           # Configuration Grafana
loki-data/              # Logs Loki
```

### 5.3 Health Checks

Chaque service expose un endpoint `/actuator/health` pour vérifier son état.

```bash
curl http://localhost:8080/actuator/health
→ { "status": "UP" }
```

---

## 6. Monitoring et Observabilité

### 6.1 Métriques Prometheus

**Métriques collectées :**
- `http_server_requests_seconds_count` : nombre de requêtes
- `http_server_requests_seconds_sum` : latence totale
- `http_server_requests_seconds_bucket` : histogrammes (P50, P95, P99)
- `jvm_memory_used_bytes` : utilisation mémoire JVM
- `jvm_cpu_load` : charge CPU
- `hikaricp_connections_active` : connexions DB actives
- Métriques métier : `batches_created_total`, `orders_processed_total`, `payroll_processed_total`

**Accès :**
```
http://localhost:9090
```

### 6.2 Dashboard Grafana

**Panneaux clés :**
- Latence API (P50, P95, P99) par service
- Taux d'erreur (4xx/5xx)
- Utilisation mémoire JVM
- Connexions base de données
- Requêtes par minute

**Accès :**
```
http://localhost:3000
login: admin / admin123
```

### 6.3 Logs centralisés Loki

**Configuration :**
- Format JSON pour structured logging
- Service: label de quel microservice produit le log
- Level: INFO, WARN, ERROR

**Accès :**
```
http://localhost:3100
```

### 6.4 Alertes AlertManager

**Alertes critiques :**
- Latence P95 > 500 ms
- Taux d'erreur > 1%
- Mémoire JVM > 80%
- Service indisponible (up == 0)

**Notifiants :** Slack, Email

---

## 7. Intégration Blockchain

### 7.1 Cas d'usage

Pour la **traçabilité supply-chain**, garantir l'immuabilité des événements :

```
Événement créé → Hash SHA-256 → Blockchain cloud → TxHash stocké en DB
```

### 7.2 Flux

1. Application crée `TraceabilityEvent`
2. Calcule `hashSignature` = SHA256(contenu)
3. Envoie hash à une API blockchain provider (AWS Managed Blockchain, Ethereum, etc.)
4. Reçoit `blockchainTxHash`
5. Stocke `blockchainTxHash` dans `HarvestBatch.blockchainTxHash`

### 7.3 Vérification

```
GET /api/supply/batches/{batchCode}/verify
→ Compare hash local vs blockchain
→ Retourne : { verified: true/false, txHash, timestamp }
```

---

## 8. Flux Métier Complet

### Exemple : Traçabilité lot cacao

```
1️⃣  CRÉATION PLANTATION (Supply Chain)
    POST /api/supply/plantations
    { code: "PLT001", name: "Cacao Douala", region: "Littoral", ... }
    → Stockée en DB

2️⃣  RÉCOLTE (Supply Chain)
    POST /api/supply/batches
    { plantationId: 1, harvestDate: "2026-06-15", quantityKg: 1000, qualityGrade: "A" }
    → HarvestBatch créé
    → TraceabilityEvent (HARVEST) créé
    → Hash calculé, envoyé à blockchain
    → blockchainTxHash reçu et stocké

3️⃣  VÉRIFICATION DE QUALITÉ (Supply Chain)
    PUT /api/supply/batches/{code}/status?status=AT_WAREHOUSE
    → TraceabilityEvent (QUALITY_CHECK) créé
    → Nouveau hash envoyé à blockchain

4️⃣  AUDIT (Client externe)
    GET /api/supply/batches/BATCH-2026-001/trace
    → Retourne événements complets avec hashes
    
    GET /api/supply/batches/BATCH-2026-001/verify
    → Vérifie intégrité auprès blockchain
    → { verified: true, ... }
```

### Exemple : Commande restauration

```
1️⃣  CRÉATION COMMANDE (CRM)
    POST /api/crm/orders
    { customerId: 5, items: [{ productId: 1, qty: 2 }], ... }
    → Order créée

2️⃣  PAIEMENT
    status: "PENDING" → "PAID"

3️⃣  PRÉPARATION & LIVRAISON
    status: "PAID" → "IN_PREP" → "DELIVERED"

4️⃣  FIDÉLITÉ
    POST /api/crm/loyalty/points/add?customerId=5&points=10
    → Points ajoutés au compte client
```

### Exemple : Traitement de paie

```
1️⃣  ENREGISTRER EMPLOYÉ (ERP)
    POST /api/erp/employees
    { firstName: "Jean", lastName: "Doe", salaryBase: 300000, ... }
    → Employee créé

2️⃣  METTRE À JOUR SALAIRE
    PUT /api/erp/employees/{id}/salary
    { newSalary: 350000, effectiveDate: "2026-07-01" }

3️⃣  TRAITER PAIE MENSUELLE (ERP)
    POST /api/erp/payroll/process?month=6&year=2026
    → Calcul salaire net = salaire brut - cotisations
    → Payroll enregistrée en DB

4️⃣  RAPPORT PAYE (ERP)
    GET /api/erp/payroll/2026-06
    → Liste des employés et montants
```

---

## 9. Technologies Cloud (Déploiement futur)

### 9.1 Exemple architecture cloud (AWS)

```
┌─────────────────────────────────────────┐
│     Amazon EKS (Kubernetes managé)      │
│  ├─ api-gateway pod                     │
│  ├─ erp-service pod                     │
│  ├─ crm-service pod                     │
│  └─ supply-chain-service pod            │
└─────────────────────────────────────────┘
         ↓
    AWS Application Load Balancer
         ↓
┌─────────────────────────────────────────┐
│  Amazon RDS (PostgreSQL managé)         │
│  - Backup automatique                   │
│  - Haute disponibilité                  │
│  - Encryption at rest                   │
└─────────────────────────────────────────┘
         ↓
    AWS Secrets Manager (JWT_SECRET, DB password)
         ↓
    Amazon CloudWatch (Logs + Métriques)
         ↓
    AWS Managed Blockchain ou Infura (Blockchain)
```

### 9.2 Infrastructure-as-Code (Terraform)

```
terraform/
├─ main.tf          # Déclaration EKS, RDS, IAM
├─ variables.tf     # Variables
├─ outputs.tf       # Outputs
└─ kubernetes/      # Manifests K8s
    ├─ namespace.yml
    ├─ api-gateway-deployment.yml
    ├─ erp-service-deployment.yml
    ├─ crm-service-deployment.yml
    └─ supply-chain-service-deployment.yml
```

---

## 10. Avantages de cette Architecture

| Aspect | Bénéfice |
|--------|----------|
| **Microservices** | Autonomie, scalabilité par service, déploiement indépendant |
| **JWT** | Stateless, scalable, sécurisé, standard industrie |
| **PostgreSQL centralisée** | Intégrité transactionnelle, ACID, rapports cross-services |
| **Docker Compose** | Déploiement reproductible, local et cloud compatible |
| **Prometheus/Grafana** | Observabilité temps réel, alertes proactives |
| **Blockchain** | Traçabilité immuable pour données critiques métier |
| **Offline-first (CRM)** | Résilience, UX mobile fluide |
| **Spring Boot 3.3** | Performance, sécurité patches, GraalVM AOT ready |

---

## 11. Défis et Solutions

| Défi | Solution |
|-----|----------|
| Synchronisation cross-services | Event-driven (Kafka/RabbitMQ futur) ou polling |
| Latence réseau multi-services | Caching, circuit breaker, timeout configuré |
| Sécurité keys JWT | Secrets Manager cloud, rotation régulière |
| Backup / Disaster Recovery | Snapshots DB, replicas, multi-régions cloud |
| Conformité données | Chiffrement, audit logs, anonymisation RGPD |
| Blockchain cost | Smart contract optimisé, batching transactions |

---

## 12. Métriques de Succès

### Avant déploiement production

- [ ] Tests de charge : P95 < 500 ms
- [ ] Taux d'erreur < 0.1%
- [ ] Uptime > 99.9%
- [ ] All API endpoints documentés (Swagger)
- [ ] Logs centralisés et searchable (Loki)
- [ ] Alertes configurées et testées

### En production

- [ ] Monitoring 24/7 actif
- [ ] Runbooks pour alertes critiques
- [ ] Procédure rollback testée
- [ ] Audit trails logging
- [ ] Backup DB tous les jours

---

## 13. Démonstration au Jury

### Étapes de démonstration

1. **Vue d'ensemble API**
   ```bash
   curl http://localhost:8080/swagger-ui.html
   → Montrer tous les endpoints
   ```

2. **Authentification**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -d "username=user&password=pass" \
     -H "Content-Type: application/x-www-form-urlencoded"
   → Retour JWT
   ```

3. **Créer lot traçabilité**
   ```bash
   curl -X POST http://localhost:8083/api/supply/batches \
     -H "Authorization: Bearer <token>" \
     -d '{...}'
   → HarvestBatch créé avec blockchain hash
   ```

4. **Vérifier intégrité**
   ```bash
   curl http://localhost:8083/api/supply/batches/BATCH-2026-001/verify \
     -H "Authorization: Bearer <token>"
   → { verified: true, txHash: "0x..." }
   ```

5. **Dashboard Grafana**
   ```
   http://localhost:3000
   → Montrer latence, erreurs, mémoire
   ```

6. **Logs Loki**
   ```
   http://localhost:3100
   → Montrer requête + logs structurés
   ```

---

## 14. Conclusion

**DIGITRANS-CM** est une plateforme production-ready qui :

✅ Répond aux besoins métier agricoles du Cameroun  
✅ Utilise une architecture microservices scalable et sécurisée  
✅ Intègre blockchain pour traçabilité immuable  
✅ Expose des API REST bien documentées  
✅ Dispose d'observabilité complète (métriques, logs, alertes)  
✅ Est déployable sur docker-compose (local) ou cloud managé (AWS/Azure/GCP)  

Cette implémentation démontre une maîtrise des technologies cloud modernes, de la sécurité, et des patterns d'architecture d'entreprise.

---

**Questions potentielles du jury :**

1. **Pourquoi microservices et pas monolithe ?**
   → Scalabilité par service, autonomie équipes, déploiement indépendant

2. **Comment garantir la sécurité des tokens JWT ?**
   → HMAC-SHA256, stockage secret en gestionnaire de clés, expiration 24h, rotation clés

3. **Que se passe-t-il si PostgreSQL tombe ?**
   → RDS avec multi-AZ en cloud, failover automatique, replica read-only

4. **Comment la blockchain améliore la traçabilité ?**
   → Immuabilité, audit trail public, vérification auprès tiers indépendant

5. **Avez-vous testé la charge ?**
   → Tests k6 dans `/tests/performance/`, résultats dans rapports

6. **Quel est le coût du déploiement cloud ?**
   → Estimé $500-1000/mois pour une charge moyenne (EKS, RDS, Monitoring)

