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

                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
