package BDD;

import java.sql.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataTransfer {
    // Configuration des bases de données
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/sakila";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "admin1234";

    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:5432/targetdb";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "admin1234";

    // Queues pour transférer différents types de données
    private static final ConcurrentLinkedQueue<String> actorQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> filmQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        // Démarrage des threads pour extraction et insertion
        Thread actorExtractionThread = new Thread(new ActorDataExtractor());
        Thread filmExtractionThread = new Thread(new FilmDataExtractor());
        Thread actorInsertionThread = new Thread(new ActorDataInserter());
        Thread filmInsertionThread = new Thread(new FilmDataInserter());

        // Lancer les threads
        actorExtractionThread.start();
        filmExtractionThread.start();
        actorInsertionThread.start();
        filmInsertionThread.start();
    }

    // Classe pour extraire des données d'acteurs de MySQL
    static class ActorDataExtractor implements Runnable {
        @Override
        public void run() {
            try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
                 Statement stmt = mysqlConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT first_name, last_name FROM actor LIMIT 10")) {

                // Parcourir les résultats et ajouter chaque acteur dans la queue
                while (rs.next()) {
                    String actorData = rs.getString("first_name") + " " + rs.getString("last_name");
                    actorQueue.add(actorData);
                    System.out.println("Extracted Actor: " + actorData);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Classe pour extraire des données de films de MySQL
    static class FilmDataExtractor implements Runnable {
        @Override
        public void run() {
            try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
                 Statement stmt = mysqlConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, description FROM film LIMIT 10")) {

                // Parcourir les résultats et ajouter chaque film dans la queue
                while (rs.next()) {
                    String filmData = rs.getString("title") + " - " + rs.getString("description");
                    filmQueue.add(filmData);
                    System.out.println("Extracted Film: " + filmData);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Classe pour insérer des données d'acteurs dans PostgreSQL
    static class ActorDataInserter implements Runnable {
        @Override
        public void run() {
            try (Connection postgresConnection = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
                 PreparedStatement pstmt = postgresConnection.prepareStatement("INSERT INTO actors (full_name) VALUES (?)")) {

                while (true) {
                    // Récupérer les données d'un acteur de la queue et les insérer dans PostgreSQL
                    String actorData = actorQueue.poll();
                    if (actorData != null) {
                        pstmt.setString(1, actorData);
                        pstmt.executeUpdate();
                        System.out.println("Inserted Actor: " + actorData);
                    } else {
                        Thread.sleep(100); // Attendre si la queue est vide
                    }
                }
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Classe pour insérer des données de films dans PostgreSQL
    static class FilmDataInserter implements Runnable {
        @Override
        public void run() {
            try (Connection postgresConnection = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
                 PreparedStatement pstmt = postgresConnection.prepareStatement("INSERT INTO films (title_description) VALUES (?)")) {

                while (true) {
                    // Récupérer les données d'un film de la queue et les insérer dans PostgreSQL
                    String filmData = filmQueue.poll();
                    if (filmData != null) {
                        pstmt.setString(1, filmData);
                        pstmt.executeUpdate();
                        System.out.println("Inserted Film: " + filmData);
                    } else {
                        Thread.sleep(100); // Attendre si la queue est vide
                    }
                }
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
