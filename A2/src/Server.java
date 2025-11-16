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
        }



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
                    continue;
                }

                InetAddress address = packet.getAddress();
                int clientPort = packet.getPort();
                Random rand = new Random();
                int ClientID = rand.nextInt((int)(Math.pow(2, 32) - 1));
                ClientIDtoPort.put(ClientID, clientPort);

                System.out.println("client Port: " + clientPort);
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
        private DatagramSocket socket;
        private int chatterID;
        private int clientID;
        private InetAddress  address;





        public ServerSideConnection(DatagramSocket s, int id, InetAddress address, int clientPort) {
            this.socket = s;
            chatterID = id;
            this.clientID = clientPort;

            this.address = address;

        }

        public void run() {
            System.out.printf("Server starting on port: %d\n", serverSocket.getLocalPort());
            Random rand = new Random();
            int portNum = rand.nextInt(10000) + 10000;
            try {
                socket = new DatagramSocket(portNum);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            sendServerChatPort(address, clientID, portNum);
            //sendClientListOfClients(address, clientID);


        }

        public void sendServerChatPort(InetAddress clientAddress, int clientPort, int serverPortNum){
            try {
                String msg = "CHAT_PORT:" + serverPortNum;
                byte[] data = msg.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        clientAddress,
                        clientPort
                );

                serverSocket.send(packet);
                System.out.println("[Server] Sent CHAT_PORT: " + serverPortNum);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[Server] Failed to send CHAT_PORT");
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
                    System.out.println("[Server] Sent: " + msg + " (attempt " + attempt + ")");
                    // Prepare to receive ACK
                    DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(2000); // Wait max 2 seconds for ACK

                    try {
                        socket.receive(ackPacket);
                        if (!msg.startsWith("ACK:")) {
                            System.out.println("[Server] Ignored ACK");
                            break;
                        }

                        String ackMsg = new String(ackPacket.getData(), 0, ackPacket.getLength());

                        if (ackMsg.equals(expectedAck)) {
                            System.out.println("[Server] ACK received for: " + msg);
                            return true; // SUCCESS
                        } else {
                            System.out.println("[Server] Received wrong ACK: " + ackMsg);
                        }

                    } catch (java.net.SocketTimeoutException e) {
                        System.out.println("[Server] Timeout waiting for ACK… retrying");
                    }
                }

                System.out.println("[Server] Failed to get ACK for: " + msg);
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
