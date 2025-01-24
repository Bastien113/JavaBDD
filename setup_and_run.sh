#!/bin/bash

# Variables
MYSQL_CONTAINER_NAME="javabdd-1-mysql-1"
POSTGRES_CONTAINER_NAME="javabdd-1-postgres-1"
MYSQL_USER="admin"
MYSQL_PASSWORD="adminpassword"
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="postgrespassword"
MYSQL_DB="sakila"
POSTGRES_DB="targetdb"
MYSQL_PORT=3307
POSTGRES_PORT=5432
SQL_SCHEMA_PATH="/Users/youss/Documents/JavaBDD-1/1-sakila-schema.sql"
SQL_DATA_PATH="/Users/youss/Documents/JavaBDD-1/2-sakila-data.sql"
LIB_PATH="/Users/youss/Documents/JavaBDD-1/lib"

# Fonction pour attendre que le conteneur soit prêt
wait_for_container() {
  local container_name=$1
  local port=$2
  echo "Waiting for $container_name to be ready..."
  while ! nc -z localhost $port; do
    sleep 1
  done
  echo "$container_name is ready."
}

# Démarrer les conteneurs Docker
echo "Starting Docker containers..."
docker-compose up -d

# Attendre que les conteneurs soient prêts
wait_for_container $MYSQL_CONTAINER_NAME $MYSQL_PORT
wait_for_container $POSTGRES_CONTAINER_NAME $POSTGRES_PORT

# Créer la base de données sakila dans MySQL
echo "Creating sakila database in MySQL..."
docker exec -i $MYSQL_CONTAINER_NAME mysql -u $MYSQL_USER -p$MYSQL_PASSWORD -e "DROP DATABASE IF EXISTS $MYSQL_DB; CREATE DATABASE $MYSQL_DB;"

# Charger le schéma et les données dans la base de données sakila
echo "Loading schema and data into sakila database..."
docker cp $SQL_SCHEMA_PATH $MYSQL_CONTAINER_NAME:/1-sakila-schema.sql
docker cp $SQL_DATA_PATH $MYSQL_CONTAINER_NAME:/2-sakila-data.sql
docker exec -i $MYSQL_CONTAINER_NAME mysql -u $MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DB < /1-sakila-schema.sql
docker exec -i $MYSQL_CONTAINER_NAME mysql -u $MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DB < /2-sakila-data.sql

# Créer la base de données targetdb dans PostgreSQL
echo "Creating targetdb database in PostgreSQL..."
docker exec -i $POSTGRES_CONTAINER_NAME psql -U $POSTGRES_USER -c "DROP DATABASE IF EXISTS $POSTGRES_DB;"
docker exec -i $POSTGRES_CONTAINER_NAME psql -U $POSTGRES_USER -c "CREATE DATABASE $POSTGRES_DB;"

# Créer la table actors dans la base de données targetdb
echo "Creating actors table in targetdb database..."
docker exec -i $POSTGRES_CONTAINER_NAME psql -U $POSTGRES_USER -d $POSTGRES_DB -c "
CREATE TABLE actors (
    actor_id SERIAL PRIMARY KEY,
    first_name VARCHAR(45),
    last_name VARCHAR(45),
    last_update TIMESTAMP
);"

# Compiler et exécuter le programme Java
echo "Compiling and running DataTransfer.java..."
javac -d out -cp $LIB_PATH/mysql-connector-java-8.0.26.jar:$LIB_PATH/postgresql-42.2.23.jar src/main/java/BDD/DataTransfer.java
java -cp out:$LIB_PATH/mysql-connector-java-8.0.26.jar:$LIB_PATH/postgresql-42.2.23.jar BDD.DataTransfer

echo "Script execution completed." b