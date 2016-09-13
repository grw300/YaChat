package server;

import protocol.ChatParticipant;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Greg on 9/12/16.
 * Server implementation for YaChat system.
 */
public class MemD {
    ServerSocket welcomeSocket;
    DatagramSocket clientSocket;


    HashMap<String, ChatParticipant> chatParticipants;


    MemD(String[] args) throws Exception { //Constructor
        this.chatParticipants = new HashMap<>();
        this.welcomeSocket = new ServerSocket(java.lang.Integer.parseInt(args[0]));
        this.clientSocket = new DatagramSocket();


    }

    public static void main(String[] args) throws Exception {
        DataOutputStream outToClient;
        BufferedReader inFromClient;

        MemD Y = new MemD(args); //Invoke constructor, with command line args as inputs

        while (true) {
            Socket serverSocket = Y.welcomeSocket.accept();

            // Use Y to refer to all Variables declared in the class and initialized in the Constructor
            outToClient = new DataOutputStream(serverSocket.getOutputStream());
            inFromClient = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            String m = inFromClient.readLine();

            String receivedHandle = m.substring(0, 4);
            String receivedPacket = m.substring(5).replace("\n", "");

            switch (receivedHandle) {
                case "HELO":
                    if (Y.chatParticipants.get(receivedPacket) != null) {
                        String message = "RJCT " + receivedPacket + "\n";
                        outToClient.writeBytes(message);
                    } else {
                        String[] chatterInfo = receivedPacket.split(" ");

                        ChatParticipant newChatter = new ChatParticipant();
                        newChatter.IP = ((InetSocketAddress) serverSocket.getRemoteSocketAddress()).getAddress();
                        newChatter.port = java.lang.Integer.parseInt(chatterInfo[2]);
                                //((InetSocketAddress) serverSocket.getRemoteSocketAddress()).getPort();

                        Y.chatParticipants.put(chatterInfo[0], newChatter);

                        String joinedParticipants = "";

                        for (String screenName : Y.chatParticipants.keySet()) {
                            joinedParticipants += screenName + " " + Y.chatParticipants.get(screenName).toString() + ":";
                        }
                        joinedParticipants = joinedParticipants.substring(0, joinedParticipants.length() - 1);

                        outToClient.writeBytes("ACPT " + joinedParticipants + "\n");

                        String message = "JOIN " + chatterInfo[0] + " " + newChatter.toString() + "\n";

                        for (ChatParticipant chatter : Y.chatParticipants.values()
                                ) {
                            Y.clientSocket.send(new DatagramPacket(message.getBytes(), message.getBytes().length, chatter.IP, chatter.port));
                        }

                        Child newChild = new Child(chatterInfo[0], serverSocket, Y);

                    }
                    break;
                default:
                    break;
            }
        }
    }
}

class Child implements Runnable {
    String screenName;
    Socket out;
    MemD app;

    Child(String s, Socket o, MemD Y) {
        screenName = s;
        out = o;
        app = Y;
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        try {

            BufferedReader inFromClient =
                    new BufferedReader(new
                            InputStreamReader(out.getInputStream()));

            String clientSentence;
            clientSentence = inFromClient.readLine();
            String receivedHandle = clientSentence.replace("\n", "").replace(" ", "");

            if (receivedHandle.equals("EXIT")) {
                String message = "EXIT " + screenName + "\n";

                for (ChatParticipant chatter : app.chatParticipants.values()
                        ) {
                    app.clientSocket.send(new DatagramPacket(message.getBytes(), message.getBytes().length, chatter.IP, chatter.port));
                }
                app.chatParticipants.remove(screenName);
            }
        } catch (IOException e) {
            System.out.println("Socket problems");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
