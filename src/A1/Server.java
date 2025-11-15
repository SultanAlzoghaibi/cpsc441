package A1;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;


public class Server {
    private ServerSocket ss;
    private int numPlayers;
    private HashMap<String, ServerSideConnection> nameToSsc;
    private HashSet<String> filesSet = new HashSet<>();
    // Store name -> [startTime, endTime]
    private HashMap<String, ClientSession> clientSessionLogs = new HashMap<>();
    public Server() {
        System.out.println("--chat server--");
        numPlayers = 0;
        nameToSsc = new HashMap<>();
        clientSessionLogs = new HashMap<>();
        initFilesSet();
        System.out.println("init");
        System.out.println(filesSet.toString());

        try {
            ss = new ServerSocket(30001);
        }
        catch (IOException e) {
            System.out.println("Server couldn't start server");
            e.printStackTrace();

        }
    }

    public void initFilesSet() {
        File folder = new File("src/A1/ServerFiles"); // Folder path relative to project root
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filesSet.add(file.getName());
                }
            }
        } else {
            System.out.println("Warning: ServerFiles folder not found or is empty.");
        }
    }

    public void acceptConnections(){
        try {
            System.out.println("--waiting connections--");

            while (true) {
                Socket s = ss.accept();
                numPlayers++;
                if (numPlayers > 3) {
                    System.out.println("Too many connections in server");
                    continue;
                }
                System.out.println("new connection");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
                Thread t = new Thread(ssc); //what ever is the in the ssc run in the new "THREAD"
                t.start();

            }
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
                System.out.println("nameToSsc: " + nameToSsc.keySet().toString());

                while (nameToSsc.containsKey(chatterName) || chatterName == null) {
                    System.out.println("that name is already in use or invalid, try again");
                    sendMessage("name already in use or invalid, try again");
                    chatterName = listenForChats();
                }
                sendMessage("VALID NAME");
                nameToSsc.put(chatterName, this) ;
                ClientSession seshLog = new ClientSession(LocalDateTime.now(), null );
                clientSessionLogs.put(chatterName, seshLog);
                String msg;
                boolean stayInLoop = true;
                while (stayInLoop) {

                    msg = listenForChats();

                    if (msg.equals("exit")) {
                        numPlayers--;
                        sendMessage("exiting server");
                        clientSessionLogs.get(chatterName).setEnd(LocalDateTime.now());

                        break;
                    } else if (msg.equals("list")) {
                        String errorMsg = "";
                        while (true) {

                            sendMessage(errorMsg + listFiles() +
                                    "\n type \"exit\" to exit the server or \"leave\" to leave the list file menu");
                            msg = listenForChats();
                            if (msg.equals("leave")) {
                                sendMessage("left file menu");

                                break;
                            }
                            else if (msg.equals("exit")) {
                                numPlayers--;
                                sendMessage("exiting server");
                                clientSessionLogs.get(chatterName).setEnd(LocalDateTime.now());
                                stayInLoop = false;
                                break;
                            } else if (filesSet.contains(msg)) {
                                sendFile(msg);
                                break;
                            }
                            errorMsg = " ERROR file not in the list! \n";
                        }
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

        public void sendFile(String fileName) {
            File file = new File("src/A1/ServerFiles/" + fileName);

            try {
                if (!file.exists() || !file.isFile()) {
                    dataOut.writeUTF("ERROR: File not found");
                    dataOut.flush();
                    return;
                }
                sendMessage("FILE_RECEIVE_MODE");
                dataOut.writeUTF(fileName);

                long fileSize = file.length();
                dataOut.writeLong(fileSize);


                FileInputStream fileIn = new FileInputStream(file);
                byte[] buffer = new byte[32 * 1024]; // 32 KB
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                dataOut.flush();
                fileIn.close();
                System.out.println("File sent: " + fileName);

            } catch (IOException e) {
                System.out.println("IOException in sendFile()");
                e.printStackTrace();
            }
        }


        public String listFiles() {
            if (filesSet == null || filesSet.isEmpty()) {
                System.out.println("Repo is empty");
                return "ServerFiles Repo is empty";
            }

            StringBuilder fileListStr = new StringBuilder("list:\n");

            for (String fileName : filesSet) {
                fileListStr.append("- ").append(fileName).append("\n");
            }

            return fileListStr.toString();
        }

        public void sendMessage(String message) {
            try{
                dataOut.writeUTF(message); // this is sending to server an string
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
            System.out.println("Revceived msg: "+ msg + " from " + chatterName);

            return msg;
        }

        public void close() {
            try {
                if (dataIn != null) dataIn.close();
                if (dataOut != null) dataOut.close();
                if (socket != null && !socket.isClosed()) socket.close();
                nameToSsc.remove(chatterName);
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
        System.out.println(s.clientSessionLogs);


    }
}

