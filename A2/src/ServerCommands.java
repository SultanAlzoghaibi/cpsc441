import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

// ServerCommands.java
public class ServerCommands {
    public static void handleCommand(
            String receiveMsg,
            Map<Integer, Integer> clientMap,
            ReliableSender sender, // <-- function pointer
            DatagramSocket socket,
            InetAddress clientAddress,
            int clientPort
    ) {
        System.out.println(" HANDLING THE MSG: " + receiveMsg);

        // Your switch-case logic here

        String[] tokens = receiveMsg.trim().split(" ", 3); // max 3 parts
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "connect":

                // Example: connect <client_id>
                if (tokens.length < 2) {
                    System.out.println("Usage: connect <client_id>");
                } else {
                    int targetId = Integer.parseInt(tokens[1]);
                    connectToClient(targetId);
                }
                break;

            case "msg":
                // Example: msg <client_id> <message>
                if (tokens.length < 3) {
                    System.out.println("Usage: msg <client_id> <message>");
                } else {
                    int targetId = Integer.parseInt(tokens[1]);
                    String message = tokens[2];
                    sendReliableMessage(targetId, message);
                }
                break;

            case "terminate":
                if (tokens.length < 2) {
                    System.out.println("Usage: terminate <client_id>");
                } else {
                    int targetId = Integer.parseInt(tokens[1]);
                    terminateChat(targetId);
                }
                break;

            case "chats":
                displayActiveChats();
                break;

            case "leave":
                leaveApplication();
                break;

            case "list":
                requestClientListFromServer();
                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }

    public static void connectToClient(int id) {



    }

    public static void sendReliableMessage(int id, String message) {

    }

    public static void terminateChat(int id) {  }

    public static void displayActiveChats() {  }

    public static void leaveApplication() {  }

    public static void requestClientListFromServer() {

    }
}