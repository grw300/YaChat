package client;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.HashMap;

import protocol.*;

/**
    * TODO: implement the pseudo-code
    * TODO: Use a protocol state machine
*/

public class Chatter extends JFrame {

    Socket serverSocket;
    DatagramSocket clientSocket;

    HashMap<String, ChatParticipant> chatParticipants;

    Chatter (String ip, int port) throws Exception {
        this.chatParticipants = new HashMap<>();
        this.serverSocket = new Socket(ip, port);
        this.clientSocket = new DatagramSocket();
        //TODO: Setup GUI and Action/Events
    }

    public static void main(String[] args) throws Exception {

        Chatter X = new Chatter(args[1], java.lang.Integer.parseInt(args[2]));

        DataOutputStream outToServer =
                new DataOutputStream(X.serverSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new InputStreamReader(X.serverSocket.getInputStream()));

        String string1 = args[0];
        String string2 = InetAddress.getLocalHost().getHostAddress();
        String string3 = Integer.toString(X.clientSocket.getLocalPort());

        String helloMessage = String.format("HELO %s %s %s\n", string1, string2, string3);

        outToServer.writeBytes(helloMessage);
        String serverReply = inFromServer.readLine();

        if (serverReply.startsWith("ACPT")) {
            String[] chatParticipants = serverReply.replace("ACPT ","").split(":");
            for (String chatParticipant:chatParticipants
                 ) {
                String[] chatParticipantValues = chatParticipant.split(" ");

                ChatParticipant chatter = new ChatParticipant();
                chatter.screenName = chatParticipantValues[0];
                chatter.IP = chatParticipantValues[1];
                chatter.port = java.lang.Integer.parseInt(chatParticipantValues[2]);

                X.chatParticipants.put(chatParticipantValues[0], chatter);
            }
        } else {
            /*TODO: Handle the reject more gracefully!*/
            System.exit(1);
        }

        while (true) {
            //TODO: Read message from on UDP port;
            //TODO: Act according to message contents (See protocol)
        }


    }
}
