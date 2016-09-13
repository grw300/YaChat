package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.HashMap;

import protocol.*;

/**
 * TODO: Use a protocol state machine?
 */

public class Chatter extends JFrame {

    Socket serverSocket;
    DatagramSocket clientSocket;
    DataOutputStream outToServer;
    BufferedReader inFromServer;

    String screenName;
    HashMap<String, ChatParticipant> chatParticipants;

    private JTextField enter;
    private JTextArea display;
    private JButton qbutton;

    Chatter(String ip, int port) throws Exception {
        super("Chatter");

        this.chatParticipants = new HashMap<>();
        this.serverSocket = new Socket(ip, port);
        this.clientSocket = new DatagramSocket();

        this.outToServer = new DataOutputStream(this.serverSocket.getOutputStream());
        this.inFromServer = new BufferedReader(new InputStreamReader(this.serverSocket.getInputStream()));

        Container c = getContentPane();
        enter = new JTextField();
        enter.setEnabled(true);

        enter.addActionListener(
                (e) -> {
                    this.SendMessage(e.getActionCommand());
                    enter.setText("");
                }
        );

        c.add(enter, BorderLayout.NORTH);

        display = new JTextArea();
        c.add(new JScrollPane(display), BorderLayout.CENTER);

        qbutton = new JButton("QUIT");
        qbutton.setEnabled(true);
        qbutton.addActionListener(
                (e) -> this.SendExit()
        );

        c.add(qbutton, BorderLayout.SOUTH);
        setSize(640, 480);

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.setVisible(true);
    }

    public static void main(String[] args) throws Exception {

        Chatter X = new Chatter(args[1], java.lang.Integer.parseInt(args[2]));

        X.screenName = args[0];
        ChatParticipant me = new ChatParticipant();

        me.IP = InetAddress.getLocalHost();
        me.port = X.clientSocket.getLocalPort();

        String helloMessage = String.format("HELO %s %s %s\n", X.screenName, me.IP.getHostAddress(), Integer.toString(me.port));

        X.outToServer.writeBytes(helloMessage);
        String serverReply = X.inFromServer.readLine();

        if (serverReply.startsWith("ACPT")) {
            String[] chatParticipants = serverReply.replace("ACPT ", "").split(":");
            for (String chatParticipant : chatParticipants
                    ) {
                String[] chatParticipantValues = chatParticipant.split(" ");

                ChatParticipant chatter = new ChatParticipant();
                chatter.IP = InetAddress.getByName(chatParticipantValues[1]);
                chatter.port = java.lang.Integer.parseInt(chatParticipantValues[2]);

                X.chatParticipants.put(chatParticipantValues[0], chatter);
            }
        } else {
            System.out.println("Username " + X.screenName + " rejected by server.");
            System.exit(1);
        }

        while (true) {
            try {
                // Create a byte buffer/array for the receive Datagram packet
                byte[] receiveData = new byte[1024];
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                X.clientSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());

                String receivedHandle = received.substring(0,4);
                String receivedPacket = received.substring(5).replace("\n", "");

                switch(receivedHandle) {
                    case "MESG":
                        X.WriteMessage(receivedPacket);
                        break;
                    case "JOIN":
                        String[] receivedPacketValues = receivedPacket.split(" ");
                        ChatParticipant chatter = new ChatParticipant();
                        chatter.IP = InetAddress.getByName(receivedPacketValues[1]);
                        chatter.port = java.lang.Integer.parseInt(receivedPacketValues[2]);

                        X.chatParticipants.put(receivedPacketValues[0], chatter);

                        X.WriteMessage(receivedPacketValues[0] + " has joined the fun!");
                        break;
                    case "EXIT":
                        X.chatParticipants.remove(receivedPacket);
                        if (receivedPacket.equals(X.screenName)) {
                            System.exit(0);
                        } else {
                            X.WriteMessage(receivedPacket + " has left us all alone.");
                        }
                        break;
                }

            } catch(Exception e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    private void SendMessage(String actionCommand) {
        for (ChatParticipant chatter : this.chatParticipants.values()
                ) {
            String message = "MESG " + this.screenName + ": " + actionCommand + '\n';
            try {
                this.clientSocket.send(new DatagramPacket(message.getBytes(), message.getBytes().length, chatter.IP, chatter.port));
            } catch (IOException e) {
                display.append(e.getMessage());
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    private void SendExit() {
        String exitMessage = "EXIT\n";
        try {
            this.outToServer.writeBytes(exitMessage);
        } catch (IOException e) {
            display.append(e.getMessage());
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private void WriteMessage( String message )
    {
        display.append( "\n" + message ); // write to the TextArea
    }

}
