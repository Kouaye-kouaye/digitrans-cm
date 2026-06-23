# Instructions pour déployer sur GitHub

Votre dépôt Git local est maintenant initialisé et prêt à être poussé sur GitHub.

## Étape 1 : Créer un dépôt sur GitHub

1. Allez sur https://github.com/new
2. Remplissez les informations :
   - **Repository name** : `digitrans-cm`
   - **Description** : `DIGITRANS-CM - Plateforme microservices de gestion agricole intégrée`
   - **Visibility** : Public (ou Private si vous le préférez)
   - **Initialize this repository** : NE COCHEZ PAS (vous avez déjà un repo local)
3. Cliquez **Create repository**

## Étape 2 : Ajouter l'URL distante

Remplacez `<YOUR_USERNAME>` par votre nom d'utilisateur GitHub, puis exécutez :

```bash
cd "C:\Users\KOUAYE ORNELLE\Desktop\COCOMO\Exam\backend\backend"
git remote add origin https://github.com/<YOUR_USERNAME>/digitrans-cm.git
git branch -M main
git push -u origin main
```

### Exemple complet :

Si votre username GitHub est `johnsmith` :

```bash
git remote add origin https://github.com/johnsmith/digitrans-cm.git
git branch -M main
git push -u origin main
```

### Authentification

GitHub va vous demander de vous authentifier. Vous avez 2 options :

**Option A : Token Personal Access (Recommandé)**
1. Allez sur https://github.com/settings/tokens
2. Cliquez **Generate new token (classic)**
3. Donnez-lui un nom : `digitrans-cm-push`
4. Cochez les scopes : `repo`, `workflow`
5. Cliquez **Generate token**
6. Copiez le token
7. Quand Git demande le mot de passe, collez le token

**Option B : Clé SSH**
1. Générez une clé SSH : `ssh-keygen -t ed25519 -C "your_email@example.com"`
2. Ajoutez-la à votre compte GitHub : https://github.com/settings/keys
3. Utilisez l'URL SSH : `git@github.com:username/digitrans-cm.git`

## Étape 3 : Vérifier le push

```bash
git remote -v
```

Vous devriez voir :
```
origin  https://github.com/<YOUR_USERNAME>/digitrans-cm.git (fetch)
origin  https://github.com/<YOUR_USERNAME>/digitrans-cm.git (push)
```

## Étape 4 : Consulter votre dépôt

Allez sur : `https://github.com/<YOUR_USERNAME>/digitrans-cm`

---

## Commits futurs

Une fois poussé, pour les commits futurs :

```bash
git add .
git commit -m "Description du changement"
git push origin main
```

---

## État actuel du dépôt local

```
Dépôt : C:\Users\KOUAYE ORNELLE\Desktop\COCOMO\Exam\backend\backend
Branch : master (à renommer en main)
Last commit : e5328ce - Initial commit: DIGITRANS-CM microservices platform with JWT security, blockchain traceability, and cloud integration
Fichiers : 78 fichiers ajoutés
```

---

## Fichiers importants du projet

```
digitrans-cm/
├── README.md                                    # Documentation générale
├── docker-compose.yml                           # Lancement local complet
├── pom.xml                                      # Maven parent POM
├── PRESENTATION_JURY.md                         # Présentation détaillée
├── cloud_integration_documentation.txt          # Guide cloud + blockchain
├── RAPPORT_SUPERVISION.md                       # Monitoring et métriques
├── api-gateway/                                 # Service JWT + Routage (port 8080)
├── erp-service/                                 # Gestion RH/Paie (port 8081)
├── crm-service/                                 # Restauration/Commandes (port 8082)
├── supply-chain-service/                        # Traçabilité cacao/café (port 8083)
├── bi-service/                                  # BI (futur)
├── monitoring/                                  # Prometheus, Grafana, Loki
├── docs/                                        # Architecture, services
├── tests/                                       # Tests de charge (k6)
└── e2e/                                         # Tests end-to-end (Playwright)
```

---

## Protection du dépôt (optionnel)

Pour plus de sécurité, vous pouvez configurer les protections :

1. Allez sur **Settings** → **Branches**
2. Cliquez **Add rule**
3. Réglez les protections (ex: require pull request reviews)

---

## Lignes de commande pratiques après le push

### Cloner le projet ailleurs
```bash
git clone https://github.com/<YOUR_USERNAME>/digitrans-cm.git
cd digitrans-cm
```

### Vérifier l'historique
```bash
git log --oneline --graph --all
```

### Ajouter des contributeurs
Allez sur **Settings** → **Collaborators** et invitez d'autres utilisateurs

---

## Support et documentation

- **API Documentation** : `/swagger-ui/**` une fois l'application déployée
- **Architecture** : `docs/architecture.md`
- **Monitoring** : `RAPPORT_SUPERVISION.md`
- **Présentation** : `PRESENTATION_JURY.md`

---

**Besoin d'aide ?** Consultez la [documentation GitHub](https://docs.github.com/en/get-started/quickstart/set-up-git)
