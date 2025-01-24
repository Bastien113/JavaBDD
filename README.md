# JavaBDD

## Description

Ce projet JavaBDD est conçu pour transférer des données d'une base de données MySQL vers une base de données PostgreSQL. Le projet utilise JDBC pour se connecter aux bases de données, extraire les données de MySQL et les insérer dans PostgreSQL.

## Étapes pour configurer et exécuter le projet

### 1. Télécharger et extraire le ZIP

### 2. Démarrer les conteneurs Docker

```sh
docker-compose up -d
```

### 3. Créer la table actors dans PostgreSQL et MySQL

```sh
docker exec -i javabdd-1-postgres-1 psql -U postgres -d targetdb -c "
CREATE TABLE actors (
    actor_id SERIAL PRIMARY KEY,
    first_name VARCHAR(45),
    last_name VARCHAR(45),
    last_update TIMESTAMP
);"
```

```sh
docker exec -i javabdd-1-mysql-1 mysql -u admin -padminpassword sakila -e "
CREATE TABLE actor (
    actor_id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(45) NOT NULL,
    last_name VARCHAR(45) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id),
    KEY idx_actor_last_name (last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
```

### 4. Charger les schémas et les données dans MySQL

```sh
docker exec -i javabdd-1-mysql-1 mysql -u admin -padminpassword sakila < 1-sakila-schema.sql
docker exec -i javabdd-1-mysql-1 mysql -u admin -padminpassword sakila < 2-sakila-data.sql
```

### 5. Compiler et exécuter le programme Java

```sh
javac -d out -cp lib/mysql-connector-java-8.0.26.jar:lib/postgresql-42.2.23.jar src/main/java/BDD/DataTransfer.java
java -cp out:lib/mysql-connector-java-8.0.26.jar:lib/postgresql-42.2.23.jar BDD.DataTransfer
```