package main;

import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import main.ChatDotUser;
import main.ChatDotMessage;

public class ChatDotClient
{
    private static final SimpleDateFormat dateFormatter
        = new SimpleDateFormat("HH:mm:ss");

    // Server details
    private static int serverPort,
                       userThrottle;
    private static String hostname;
    private boolean connected;

    // Interface is connected
    private ChatDotClientInterface clientInterface;

    // User details
    private String loginDate;
    private ChatDotUser user;
    private ChatDotMessage message;


    // I/O
    private ObjectInputStream iStream;
    private ObjectOutputStream oStream;
    private Socket socket;

    /*
     * Constructors
     */
    public ChatDotClient()
    {
        this("localhost", 4444, 200);
    }  // end constructor

    public ChatDotClient(ChatDotUser user, ChatDotClientInterface clientInterface)
    {
        this(user, "localhost", 4444, 200, clientInterface);
    }  // end constructor

    public ChatDotClient(String hostname, int serverPort, int userThrottle)
    {
        this(new ChatDotUser("Anonymous"), hostname, serverPort, userThrottle);
    }  // end constructor

    public ChatDotClient(ChatDotUser user, String hostname, int serverPort, int userThrottle)
    {
        this(user, hostname, serverPort, userThrottle, null);
    }  // end constructor

    public ChatDotClient(ChatDotUser user, String hostname, int serverPort, int userThrottle, ChatDotClientInterface clientInterface)
    {
        this.hostname        = hostname;
        this.serverPort      = serverPort;
        this.userThrottle    = userThrottle;
        this.clientInterface = clientInterface;
        this.connected       = false;
        this.user            = user;

    }  // end constructor

    /*
     * Main
     *
     * To start the Client in CLI mode use one of:
     * > java ChatDotClient username
     * > java ChatDotClient username hostname
     * > java ChatDotClient username hostname portNumber
     * > java ChatDotClient username hostname portNumber userThrottle
     */
    public static void main(String[] args)
    {
        String hostname  = "localhost",
               username  = "Anonymous",
               password  = "";
        int serverPort   = 4444,
            userThrottle = 200;
        switch (args.length) {
            case 5:
                try {
                    userThrottle = Integer.parseInt(args[4]);
                } catch (Exception e) {
                    display_usage(e.toString());
                    return;
                }
            case 4:
                try {
                    serverPort = Integer.parseInt(args[3]);
                } catch (Exception e) {
                    display_usage(e.toString());
                    return;
                }
            case 3:
                hostname = args[2];
            case 2:
                password = args[1];
            case 1:
                username = args[0];
            case 0:
                break;
            default:
                display_usage("");
                return;
        }  // end switch

        ChatDotUser user     = new ChatDotUser(username, password);
        ChatDotClient client = new ChatDotClient(user, hostname, serverPort, userThrottle);
        if (!client.start()) return;

        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String msg = "";
            try {
                msg = scan.nextLine();
            } catch (Exception e) {
                // Can't do much
            }
            if (!client.isConnected()) {
                System.out.println("No longer connected to server. Exiting...");
                break;
            }
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatDotMessage(MessageType.LOGOUT, "", user));
                break;
            } else if (msg.equalsIgnoreCase("LOGIN")) {
                client.sendMessage(new ChatDotMessage(MessageType.LOGIN, "", user));
            } else if (msg.equalsIgnoreCase("REGISTER")) {
                client.sendMessage(new ChatDotMessage(MessageType.REGISTER, "", user));
            } else if (msg.equalsIgnoreCase("WHO")) {
                client.sendMessage(new ChatDotMessage(MessageType.WHO, "", user));
            } else {
                client.sendMessage(new ChatDotMessage(MessageType.MESSAGE, msg, user));
            }
        }  // end while
        client.disconnect();
    }  // end main

    /*
     * Public Methods
     */
    public boolean start()
    {
        try {
            socket = new Socket(hostname, serverPort);
        } catch (Exception e) {
            display("Failed to connect to server: " + hostname + ":" + serverPort + ". Error: " + e);
            return false;
        }

        display("Connection accepted " + socket.getInetAddress()
                + ":" + socket.getPort());

        try {
            iStream = new ObjectInputStream(socket.getInputStream());
            oStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            display("Failed to set up input/output streams: " + e);
            return false;
        }

        new ListenFromServer().start();
        try {
            oStream.writeObject(user);
        } catch (IOException e) {
            display("Failed to login: " + e);
            disconnect();
            return false;
        }
        connected = true;
        return true;
    }  // end start

    public void disconnect() {
        connected = false;
        try {
            if (iStream != null) iStream.close();
        } catch (Exception e) {}
        try {
            if (oStream != null) oStream.close();
        } catch (Exception e) {}
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {}
        if (clientInterface != null) clientInterface.connectionFailed();
    }

    public boolean register()
    {
        try {
            oStream.writeObject(new ChatDotMessage(MessageType.REGISTER, "", user));
        } catch (IOException e) {
            display("Failed to write to server: " + e);
            return false;
        }
        return true;
    }  // end register

    public boolean login()
    {
        try {
            oStream.writeObject(new ChatDotMessage(MessageType.LOGIN, "", user));
        } catch (IOException e) {
            display("Failed to write to server: " + e);
            return false;
        }
        return true;
    }  // end login

    public boolean getBuddyStatus()
    {
        try {
            oStream.writeObject(new ChatDotMessage(MessageType.WHO, "", user));
        } catch (IOException e) {
            display("Failed to write to server: " + e);
            return false;
        }
        return true;
    }  // end getBuddyStatus

    public boolean getChatHistory()
    {
        try {
            oStream.writeObject(new ChatDotMessage(MessageType.HISTORY, "", user));
        } catch (IOException e) {
            display("Failed to write to server: " + e);
            return false;
        }
        return true;
    }  // end getBuddyStatus

    /*
     * Getters/Setters
     */
    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setLoggedIn() {
        if (clientInterface != null) {
            clientInterface.login();
        }
    }

    /*
     * Unmodified Methods
     */
    void sendMessage(ChatDotMessage msg)
    {
        try {
            oStream.writeObject(msg);
        } catch (IOException e) {
            display("Failed to write to server: " + e);
        }
    }  // end sendMessage

    /*
     * Private Methods
     */
    private static void display_usage(String error)
    {
        System.out.println("Invalid arguments: " + error
                + ".\nUsage: java ChatDotServer "
                + "[username] [hostname] [serverPort] [userThrottle]");
    }

    private void display(String msg)
    {
        String message = "[" + dateFormatter.format(new Date()) + "] " + msg;
        if (clientInterface == null) {
            System.out.println(message);
        }
    }  // end display

    /*
     * Internal Classes
     */
    class ListenFromServer extends Thread
    {
        /*
         * Public Methods
         */
        public void run()
        {
            display("Logged in...");
            while (true) {
                try {
                    ChatDotMessage msg = (ChatDotMessage) iStream.readObject();
                    MessageType type = msg.getType();
                    if (clientInterface == null) {
                        System.out.println();
                        System.out.println(msg.getContent());
                        System.out.print("> ");
                    } else {
                        System.out.println(msg.getContent());
                        ChatDotUser sender = msg.getSender();
                        String username = "";
                        if (sender != null) username = sender.getUsername();
                        switch (type) {
                            case MESSAGE:
                                clientInterface.sendMessage(username, msg.getContent());
                                break;
                            case ERROR:
                                clientInterface.displayError(msg.getContent());
                                break;
                            case STATUS:
                                clientInterface.updateStatus(username, msg.getStatus());
                                break;
                            case LOGIN:
                                clientInterface.login();
                                break;
                            case HISTORY:
                                clientInterface.updateChatHistory(msg.getContent());
                                break;
                            default:
                                // Nothing
                        }  // end switch
                    }
                } catch (EOFException e) {
                    System.out.println();
                    display("Server disconnected.");
                    break;
                } catch (IOException e2) {
                    System.out.println();
                    display("Failed to read input stream: " + e2);
                    break;
                } catch (ClassNotFoundException e2) {
                    // Need to catch this but it won't fail
                }
            }  // end while
            if (clientInterface != null) {
                clientInterface.connectionFailed();
            }
            connected = false;
        }  // end run
    }  // end ListenFromServer extends Thread
}  // end ChatDotClient
