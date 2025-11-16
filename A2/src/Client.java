import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

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
        private int serverConnectionPort = 7070;
        private int serverReqSendingPort;

        public ClientToServer() {
            this.socket = socket;

            try{
                socket = new DatagramSocket();
                serverAddress = InetAddress.getByName("localhost");
                System.out.println("[Client] Connected to server on port " + serverConnectionPort);
                Random rand = new Random();
                int seqNum = rand.nextInt(9999);

                String message = "REGISTER:SEQ:" + seqNum;

                byte[] sendData = message.getBytes();

                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, serverAddress, serverConnectionPort);
                socket.send(packet);
                System.out.println("[Client] Sent packet: " + message);
                serverReqSendingPort = receivePortNumber();


                while(true){
                    receiveReliable();

                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public int receivePortNumber() {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Wait for server to send: CHAT_PORT:xxxx
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Client] Received: " + msg);

                if (!msg.startsWith("CHAT_PORT:")) {
                    System.out.println("[Client] ERROR: Expected CHAT_PORT message!");
                    return -1;
                }

                // Extract the port number
                int port = Integer.parseInt(msg.split(":")[1]);

                System.out.println("[Client] Chat port from server = " + port);

                return port;

            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }



        public void receiveReliable() {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Wait for message from server
                socket.receive(packet);

                // Extract message
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Client] Received: " + msg);

                // ==== SEND ACK BACK ====
                String ack = "ACK:" + msg;
                byte[] ackData = ack.getBytes();

                DatagramPacket ackPacket = new DatagramPacket(
                        ackData,
                        ackData.length,
                        packet.getAddress(),
                        packet.getPort()
                );

                socket.send(ackPacket);
                System.out.println("[Client] Sent ACK: " + ack);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[Client] Error receiving message.");
            }
        }


    }


    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
