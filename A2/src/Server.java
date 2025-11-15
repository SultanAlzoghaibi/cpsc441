import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Server {
    int numberOfClients = 0;
    DatagramSocket serverSocket;
    int BufferLength = 1024;
    int maxNumberOfClients = 4;


    int[] arrClientPorts = new int[maxNumberOfClients];


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
                int Clinetport = packet.getPort();

                arrClientPorts[numberOfClients] = Clinetport;


                System.out.println("client Port: " + Clinetport);
                ServerSideConnection ssc = new ServerSideConnection(serverSocket, numberOfClients);
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


        public ServerSideConnection(DatagramSocket s, int id) {
            this.socket = s;
            chatterID = id;


        }

        public void run() {
            System.out.printf("Server starting on port: %d\n", serverSocket.getLocalPort());


        }
    }



    public static void main(String[] args) throws IOException {

        Server server = new Server();
        server.acceptConnections();


    }
}
