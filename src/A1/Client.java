package A1;


import oldWork.Player;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    private ClientSideConnection csc;
    private int playerID;



    public Client(int playerID) {

        this.playerID = playerID;
    }

    public void connectToServer(){
        csc = new Client.ClientSideConnection();
    }

    private class ClientSideConnection{


        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection() {
            System.out.println("Creating Client");
            this.socket = socket;
            try{
                socket = new Socket("localhost", 30001);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                System.out.println("Connected to Server");
            } catch (IOException e) {
                System.out.println("Error creating socket in CSC constructor");
                e.printStackTrace();
            }
        }


    }

    public static void main(String[] args) {
        Client c = new Client(1);
        c.connectToServer();



    }

}
