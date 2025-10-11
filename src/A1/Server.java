package A1;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;



public class Server {
    private ServerSocket ss;
    private int numPlayers;
    private HashMap<String, ServerSideConnection> nameToSsc;

    // Store name -> [startTime, endTime]
    private HashMap<String, ClientSession> clientSessionLogs = new HashMap<>();
    public Server() {
        System.out.println("--chat server--");
        numPlayers = 0;
        nameToSsc = new HashMap<>();
        clientSessionLogs = new HashMap<>();

        try {
            ss = new ServerSocket(30001);
        }
        catch (IOException e) {
            System.out.println("Server couldn't start server");
            e.printStackTrace();

        }
    }

    public void acceptConnections(){
        try {
            System.out.println("--waiting connections--");

            while (numPlayers < 3) {
                Socket s = ss.accept();
                numPlayers++;
                System.out.println("new connection");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
                Thread t = new Thread(ssc); //what ever is the in the ssc run in the new "THREAD"
                t.start();

            }
            System.out.println("3 players reach no longer accepting chatters");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("IOException in acceptConnections()");

        }

    }


    private class ServerSideConnection implements Runnable {
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int chatterID;
        private String chatterName;

        public ServerSideConnection(Socket s, int id) {
            this.socket = s;
            this.chatterID = id;

            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("Server couldn't start server, ServerSideConnection");
                e.printStackTrace();
            }
        }
        public void run() {
            try {
                sendMessage("plz input ur name: ");
                chatterName = listenForChats();
                while (nameToSsc.containsKey(chatterName) || chatterName == null) {
                    System.out.println("that name is already in use or invalid, try again");
                    chatterName = listenForChats();
                }
                nameToSsc.put(chatterName, this) ;
                ClientSession seshLog = new ClientSession(LocalDateTime.now(), null );
                clientSessionLogs.put(chatterName, seshLog);
                String msg;
                while (true) {

                    //todo: add chatting logic?

                    msg = listenForChats();
                    if (msg.equals("exit")) {
                        numPlayers--;
                        sendMessage("exiting server");
                        clientSessionLogs.get(chatterName).setEnd(LocalDateTime.now());
                        break;
                    } else if (msg.equals("list")) {
                        sendMessage("input file list here");
                        //todo: file path system
                    } else {
                        sendMessage("ACK: " + msg);
                    }



                }

            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Server couldn't start server, Run()");

            } finally { //always runs
                close();
            }
        }

        public void sendMessage(String message) {
            try{
                dataOut.writeUTF(message); // this is sending to server an int
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException in AskForName()");
            }

        }
        public String listenForChats(){
            String msg = null;
            try {
                msg = dataIn.readUTF();

            }catch (IOException e) {
                System.out.println("IOException in listenForName()");
            }
            System.out.println(msg);
            return msg;
        }

        public void close() {
            try {
                if (dataIn != null) dataIn.close();
                if (dataOut != null) dataOut.close();
                if (socket != null && !socket.isClosed()) socket.close();
                System.out.println("Closed connection for chatter ID: " + chatterID);
            } catch (IOException e) {
                System.out.println("Error closing connection for chatter ID: " + chatterID);
                e.printStackTrace();
            }
        }




    }

    public static void main(String[] args) {
        Server s = new Server();
        s.acceptConnections();


    }
}

