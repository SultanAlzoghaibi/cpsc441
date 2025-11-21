import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    int numberOfClients = 0;

    DatagramSocket serverSocket;
    int BufferLength = 1024;
    int maxNumberOfClients = 4;


    HashMap<Integer, Integer> ClientIDtoPort = new HashMap<>();


    public Server() throws IOException {


        try{
            serverSocket = new DatagramSocket(7070); // Bind to port

        } catch(Exception e){
            System.out.printf("Server could not be started");
            System.exit(1);
        }



    }
    public HashMap<Integer, Integer> getClientIDtoPort(){
        return ClientIDtoPort;
    }

    public void acceptConnections(){
        try {
            System.out.println("--waiting connections A2--");
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[BufferLength], BufferLength);
                serverSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.printf("Server received packet: %s\n", msg);
                if (!msg.startsWith("REGISTER:")) {
                    continue;
                }
                numberOfClients++;
                if (numberOfClients > maxNumberOfClients){
                    System.out.println("Maximum number of connections reached");
                    numberOfClients--;
                    continue;
                }

                InetAddress address = packet.getAddress();
                int clientPort = packet.getPort();
                Random rand = new Random();
                int ClientID = rand.nextInt((int)(Math.pow(2, 32) - 1));

                String[] tokens = msg.trim().split(":");
                int ListenPort = Integer.parseInt(tokens[4]);

                synchronized (ClientIDtoPort) {
                    ClientIDtoPort.put(ClientID, ListenPort);
                }

                System.out.println("client LPort: " + ListenPort);
                ServerSideConnection ssc = new ServerSideConnection(serverSocket, ClientID, address, clientPort);
                Thread t = new Thread(ssc);
                t.start();

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("IOException in acceptConnections()");

        }

    }
    private class ServerSideConnection implements Runnable {
        // ...
        private DatagramSocket chatServerSocket;
        private int chatterID;
        private int clientID;
        private InetAddress  address;
        private int clientPort;





        public ServerSideConnection(DatagramSocket s, int id, InetAddress address, int clientPort) {
            this.chatServerSocket = s;
            chatterID = id;
            this.clientPort = clientPort;
            this.clientID = id;

            this.address = address;

        }

        public void run() {
            Random rand = new Random();
            int portNum = rand.nextInt(10000) + 10000;
            try {
                chatServerSocket = new DatagramSocket(portNum);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            int seqNum = rand.nextInt(10000) ;
            sendServerChatPort(address, this.clientPort, portNum, seqNum);
            if (!receiveNewSocketACK(chatServerSocket, seqNum)) {
                System.out.println("error");
                return;
            }
            boolean didrecieve = false;

            String msg = """
                    Welcome to the Chat System! Here are the available commands:
                    
                    connect <client_id>         - Connect to another client
                    msg <client_id> <message>   - Send a message to a connected client
                    terminate <client_id>       - Terminate chat with a client
                    chats                       - Show active chats
                    list                        - Request list of all online clients
                    leave                       - Leave the application
                    Help                        - list out all the options
                    
                    """;

            didrecieve = sendReliableToClient(chatServerSocket, address, clientPort, msg, seqNum);
            while (true) {
                if (!didrecieve) {
                    System.out.println("error");
                    continue;
                }
                //System.out.println("they got out msg");
                String receiveMsg = receiveClientMessageAndAck(chatServerSocket);
                //System.out.println("[Server] Received: " + receiveMsg);
                assert receiveMsg != null;

                ServerCommands.handleCommand(
                        receiveMsg,
                        getClientIDtoPort(),
                        this::sendReliableToClient, // <-- method reference to pass the function
                        this::receiveClientMessageAndAck,
                        chatServerSocket,
                        address,
                        clientPort,
                        clientID
                );


                seqNum++;  // MUST increment by 1

            }
            //sendClientListOfClients(address, clientID);

        }

        public boolean sendReliableToClient(DatagramSocket socket, InetAddress clientAddress, int clientPort, String body, int seqNum) {
            try {
                String msg = body + ":SEQ:" + seqNum;
                byte[] data = msg.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        data, data.length,
                        clientAddress, clientPort
                );

                for (int attempt = 1; attempt <= 3; attempt++) {
                    socket.send(packet);
                    //System.out.println("[Server] Sent: " + msg + " (attempt " + attempt + ")");

                    // wait for ACK
                    byte[] buffer = new byte[1024];
                    DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(2000);

                    try {
                        socket.receive(ackPacket);
                        String ackMsg = new String(ackPacket.getData(), 0, ackPacket.getLength());
                        //System.out.println("[Server] Received ACK: " + ackMsg);

                        if (!ackMsg.startsWith("ACK:SEQ:"))
                            continue;

                        int ackSeq = Integer.parseInt(ackMsg.split(":")[2]);

                        if (ackSeq == seqNum + 1) {
                            //System.out.println("[Server] Correct ACK received");
                            return true;
                        }
                        else {
                            //System.out.println("[Server] Wrong ACK: expected " + (seqNum+1) + " got " + ackSeq);
                        }

                    } catch (SocketTimeoutException e) {
                        //System.out.println("[Server] Timeout waiting for ACK… retrying");
                    }
                }

                //System.out.println("[Server] FAILED to deliver reliable message");
                return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }


        public String receiveClientMessageAndAck(DatagramSocket socket) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(2000000000);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());


                // Parse sequence number
                if (!msg.contains("SEQ:")) {
                    //System.out.println("[Server] ERROR: no seq");
                    return "ERROR: no seq";
                }

                int seq = Integer.parseInt(msg.split("SEQ:")[1].trim());

                // Build ACK
                int ackSeq = seq + 1;
                String ack = "ACK:SEQ:" + ackSeq;

                DatagramPacket ackPacket = new DatagramPacket(
                        ack.getBytes(),
                        ack.length(),
                        packet.getAddress(),
                        packet.getPort()
                );

                socket.send(ackPacket);
                //System.out.println("[Server] Sent ACK:SEQ:" + ackSeq);
                return msg;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        public boolean receiveNewSocketACK(DatagramSocket socket, int seqNum) {
            try {
                byte[] buffer = new byte[BufferLength];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.setSoTimeout(2000);  // 2 second timeout like rdt
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Server] Received ACK: " + msg);

                // Parse expected ACK format
                // Format: ACK:<something> SEQ:<number>
                if (!msg.startsWith("ACK:")) {
                    System.out.println("[Server] Not an ACK packet.");
                    return false;
                }

                // Extract the sequence number
                String[] parts = msg.split("SEQ:");
                if (parts.length < 2) {
                    System.out.println("[Server] ACK missing SEQ.");
                    return false;
                }

                int ackSeq = Integer.parseInt(parts[1].trim());

                // Must equal seqNum + 1 to be valid
                if (ackSeq == seqNum + 1) {
                    System.out.println("[Server] Correct ACK received with SEQ: " + ackSeq);
                    return true;
                } else {
                    System.out.println("[Server] Wrong ACK SEQ: expected " + (seqNum+1) + " but got " + ackSeq);
                    return false;
                }

            } catch (IOException e) {
                System.out.println("[Server] Timeout or socket error while waiting for ACK");
                return false;
            }
        }

        public void sendServerChatPort(InetAddress clientAddress, int clientPort, int serverPortNum, int seqNum){
            try {
                String msg = "CHAT_PORT:" + serverPortNum + ":SEQ:" + seqNum;
                byte[] data = msg.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        clientAddress,
                        clientPort
                );

                serverSocket.send(packet);
                //System.out.println("[Server] Sent: " + msg);

            } catch (Exception e) {
                e.printStackTrace();
                //System.out.println("[Server] Failed to send CHAT_PORT");
            }
        }


        public boolean sendReliable(String msg, InetAddress clientAddress, int clientPort, DatagramSocket socket) {
            try {
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);

                // Expected ACK message
                String expectedAck = "ACK:" + msg;
                byte[] buffer = new byte[1024];

                // Try up to 3 times
                for (int attempt = 1; attempt <= 3; attempt++) {
                    // Send message
                    socket.send(packet);
                    //System.out.println("[Server] Sent: " + msg + " (attempt " + attempt + ")");
                    // Prepare to receive ACK
                    DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(2000); // Wait max 2 seconds for ACK

                    try {
                        socket.receive(ackPacket);
                        if (!msg.startsWith("ACK:")) {
                            //System.out.println("[Server] Ignored ACK");
                            break;
                        }

                        String ackMsg = new String(ackPacket.getData(), 0, ackPacket.getLength());

                        if (ackMsg.equals(expectedAck)) {
                            //System.out.println("[Server] ACK received for: " + msg);
                            return true; // SUCCESS
                        } else {
                            //System.out.println("[Server] Received wrong ACK: " + ackMsg);
                        }

                    } catch (java.net.SocketTimeoutException e) {
                       //System.out.println("[Server] Timeout waiting for ACK… retrying");
                    }
                }

                //System.out.println("[Server] Failed to get ACK for: " + msg);
                return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public void sendClientListOfClients(InetAddress clientAddress, int clientPort) {
            try {
                if (ClientIDtoPort.isEmpty()) {
                    String msg = "No other clients are connected.";
                    DatagramPacket packet = new DatagramPacket(
                            msg.getBytes(),
                            msg.length(),
                            clientAddress,
                            clientPort
                    );
                    serverSocket.send(packet);
                    return;
                }

                // Build the list of connected clients
                StringBuilder sb = new StringBuilder("Active Clients:\n");
                for (Map.Entry<Integer, Integer> entry : ClientIDtoPort.entrySet()) {
                    sb.append("Client ID: ").append(entry.getKey())
                            .append(" | Port: ").append(entry.getValue())
                            .append("\n");
                }

                String message = sb.toString();

                // Send the list back to the requesting client
                sendReliable(message, clientAddress, clientPort, serverSocket);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error sending client list.");
            }
        }
    }



    public static void main(String[] args) throws IOException {

        Server server = new Server();
        server.acceptConnections();



    }
}
