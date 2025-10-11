package A1;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

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
                System.out.println(  listenForChats());
                Scanner sc = new Scanner(System.in);
                String name = sc.nextLine();
                sendMessage(name);
                String msg;
                while(true){
                    System.out.print("send next message: ");
                    msg = sc.nextLine();
                    sendMessage(msg);
                    System.out.println(listenForChats());


                }
            } catch (IOException e) {
                System.out.println("Error creating socket in CSC constructor");
                e.printStackTrace();
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
            String message = null;
            try {
                message = dataIn.readUTF();

            }catch (IOException e) {
                System.out.println("IOException in listenForName()");
            }
            return message;
        }





    }

    public static void main(String[] args) {
        Client c = new Client(1);
        c.connectToServer();



    }

}
