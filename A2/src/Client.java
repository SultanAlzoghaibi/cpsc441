import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    private ClientToServer csc;
    private DatagramSocket socket;
    public Client() {

    }

    public void connectToServer(){
        csc = new ClientToServer();
    }

    public void connectToOtherClient(){
        csc = new ClientToServer();
    }


    private class ClientToClient extends Thread{
        private DatagramSocket socket;
        public ClientToClient(){


        }
        @Override
        public void run() {
            System.out.println("Client Client");
        }

    }

    private class ClientToServer{
        private DatagramSocket socket;
        private InetAddress serverAddress;
        private int serverPort = 7070;

        public ClientToServer() {
            this.socket = socket;
            try{
                socket = new DatagramSocket();
                serverAddress = InetAddress.getByName("localhost");
                System.out.println("[Client] Connected to server on port " + serverPort);

                String message = "Hello server! This is the client.";
                byte[] sendData = message.getBytes();

                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                socket.send(packet);
                System.out.println("[Client] Sent packet: " + message);

                while(true){
                    receiveClientList();


                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void receiveClientList() {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                System.out.println("Waiting for client list from server...");
                socket.receive(packet);  // 'socket' is your DatagramSocket

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("\n---- Client List Received ----");
                System.out.println(message);
                System.out.println("-----------------------------\n");

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error receiving client list from server.");
            }
        }


    }


    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
