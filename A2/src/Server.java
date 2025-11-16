import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
            sendClientListOfClients(address, clientID);


        }

        public void ReceiveAck(){

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
                DatagramPacket packet = new DatagramPacket(
                        message.getBytes(),
                        message.length(),
                        clientAddress,
                        clientPort
                );

                serverSocket.send(packet);
                System.out.println("Sent active client list to client on port " + clientPort);
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
