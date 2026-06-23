# Rapport de Tests de Charge — DIGITRANS-CM

**Date :** 21 mai 2026  
**Environnement :** Local (Windows 11, Java 21, H2 en mémoire)  
**Outil :** PowerShell `Invoke-WebRequest` + K6 (scripts fournis)

---

## 1. Résumé des KPI

| Métrique | API Gateway | Supply Chain |
|----------|-------------|-------------|
| **Temps moyen (avg)** | 103 ms | 41 ms |
| **P95** | 117 ms | 61 ms |
| **Débit max observé** | ~10 req/s | ~25 req/s |
| **Taux d'échec** | 0 % | 0 % |
| **Surcharge JWT (avg)** | 4,9 ms | — |

---

## 2. Scénarios de Test

### 2.1 API Gateway — Authentification JWT

**Endpoint :** `POST /api/auth/login` (50 requêtes)

| Métrique | Valeur |
|----------|--------|
| Moyenne | 103,2 ms |
| Minimum | 92,3 ms |
| Maximum | 250,3 ms |
| P50 (médiane) | 98,7 ms |
| P95 | 117,4 ms |
| Taux succès | 100 % (50/50) |

**Analyse :** La latence est dominée par le hachage BCrypt (vérification du mot de passe) et la signature HMAC-SHA256 du JWT. Les valeurs sont stables (écart-type faible).

### 2.2 API Gateway — Surcharge de validation JWT

**Endpoint :** `GET /api/auth/test` (50 requêtes avec token valide)

| Métrique | Valeur |
|----------|--------|
| Moyenne | 4,9 ms |
| Minimum | 3,0 ms |
| Maximum | 16,0 ms |
| P50 | 4,3 ms |
| P95 | 7,1 ms |
| P99 | 16,0 ms |

**Analyse :** La validation du token (parsing, vérification HMAC, extraction du sujet) est très rapide (< 5 ms en moyenne). Ce goulot d'étranglement potentiel est négligeable.

### 2.3 Supply Chain — Création de lot

**Endpoint :** `POST /api/supply/batches` (20 requêtes)

| Métrique | Valeur |
|----------|--------|
| Moyenne | 41,3 ms |
| Minimum | 32,0 ms |
| Maximum | 174,2 ms |
| P50 | 34,0 ms |
| P95 | 174,2 ms |

**Analyse :** L'opération inclut :
- Création JPA de l'entité `HarvestBatch`
- Génération du hash blockchain (fake UUID)
- Création de l'événement `HARVEST` avec signature SHA-256
- Commit transaction

Le P95 élevé (174 ms) est dû à une valeur aberrante (première requête avec chargement JPA à froid). Le régime permanent est autour de 34 ms.

### 2.4 Supply Chain — Traçabilité complète

**Endpoint :** `GET /api/supply/batches/{code}/trace` (20 requêtes)

| Métrique | Valeur |
|----------|--------|
| Moyenne | 38,6 ms |
| Minimum | 32,6 ms |
| Maximum | 60,5 ms |
| P50 | 37,5 ms |
| P95 | 60,5 ms |

**Analyse :** L'appel recharge tous les événements du lot et re-calcule les hashs SHA-256 pour vérification. Performances stables, pas de point chaud identifié.

---

## 3. Goulots d'Étranglement Identifiés

| Priorité | Problème | Impact | Recommandation |
|----------|----------|--------|----------------|
| Critique | **BCrypt sur login** | Chaque login coûte ~100 ms (hachage + vérification) | Passer à un cache de tokens (Redis) ou JWT longue durée |
| Moyen | **JPA N+1 sur trace** | Les événements sont chargés par `batchId` puis les hashs sont recomputés en boucle | Ajouter un index sur `traceability_events.batch_id` |
| Faible | **SHA-256 synchrone** | Le calcul de hash est CPU-bound mais reste < 1 ms | Déjà optimal (pas de bottleneck) |

---

## 4. Recommandations

1. **Mettre en cache les sessions** : Éviter BCrypt à chaque requête en stockant les tokens dans un cache Redis (TTL = expiration JWT)
2. **Indexer les colonnes fréquentes** : `traceability_events.batch_id`, `harvest_batches.batch_code`, `orders.restaurant_id`
3. **Monitorer le CPU** : Hachage SHA-256 + BCrypt sont CPU-bound — surveiller sous charge maximale avec 100+ VUs
4. **Pool de connexions JPA** : Configurer `spring.datasource.hikari.maximum-pool-size=20` pour la production

---

## 5. Exécution des Tests

### Avec PowerShell (test rapide)

```powershell
# 1. Démarrer un service
cd backend
mvn spring-boot:run -pl api-gateway

# 2. Lancer le test (50 requêtes login)
$login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method Post -Body '{"username":"admin","password":"admin123"}' `
  -ContentType "application/json"
# Mesurer le temps de réponse
Measure-Command { Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" ... }
```

### Avec K6 (test de charge complet)

```bash
# Installer K6 (Windows)
winget install k6

# Test de fumée (1 VU, 5 itérations)
k6 run tests/performance/k6-smoke-test.js

# Test de charge complet (montée jusqu'à 100 VUs)
k6 run tests/performance/k6-load-test.js

# Avec seuils personnalisés
k6 run --threshold "http_req_duration[95]=<500" tests/performance/k6-load-test.js
```

### Avec Postman

Alternativement, utiliser la collection Postman `postman_collection.json` avec le **Collection Runner** :
- Itérations : 50
- Délai entre requêtes : 1000 ms
- Exporter les résultats CSV pour analyse

### Dans le pipeline CI/CD (GitLab CI)

```yaml
k6-load-test:
  image: grafana/k6:latest
  script:
    - k6 run tests/performance/k6-load-test.js
```

---

## 6. Scripts de Test Fournis

| Fichier | Description |
|---------|-------------|
| `tests/performance/k6-load-test.js` | Test de charge complet : 10→50→100 VUs, palier de 1 min |
| `tests/performance/k6-smoke-test.js` | Test de fumée : 1 VU, 5 itérations, validation basique |
