# Java 21  avec Alpine Linux(leger)
FROM eclipse-temurin:21-jre-alpine 

# Créer un groupe et un utilisateur système non-privilégié ("app") pour la sécurité
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Copier le fichier .jar compilé depuis la machine hôte(DAns mon cas c le conteneur jenkins-test) vers le conteneur
COPY target/*.jar app.jar

# Basculer sur l'utilisateur non-root pour exécuter l'application en toute sécurité
USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["Ibrahim@expert", "Ordinateur@naif"]