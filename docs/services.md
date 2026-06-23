# Documentation Technique des Microservices DIGITRANS-CM

---

## Table des Matières

1. [Architecture Générale](#1-architecture-générale)
2. [API Gateway](#2-api-gateway)
3. [ERP Service (Gestion des Ressources Humaines)](#3-erp-service)
4. [CRM Service (SavoirManger Restaurants)](#4-crm-service)
5. [Supply Chain Service (Traçabilité Cacao/Café)](#5-supply-chain-service)
6. [BI Service (Business Intelligence)](#6-bi-service)
7. [Sécurité Transversale](#7-sécurité-transversale)
8. [Schémas de Bases de Données](#8-schémas-de-bases-de-données)
9. [Flux de Données](#9-flux-de-données)

---

## 1. Architecture Générale

### 1.1 Vue d'Ensemble

DIGITRANS-CM est une architecture microservices développée en **Java 21** avec **Spring Boot 3.3.5** pour la filière agroalimentaire camerounaise (cacao, café, restauration). Le système se compose de **5 services** :

| Service | Port | Rôle |
|---------|------|------|
| `api-gateway` | 8080 | Authentification JWT, routage CORS |
| `erp-service` | 8081 | Gestion RH, paie |
| `crm-service` | 8082 | Restaurants « SavoirManger », commandes, offline-first |
| `supply-chain-service` | 8083 | Traçabilité blockchain, plantations, stocks |
| `bi-service` | — | Squelette BI (en cours de développement) |

### 1.2 Stack Technique Commune

- **Langage** : Java 21
- **Framework** : Spring Boot 3.3.5
- **Base de données** : H2 (dev) / PostgreSQL 16 (prod)
- **ORM** : Spring Data JPA / Hibernate 6
- **Sécurité** : JWT HMAC-SHA256 / BCrypt
- **Documentation API** : SpringDoc OpenAPI 2.5.0 (Swagger UI)
- **Validation** : Jakarta Bean Validation
- **Build** : Maven (multi-module parent `com.digitrans:backend-parent`)

### 1.3 Dépendances Transversales (Parent POM)

```xml
<parent>
    <groupId>com.digitrans</groupId>
    <artifactId>backend-parent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>
```

Tous les services (sauf `bi-service`) héritent de ce POM parent qui centralise :
- Spring Boot Starter Web, Security, Data JPA, Validation
- jjwt 0.12.3 (signature et parsing JWT)
- SpringDoc OpenAPI 2.5.0
- Lombok
- H2 (base de développement en mémoire)
- PostgreSQL JDBC Driver (production)
- Spring Boot Starter Test + Spring Security Test

### 1.4 Port Mapping et Profiles

Chaque service expose deux profils Spring :
- **`dev`** (actif par défaut) : H2 en mémoire, `create-drop`
- **`prod`** : PostgreSQL avec schéma géré par Hibernate `update`

| Service | Profil dev (H2) | Profil prod (PostgreSQL) |
|---------|-----------------|--------------------------|
| api-gateway | `jdbc:h2:mem:api_gateway_db` | `jdbc:postgresql://postgres:5432/digitrans_gateway` |
| erp-service | `jdbc:h2:mem:erp_db` | `jdbc:postgresql://postgres:5432/digitrans_erp` |
| crm-service | `jdbc:h2:mem:crm_db` | `jdbc:postgresql://postgres:5432/digitrans_crm` |
| supply-chain-service | `jdbc:h2:mem:supply_db` | `jdbc:postgresql://postgres:5432/digitrans_supply` |

---

## 2. API Gateway

### 2.1 Présentation

L'API Gateway est le point d'entrée unique du système. Il gère l'authentification et la délivrance des jetons JWT. Contrairement aux autres services, il **ne contient pas de base de données JPA** : les utilisateurs sont stockés en mémoire (`ConcurrentHashMap`), ce qui le rend léger et sans état pour le démarrage.

**Package** : `com.digitrans.api_gateway.security`

### 2.2 Fonctionnalités

#### 2.2.1 Authentification

**Inscription** — `POST /api/auth/register`

```json
// Requête
{
    "username": "admin",
    "password": "admin123",
    "role": "ADMIN"
}

// Réponse
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "role": "ADMIN",
    "expiresIn": 86400000
}
```

Flux :
1. Vérification que le `username` n'est pas déjà pris
2. Hachage du mot de passe avec **BCrypt** (coût ~100 ms)
3. Création d'un `UserEntity` implémentant `UserDetails`
4. Génération d'un JWT incluant le rôle dans les **claims** (`extraClaims.put("role", ...)`)
5. Retour du token, du nom d'utilisateur, du rôle et de la durée d'expiration

**Connexion** — `POST /api/auth/login`

```json
// Requête
{
    "username": "admin",
    "password": "admin123"
}
```

Flux :
1. Recherche de l'utilisateur par `username` dans le repository mémoire
2. Vérification du mot de passe avec `BCryptPasswordEncoder.matches()`
3. Génération JWT avec les claims supplémentaires (rôle)
4. Retour du token

#### 2.2.2 Filtre JWT

**Classe** : `JwtAuthenticationFilter` (étend `OncePerRequestFilter`)

Le filtre intercepte **toutes les requêtes entrantes** sauf `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`.

Fluc :
1. Extraction du header `Authorization: Bearer <token>`
2. Parsing du JWT avec `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`
3. Extraction du `subject` (nom d'utilisateur)
4. Chargement du `UserDetails` depuis le `UserDetailsService`
5. Validation de la non-expiration et de la correspondance utilisateur
6. Création du `UsernamePasswordAuthenticationToken` dans le `SecurityContextHolder`

Si le token est invalide ou expiré, une réponse `401 Unauthorized` est retournée avec un timestamp.

#### 2.2.3 Configuration CORS

```java
configuration.setAllowedOrigins(List.of("*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
configuration.setExposedHeaders(List.of("Authorization"));
```

CORS est complètement ouvert (toutes les origines, toutes les méthodes standards) pour permettre le développement frontend.

### 2.3 Architecture de Sécurité

```
Requête → JwtAuthenticationFilter
               │
               ├─ AuthController (si /api/auth/**) → JwtService → token
               │
               └─ Autres endpoints
                       │
                       ├─ Token présent ? → Extraction username → Validation
                       │       │
                       │       ├─ Valide → SecurityContextHolder → Controller
                       │       └─ Invalide → 401 Unauthorized
                       │
                       └─ Token absent → 403 Forbidden
```

### 2.4 Classes et Fichiers

| Fichier | Type | Rôle |
|---------|------|------|
| `AuthController` | Controller | Endpoints `/api/auth/register`, `/api/auth/login` |
| `JwtService` | Service | Génération, parsing et validation des tokens JWT |
| `JwtAuthenticationFilter` | Filter | Interception HTTP, validation JWT, injection SecurityContext |
| `SecurityConfig` | Configuration | `SecurityFilterChain`, `UserDetailsService`, `PasswordEncoder`, CORS |
| `JwtProperties` | Configuration | `jwt.secret`, `jwt.expiration` (via `@ConfigurationProperties`) |
| `SwaggerConfig` | Configuration | OpenAPI avec schéma Bearer JWT |
| `UserEntity` | Model | Implémente `UserDetails` (Spring Security) |
| `UserRole` | Enum | `ADMIN`, `MANAGER`, `EMPLOYEE` |
| `RegisterRequest` | DTO | Record avec `username`, `password`, `role` |
| `LoginRequest` | DTO | Record avec `username`, `password` |
| `AuthResponse` | DTO | Record avec `token`, `username`, `role`, `expiresIn` |
| `InMemoryUserRepository` | Repository | Stockage en mémoire (`ConcurrentHashMap`) |
| `GlobalExceptionHandler` | Handler | Gestion centralisée des exceptions |

### 2.5 Détail du JwtService

**Algorithme** : HMAC-SHA256

**Génération du token** (jjwt 0.12.3 API) :
```java
Jwts.builder()
    .claims(extraClaims)          // claims supplémentaires (rôle)
    .subject(userDetails.getUsername())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + expiration))
    .signWith(getSigningKey(), Jwts.SIG.HS256)
    .compact();
```

**Parsing et validation** :
```java
Jwts.parser()
    .verifyWith(getSigningKey())
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

**Clé secrète** : `digitrans-cm-secret-key-agrocam-2026-camtech-minimum-256bits` (256 bits minimum requis par HMAC-SHA256)

**Expiration** : 24 heures (86 400 000 ms)

---

## 3. ERP Service

### 3.1 Présentation

Le service ERP gère les **ressources humaines** de l'entreprise DIGITRANS-CM : employés, départements, et traitement de la paie mensuelle.

**Package métier** : `com.digitrans.erp_service.hr`  
**Port** : 8081  
**Base** : `erp_db` (H2) / `digitrans_erp` (PostgreSQL)

### 3.2 Fonctionnalités

#### 3.2.1 Gestion des Employés

L'ERP permet de gérer le cycle de vie complet d'un employé :

| Opération | Endpoint | Description |
|-----------|----------|-------------|
| Création | `POST /api/erp/employees` | Crée un employé avec code automatique (`EMP-001`, `EMP-002`...) |
| Liste | `GET /api/erp/employees` | Liste paginée, filtrée par département |
| Recherche | `GET /api/erp/employees/search?name=...` | Recherche par prénom ou nom |
| Détail | `GET /api/erp/employees/{id}` | Détail complet d'un employé |
| Modification | `PUT /api/erp/employees/{id}` | Mise à jour des informations |
| Suppression | `DELETE /api/erp/employees/{id}` | Suppression logique (soft delete : `active = false`) |

**Création d'un employé** — `POST /api/erp/employees`

```json
// Requête
{
    "firstName": "Jean",
    "lastName": "Dupont",
    "email": "jean.dupont@digitrans.com",
    "phone": "+33123456789",
    "department": "CACAO_CAFE",
    "position": "Agronome",
    "baseSalary": 4200.00,
    "hireDate": "2024-05-10",
    "active": true
}

// Réponse
{
    "success": true,
    "message": "Employee created successfully",
    "data": {
        "id": 1,
        "employeeCode": "EMP-001",
        "firstName": "Jean",
        "lastName": "Dupont",
        ...
    },
    "timestamp": "2026-05-21T10:00:00Z"
}
```

**Contraintes métier** :
- L'email est unique (vérification avant création et modification)
- Le code employé est auto-généré au format `EMP-XXX`
- La suppression est logique : `active` passe à `false`

#### 3.2.2 Gestion de la Paie

Le module paie suit un workflow en 3 états : `DRAFT → VALIDATED → PAID`

| Opération | Endpoint | Description |
|-----------|----------|-------------|
| Calcul | `POST /api/erp/payroll/process` | Crée une fiche de paie en brouillon |
| Validation | `PUT /api/erp/payroll/{id}/validate` | Valide la fiche (DRAFT → VALIDATED) |
| Paiement | `PUT /api/erp/payroll/{id}/mark-paid` | Marque comme payée (VALIDATED → PAID) |
| Rapport mensuel | `GET /api/erp/payroll/monthly-report?month=...&year=...` | Liste des paies du mois |

**Calcul de la paie** — `POST /api/erp/payroll/process`

```json
// Requête
{
    "employeeId": 1,
    "month": 5,
    "year": 2026,
    "bonuses": 200.00,
    "deductions": 150.00
}
```

**Règles métier** :
- Un employé ne peut avoir qu'une seule fiche de paie par mois (contrainte d'unicité `employee_id + payroll_month + payroll_year`)
- Le statut `DRAFT` permet encore les modifications
- Seul un `VALIDATED` peut passer à `PAID`
- Le salaire net est calculé : `baseSalary + bonuses - deductions`
- Les montants sont stockés en `BigDecimal` (précision 15, échelle 2)

### 3.3 Modèle de Données

#### Entité Employee

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK, auto-généré | Identifiant unique |
| `employeeCode` | `String` | UNIQUE, NOT NULL | Code formaté `EMP-XXX` |
| `firstName` | `String` | NOT NULL | Prénom |
| `lastName` | `String` | NOT NULL | Nom |
| `email` | `String` | UNIQUE, NOT NULL, `@Email` | Email professionnel |
| `phone` | `String` | — | Numéro de téléphone |
| `department` | `Department` (enum) | NOT NULL | Département (`CACAO_CAFE`, `DISTRIBUTION`, `RESTAURATION`) |
| `position` | `String` | — | Poste occupé |
| `baseSalary` | `BigDecimal` | precision=15, scale=2 | Salaire de base |
| `hireDate` | `LocalDate` | — | Date d'embauche |
| `active` | `Boolean` | Default `true` | Actif ou supprimé logiquement |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` | Date de création |
| `updatedAt` | `LocalDateTime` | `@UpdateTimestamp` | Date de dernière modification |

#### Entité PayrollRecord

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK, auto-généré | Identifiant unique |
| `employee` | `Employee` (ManyToOne) | NOT NULL | Employé concerné |
| `payrollMonth` | `Integer` | NOT NULL | Mois de paie (1-12) |
| `payrollYear` | `Integer` | NOT NULL | Année de paie |
| `baseSalary` | `BigDecimal` | precision=15, scale=2 | Salaire de base (copie) |
| `bonuses` | `BigDecimal` | precision=15, scale=2 | Primes |
| `deductions` | `BigDecimal` | precision=15, scale=2 | Déductions |
| `netSalary` | `BigDecimal` | precision=15, scale=2 | Salaire net calculé |
| `status` | `PayrollStatus` (enum) | NOT NULL | `DRAFT`, `VALIDATED`, `PAID` |
| `processedAt` | `LocalDateTime` | — | Date de traitement |

**Contrainte d'unicité** : `(employee_id, payroll_month, payroll_year)` — nommée `uc_payroll_record_employee_month_year`

### 3.4 Endpoints Complets

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/erp/employees` | Créer un employé |
| `GET` | `/api/erp/employees` | Liste paginée (filtre optionnel `department`) |
| `GET` | `/api/erp/employees/search?name=` | Recherche par nom |
| `GET` | `/api/erp/employees/{id}` | Détail employé |
| `PUT` | `/api/erp/employees/{id}` | Modifier employé |
| `DELETE` | `/api/erp/employees/{id}` | Supprimer (soft) |
| `POST` | `/api/erp/payroll/process` | Calculer paie |
| `PUT` | `/api/erp/payroll/{id}/validate` | Valider paie |
| `PUT` | `/api/erp/payroll/{id}/mark-paid` | Marquer comme payée |
| `GET` | `/api/erp/payroll/monthly-report` | Rapport mensuel (`month`, `year`) |

### 3.5 Classes et Architecture

```
ErpController (Point d'entrée API REST)
    │
    └── EmployeeService (Logique métier)
            │
            ├── EmployeeRepository (JPA)
            │       ├── findByEmail(String)
            │       ├── findByActiveTrue(Pageable)
            │       ├── findByDepartmentAndActiveTrue(Department, Pageable)
            │       ├── findByFirstNameContainingIgnoreCaseAndActiveTrue(String)
            │       ├── findByLastNameContainingIgnoreCaseAndActiveTrue(String)
            │       └── existsByEmployeeCode(String)
            │
            └── PayrollRepository (JPA)
                    ├── findByPayrollMonthAndPayrollYearOrderByEmployee(int, int)
                    └── findByEmployeeIdAndPayrollMonthAndPayrollYear(Long, int, int)
```

### 3.6 Données de Démo (DataSeeder)

```java
// 5 employés pré-injectés au démarrage
1. Jean Dupont — CACAO_CAFE — Agronome
2. Sophie Martin — DISTRIBUTION — Logistics Manager
3. Ali Traoré — RESTAURATION — Chef
4. Amélie Nguyen — CACAO_CAFE — Quality Analyst
5. Marc Bernard — DISTRIBUTION — Fleet Supervisor
```

---

## 4. CRM Service

### 4.1 Présentation

Le CRM « SavoirManger » est le système de gestion de la relation client pour les restaurants DIGITRANS-CM. Il supporte un mode **offline-first** permettant aux restaurants de passer commande même sans connexion Internet, avec synchronisation ultérieure.

**Package métier** : `com.digitrans.crm_service.restaurant`  
**Port** : 8082  
**Base** : `crm_db` (H2) / `digitrans_crm` (PostgreSQL)

### 4.2 Fonctionnalités

#### 4.2.1 Gestion des Restaurants

| Opération | Endpoint |
|-----------|----------|
| Création | `POST /api/crm/restaurants` |
| Liste | `GET /api/crm/restaurants` |
| Filtre par ville | `GET /api/crm/restaurants/by-city/{city}` |
| Détail | `GET /api/crm/restaurants/{id}` |
| Modification | `PUT /api/crm/restaurants/{id}` |
| Suppression | `DELETE /api/crm/restaurants/{id}` (soft delete) |

**Villes supportées** : `DOUALA`, `YAOUNDE`, `BAFOUSSAM`, `GAROUA`, `NGAOUNDERE`

Le restaurant possède une propriété `offlineCapable` (true par défaut) indiquant s'il peut fonctionner hors ligne. La suppression est logique (passe `active = false`).

#### 4.2.2 Gestion des Clients

| Opération | Endpoint |
|-----------|----------|
| Recherche | `GET /api/crm/customers/search?name=` |
| Meilleurs clients | `GET /api/crm/customers/top/{restaurantId}?limit=10` |
| Détail | `GET /api/crm/customers/{id}` |

**Système de fidélité** :
- 1 point de fidélité = 500 FCFA dépensés
- Les points sont cumulés automatiquement à chaque commande
- Le classement des meilleurs clients est basé sur le nombre de points

**Création ou recherche par téléphone** :
La méthode `createOrFindByPhone()` du `CustomerService` est utilisée en interne :
- Si le numéro de téléphone existe déjà, retourne le client existant
- Sinon, crée un nouveau client avec un code formaté `CUST-XXXXXX`

#### 4.2.3 Gestion des Menus

| Opération | Endpoint |
|-----------|----------|
| Création | `POST /api/crm/menu-items` |
| Disponibles par restaurant | `GET /api/crm/menu-items/available/{restaurantId}` |
| Détail | `GET /api/crm/menu-items/{id}` |
| Modification | `PUT /api/crm/menu-items/{id}` |
| Suppression | `DELETE /api/crm/menu-items/{id}` (suppression physique) |

**Catégories de menu** : `PLAT_PRINCIPAL`, `ACCOMPAGNEMENT`, `BOISSON`, `DESSERT`

#### 4.2.4 Gestion des Commandes

| Opération | Endpoint |
|-----------|----------|
| Création (en ligne) | `POST /api/crm/orders?restaurantId=...&customerId=...` |
| Synchronisation offline | `POST /api/crm/orders/sync` |
| Rapport quotidien | `GET /api/crm/orders/restaurant/{restaurantId}/daily?date=...` |
| Mise à jour statut | `PUT /api/crm/orders/{id}/status?status=...` |

**Création de commande en ligne** :

```json
// POST /api/crm/orders?restaurantId=1&customerId=1
// Body:
[
    { "menuItemId": 1, "quantity": 2 },
    { "menuItemId": 2, "quantity": 1 }
]
```

**Flux** :
1. Vérification de l'existence du restaurant et du client (optionnel)
2. Génération d'un numéro de commande : `ORD-20260521-XXXX`
3. Pour chaque item : vérification du `MenuItem`, calcul du sous-total
4. Calcul du montant total
5. Calcul des points de fidélité gagnés (si client renseigné)
6. Création de la commande avec statut `PENDING`, mode `ONLINE`
7. Mise à jour du client : `totalOrders++`, `totalSpent += amount`, `lastVisit = now`, points de fidélité

#### 4.2.5 Mode Offline-First (Fonctionnalité Clé)

Le CRM est conçu pour fonctionner dans des zones à connectivité Internet limitée. Le mécanisme offline-first permet :

```
POS Terminal (hors ligne)                     Serveur DIGITRANS (en ligne)
        │                                              │
        │  1. Commande passée hors ligne               │
        │     (offlineId généré côté client)            │
        │─────────────────────────────────────────────>│
        │                                              │
        │  2. Synchronisation (POST /orders/sync)      │
        │     [{offlineId, restaurantId, items...}]     │
        │─────────────────────────────────────────────>│
        │                                              │
        │  3. Réponse :                                │
        │     {created: 15, skipped: 2, errors: []}    │
        │<─────────────────────────────────────────────│
        │                                              │
        │  4. Si offlineId existe déjà → skipped        │
        │     Si erreur → listée dans errors            │
```

**Endpoint de synchronisation** — `POST /api/crm/orders/sync`

```json
// Requête : tableau de commandes offline
[
    {
        "offlineId": "OFF-20260521-001",
        "restaurantId": 1,
        "customerId": 1,
        "items": [
            { "menuItemId": 1, "quantity": 2 }
        ],
        "totalAmount": 5000.00,
        "orderedAt": "2026-05-21T08:30:00"
    }
]

// Réponse
{
    "success": true,
    "message": "Orders synced successfully",
    "data": {
        "created": 1,
        "skipped": 0,
        "errors": []
    }
}
```

**Garanties** :
- Idempotence : si un `offlineId` existe déjà, la commande est **ignorée** (skipped) et non dupliquée
- Résilience : chaque commande est traitée individuellement ; une erreur sur une commande n'impacte pas les autres
- Traçabilité : les commandes synchronisées ont `orderMode = OFFLINE_SYNC` et conservent leur `offlineId` d'origine

#### 4.2.6 Rapport Quotidien

**Endpoint** — `GET /api/crm/orders/restaurant/{restaurantId}/daily?date=2026-05-21`

```json
// Réponse
{
    "success": true,
    "data": {
        "totalOrders": 42,
        "totalRevenue": 125000.00,
        "averageTicket": 2976.19
    }
}
```

Le rapport calcule par restaurant et par date :
- Nombre total de commandes
- Chiffre d'affaires total
- Ticket moyen (CA ÷ nombre de commandes, arrondi à 2 décimales)

### 4.3 Modèle de Données

#### Entité Restaurant

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `code` | `String` | UNIQUE, NOT NULL | Code restaurant |
| `name` | `String` | NOT NULL | Nom |
| `address` | `String` | — | Adresse |
| `city` | `City` (enum) | NOT NULL | Ville (`DOUALA`, `YAOUNDE`, etc.) |
| `managerName` | `String` | — | Nom du gérant |
| `phone` | `String` | — | Téléphone |
| `email` | `String` | — | Email |
| `active` | `Boolean` | Default `true` | Actif |
| `offlineCapable` | `Boolean` | Default `true` | Mode offline autorisé |
| `lastSyncAt` | `LocalDateTime` | — | Dernière synchronisation |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` | Date de création |

#### Entité Customer

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `customerCode` | `String` | UNIQUE, NOT NULL | Code `CUST-XXXXXX` |
| `firstName` | `String` | — | Prénom |
| `lastName` | `String` | — | Nom |
| `phone` | `String` | UNIQUE, NOT NULL | Téléphone (identifiant métier) |
| `email` | `String` | — | Email |
| `city` | `String` | — | Ville |
| `loyaltyPoints` | `Integer` | Default `0` | Points de fidélité |
| `totalOrders` | `Integer` | Default `0` | Nombre total de commandes |
| `totalSpent` | `BigDecimal` | Default `0` | Montant total dépensé |
| `lastVisit` | `LocalDateTime` | — | Dernière visite |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` | Date de création |

#### Entité MenuItem

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `code` | `String` | NOT NULL | Code article |
| `name` | `String` | NOT NULL | Nom |
| `description` | `String` | — | Description |
| `category` | `MenuItem_Category` (enum) | NOT NULL | Catégorie |
| `price` | `BigDecimal` | precision=10, scale=0, NOT NULL | Prix |
| `available` | `Boolean` | Default `true` | Disponible |
| `restaurant` | `Restaurant` (ManyToOne) | NOT NULL | Restaurant associé |

#### Entité Order

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `orderNumber` | `String` | UNIQUE, NOT NULL | `ORD-YYYYMMDD-XXXX` |
| `customer` | `Customer` (ManyToOne) | — | Client (nullable pour anonyme) |
| `restaurant` | `Restaurant` (ManyToOne) | NOT NULL | Restaurant |
| `status` | `OrderStatus` (enum) | NOT NULL | `PENDING`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED` |
| `orderMode` | `OrderMode` (enum) | NOT NULL | `ONLINE`, `OFFLINE_SYNC` |
| `offlineId` | `String` | UNIQUE | Identifiant offline client |
| `totalAmount` | `BigDecimal` | precision=15, scale=2 | Montant total |
| `loyaltyPointsEarned` | `Integer` | — | Points gagnés |
| `orderedAt` | `LocalDateTime` | NOT NULL | Date de commande |
| `deliveredAt` | `LocalDateTime` | — | Date de livraison |
| `items` | `List<OrderItem>` | OneToMany, Cascade ALL | Lignes de commande |

#### Entité OrderItem

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `order` | `Order` (ManyToOne) | NOT NULL | Commande parente |
| `menuItem` | `MenuItem` (ManyToOne) | NOT NULL | Article commandé |
| `quantity` | `Integer` | — | Quantité |
| `unitPrice` | `BigDecimal` | — | Prix unitaire (copie) |
| `subtotal` | `BigDecimal` | — | Sous-total |

### 4.4 Workflow des Statuts de Commande

```
PENDING ──> PREPARING ──> READY ──> DELIVERED
    │                                      │
    └──────────> CANCELLED <───────────────┘
```

- `PENDING` : commande créée, en attente de préparation
- `PREPARING` : en cours de préparation
- `READY` : prête à être servie
- `DELIVERED` : servie au client (déclenche `deliveredAt = now()`)
- `CANCELLED` : annulée

### 4.5 Classes et Architecture

```
CrmController (Point d'entrée API REST)
    │
    ├── RestaurantService
    │       └── RestaurantRepository
    │
    ├── CustomerService
    │       ├── createOrFindByPhone(phone, firstName, lastName, city)
    │       ├── addLoyaltyPoints(customerId, amount)
    │       └── CustomerRepository
    │
    ├── MenuItemService
    │       └── MenuItemRepository
    │
    └── OrderService
            ├── createOrder(restaurantId, customerId, items)
            ├── syncOfflineOrders(requests)
            ├── updateStatus(orderId, newStatus)
            └── getDailyReport(restaurantId, date)
            └── OrderRepository
            └── MenuItemRepository
            └── CustomerRepository
```

---

## 5. Supply Chain Service

### 5.1 Présentation

Le service Supply Chain est le cœur de la **traçabilité numérique** de la filière cacao/café. Il assure le suivi complet de la récolte à l'exportation avec une **simulation blockchain** (hachage SHA-256) garantissant l'intégrité des données.

**Package métier** : `com.digitrans.supply_service.supply`  
**Port** : 8083  
**Base** : `supply_db` (H2) / `digitrans_supply` (PostgreSQL)

### 5.2 Fonctionnalités

#### 5.2.1 Gestion des Plantations

| Opération | Endpoint |
|-----------|----------|
| Création | `POST /api/supply/plantations` |
| Liste | `GET /api/supply/plantations` |
| Détail | `GET /api/supply/plantations/{id}` |
| Modification | `PUT /api/supply/plantations/{id}` |
| Suppression | `DELETE /api/supply/plantations/{id}` |

**Types de produit** : `CACAO`, `CAFE`

**Coordonnées géographiques** : stockées sous forme de chaîne libre (ex: `"3.8667, 11.5167"`) pour flexibilité.

#### 5.2.2 Gestion des Lots de Récolte

| Opération | Endpoint |
|-----------|----------|
| Création | `POST /api/supply/batches` |
| Traçabilité complète | `GET /api/supply/batches/{batchCode}/trace` |
| Vérification d'intégrité | `GET /api/supply/batches/{batchCode}/verify` |
| Mise à jour statut | `PUT /api/supply/batches/{batchCode}/status` |
| Historique par plantation | `GET /api/supply/plantations/{id}/batches?from=...&to=...` |

**Création d'un lot** — `POST /api/supply/batches`

```json
// Requête
{
    "plantationId": 1,
    "harvestDate": "2026-05-21",
    "quantityKg": 500,
    "qualityGrade": "A",
    "currentLocation": "Nkolbisson"
}

// Réponse
{
    "success": true,
    "data": {
        "id": 1,
        "batchCode": "BATCH-2026-001",
        "plantation": { ... },
        "harvestDate": "2026-05-21",
        "quantityKg": 500,
        "qualityGrade": "A",
        "status": "HARVESTED",
        "currentLocation": "Nkolbisson",
        "blockchainTxHash": "0x7c4f... (64 hex chars)",
        "createdAt": "2026-05-21T10:00:00"
    }
}
```

**Flux de création** :
1. Récupération de la plantation par `plantationId`
2. Génération du code lot : `BATCH-YYYY-SEQ`
3. Génération d'un faux hash blockchain : `0x` + UUID.randomUUID() + UUID.randomUUID() (64 caractères hexadécimaux)
4. Création de l'entité `HarvestBatch` avec statut `HARVESTED`
5. Création automatique d'un événement de traçabilité `HARVEST`
6. Retour du lot complet

**Grades de qualité** : `A`, `B`, `C`  
**Statuts d'un lot** : `HARVESTED → IN_TRANSIT → AT_WAREHOUSE → PROCESSED → EXPORTED`

#### 5.2.3 Traçabilité et Vérification d'Intégrité

**Traçabilité complète** — `GET /api/supply/batches/{batchCode}/trace`

```json
{
    "success": true,
    "data": {
        "batchId": 1,
        "batchCode": "BATCH-2026-001",
        "plantationId": 1,
        "plantationName": "Plantation Mbanga",
        "harvestDate": "2026-05-21",
        "quantityKg": 500,
        "qualityGrade": "A",
        "status": "AT_WAREHOUSE",
        "currentLocation": "Entrepôt Douala",
        "blockchainTxHash": "0x7c4f...",
        "createdAt": "2026-05-21T10:00:00",
        "events": [
            {
                "id": 1,
                "eventType": "HARVEST",
                "location": "Nkolbisson",
                "operatorName": "System",
                "eventAt": "2026-05-21T10:00:00",
                "metadata": null,
                "hashSignature": "a3f8b2..."
            },
            {
                "id": 2,
                "eventType": "TRANSPORT_START",
                "location": "Nkolbisson",
                "operatorName": "Jean Transport",
                "eventAt": "2026-05-21T14:30:00",
                "metadata": null,
                "hashSignature": "b4e7c1..."
            }
        ],
        "verificationStatus": "ALL_VALID"
    }
}
```

**Vérification d'intégrité** — `GET /api/supply/batches/{batchCode}/verify`

```json
{
    "success": true,
    "data": {
        "isValid": true,
        "status": "ALL_VALID",
        "totalEvents": 2,
        "validEvents": 2,
        "message": "2/2 events verified"
    }
}
```

#### 5.2.4 Mise à Jour du Statut

**Endpoint** — `PUT /api/supply/batches/{batchCode}/status`

```json
// Requête
{
    "status": "IN_TRANSIT",
    "location": "En transit vers Douala",
    "operatorName": "Jean Transport"
}
```

La mise à jour de statut **déclenche automatiquement** la création d'un événement de traçabilité correspondant :

| Nouveau statut | Événement créé |
|----------------|----------------|
| `IN_TRANSIT` | `TRANSPORT_START` |
| `AT_WAREHOUSE` | `WAREHOUSE_ARRIVAL` |
| `PROCESSED` | `PROCESSING` |
| `EXPORTED` | `EXPORT_READY` |

#### 5.2.5 Gestion des Stocks d'Entrepôt

| Opération | Endpoint |
|-----------|----------|
| Réception | `POST /api/supply/warehouse/stock` |
| Réservation | `PUT /api/supply/warehouse/stock/{stockId}/reserve` |
| Expédition | `PUT /api/supply/warehouse/stock/{stockId}/dispatch` |
| Rapport de stock | `GET /api/supply/warehouse/stock` |

**Workflow des stocks** : `AVAILABLE → RESERVED → DISPATCHED`

```json
// POST /api/supply/warehouse/stock
{
    "warehouseCode": "DLA-01",
    "warehouseName": "Entrepôt Douala",
    "location": "Douala",
    "batchId": 1,
    "quantityKg": 500,
    "expiryDate": "2026-12-31"
}
```

**Rapport de stock** : retourne la quantité totale disponible par entrepôt, le total général et le nombre d'entrepôts actifs.

#### 5.2.6 Événements de Traçabilité

**Endpoint** — `GET /api/supply/events?batchCode=BATCH-2026-001`

Retourne la liste de tous les événements de traçabilité pour un lot donné, ordonnés par date croissante.

### 5.3 Mécanisme d'Intégrité (SHA-256)

Le système simule une blockchain à travers un chaînage cryptographique des événements :

```
Lot: BATCH-2026-001
    │
    ├── Événement 1 (HARVEST)
    │     hash = SHA-256("BATCH-2026-001|HARVEST|Nkolbisson|2026-05-21T10:00:00.000")
    │
    ├── Événement 2 (TRANSPORT_START)
    │     hash = SHA-256("BATCH-2026-001|TRANSPORT_START|Nkolbisson|2026-05-21T14:30:00.000")
    │
    └── ...
```

**Calcul du hash** :
```java
String input = batchCode + "|" + eventType + "|" + location + "|" + eventAt.truncatedTo(ChronoUnit.MILLIS);
byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(UTF_8));
```

**Détection de falsification** : La vérification recompute le hash de chaque événement et le compare au hash stocké. Si un seul événement a été modifié, la validation échoue (`TAMPERED_DETECTED`).

**Précision temporelle** : `eventAt` est tronqué à la milliseconde (`truncatedTo(ChronoUnit.MILLIS)`) avant le calcul du hash pour garantir la cohérence entre Java (nanosecondes) et H2/PostgreSQL (microsecondes).

### 5.4 Modèle de Données

#### Entité Plantation

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `code` | `String` | UNIQUE, NOT NULL | Code plantation |
| `name` | `String` | NOT NULL | Nom |
| `region` | `String` | — | Région |
| `ownerName` | `String` | — | Propriétaire |
| `productType` | `ProductType` (enum) | NOT NULL | `CACAO` ou `CAFE` |
| `surfaceHectares` | `Double` | — | Surface en hectares |
| `active` | `Boolean` | Default `true` | Active |
| `geoCoordinates` | `String` | — | Coordonnées GPS |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` | Date de création |

#### Entité HarvestBatch

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `batchCode` | `String` | UNIQUE, NOT NULL | Code lot `BATCH-YYYY-SEQ` |
| `plantation` | `Plantation` (ManyToOne) | NOT NULL | Plantation d'origine |
| `harvestDate` | `LocalDate` | NOT NULL | Date de récolte |
| `quantityKg` | `Integer` | NOT NULL | Quantité en kg |
| `qualityGrade` | `QualityGrade` (enum) | NOT NULL | Grade A, B ou C |
| `status` | `BatchStatus` (enum) | NOT NULL | HARVESTED → ... → EXPORTED |
| `currentLocation` | `String` | — | Localisation actuelle |
| `notes` | `String` (TEXT) | — | Notes |
| `blockchainTxHash` | `String` | — | Hash blockchain simulé |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` | Date de création |

#### Entité WarehouseStock

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `warehouseCode` | `String` | — | Code entrepôt |
| `warehouseName` | `String` | — | Nom entrepôt |
| `location` | `String` | — | Localisation |
| `batch` | `HarvestBatch` (ManyToOne) | NOT NULL | Lot associé |
| `quantityKg` | `Integer` | NOT NULL | Quantité en kg |
| `arrivalDate` | `LocalDate` | — | Date d'arrivée |
| `expiryDate` | `LocalDate` | — | Date d'expiration |
| `status` | `WarehouseStockStatus` (enum) | NOT NULL | `AVAILABLE`, `RESERVED`, `DISPATCHED` |

#### Entité TraceabilityEvent

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Identifiant unique |
| `batch` | `HarvestBatch` (ManyToOne) | NOT NULL | Lot concerné |
| `eventType` | `EventType` (enum) | NOT NULL | `HARVEST`, `QUALITY_CHECK`, `TRANSPORT_START`, `WAREHOUSE_ARRIVAL`, `PROCESSING`, `EXPORT_READY` |
| `location` | `String` | — | Lieu de l'événement |
| `operatorName` | `String` | — | Opérateur |
| `eventAt` | `LocalDateTime` | NOT NULL | Date/heure de l'événement |
| `metadata` | `String` (TEXT) | — | Métadonnées libres |
| `hashSignature` | `String` | — | Signature SHA-256 |

### 5.5 Workflow Complet de Traçabilité

```
1. RÉCOLTE
   Plantation → Création du lot (HARVESTED)
   │   hash blockchain généré
   │   Événement: HARVEST
   │
2. TRANSPORT
   Mise à jour statut → IN_TRANSIT
   │   Événement: TRANSPORT_START
   │
3. ARRIVÉE ENTREPÔT
   Mise à jour statut → AT_WAREHOUSE
   │   Événement: WAREHOUSE_ARRIVAL
   │   Réception du stock dans l'entrepôt
   │
4. TRANSFORMATION
   Mise à jour statut → PROCESSED
   │   Événement: PROCESSING
   │
5. EXPORTATION
   Mise à jour statut → EXPORTED
   │   Événement: EXPORT_READY
   │
6. VÉRIFICATION
   GET /verify → Recalcul SHA-256 de tous les événements
   │   Résultat: ALL_VALID ou TAMPERED_DETECTED
```

### 5.6 Classes et Architecture

```
BatchController (Endpoint: /api/supply/batches)
├── HarvestService
│       ├── createBatch()
│       ├── updateStatus()
│       ├── getFullTraceability()
│       └── getByPlantationAndDateRange()
│
PlantationController (Endpoint: /api/supply/plantations)
│
WarehouseController (Endpoint: /api/supply/warehouse/stock)
├── WarehouseService
│       ├── receiveStock()
│       ├── reserveStock()
│       ├── dispatchStock()
│       └── getStockReport()
│
EventController (Endpoint: /api/supply/events)
└── TraceabilityService
        ├── addEvent()
        ├── computeHash()     ← SHA-256
        ├── verifyIntegrity() ← Détection falsification
        └── getEventResponses()
```

---

## 6. BI Service

### 6.1 Présentation

Le service BI est un **squelette d'application** prévu pour le reporting et la business intelligence. Il est actuellement en phase initiale de développement.

**Package** : `com.digitrans.bi_service`  
**Port** : 8080 (par défaut — conflit potentiel avec api-gateway)

### 6.2 État Actuel

Le service ne contient qu'une classe principale (`BiServiceApplication`) :
- Aucun contrôleur REST
- Aucune entité JPA
- Aucun service métier
- Aucune couche de sécurité

### 6.3 Spécificités Techniques

- **Parent** : Spring Boot 4.0.6 (standalone, pas sous `backend-parent`)
- **Dépendances** : `spring-boot-starter-webmvc`, PostgreSQL, Lombok
- **Pas de JWT** ni d'authentification configurée

### 6.4 Architecture Cible (Recommandée)

Le service BI est destiné à évoluer vers :
- Agrégation de données depuis les 4 autres services
- Rapports statistiques (KPI métier : volumes récoltés, CA par restaurant, etc.)
- Tableaux de bord via API REST dédiée
- Export CSV/Excel des rapports

---

## 7. Sécurité Transversale

### 7.1 Architecture JWT

Tous les services partagent le même **schéma de sécurité JWT** :

```
                    Secret HMAC partagé
                    "digitrans-cm-secret-key-..."
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   api-gateway        erp-service      crm-service    supply-chain-service
   (JWT issu)        (JWT vérifié)    (JWT vérifié)    (JWT vérifié)
          │                │                │                │
          └────────────────┴────────────────┴────────────────┘
                      Même secret → tokens interopérables
```

Chaque service possède **sa propre copie des classes de sécurité** (pas de module partagé) :

| Classe | Rôle |
|--------|------|
| `JwtService` | Génération, extraction, validation |
| `JwtAuthenticationFilter` | Interception HTTP, parsing JWT |
| `SecurityConfig` | `SecurityFilterChain`, CORS, BCrypt |
| `JwtProperties` | Configuration `jwt.secret`, `jwt.expiration` |
| `SwaggerConfig` | Documentation OpenAPI avec Bearer JWT |

### 7.2 Points d'Entrée Publics

Les endpoints suivants sont accessibles sans authentification :
- `POST /api/auth/register` (inscription)
- `POST /api/auth/login` (connexion)
- `/swagger-ui/**`, `/v3/api-docs/**` (documentation Swagger)

Tous les autres endpoints nécessitent un JWT valide.

### 7.3 Gestion des Erreurs

Chaque service implémente `GlobalExceptionHandler` avec `@RestControllerAdvice` :

| Exception | Code HTTP | Format |
|-----------|-----------|--------|
| `MethodArgumentNotValidException` | 400 | `{timestamp, status, error, errors}` |
| `UsernameNotFoundException` | 404 | `{timestamp, status, error, message}` |
| `RuntimeException` (générique) | 500 | `{timestamp, status, error, message}` |
| Token invalide/absent | 401 | `{"error":"Unauthorized","timestamp":"..."}` |

---

## 8. Schémas de Bases de Données

### 8.1 api-gateway

Aucune table JPA. Les utilisateurs sont stockés en mémoire (`ConcurrentHashMap`).

### 8.2 erp-service

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_code VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255),
    department VARCHAR(255) NOT NULL,
    position VARCHAR(255),
    base_salary DECIMAL(15,2),
    hire_date DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE payroll_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    payroll_month INTEGER NOT NULL,
    payroll_year INTEGER NOT NULL,
    base_salary DECIMAL(15,2),
    bonuses DECIMAL(15,2),
    deductions DECIMAL(15,2),
    net_salary DECIMAL(15,2),
    status VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP,
    UNIQUE (employee_id, payroll_month, payroll_year),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);
```

### 8.3 crm-service

```sql
CREATE TABLE restaurants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(255) NOT NULL,
    manager_name VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    offline_capable BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    city VARCHAR(255),
    loyalty_points INTEGER DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    total_spent DECIMAL(15,2) DEFAULT 0,
    last_visit TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE menu_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    category VARCHAR(255) NOT NULL,
    price DECIMAL(10,0) NOT NULL,
    available BOOLEAN DEFAULT TRUE,
    restaurant_id BIGINT NOT NULL,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id BIGINT,
    restaurant_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    order_mode VARCHAR(255) NOT NULL,
    offline_id VARCHAR(255) UNIQUE,
    total_amount DECIMAL(15,2),
    loyalty_points_earned INTEGER,
    ordered_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    quantity INTEGER,
    unit_price DECIMAL(15,2),
    subtotal DECIMAL(15,2),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);
```

### 8.4 supply-chain-service

```sql
CREATE TABLE plantations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(255),
    owner_name VARCHAR(255),
    product_type VARCHAR(255) NOT NULL,
    surface_hectares DOUBLE,
    active BOOLEAN DEFAULT TRUE,
    geo_coordinates VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE harvest_batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_code VARCHAR(255) NOT NULL UNIQUE,
    plantation_id BIGINT NOT NULL,
    harvest_date DATE NOT NULL,
    quantity_kg INTEGER NOT NULL,
    quality_grade VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    current_location VARCHAR(255),
    notes TEXT,
    blockchain_tx_hash VARCHAR(255),
    created_at TIMESTAMP,
    FOREIGN KEY (plantation_id) REFERENCES plantations(id)
);

CREATE TABLE warehouse_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(255),
    warehouse_name VARCHAR(255),
    location VARCHAR(255),
    batch_id BIGINT NOT NULL,
    quantity_kg INTEGER NOT NULL,
    arrival_date DATE,
    expiry_date DATE,
    status VARCHAR(255) NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES harvest_batches(id)
);

CREATE TABLE traceability_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    operator_name VARCHAR(255),
    event_at TIMESTAMP NOT NULL,
    metadata TEXT,
    hash_signature VARCHAR(255),
    FOREIGN KEY (batch_id) REFERENCES harvest_batches(id)
);
```

---

## 9. Flux de Données

### 9.1 Parcours Complet (Farm-to-Fork)

```
PLANTATION (cacao/café)
    │
    ├── 1. Enregistrement plantation
    │        → supply-chain-service (POST /api/supply/plantations)
    │
    ├── 2. Récolte → création du lot
    │        → supply-chain-service (POST /api/supply/batches)
    │        → Événement HARVEST + hash blockchain (SHA-256)
    │
    ├── 3. Transport → mise à jour statut
    │        → supply-chain-service (PUT /api/supply/batches/{code}/status)
    │        → Événement TRANSPORT_START
    │
    ├── 4. Arrivée entrepôt
    │        → supply-chain-service : réception stock
    │        → Événement WAREHOUSE_ARRIVAL
    │
    ├── 5. Transformation
    │        → Événement PROCESSING
    │
    ├── 6. Exportation
    │        → Événement EXPORT_READY
    │
    ├── 7. Vérification traçabilité
    │        → GET /api/supply/batches/{code}/verify
    │        → ALL_VALID ou TAMPERED_DETECTED
    │
    └── 8. Mise à disposition restaurant
             → crm-service : création menu items
             → crm-service : commande client
             → crm-service : synchronisation offline
```

### 9.2 Flux d'Authentification (JWT)

```
Client
    │
    ├── POST /api/auth/register
    │       → BCrypt hash du mot de passe
    │       → Stockage en mémoire (ou base)
    │       → Génération JWT (HMAC-SHA256, 24h)
    │       → Retour du token
    │
    ├── POST /api/auth/login
    │       → Vérification BCrypt
    │       → Génération JWT
    │       → Retour du token
    │
    └── Requête authentifiée (Authorization: Bearer <token>)
            → JwtAuthenticationFilter
            → Parsing JWT (vérification HMAC + expiration)
            → Vérification cohérence UserDetails
            → Injection SecurityContext
            → Accès à la ressource
```

### 9.3 Flux Offline-First (CRM)

```
Restaurant (hors ligne)
    │
    ├── 1. Client passe commande
    │        → Génération d'un offlineId côté POS
    │        → Commande stockée localement
    │
    └── 2. Connexion rétablie
             → POST /api/crm/orders/sync
             → Pour chaque commande :
                 ├── offlineId existe ? → skipped
                 └── offlineId nouveau ? → création
             → Retour du rapport (created/skipped/errors)
```

### 9.4 Flux de Paie (ERP)

```
RH
    │
    ├── 1. Saisie des employés
    │        → POST /api/erp/employees
    │        → Code auto-généré (EMP-XXX)
    │
    ├── 2. Fin de mois : traitement paie
    │        → POST /api/erp/payroll/process
    │        → Statut DRAFT
    │        → Calcul : net = base + primes - déductions
    │
    ├── 3. Validation comptable
    │        → PUT /api/erp/payroll/{id}/validate
    │        → Statut VALIDATED
    │
    └── 4. Paiement
             → PUT /api/erp/payroll/{id}/mark-paid
             → Statut PAID
             → Rapport mensuel disponible
```

---

## Index des Fichiers Sources par Service

### api-gateway (11 fichiers)
- `security/AuthController.java`
- `security/AuthResponse.java` (record)
- `security/GlobalExceptionHandler.java`
- `security/InMemoryUserRepository.java`
- `security/JwtAuthenticationFilter.java`
- `security/JwtProperties.java`
- `security/JwtService.java`
- `security/LoginRequest.java` (record)
- `security/RegisterRequest.java` (record)
- `security/SecurityConfig.java`
- `security/SwaggerConfig.java`
- `security/UserEntity.java`
- `security/UserRole.java` (enum)
- `ApiGatewayApplication.java`

### erp-service (14 fichiers)
- `hr/DataSeeder.java`
- `hr/controller/ErpController.java`
- `hr/dto/ApiResponse.java` (record)
- `hr/dto/EmployeeRequest.java` (record)
- `hr/dto/EmployeeResponse.java` (record)
- `hr/dto/PagedResponse.java` (record)
- `hr/dto/PayrollRequest.java` (record)
- `hr/dto/PayrollResponse.java` (record)
- `hr/entity/Department.java` (enum)
- `hr/entity/Employee.java`
- `hr/entity/PayrollRecord.java`
- `hr/entity/PayrollStatus.java` (enum)
- `hr/repository/EmployeeRepository.java`
- `hr/repository/PayrollRepository.java`
- `hr/service/EmployeeService.java`

### crm-service (21 fichiers métier + 13 fichiers sécurité)
- `restaurant/controller/CrmController.java`
- `restaurant/dto/*.java` (9 records)
- `restaurant/entity/*.java` (7 fichiers : 5 entités + 2 énums)
- `restaurant/repository/*.java` (4 repositories)
- `restaurant/service/*.java` (4 services)
- `restaurant/CrmDataSeeder.java`

### supply-chain-service (17 fichiers métier + 13 fichiers sécurité)
- `supply/controller/*.java` (4 controllers)
- `supply/dto/*.java` (6 fichiers : ApiResponse, TraceabilityResponse, EventResponse, StockReport, VerificationResult)
- `supply/entity/*.java` (8 fichiers : 4 entités + 4 énums)
- `supply/repository/*.java` (4 repositories)
- `supply/service/*.java` (3 services)
- `config/DataSeeder.java`

### bi-service (1 fichier)
- `BiServiceApplication.java`

---

*Documentation générée le 21 mai 2026 — DIGITRANS-CM v1.0*
