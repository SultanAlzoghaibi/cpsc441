package A1;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
                System.out.println("trying to Connect to Server (it may be full)");

                System.out.println(listenForChats());
                Scanner sc = new Scanner(System.in);
                String name = sc.nextLine();
                sendMessage(name);
                String isvalidName = listenForChats();
                System.out.println(isvalidName);
                while (!isvalidName.equals("VALID NAME")){
                    name = sc.nextLine();
                    sendMessage(name);
                    isvalidName = listenForChats();
                    System.out.println(isvalidName);

                }

                String msg;
                String receivedMsg;

                while(true){
                    System.out.print("send next message OR \"list\" to list server files (type 'exit' to quit) : ");
                    msg = sc.nextLine();
                    sendMessage(msg);
                    receivedMsg = listenForChats();
                    System.out.println(receivedMsg);
                    if (receivedMsg.equals("exiting server")){
                        break;
                    }

                    if (receivedMsg.equals("FILE_RECEIVE_MODE")){
                        listenForFile();
                        System.out.println("File received");

                    }


                }
            } catch (IOException e) {
                System.out.println("Error creating socket in CSC constructor");
                e.printStackTrace();
            }

        }

        // Listens But also add the file ot eht ClientDownloads repo
        public void listenForFile(){

               try {
                   String fileName = dataIn.readUTF();

                   if (fileName.startsWith("ERROR:")) {
                       System.out.println("server end error" + fileName);
                       return;
                   }

                   long fileSize = dataIn.readLong();

                   File dir = new File("src/A1/ClientDownloads");
                   if (!dir.exists()) {
                       dir.mkdirs(); // Create if missing
                   }
                   FileOutputStream fos = new FileOutputStream("src/A1/ClientDownloads/" + fileName);
                   byte[] buffer = new byte[32 * 1024]; // 32 KB
                   int bytesRead;
                   long totalRead = 0;

                   while (totalRead < fileSize && (bytesRead = dataIn.read(buffer, 0,
                           (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
                       fos.write(buffer, 0, bytesRead);
                       totalRead += bytesRead;
                   }

                   fos.close();
                   System.out.println("File received and saved as: " + fileName + "plz wait 10secs+");


               } catch (IOException e){
                   System.out.println("Error Listening For File");
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
