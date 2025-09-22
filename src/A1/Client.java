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



    public Client(ClientSideConnection csc, int playerID) {

        this.playerID = playerID;
    }

    private class ClientSideConnection{


        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection(Socket socket) {
            System.out.println("Creating Client");
            this.socket = socket;
            try{
                socket = new Socket("localhost", 30000);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());

            } catch (IOException e) {
                System.out.println("Error creating socket in CSC constructor");
                e.printStackTrace();
            }
        }


    }

}
