package A1;


import oldWork.GameServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;


public class Server {
    private ServerSocket ss;
    private int numPlayers;

    private ServerSideConnection[] chatConnections;


    public Server() {
        System.out.println("--chat server--");
        numPlayers = 0;

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
                chatConnections = new ServerSideConnection[numPlayers];
                System.out.println("new connection");

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
                while (true) {


                }

            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Server couldn't start server, Run()");

            }
        }

    }

    public static void main(String[] args) {
        Server s = new Server();
        s.acceptConnections();


    }
}

