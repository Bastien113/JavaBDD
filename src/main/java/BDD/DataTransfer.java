package BDD;

import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingDeque;

// Message contenant des données
class MessageDeDonnees {
    String data;

    public MessageDeDonnees(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}

// Message de commande pour signaler la fin
class MessageDeCommande {
    // Un simple marqueur pour signaler la fin de l'envoi des données
}

public class DataTransfer {
    // URLs de connexion MySQL et PostgreSQL
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3307/sakila";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "rootpassword";
    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:5432/targetdb";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "postgrespassword";

    // Canal de communication entre les tâches
    private static final LinkedBlockingDeque<Object> messageQueue = new LinkedBlockingDeque<>();

    public static void main(String[] args) {
        // Scanner pour contrôler l'exécution
        Scanner scanner = new Scanner(System.in);

        // Création des threads pour l'émetteur et le récepteur
        Thread emitterThread = new Thread(new TacheEmettrice());
        Thread receiverThread = new Thread(new TacheReceptrice());

        // Démarrer les threads
        emitterThread.start();
        receiverThread.start();

        // Attendre une entrée pour terminer l'exécution
        System.out.println("Appuyez sur Entrée pour arrêter...");
        scanner.nextLine();

        try {
            // Envoyer un message de commande pour signaler la fin de l'envoi
            messageQueue.put(new MessageDeCommande());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Tâche Émettrice (extraction des données de MySQL et envoi dans le canal)
    static class TacheEmettrice implements Runnable {
        @Override
        public void run() {
            try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
                 Statement stmt = mysqlConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT first_name, last_name FROM actor LIMIT 10")) {

                // Parcourir les résultats et envoyer chaque acteur dans la queue
                while (rs.next()) {
                    String actorData = rs.getString("first_name") + " " + rs.getString("last_name");
                    messageQueue.put(new MessageDeDonnees(actorData)); // Envoi des données
                    System.out.println("Envoyé: " + actorData);
                }
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Tâche Réceptrice (réception des messages et insertion dans PostgreSQL)
    static class TacheReceptrice implements Runnable {
        @Override
        public void run() {
            try (Connection postgresConnection = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
                 PreparedStatement pstmt = postgresConnection.prepareStatement("INSERT INTO actors (full_name) VALUES (?)")) {

                while (true) {
                    Object message = messageQueue.take(); // Prendre un message de la queue

                    if (message instanceof MessageDeCommande) {
                        // Si c'est un MessageDeCommande, la Tâche Émettrice a fini d'envoyer les données
                        System.out.println("Tâche Émettrice terminée. Fin de la réception.");
                        break;
                    } else if (message instanceof MessageDeDonnees) {
                        // Si c'est un MessageDeDonnees, insérer les données dans PostgreSQL
                        String actorData = ((MessageDeDonnees) message).getData();
                        pstmt.setString(1, actorData);
                        pstmt.executeUpdate();
                        System.out.println("Inséré dans PostgreSQL: " + actorData);
                    }
                }
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}