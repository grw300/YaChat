package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;

/**
 * Created by Greg on 9/12/16.
 * Server implementation for YaChat system.
 */
public class MemD {
    ServerSocket welcomeSocket;
    DatagramSocket clientSocket;

    HashMap<String, String> chatParticipants;

    MemD(String[] args) throws IOException {
        chatParticipants = new HashMap<>();
        welcomeSocket = new ServerSocket(Integer.parseInt(args[0]));
        clientSocket = new DatagramSocket();
    }

    static String[] processMessage(String message) {
        return message.replace("\n", "").split(" ", 2);
    }

    public static void main(String[] args) throws IOException {
        DataOutputStream outToClient;
        BufferedReader inFromClient;

        MemD Y = new MemD(args);

        while (true) {
            Socket serverSocket = Y.welcomeSocket.accept();

            outToClient = new DataOutputStream(serverSocket.getOutputStream());
            inFromClient = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            String[] message = MemD.processMessage(inFromClient.readLine());

            switch (message[0]) {
                case "HELO":
                    if (Y.chatParticipants.get(message[0]) != null) {
                        outToClient.writeBytes(String.format("RJCT %s\n", message[0]));
                    } else {
                        String[] chatterInfo = message[1].split(" ");

                        Y.chatParticipants.put(chatterInfo[0], String.format("%s %s", chatterInfo[1], chatterInfo[2]));

                        StringBuilder joinedParticipants = new StringBuilder();

                        for (String screenName : Y.chatParticipants.keySet()) {
                            joinedParticipants.append(String.format("%s %s:", screenName, Y.chatParticipants.get(screenName)));
                        }
                        String participants = joinedParticipants.substring(0, joinedParticipants.length() - 1);

                        outToClient.writeBytes(String.format("ACPT %s\n", participants));

                        String out = String.format("JOIN %s %s %s\n", chatterInfo[0], chatterInfo[1], chatterInfo[2]);

                        for (String chatter : Y.chatParticipants.values()) {
                            String[] chatterIPPort = chatter.split(" ", 2);
                            Y.clientSocket.send(
                                    new DatagramPacket(
                                            out.getBytes(),
                                            out.getBytes().length,
                                            InetAddress.getByName(chatterIPPort[0]),
                                            Integer.parseInt(chatterIPPort[1])
                                    )
                            );
                        }

                        Thread t = new Thread(new Child(chatterInfo[0], serverSocket, Y));
                        t.start();
                    }
                    break;
                default:
                    System.out.println(String.format("Incorrect protocol: %s %s", message[0], message[1]));
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
    }

    public void run() {
        try {
            BufferedReader inFromClient =
                    new BufferedReader(new
                            InputStreamReader(out.getInputStream()));

            String[] in = MemD.processMessage(inFromClient.readLine());

            if (in[0].equals("EXIT")) {
                String message = String.format("EXIT %s\n", screenName);

                for (String chatter : app.chatParticipants.values()) {
                    String[] chatterIPPort = chatter.split(" ", 2);
                    app.clientSocket.send(
                            new DatagramPacket(
                                    message.getBytes(),
                                    message.getBytes().length,
                                    InetAddress.getByName(chatterIPPort[0]),
                                    Integer.parseInt(chatterIPPort[1])
                            )
                    );
                }
                app.chatParticipants.remove(screenName);
            } else {
                System.out.println(String.format("Protocol problems with %s", screenName));
            }
        } catch (IOException e) {
            System.out.println(String.format("Socket problems with %s", screenName));
        }
    }
}
