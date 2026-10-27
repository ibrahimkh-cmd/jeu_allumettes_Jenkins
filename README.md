#  Jeu des allumettes — Pipeline CI/CD Jenkins

Pipeline **CI/CD complet avec Jenkins** pour une application Java (Maven) : à chaque `git push`, le code est compilé, testé (92 tests JUnit 5), packagé en `.jar`, conteneurisé avec Docker, vérifié par un smoke test, puis publié sur **Docker Hub** — sans aucune action manuelle.

L'application est le **jeu des 13 allumettes** (projet Java réalisé à l'ENSEEIHT) : deux joueurs retirent à tour de rôle 1 à 3 allumettes, celui qui prend la dernière perd.

 Image publiée : [`ikl0934/jeu-allumettes`](https://hub.docker.com/r/ikl0934/jeu-allumettes)

---

## Sommaire

- [Vue d'ensemble du pipeline](#vue-densemble-du-pipeline)
- [Stack technique](#stack-technique)
- [Arborescence du repo](#arborescence-du-repo)
- [Lancer l'application en local](#lancer-lapplication-en-local)
- [Installer Jenkins (dans Docker)](#installer-jenkins-dans-docker)
- [Configurer Jenkins](#configurer-jenkins)
- [Le pipeline étape par étape](#le-pipeline-étape-par-étape)
- [Sécurité](#sécurité)
- [Limites et améliorations](#limites-et-améliorations)

---

## Vue d'ensemble du pipeline

```
 git push (GitHub)
       │
       ▼   Jenkins vérifie le repo toutes les ~2 min (pollSCM)
┌──────────────────────────────────────────────────────────────────────────┐
│  Build  →  Test  →  Package  →  Docker Build  →  Smoke Test  →  Push    │
│  mvn       mvn      mvn         docker build     docker run     Docker  │
│  clean     test     package                      (une partie)   Hub     │
│  compile   (92)     (.jar)                                               │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼   post : rapport JUnit + archivage du .jar
```

Si une étape échoue (ex. un test cassé), **le pipeline s'arrête** : aucune image n'est construite ni publiée à partir d'un code défectueux.

---

## Stack technique

| Outil | Rôle |
|-------|------|
| **Java 8+ / Maven 3.9** | Langage et outil de build (dépendances, compilation, tests, `.jar`) |
| **JUnit 5** | Tests unitaires (tests répétés et paramétrés) |
| **Jenkins (LTS, JDK 21)** | Serveur d'intégration continue, lancé dans un conteneur Docker |
| **Docker** | Conteneurisation de l'application et de Jenkins |
| **Docker Hub** | Registre où l'image est publiée |
| **GitHub** | Hébergement du code, source du pipeline |

---

## Arborescence du repo

```
.
├── src/
│   ├── main/java/allumettes/      # Code de l'application
│   │   ├── Jouer.java             # Point d'entrée (main) : lit les arguments, lance la partie
│   │   ├── Arbitre.java           # Fait jouer les joueurs à tour de rôle, détecte la triche
│   │   ├── Jeu.java               # Interface du jeu (PRISE_MAX = 3)
│   │   ├── JeuClassique.java      # Implémentation : 13 allumettes, vérifie les coups
│   │   ├── JeuProcurration.java   # Proxy en lecture seule : empêche un joueur de tricher
│   │   ├── Joueur.java            # Un nom + une stratégie
│   │   ├── Strategie*.java        # Stratégies : Naif, Rapide, Experte, Humaine, Triche
│   │   └── *Exception.java        # CoupInvalide, OperationInterdite, Configuration
│   └── test/java/allumettes/      # 8 classes de tests JUnit 5 (92 tests)
├── pom.xml                        # Configuration Maven (dépendances JUnit, plugins, main class)
├── Dockerfile                     # Image de l'application (JRE 21 Alpine, utilisateur non-root)
├── Jenkinsfile                    # Définition du pipeline CI/CD (versionnée avec le code)
├── jenkins-docker/
│   └── Dockerfile                 # Image Jenkins custom avec le client Docker
├── .gitignore                     # Ignore target/ (fichiers générés)
└── README.md
```

> `target/` n'est **pas** versionné : il est régénéré à chaque build (`mvn clean`).

---

## Lancer l'application en local

### Avec Maven

```bash
mvn clean package          # compile, lance les tests, crée target/jeu-allumettes-1.0.jar
java -jar target/jeu-allumettes-1.0.jar Ibrahim@humain Ordinateur@expert
```

### Avec Docker (image publiée)

```bash
docker run --rm -it ikl0934/jeu-allumettes Ibrahim@humain Ordinateur@expert
```

- `--rm` : supprime le conteneur à la fin de la partie
- `-it` : mode interactif, nécessaire pour la stratégie `humain` (saisie clavier)

### Arguments

```
java -jar app.jar [-confiant] joueur1 joueur2
       joueur = nom@strategie
       strategie = naif | rapide | expert | humain | tricheur
```

| Stratégie | Comportement |
|-----------|--------------|
| `naif` | Prend un nombre aléatoire entre 1 et 3 |
| `rapide` | Prend toujours le maximum possible |
| `expert` | Stratégie gagnante : laisse à l'adversaire un nombre de la forme 4k+1 |
| `humain` | Demande au clavier (taper `triche` pour tricher…) |
| `tricheur` | Essaie de retirer des allumettes en douce |
| `-confiant` | L'arbitre donne le vrai jeu aux joueurs (la triche n'est plus détectée) |

---

## Installer Jenkins (dans Docker)

Jenkins tourne dans un conteneur. Il doit pouvoir lancer des commandes `docker` (build et push de l'image) : on lui ajoute donc le **client Docker** et on lui donne accès au **daemon Docker de la machine hôte**.

### 1. L'image Jenkins custom — `jenkins-docker/Dockerfile`

```dockerfile
FROM jenkins/jenkins:lts-jdk21
USER root
# Client Docker uniquement (le daemon reste celui de l'hôte)
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
USER jenkins
```

| Ligne | Explication |
|-------|-------------|
| `FROM jenkins/jenkins:lts-jdk21` | Image officielle Jenkins, version LTS (stable), avec Java 21 |
| `USER root` | Passe en root le temps de copier un binaire dans `/usr/local/bin` |
| `COPY --from=docker:27-cli ...` | Récupère uniquement le binaire `docker` depuis l'image officielle du client Docker |
| `USER jenkins` | Revient à l'utilisateur non privilégié : Jenkins ne tourne **pas** en root |

### 2. Build et lancement

```bash
cd jenkins-docker
docker build -t jenkins-docker .

docker run -d --name jenkins -p 8080:8080 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add $(stat -c '%g' /var/run/docker.sock) \
  jenkins-docker
```

| Option | Explication |
|--------|-------------|
| `-d` | Lance le conteneur en arrière-plan |
| `--name jenkins` | Nom du conteneur (pour `docker logs jenkins`, `docker exec jenkins ...`) |
| `-p 8080:8080` | Interface web accessible sur http://localhost:8080 |
| `-v jenkins_home:/var/jenkins_home` | Volume persistant : jobs, config, plugins et credentials survivent à la suppression du conteneur |
| `-v /var/run/docker.sock:...` | Monte le socket Docker : Jenkins pilote le Docker de l'hôte |
| `--group-add $(stat -c '%g' /var/run/docker.sock)` | Ajoute l'utilisateur `jenkins` au groupe propriétaire du socket → droit d'utiliser Docker **sans être root** |

### 3. Vérifications

```bash
docker exec jenkins docker ps      # doit lister les conteneurs (pas de "permission denied")
docker logs jenkins                # affiche le mot de passe admin initial
```

---

## Configurer Jenkins

1. **Premier lancement** : http://localhost:8080 → coller le mot de passe initial → *Install suggested plugins* → créer le compte admin.
2. **Maven** : *Administrer Jenkins → Tools → Installations Maven* → Ajouter
   - Nom : `maven3` (référencé dans le Jenkinsfile)
   - Cocher *Installer automatiquement* (3.9.x)
3. **Credential Docker Hub** : *Administrer Jenkins → Credentials → System → Global → Add Credentials*
   - Type : *Nom d'utilisateur et mot de passe*
   - Username : utilisateur Docker Hub
   - Password : **token d'accès** Docker Hub (*Read & Write*), jamais le mot de passe du compte
   - ID : `dockerhub` (référencé dans le Jenkinsfile)
4. **Job** : *Nouveau Item* → type **Pipeline**
   - Definition : *Pipeline script from SCM*
   - SCM : Git → URL du repo, branche `*/main`
   - Script Path : `Jenkinsfile`
5. Lancer un premier **Build Now** à la main (active le déclencheur `pollSCM`).

---

## Le pipeline étape par étape

Le pipeline est écrit en **pipeline déclaratif** dans le `Jenkinsfile`, versionné avec le code : toute modification du pipeline passe par Git, comme le code.

### En-tête

```groovy
pipeline {
    agent any
    tools {
        maven 'maven3'
    }
    triggers {
        pollSCM('H/2 * * * *')
    }
```

| Élément | Explication |
|---------|-------------|
| `agent any` | Exécute le pipeline sur n'importe quel nœud disponible (ici le contrôleur Jenkins) |
| `tools { maven 'maven3' }` | Jenkins installe Maven (configuré dans *Tools*) et l'ajoute au `PATH` |
| `pollSCM('H/2 * * * *')` | Syntaxe cron : Jenkins interroge GitHub toutes les ~2 minutes et lance un build s'il y a un nouveau commit. `H` répartit la charge entre les jobs |

> **Pourquoi du polling et pas un webhook ?** Un webhook (GitHub prévient Jenkins à chaque push) est plus réactif, mais GitHub ne peut pas joindre un Jenkins sur `localhost`. En production, avec un Jenkins accessible, on utiliserait un webhook.

### 1. Build

```groovy
stage('Build') {
    steps { sh 'mvn -B clean compile' }
}
```

- `clean` : supprime `target/` → on repart de zéro à chaque build (**build reproductible**, aucun fichier d'un ancien build ne traîne)
- `compile` : compile `src/main/java` vers `target/classes`
- `-B` (*batch mode*) : sortie non interactive, adaptée aux logs de CI

### 2. Test

```groovy
stage('Test') {
    steps { sh 'mvn -B test' }
}
```

Lance les **92 tests JUnit 5** via le plugin Maven Surefire, qui écrit ses rapports XML dans `target/surefire-reports/`. Si un test échoue, `mvn` renvoie un code d'erreur → le stage passe en rouge → le pipeline s'arrête.

| Classe de test | Ce qui est testé |
|----------------|------------------|
| `JeuClassiqueTest` | Retraits valides, coups invalides (< 1, > 3, plus que le reste) |
| `JeuProcurrationTest` | Le proxy laisse lire mais interdit de retirer |
| `StrategieRapideTest` | Prend toujours le maximum |
| `StrategieExperteTest` | Laisse une position perdante (tests **paramétrés**) |
| `StrategieNaifTest` | Prise toujours entre 1 et 3 (test **répété 50 fois**, car aléatoire) |
| `StrategieTricheTest` | Triche sur un vrai jeu, détectée via le proxy |
| `JoueurTest` | Délégation à la stratégie |
| `ArbitreTest` | Parties complètes (qui gagne, triche avec arbitre confiant) |

### 3. Package

```groovy
stage('Package') {
    steps { sh 'mvn -B package -DskipTests' }
}
```

Crée le `.jar` exécutable `target/jeu-allumettes-1.0.jar` (la classe `main` est déclarée dans le `pom.xml`). `-DskipTests` évite de relancer les tests, déjà validés au stage précédent.

### 4. Docker Build

```groovy
stage('Docker Build') {
    steps { sh 'docker build -t jeu-allumettes:${BUILD_NUMBER} .' }
}
```

Construit l'image à partir du `Dockerfile`. `${BUILD_NUMBER}` est une variable fournie par Jenkins (numéro du build) : chaque build produit une image avec un **tag unique** et traçable.

Le `Dockerfile` de l'application :

```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY target/*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["Ibrahim@expert", "Ordinateur@naif"]
```

| Ligne | Explication |
|-------|-------------|
| `FROM eclipse-temurin:21-jre-alpine` | JRE seule (pas de JDK, pas de Maven) sur Alpine → image légère |
| `RUN addgroup ... adduser ...` | Crée un utilisateur système `app` |
| `WORKDIR /app` | Répertoire de travail dans le conteneur |
| `COPY target/*.jar app.jar` | Copie le `.jar` produit par le stage *Package* |
| `USER app` | L'application ne tourne **pas** en root |
| `ENTRYPOINT [...]` | Commande toujours exécutée au démarrage |
| `CMD [...]` | Arguments par défaut, remplaçables au `docker run` |

### 5. Smoke Test

```groovy
stage('Smoke Test') {
    steps { sh 'docker run --rm jeu-allumettes:${BUILD_NUMBER} A@expert B@rapide' }
}
```

Lance une **vraie partie** dans le conteneur (deux stratégies automatiques, sans clavier). Si l'image est cassée (jar manquant, mauvaise version de Java…), la commande échoue et l'image n'est pas publiée.

### 6. Push

```groovy
stage('Push') {
    steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                          usernameVariable: 'DOCKER_USER',
                                          passwordVariable: 'DOCKER_PASS')]) {
            sh '''
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                docker tag jeu-allumettes:$BUILD_NUMBER $DOCKER_USER/jeu-allumettes:$BUILD_NUMBER
                docker tag jeu-allumettes:$BUILD_NUMBER $DOCKER_USER/jeu-allumettes:latest
                docker push $DOCKER_USER/jeu-allumettes:$BUILD_NUMBER
                docker push $DOCKER_USER/jeu-allumettes:latest
                docker logout
            '''
        }
    }
}
```

| Élément | Explication |
|---------|-------------|
| `withCredentials(...)` | Injecte le credential `dockerhub` dans des variables d'environnement, **uniquement le temps du bloc**. Le token est masqué (`****`) dans les logs |
| `--password-stdin` | Le token passe par l'entrée standard, pas en argument (sinon visible dans `ps` / l'historique) |
| `'''...'''` (guillemets simples) | C'est le **shell** qui lit `$DOCKER_PASS`, pas Groovy → le secret n'est pas interpolé dans le script |
| `docker tag` | Renomme l'image au format `utilisateur/image:tag` attendu par Docker Hub |
| Tag `:<numéro>` + `:latest` | Le numéro permet de savoir quel build a produit l'image ; `latest` pointe vers la dernière version |
| `docker logout` | Supprime le token stocké dans `~/.docker/config.json` après usage |

### Post-actions

```groovy
post {
    always  { junit 'target/surefire-reports/*.xml' }
    success { archiveArtifacts artifacts: 'target/*.jar' }
}
```

| Bloc | Explication |
|------|-------------|
| `always` | S'exécute que le build réussisse ou échoue : publie les résultats de tests (onglet **Tests** du build, historique et tendance) |
| `success` | Seulement si tout est vert : archive le `.jar`, téléchargeable depuis la page du build |

---

## Sécurité

| Point | Choix fait |
|-------|-----------|
| Secrets | Token Docker Hub stocké dans les **Credentials Jenkins**, jamais dans le code ni dans Git ; masqué dans les logs |
| Token vs mot de passe | Token d'accès révocable avec des droits limités, plutôt que le mot de passe du compte |
| Jenkins non-root | Jenkins tourne avec l'utilisateur `jenkins` ; l'accès Docker est donné par `--group-add` (**principe du moindre privilège**) plutôt que `-u root` |
| Image applicative non-root | L'application tourne avec l'utilisateur `app` dans le conteneur |
|  Socket Docker monté | Donner accès au socket Docker équivaut à un accès root sur l'hôte. Acceptable pour un lab local, **pas en production** (voir améliorations) |

---

## Limites et améliorations

- **Webhook GitHub** au lieu du polling, avec un Jenkins exposé (ou via un tunnel).
- **Agents de build dédiés** : le contrôleur Jenkins ne devrait pas exécuter de builds ; les builds tourneraient sur des agents jetables (VM ou conteneurs).
- **Builds d'images sans daemon** (Kaniko, Buildah, BuildKit rootless) pour supprimer le montage du socket Docker.
- **Dockerfile multi-stage** (build Maven + runtime JRE) pour que l'image soit reproductible indépendamment de la CI.
- **Passer le projet en Java 21** (`maven.compiler.release`) pour supprimer les warnings de compilation Java 8.
- **Analyse statique** (Checkstyle, SpotBugs) et **couverture de tests** (JaCoCo) dans le pipeline.
- **BuildKit / buildx** à la place du builder Docker legacy (déprécié).
- **Migration vers GitLab CI** : même pipeline réécrit en `.gitlab-ci.yml` (projet séparé).

---

## Auteur

**Ibrahim KHELIL** — Élève ingénieur à l'ENSEEIHT 
