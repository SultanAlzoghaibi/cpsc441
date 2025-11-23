import java.net.*;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class Client {

    private ClientToServer csc;
    private NewClientListner ncL;
    Scanner sc ;

    private final ReentrantLock inputLock = new ReentrantLock(true); // fair lock



    public Client() {
        sc = new Scanner(System.in);

    }

    public void connectToServer(){
        csc = new ClientToServer();

    }




    private class NewClientListner extends Thread{
        private DatagramSocket socket;
        public NewClientListner(ReliableSender sender){


        }
        @Override
        public void run() {
            System.out.println("Client Client");
        }

    }


    public class ClientToServer{
        private DatagramSocket ServerConnectsocket;
        private DatagramSocket ServerReqsocket;
        private InetAddress serverAddress;
        private int serverConnectionPort = 7070;
        private DatagramSocket ServerNewChatListnerSocket;


        public ClientToServer() {

            try{
                ServerConnectsocket = new DatagramSocket();
                ServerNewChatListnerSocket  = new DatagramSocket();
                int LisnerPort = ServerNewChatListnerSocket.getLocalPort();

                serverAddress = InetAddress.getByName("localhost");
                System.out.println("[Client] Connected to server on port " + serverConnectionPort);
                Random rand = new Random();
                int seqNum = rand.nextInt(9999);

                String message = "REGISTER:SEQ:" + seqNum+ ":LISTNERPORT:" +LisnerPort;


                byte[] sendData = message.getBytes();

                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, serverAddress, serverConnectionPort);
                ServerConnectsocket.send(packet);
                //System.out.println("[Client] Sent packet: " + message);
                int serverReqSendingPort = receivePortNumber();
                //System.out.println("serverReqSendingPort: "+ serverReqSendingPort);
                ServerReqsocket = ServerConnectsocket;


                String recieveMsg;
                new Thread(() -> incommingNewChat(ServerNewChatListnerSocket)).start();

                while(true){

                    recieveMsg = receiveServerMsg(ServerReqsocket);
                    System.out.println(recieveMsg);
                    System.out.print("enter message to send to server: ");
                    String msg = "";
                    inputLock.lock();
                    try {
                         msg = sc.nextLine();
                    }finally {
                        inputLock.unlock();
                    }

                    sendReliableToServer(ServerReqsocket, serverAddress, serverReqSendingPort, msg, seqNum);
                    seqNum++;
                    Thread.sleep(100);

                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public boolean sendReliableToServer(DatagramSocket socket, InetAddress serverAddr, int serverPort, String body, int seqNum) {
            try {
                String msg = body + ":SEQ:" + seqNum;
                byte[] data = msg.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        data, data.length,
                        serverAddr, serverPort
                );

                // Try up to 3 times (rdt style)
                for (int attempt = 1; attempt <= 3; attempt++) {
                    socket.send(packet);
                    //System.out.println("[Client] Sent: " + msg + " (attempt " + attempt + ")");


                    // Wait for ACK
                    byte[] buffer = new byte[1024];
                    DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(2000);

                    try {
                        socket.receive(ackPacket);
                        String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                        //System.out.println("[Client] Received: " + ack);

                        if (!ack.startsWith("ACK:SEQ:")) continue;

                        int ackNum = Integer.parseInt(ack.split(":")[2]);

                        if (ackNum == seqNum + 1) {
                            //System.out.println("[Client] Correct ACK received");
                            return true;
                        } else {
                            //System.out.println("[Client] Wrong ACK received");

                        }

                    } catch (SocketTimeoutException e) {
                        //System.out.println("[Client] Timeout waiting for ACK… retrying");
                    }
                }

                //System.out.println("[Client] FAILED to deliver message");
                return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public String receiveServerMsg(DatagramSocket socket) {
            try {
                // 1. Receive packet
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // 2. Convert to string
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[in receiveServerMsg] Received from server: " + msg);

                // ---------------- Parse sequence number ----------------
                // Format expected: SOMETHING:SEQ:<num>
                if (!msg.contains("SEQ:")) {
                    System.out.println("[Client] ERROR: Message missing SEQ number");
                    return msg; // still return the message
                }

                String[] parts = msg.split("SEQ:");
                int seq = Integer.parseInt(parts[1].trim());

                // ---------------- Send ACK ---
                // -------------
                int ackSeq = seq + 1;
                String ackMsg = "ACK:SEQ:" + ackSeq;
                byte[] ackBytes = ackMsg.getBytes();

                DatagramPacket ackPacket = new DatagramPacket(
                        ackBytes,
                        ackBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                );

                socket.send(ackPacket);
                //System.out.println("[Client] Sent ACK:SEQ:" + ackSeq);

                return msg;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public void incommingNewChat(DatagramSocket listenerSocket) {
            while (true) {


                try {

                    // 1. Receive the connection request (this already sends ACK)
                    String msg = receiveServerMsgWithPort(listenerSocket);
                    System.out.println("WEEE GOT A SOMTHING!!! " + msg);
                    if (msg == null) continue;

                    if (msg.startsWith("CHATFROM:")) {
                        // Format: CHATFROM:<id>:<text>:SEQ:<num>:FROMPORT:<p>
                        try {
                            String[] parts = msg.split(":");
                            int fromId = Integer.parseInt(parts[1]);
                            String chatText = parts[2];

                            System.out.println("\n[CHAT] Message from client " + fromId + ": " + chatText);

                        } catch (Exception e) {
                            System.out.println("[Client] ERROR parsing chat message: " + msg);
                        }

                        continue;
                    }

                    if (!msg.startsWith("CONNECTION_REQUEST")) {
                        System.out.println("[Client] Unknown incoming message: " + msg);
                        return;
                    }




                    // 2. Parse the message
                    // Format: CONNECTION_REQUEST:FROM:<id>:SEQ:<num>
                    String[] parts = msg.split(":");
                    int fromClientId = Integer.parseInt(parts[2]);

                    //System.out.println("\n*** Incoming chat request ***");
                    //System.out.println("Client " + fromClientId + " wants to connect with you.");

                    // 3. Ask user


                    inputLock.lock();
                    String input = "CONNECTION_REJECT:FROM:";

                    try {
                        System.out.print("Accept? (y/n): ");
                        input = sc.nextLine().trim().toLowerCase();
                    } finally {
                        inputLock.unlock();
                    }


                    // 4. Build reply
                    String reply;
                    if (input.equals("y")) {
                        reply = "CONNECTION_ACCEPT:FROM:" + fromClientId;
                        System.out.println("[Client] You accepted the connection.");
                    } else {
                        reply = "CONNECTION_REJECT:FROM:" + fromClientId;
                        System.out.println("[Client] You rejected the connection.");
                    }

                    // 5. Send reply using RDT

                    int seq = new Random().nextInt(10000);

                    InetAddress addr = InetAddress.getByName("localhost");


                    String[] t = msg.split(":");
                    int fromPort = Integer.parseInt(t[6]);
                    System.out.println(fromPort);
                    sendReliableToServer(listenerSocket, addr, fromPort, reply, seq);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public String receiveServerMsgWithPort(DatagramSocket socket) {
            try {
                socket.setSoTimeout(300000);
                // 1. Receive packet
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // 2. Convert to string
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[in receiveServerMsg] Received from server: " + msg);

                // ---------------- Parse sequence number ----------------
                // Format expected: SOMETHING:SEQ:<num>
                if (!msg.contains("SEQ:")) {
                    System.out.println("[Client] ERROR: Message missing SEQ number");
                    return msg; // still return the message
                }

                String[] parts = msg.split("SEQ:");
                int seq = Integer.parseInt(parts[1].trim());

                // ---------------- Send ACK ---
                // -------------
                int ackSeq = seq + 1;
                String ackMsg = "ACK:SEQ:" + ackSeq;
                byte[] ackBytes = ackMsg.getBytes();

                DatagramPacket ackPacket = new DatagramPacket(
                        ackBytes,
                        ackBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                );

                socket.send(ackPacket);
                //System.out.println("[Client] Sent ACK:SEQ:" + ackSeq);
                msg +=  ":FROMPORT:" + packet.getPort();
                return msg;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }





        public int receivePortNumber() {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                ServerConnectsocket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                //System.out.println("[Client] Received: " + msg);

                if (!msg.startsWith("CHAT_PORT:")) {
                    System.out.println("[Client] ERROR: expected CHAT_PORT");
                    return -1;
                }

                String[] parts = msg.split(":");
                int port = Integer.parseInt(parts[1]);
                int seq = Integer.parseInt(parts[3]);

                //System.out.println("[Client] Chat port = " + port + ", seq = " + seq);


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
                ServerConnectsocket.send(ackPacket);
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
