package BDD;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataTransfer {
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3307/sakila";
    private static final String MYSQL_USER = "admin";
    private static final String MYSQL_PASSWORD = "adminpassword";
    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:5432/targetdb";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "postgrespassword";
    private static final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private static int totalRecordsExtracted = 0;

    public static void main(String[] args) {
        Thread receiverThread = new Thread(new TacheReceptrice());
        receiverThread.start();

        Thread emitterThread = new Thread(new TacheEmettrice());
        emitterThread.start();
    }

    // Tâche Émettrice (extraction des données de MySQL et envoi dans le canal)
    static class TacheEmettrice implements Runnable {
        @Override
        public void run() {
            try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
                 Statement stmt = mysqlConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT actor_id, first_name, last_name, last_update FROM actor")) {

                System.out.println("Connexion à la base de données MySQL réussie.");

                if (!rs.isBeforeFirst()) {
                    System.out.println("Aucune donnée trouvée dans la table 'actor'.");
                }

                while (rs.next()) {
                    int actorId = rs.getInt("actor_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String lastUpdate = rs.getString("last_update");
                    String actorData = actorId + "," + firstName + "," + lastName + "," + lastUpdate;
                    System.out.println("Données extraites: " + actorData);
                    messageQueue.put(new MessageDeDonnees(actorData));
                    System.out.println("Envoyé: " + actorData);
                    totalRecordsExtracted++;
                }

                // Envoyer un message de commande pour indiquer la fin de l'extraction
                messageQueue.put(new MessageDeCommande());
            } catch (SQLException e) {
                System.out.println("Erreur SQL: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Tâche Réceptrice (réception des données du canal et insertion dans PostgreSQL)
    static class TacheReceptrice implements Runnable {
        @Override
        public void run() {
            try (Connection postgresConnection = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);
                 Statement stmt = postgresConnection.createStatement()) {

                System.out.println("Connexion à la base de données PostgreSQL réussie.");

                while (true) {
                    Message message = messageQueue.take();
                    if (message instanceof MessageDeCommande) {
                        System.out.println("Tâche Émettrice terminée. Fin de la réception.");
                        verifyDataIntegrity(postgresConnection);
                        break;
                    } else if (message instanceof MessageDeDonnees) {
                        String data = ((MessageDeDonnees) message).toString();
                        System.out.println("Message reçu: " + data);
                        String[] parts = data.split(",");
                        int actorId = Integer.parseInt(parts[0]);
                        String firstName = parts[1];
                        String lastName = parts[2];
                        String lastUpdate = parts[3];
                        stmt.executeUpdate("INSERT INTO actors (actor_id, first_name, last_name, last_update) VALUES (" + actorId + ", '" + firstName + "', '" + lastName + "', '" + lastUpdate + "') ON CONFLICT (actor_id) DO NOTHING");
                        System.out.println("Données insérées: " + data);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Erreur SQL: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void verifyDataIntegrity(Connection postgresConnection) {
            try (Statement stmt = postgresConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM actors")) {
                if (rs.next()) {
                    int totalRecordsInserted = rs.getInt(1);
                    if (totalRecordsInserted == totalRecordsExtracted) {
                        System.out.println("Toutes les données ont été correctement insérées dans PostgreSQL.");
                    } else {
                        System.out.println("Erreur : le nombre de données insérées ne correspond pas au nombre de données extraites.");
                        System.out.println("Données extraites : " + totalRecordsExtracted);
                        System.out.println("Données insérées : " + totalRecordsInserted);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Erreur lors de la vérification de l'intégrité des données : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Classes de messages
    static class Message {}
    static class MessageDeCommande extends Message {}
    static class MessageDeDonnees extends Message {
        private final String data;

        public MessageDeDonnees(String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return data;
        }
    }
}