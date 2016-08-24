package client;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.HashMap;

public class Chatter extends JFrame {

    Socket serverSocket;
    Socket clientSocket;
    HashMap<String, int[]> chatParticipants;

    Chatter (String ip, int port) throws Exception {
        this.serverSocket = new Socket(ip, port);

    }

    public static void main(String[] args) throws Exception {
        Chatter X = new Chatter(args[1], java.lang.Integer.parseInt(args[2]));

        DataOutputStream outToServer =
                new DataOutputStream(X.serverSocket.getOutputStream());

        BufferedReader inFromServer =
                new BufferedReader(new InputStreamReader(X.serverSocket.getInputStream()));

        String helloMessage = String.format("HELO %s %s %s\n", args[0], X.clientSocket.getInetAddress().toString(), X.clientSocket.getPort());

        outToServer.writeBytes(helloMessage);
        String serverReply = inFromServer.readLine();

    }
}
