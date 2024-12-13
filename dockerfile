# Utiliser une image de base avec OpenJDK
FROM openjdk:23-jdk-slim

# Répertoire de travail dans le conteneur
WORKDIR /app

# Copier votre code source dans le conteneur
COPY . /app

# Installer Maven pour gérer la compilation (si nécessaire)
RUN apt-get update && apt-get install -y maven

# Compiler l'application Java (ajoutez si nécessaire, sinon vous pouvez ignorer)
# RUN mvn clean package

# Exposer le port que votre application utilise, si applicable
EXPOSE 8080

# Commande pour exécuter l'application Java (remplacez avec la classe principale de votre app)
CMD ["java", "-cp", "target/classes:target/libs/*", "BDD.DataTransfer"]