import java.net.*;
import java.util.*;

// ServerCommands.java
public class ServerCommands {
    static HashMap<Integer, HashSet<Integer>> connectionsMap = new HashMap<>();
    static ReliableSender sender;
    static Map<Integer, Integer> clientMap;
    public static void handleCommand(
            String receiveMsg,
            Map<Integer, Integer> clientMapPara,
            ReliableSender senderPara, // <-- function pointe
            ReliableReceive receiver,
            DatagramSocket chatServerSocket,
            InetAddress clientAddress,
            int clientPort,
            int ourClientId,
            HashMap<Integer, HashSet<Integer>> connectionsMapPara
    ) throws SocketException {
       connectionsMap = connectionsMapPara;
       sender = senderPara;
       clientMap  = clientMapPara;

        //System.out.println(" HANDLING THE MSG: " + receiveMsg);

        // Your switch-case logic here
        String clean = receiveMsg.trim();

// Remove SEQ portion if present
        int seqIndex = clean.indexOf(":SEQ:");
        if (seqIndex != -1) {
            clean = clean.substring(0, seqIndex).trim();
        }

        String[] tokens = clean.trim().split(" ", 8); // max 3 parts
        String command = tokens[0].toLowerCase();
        System.out.println(" COMMAND: " + Arrays.toString(tokens));

        switch (command) {
            case "connect":

                // Example: connect <client_id>
                if (tokens.length < 2) {
                    System.out.println("Usage: connect <client_id>");
                } else {

                    int targetId = Integer.parseInt(tokens[1]);
                    connectToClient(targetId,
                            clientMap,
                            chatServerSocket,
                            clientPort,
                            clientAddress,
                            sender,
                            receiver,
                            ourClientId);

                }
                break;
            case "msg":
                // Example: msg <client_id> <message>
                System.out.println("MSG!");
                if (tokens.length < 3) {
                    System.out.println("Usage: msg <client_id> <message>");
                } else {
                    int targetId = Integer.parseInt(tokens[1]);

                    // Join all remaining tokens to preserve spaces in message
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < tokens.length; i++) {
                        sb.append(tokens[i]);
                        if (i < tokens.length - 1) sb.append(" ");
                    }
                    String message = sb.toString();

                    chat(
                            ourClientId,
                            targetId,
                            message,
                            sender,
                            chatServerSocket,
                            clientAddress,
                            clientPort,
                            clientMap,
                            connectionsMap
                    );


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
                requestClientListFromServer(sender, chatServerSocket, clientAddress, clientPort, clientMap);

                break;

            default:
                System.out.println("Unknown command: " + command);
                sender.send(chatServerSocket, clientAddress, clientPort, "error", 100233);

        }

    }

    public static void connectToClient(int targetId,
                                       Map<Integer, Integer> clientMap,
                                       DatagramSocket chatServerSocket,
                                       int clientPort,
                                       InetAddress serverAddress,
                                       ReliableSender sender,
                                       ReliableReceive receiver,
                                       int myClientId) throws SocketException {

        String msgToClienConnecter = "Server sent a connection message to (wait 10 secs) " + targetId;
        sender.send(chatServerSocket, serverAddress, clientPort, msgToClienConnecter, 122033);

        System.out.println("running connectToClient");
        // 1. Make sure client exists in the map
        if (!clientMap.containsKey(targetId)) {
            System.out.println("[Client] ERROR: Client " + targetId + " is inactive");
            return;
        }
        DatagramSocket serverThreadSocket = new DatagramSocket();

        int targetPort = clientMap.get(targetId);
        System.out.println("[Client] Trying to connect to Client " + targetId +
                " at port " + targetPort);

        Random rand = new Random();
        int seqNum = rand.nextInt(10000);


        try {
            // 2. Build connection request message
            String message = "CONNECTION_REQUEST:FROM:" + myClientId;
            seqNum += 5;
            // 3. Send using YOUR reliable sender (rdt)
            System.out.println("[Client] Sending request: " + message);
            boolean didreceive = sender.send(serverThreadSocket, serverAddress, targetPort, message, seqNum);

            if (!didreceive) {
                System.out.println("[Client] ERROR: Unable to send request");
                return;
            }

            System.out.println("waiting reply");
            String reply = receiver.receive(serverThreadSocket);
            System.out.println(" REEECEIVRED: " + reply);

            if (reply.startsWith("CONNECTION_ACCEPT")) {
                System.out.println("[Client] Connection established!");

                connectionsMap.get(myClientId).add(targetId);
                connectionsMap.get(targetId).add(myClientId);


                return;
            }

            if (reply.startsWith("CONNECTION_REJECT")) {
                System.out.println("[Client] Connection rejected.");
                return;
            }



            // ignore anything else and retry

        } catch (Exception e) {

            e.printStackTrace();
            return;
        }

    }

    public static void chat(int clientId,
                            int targetId,
                            String msg,
                            ReliableSender sender,
                            DatagramSocket chatServerSocket,
                            InetAddress clientAddress,
                            int clientPort,
                            Map<Integer, Integer> clientMap,
                            HashMap<Integer, HashSet<Integer>> connectionsMap) {

        if (!connectionsMap.get(clientId).contains(targetId)) {
            System.out.println("[Client] ERROR: Client " + targetId + " is inactive");

            sender.send(chatServerSocket,
                    clientAddress,
                    clientPort,
                    "U dont have permission to chat with hime",
                    100233);
            return;
        }
        msg = "CHATFROM:" + clientId + ":" + msg;


        sender.send(chatServerSocket,
                clientAddress,
                clientMap.get(targetId),
                msg,
                122033);
        System.out.println("sent to port: " +  clientMap.get(targetId));
        sender.send(chatServerSocket,
                clientAddress,
                clientPort,
                "Sent to client " + targetId,
                122033);
    }




    public static void terminateChat(int id) {  }

    public static void displayActiveChats() {  }

    public static void leaveApplication() {  }

    public static void requestClientListFromServer( ReliableSender sender,
                                                    DatagramSocket chatServerSocket,
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
        sender.send(chatServerSocket, clientAddress, clientPort, message, 1);
    }
}