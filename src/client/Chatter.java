package client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by Greg on 8/23/16.
 * Client implementation for YaChat system.
 */
interface State {
    boolean process(Chatter chatter) throws IOException;
}

enum Chat implements State {

    SEND_HELO {
        @Override
        public boolean process(Chatter chatter) throws IOException {
            String message = String.format("HELO %s %s %s\n", chatter.getScreenName(), chatter.getIP(), chatter.getPort());
            chatter.out().writeBytes(message);
            chatter.state(Chat.RECV_SERV);
            return true;
        }
    },
    SEND_MESG {
        @Override
        public boolean process(Chatter chatter) throws IOException {
            String message = String.format("MESG %s: %s\n", chatter.getScreenName(), chatter.getMessage());

            for (String chatterLocation : chatter.getChattersMap().values()) {
                String[] chatterIPPort = chatterLocation.split(" ");
                chatter.getSocket().send(
                        new DatagramPacket(
                                message.getBytes(),
                                message.getBytes().length,
                                InetAddress.getByName(chatterIPPort[0]),
                                Integer.parseInt(chatterIPPort[1])
                        )
                );
            }
            chatter.state(Chat.RECV_CHAT);
            return true;
        }
    },
    SEND_EXIT {
        @Override
        public boolean process(Chatter chatter) throws IOException {
            String message = "EXIT\n";
            chatter.out().writeBytes(message);
            chatter.state(Chat.RECV_CHAT);
            return true;
        }
    },
    RECV_SERV {
        @Override
        public boolean process(Chatter chatter) throws IOException {
            String[] message = processMessage(chatter.in().readLine());
            switch (message[0]) {
                case "ACPT":
                    chatter.setChattersMap(message[1]);
                    chatter.state(Chat.RECV_CHAT);
                    return true;
                case "RJCT":
                    System.out.println(String.format("Username %s is taken.", message[1]));
                    return false;
                default:
                    System.out.println("Incorrect protocol.");
                    return false;
            }
        }
    },
    RECV_CHAT {
        @Override
        public boolean process(Chatter chatter) throws IOException {
            byte[] receiveData = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            chatter.getSocket().receive(packet);
            String[] message = processMessage(new String(packet.getData(), 0, packet.getLength()));
            switch (message[0]) {
                case "MESG":
                    chatter.display.append(String.format("%s\n", message[1]));
                    return true;
                case "JOIN":
                    String[] newChatter = message[1].split(" ");
                    String newChatterScreenName = newChatter[0];
                    if (newChatterScreenName.equals(chatter.getScreenName())) {
                        chatter.setGui();
                        chatter.display.append(String.format("%s, I want to thank you for coming. Welcome.\n", newChatterScreenName));
                        System.out.println(String.format("Your IP is %s.\nYour port is %s.", newChatter[1], newChatter[2]));
                    } else {
                        chatter.display.append(String.format("%s has joined us!\n", newChatterScreenName));
                    }
                    chatter.setChattersMap(message[1]);
                    return true;
                case "EXIT":
                    if (message[1].equals(chatter.getScreenName())) {
                        System.out.println("Leaving the chat room.");
                        return false;
                    } else {
                        chatter.display.append(String.format("%s has left us all alone.\n", message[1]));
                    }
                    return true;
                default:
                    System.out.println("Incorrect protocol.");
                    return false;
            }

        }
    };

    String[] processMessage(String message) {
        return message.replace("\n", "").split(" ", 2);
    }
}

public class Chatter {

    private String screenName;
    private String message;
    private String IP;
    private String port;

    private DataOutputStream out;
    private BufferedReader in;
    private DatagramSocket socket = new DatagramSocket();
    private HashMap<String, String> chattersMap = new HashMap<>();

    private Chat chat;

    private JTextField enter;
    public JTextArea display;

    public String getScreenName() {
        return screenName;
    }

    public String getIP() {
        return IP;
    }

    public String getPort() {
        return port;
    }

    public String getMessage() {
        return message;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public HashMap<String, String> getChattersMap() {
        return chattersMap;
    }

    public void setChattersMap(String chatters) {
        for (String chatter : chatters.split(":")) {
            String[] chatID = chatter.split(" ", 2);
            chattersMap.put(chatID[0], chatID[1]);
        }
    }

    public DataOutputStream out() throws IOException {
        return out;
    }

    public BufferedReader in() throws IOException {
        return in;
    }

    public State state() {
        return chat;
    }

    public void state(State state) {
        chat = ((Chat) state);
    }

    Chatter(String[] args) throws IOException {
        screenName = args[0];
        Socket serverSocket = new Socket(args[1], Integer.parseInt(args[2]));

        IP = InetAddress.getLocalHost().getHostAddress();
        port = Integer.toString(socket.getLocalPort());

        out = new DataOutputStream(serverSocket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
    }

    void setGui() {
        JFrame gui = new JFrame(screenName);

        Container c = gui.getContentPane();

        display = new JTextArea();

        enter = new JTextField();
        enter.setEnabled(true);
        enter.addActionListener(
                (a) -> {
                    message = a.getActionCommand();
                    enter.setText("");
                    this.state(Chat.SEND_MESG);
                    guiSendMessage(message);
                }
        );

        JButton qButton = new JButton("QUIT");
        qButton.setEnabled(true);
        qButton.addActionListener(
                (a) -> {
                    message = "EXIT\n";
                    this.state(Chat.SEND_EXIT);
                    guiSendMessage(message);
                }
        );

        c.add(new JScrollPane(display), BorderLayout.CENTER);
        c.add(enter, BorderLayout.NORTH);
        c.add(qButton, BorderLayout.SOUTH);

        gui.setSize(640, 480);
        gui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        gui.setVisible(true);
    }

    private void guiSendMessage(String message) {
        try {
            this.state().process(this);
        } catch (IOException e) {
            System.out.println(String.format("There was a problem sending your message: %s.", message));
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        Chatter X = new Chatter(args);

        X.state(Chat.SEND_HELO);

        while (X.state().process(X)) ;

        System.exit(0);
    }
}
