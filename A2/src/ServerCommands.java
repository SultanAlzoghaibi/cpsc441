import java.net.*;
import java.util.HashMap;
import java.util.Map;
import  java.util.Random;

// ServerCommands.java
public class ServerCommands {

    public static void handleCommand(
            String receiveMsg,
            Map<Integer, Integer> clientMap,
            ReliableSender sender, // <-- function pointer
            DatagramSocket clientSocket,
            InetAddress clientAddress,
            int clientPort,
            int ourClientId
    ) {
        System.out.println(" HANDLING THE MSG: " + receiveMsg);

        // Your switch-case logic here

        String[] tokens = receiveMsg.trim().split(":", 8); // max 3 parts
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "connect":

                // Example: connect <client_id>
                if (tokens.length < 2) {
                    System.out.println("Usage: connect <client_id>");
                } else {
                    int targetId = Integer.parseInt(tokens[1]);
                    connectToClient(targetId, clientMap, clientSocket, clientAddress, sender, ourClientId);

                }
            case "msg":
                // Example: msg <client_id> <message>
                if (tokens.length < 3) {
                    System.out.println("Usage: msg <client_id> <message>");
                } else {
                    int targetId = Integer.parseInt(tokens[1]);
                    String message = tokens[2];

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
                requestClientListFromServer(sender, clientSocket, clientAddress, clientPort, clientMap);

                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }

    public static void connectToClient(int targetId,
                                       Map<Integer, Integer> clientMap,
                                       DatagramSocket clientSocket,
                                       InetAddress serverAddress,
                                       ReliableSender sender,
                                       int myClientId) {

        // 1. Make sure client exists in the map
        if (!clientMap.containsKey(targetId)) {
            System.out.println("[Client] ERROR: Client " + targetId + " is inactive");
            return;
        }

        int targetPort = clientMap.get(targetId);
        System.out.println("[Client] Trying to connect to Client " + targetId +
                " at port " + targetPort);

        Random rand = new Random();
        int seqNum = rand.nextInt(10000);

        while (true) {
            try {
                // 2. Build connection request message
                String message = "CONNECTION_REQUEST:FROM:" + myClientId + ":SEQ:" + seqNum;

                // 3. Send using YOUR reliable sender (rdt)
                System.out.println("[Client] Sending request...");
                sender.send(clientSocket, serverAddress, targetPort, message, seqNum);

                // 4. Wait up to 10 seconds for accept/reject
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                clientSocket.setSoTimeout(10_000);
                clientSocket.receive(packet);

                String reply = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Client] Received: " + reply);

                if (reply.startsWith("CONNECTION_ACCEPT")) {
                    System.out.println("[Client] Connection established!");
                    return;
                }

                if (reply.startsWith("CONNECTION_REJECT")) {
                    System.out.println("[Client] Connection rejected.");
                    return;
                }

                // ignore anything else and retry

            } catch (SocketTimeoutException e) {
                System.out.println("[Client] No reply… retrying request");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }




        public static void terminateChat(int id) {  }

    public static void displayActiveChats() {  }

    public static void leaveApplication() {  }

    public static void requestClientListFromServer( ReliableSender sender,
                                                    DatagramSocket clientSocket,
                                                    InetAddress clientAddress,
                                                    int clientPort,
                                                    Map<Integer, Integer> clientMap
        ) {
        StringBuilder sb = new StringBuilder();

// Step 1: Build menu or client list string
        for (Integer clientID : clientMap.keySet()) {
            int port = clientMap.get(clientID);
            sb.append("Client ID: ").append(clientID)
                    .append("\n");
        }

// Step 2: Send the string to the target client
        String message = sb.toString();
        sender.send(clientSocket, clientAddress, clientPort, message, 1);
    }
}