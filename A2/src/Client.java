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
                System.out.println("serverReqSendingPort: "+ serverReqSendingPort);

                while(true){


                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }



        public int receivePortNumber() {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Client] Received: " + msg);

                if (!msg.startsWith("CHAT_PORT:")) {
                    System.out.println("[Client] ERROR: expected CHAT_PORT");
                    return -1;
                }

                String[] parts = msg.split(":");
                int port = Integer.parseInt(parts[1]);
                int seq = Integer.parseInt(parts[3]);

                System.out.println("[Client] Chat port = " + port + ", seq = " + seq);

                // ------------ FIXED ACK ------------
                int ackSeq = seq + 1;
                String ack = "ACK:SEQ:" + ackSeq;
                byte[] ackBytes = ack.getBytes();
                DatagramPacket ackPacket = new DatagramPacket(
                        ackBytes,
                        ackBytes.length,
                        packet.getAddress(),
                        port // reply to NEW server port
                );
                socket.send(ackPacket);
                // -----------------------------------

                return port;

            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }


    }


    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
