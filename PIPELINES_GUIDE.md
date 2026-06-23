# Guide Complet des Pipelines CI/CD GitHub Actions - DIGITRANS-CM

## 📋 Vue d'ensemble

Le projet DIGITRANS-CM inclut 3 pipelines CI/CD automatisés :

1. **build-test-deploy.yml** — Build, tests unitaires, tests d'intégration, tests E2E
2. **performance-testing.yml** — Tests de charge, audit de sécurité, SAST scan
3. **deploy-cloud.yml** — Déploiement Kubernetes et vérification de santé

---

## 1. Pipeline Build & Test (`build-test-deploy.yml`)

### Déclenchement

```yaml
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
```

**Se déclenche sur :**
- Chaque push vers `main` ou `develop`
- Chaque pull request vers ces branches

### Étapes

#### 1.1 Build et Tests Unitaires

```bash
git add .
git commit -m "New feature: add blockchain verification"
git push origin feature/blockchain
```

→ Pipeline se déclenche automatiquement
→ Maven compile le code
→ Tests unitaires exécutés
→ Résultats disponibles dans l'onglet **Actions**

#### 1.2 Build Docker

**Pour chaque service :**
- `api-gateway`
- `erp-service`
- `crm-service`
- `supply-chain-service`

Les images Docker sont construites et poussées dans `ghcr.io` (GitHub Container Registry).

#### 1.3 Scan de Sécurité

- **Trivy** : scanne les vulnérabilités dans le code source et les dépendances
- **OWASP Dependency Check** : vérifie les dépendances connues comme vulnérables
- Résultats disponibles dans l'onglet **Security** du repo

#### 1.4 Tests d'Intégration

```yaml
services:
  postgres:
    image: postgres:16
```

Tests lancés contre une vraie PostgreSQL en conteneur.

#### 1.5 Tests E2E (End-to-End)

Utilise **Playwright** pour tester les scénarios métier complets :
```bash
npx playwright test
```

### Configuration des Secrets

Pour que le pipeline fonctionne, configurez ces secrets dans GitHub :

1. Allez sur votre repo : **Settings** → **Secrets and variables** → **Actions**
2. Cliquez **New repository secret**
3. Ajoutez :

```
SONAR_TOKEN                 # Token SonarCloud (optionnel)
GITHUB_TOKEN               # Généré automatiquement par GitHub
```

### Accéder aux résultats

1. Allez sur : **Actions** (onglet de votre repo GitHub)
2. Cliquez sur un workflow pour voir les détails
3. Consultez :
   - **Logs** des étapes
   - **Artifacts** (fichiers générés)
   - **Annotations** (erreurs/warnings)

---

## 2. Pipeline Performance & Load Testing (`performance-testing.yml`)

### Déclenchement

```yaml
on:
  schedule:
    - cron: '0 2 * * *'  # Quotidien à 2 AM UTC
  workflow_dispatch      # Manuel
```

**Se déclenche :**
- Tous les jours à 2h UTC
- Manuellement via **Actions** → **Performance & Load Testing** → **Run workflow**

### Étapes

#### 2.1 Tests de Charge avec k6

**Test de fumée (smoke test) :**
```javascript
// tests/performance/k6-smoke-test.js
import http from 'k6/http';
import { check } from 'k6';

export default function () {
  let res = http.get(__ENV.BASE_URL + '/actuator/health');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

**Test de charge :**
- 50 utilisateurs virtuels
- 5 minutes de duration
- Mesure : latence, throughput, erreurs

#### 2.2 Audit de Sécurité

- Recherche des secrets (mots de passe, tokens, clés)
- Audit des dépendances npm
- Résultats stockés en artifacts

#### 2.3 SAST (Static Application Security Testing)

- **SpotBugs** : détecte les bugs potentiels en Java
- **Checkstyle** : applique les standards de code

### Résultats

Les rapports sont disponibles dans **Actions** → **Artifacts** sous :
- `k6-results` — résultats de charge
- `final-report` — rapport récapitulatif

---

## 3. Pipeline Déploiement Cloud (`deploy-cloud.yml`)

### Déclenchement

```yaml
on:
  push:
    tags:
      - 'v*'               # Tags de version (v1.0.0, v2.1.3)
  workflow_dispatch        # Manuel avec paramètres
```

**Se déclenche sur :**
- Création d'un tag `v*` (ex: `v1.0.0`)
- Déclenchement manuel avec choix d'environnement (staging/production)

### Étapes

#### 3.1 Déploiement Kubernetes

**Prérequis (à configurer une seule fois) :**

1. Créer un cluster Kubernetes (GCP GKE, AWS EKS, Azure AKS)
2. Créer un service account GCP avec accès au cluster
3. Encoder la clé en base64

```bash
cat /path/to/gcp-sa-key.json | base64
```

4. Ajouter en secrets GitHub :
   - `GCP_SA_KEY` : clé encodée
   - `GCP_PROJECT_ID` : ID du projet
   - `GCP_ZONE` : zone du cluster

#### 3.2 Build et Push des images

Les images Docker sont construites avec le tag du commit :
```
ghcr.io/kouaye-kouaye/digitrans-cm/api-gateway:sha-abcd1234
```

#### 3.3 Application des manifests Kubernetes

```yaml
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
```

#### 3.4 Vérification de santé

Le pipeline vérifie que les services démarrés correctement :
```bash
kubectl rollout status deployment/api-gateway -n digitrans --timeout=5m
```

#### 3.5 Rollback automatique

Si une étape échoue, rollback automatique :
```bash
kubectl rollout undo deployment/api-gateway
```

---

## 4. Créer une release et déclencher le déploiement

### Via terminal

```bash
# À partir de votre local
cd C:\Users\KOUAYE ORNELLE\Desktop\COCOMO\Exam\backend\backend

# Créer un tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Pousser le tag
git push origin v1.0.0
```

→ Pipeline `deploy-cloud.yml` se déclenche automatiquement

### Via GitHub Web

1. Allez sur votre repo : https://github.com/Kouaye-kouaye/digitrans-cm
2. Onglet **Releases**
3. Cliquez **Create a new release**
4. Remplissez :
   - Tag version : `v1.0.0`
   - Release title : `DIGITRANS-CM v1.0.0`
   - Description : notes de version
5. Cliquez **Publish release**

→ Pipeline se déclenche

---

## 5. Configuration des envs (Staging & Production)

### Dans GitHub

1. **Settings** → **Environments**
2. Cliquez **New environment**
3. Créez 2 environments :
   - `staging`
   - `production`

Pour chaque environment :
1. Ajoutez des **Deployment branches** (qui peuvent déclencher)
2. Configurez des **Secrets** spécifiques à l'env

Exemple pour `production` :
```
SECRET_ENV_PROD_DB_PASSWORD
SECRET_ENV_PROD_JWT_KEY
SECRET_ENV_PROD_BLOCKCHAIN_API_KEY
```

---

## 6. Fichiers Kubernetes à créer

Créez la structure `k8s/` :

```
k8s/
├── namespaces/
│   └── digitrans-namespace.yml
├── secrets/
│   └── secrets.yml
├── configmaps/
│   └── app-config.yml
├── services/
│   ├── api-gateway-service.yml
│   ├── erp-service-service.yml
│   ├── crm-service-service.yml
│   └── supply-chain-service-service.yml
└── deployments/
    ├── api-gateway-deployment.yml
    ├── erp-service-deployment.yml
    ├── crm-service-deployment.yml
    └── supply-chain-service-deployment.yml
```

### Exemple : api-gateway-deployment.yml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: digitrans
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: IMAGE_REGISTRY/api-gateway:IMAGE_TAG
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: database-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## 7. Surveillance des pipelines

### Dashboard GitHub

1. Allez sur **Actions**
2. Consultez :
   - **Workflow runs** : historique d'exécution
   - **Artifacts** : fichiers générés
   - **Workflow summary** : succès/échecs

### Notifications Slack (optionnel)

Ajoutez un secret `SLACK_WEBHOOK` et les étapes du pipeline enverront des notifications.

```yaml
- uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: 'Deployment to production completed'
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 8. Troubleshooting courants

### Problem 1 : Pipeline échoue au build Maven

**Solution :**
```bash
# Vérifiez pom.xml localement
mvn clean package -DskipTests

# Consultez les logs du workflow
```

### Problem 2 : Docker images non trouvées

**Solution :**
```bash
# Vérifiez les Dockerfile
ls -la api-gateway/Dockerfile

# Testez build local
docker build -f api-gateway/Dockerfile -t test:latest .
```

### Problem 3 : Déploiement Kubernetes échoue

**Solution :**
1. Vérifiez les credentials GCP
2. Testez la connexion cluster :
   ```bash
   gcloud container clusters get-credentials CLUSTER_NAME
   kubectl get nodes
   ```

### Problem 4 : Tests échouent

**Solution :**
1. Consultez les logs du pipeline
2. Reproduisez localement :
   ```bash
   mvn test
   npx playwright test
   ```

---

## 9. Améliorations futures

- [ ] Ajouter un env `review` pour les branches de feature
- [ ] Ajouter des notifications email
- [ ] Intégrer Snyk pour la détection de vulnérabilités
- [ ] Ajouter un step d'analyse de qualité SonarQube
- [ ] Automatiser les releases via semantic-release
- [ ] Ajouter des tests de chaos (Gremlin)

---

## 10. Checklist avant production

- [ ] Tous les secrets GitHub configurés
- [ ] Kubernetes cluster créé et accessible
- [ ] Namespaces et configmaps Kubernetes créés
- [ ] Pipeline d'autres branches testées
- [ ] Rollback procedure documentée
- [ ] Monitoring/alertes configurés en production
- [ ] Logs centralisés (Loki/CloudWatch) opérationnels

---

## Ressources utiles

- **GitHub Actions Docs** : https://docs.github.com/en/actions
- **Kubernetes Docs** : https://kubernetes.io/docs/
- **k6 Load Testing** : https://k6.io/docs/
- **Trivy Scanning** : https://aquasecurity.github.io/trivy/
- **Docker Build Action** : https://github.com/docker/build-push-action

