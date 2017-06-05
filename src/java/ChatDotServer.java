package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import main.ChatDotClient;
import main.ChatDotMessage;
import main.ChatDotUser;

public class ChatDotServer
{
    private static final SimpleDateFormat dateFormatter
        = new SimpleDateFormat("HH:mm:ss");
    private static int uniqueId,
                       listenPort,
                       roomThrottle,
                       maxConnections;  // 0 is unlimited
    private ServerSocket serverSocket;
    // private InetAddress hostAddress;
    private String hostAddress;
    private Socket socket;
    private ArrayList<ClientThread> clients;
    private boolean keepGoing;

    /*
     * Constructors
     */
    ChatDotServer()
    {
        this(4444, 200, 100);
    }  // end constructor

    ChatDotServer(int listenPort, int roomThrottle, int maxConnections)
    {
        this.uniqueId       = 0;
        this.listenPort     = listenPort;
        this.roomThrottle   = roomThrottle;
        this.maxConnections = maxConnections;
        this.clients        = new ArrayList<ClientThread>();

        // try {
            // hostAddress = InetAddress.getLocalHost();
            hostAddress = "localhost";
        // } catch (UnknownHostException e) {
        //     display("Could not get the host address.");
        //     return;
        // }
        display("Server host address is: " + hostAddress);
    }  // end constructor

    /*
     * Main
     */
    public static void main(String[] args) throws IOException
    {
        int listenPort     = 4444,
            roomThrottle   = 200,
            maxConnections = 100;
        // Parse custom listenPort and roomThrottle
        try {
            switch (args.length) {
                case 3:
                    maxConnections = Integer.parseInt(args[2]);
                case 2:
                    roomThrottle = Integer.parseInt(args[1]);
                case 1:
                    listenPort = Integer.parseInt(args[0]);
                case 0:
                    break;
                default:
                    System.out.println(
                            "Usage: java ChatDotServer [portNumber]");
                    return;
            }  // end switch
        } catch (Exception e) {
            System.out.println("Invalid arguments: " + e
                    + ".\nUsage: java ChatDotServer [portNumber]");
            return;
        }  // end try/catch

        ChatDotServer server = new ChatDotServer(listenPort, roomThrottle,
                maxConnections);
        server.start();
    }  // end main

    /*
     * Public Methods
    */
    public void start()
    {
        keepGoing = true;
        if (!initSocket()) return;

        while (keepGoing) {
            purgeDisconnectedClients();
            if (!initClientSocket()) break;
            if (!keepGoing) break;

            // Add our client
            ClientThread thread = new ClientThread(socket);
            clients.add(thread);
            thread.start();

            try {
                Thread.sleep(roomThrottle);
            } catch (InterruptedException e) {
                display("ChatDotServer was interrrupted.");
                break;
            }
        }  // end while (keepGoing)

        cleanup();
    }  // end start

    public synchronized void broadcast(ChatDotMessage msg)
    {
        String timestamp = "[" + dateFormatter.format(new Date()) + "] ";
        String message   = msg.getSender().getUsername() + ": " + msg.getContent();
        display(message);
        // Loop in reverse order in case clients have disconnected
        for (int i = clients.size() - 1; i >= 0; --i) {
            ClientThread thread = clients.get(i);
            // TODO: Save message to ChatHistory
            if (!thread.sendMessage(timestamp + message)) {
                clients.remove(i);
                display("Disconnected Client " + thread.getUsername()
                        + " removed from client list.");
            }
        }  // end for
    }  // end broadcast

    /*
     * Protected Methods
    */
    // For outside service to stop server - such as a UI
    protected void stop()
    {
        keepGoing = false;
        // Connect as a client to exit
        try {
            new Socket(hostAddress, listenPort);
        } catch(Exception e){
            // Nothing can be done
        }
    }  // end stop

    /*
     * Unmodified Methods
     */
    // For a client to logout using LOGOUT message
    synchronized void logout(int threadId)
    {
        for (int i = 0; i < clients.size(); ++i) {
            ClientThread thread = clients.get(i);
            if (thread.getThreadId() == threadId) {
                display(thread.getUsername() + " logged out.");
                thread.close();
                clients.remove(i);
                return;
            }
        }  // end for
    }  // end logout

    /*
     * Private Methods
     */
    private void display(String msg)
    {
        String message = "[" + dateFormatter.format(new Date()) + "] " + msg;
        System.out.println(message);
    }  // end display

    private boolean initSocket()
    {
        // Initialize the server socket
        try {
            serverSocket = new ServerSocket(listenPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + listenPort);
            return false;
        }
        display("ChatDotServer running on port "
            + listenPort + "...");
        return true;
    }  // end initSocket

    private void purgeDisconnectedClients()
    {
        for (int i = 0; i < clients.size(); ++i) {
            if (!clients.get(i).isConnected()) {
                display(clients.get(i)
                    + " removed due to lack of connection.");
                clients.get(i).close();
                clients.remove(i);
            }
        }
    }  // end purgeDisconnectedClients

    private boolean initClientSocket()
    {
        // Initialize the client socket for each connection
        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
            System.err.println("Failed to accept client.");
            return false;
        }
        display("Client " + socket + " has connected.");
        return true;
    }  // end initClientSocket

    private void cleanup()
    {
        try {
            serverSocket.close();
            for (int i = 0; i < clients.size(); ++i) {
                try {
                    clients.get(i).close();
                } catch (Exception e) {
                    // Not much can be done
                }
                clients.remove(i);
            }
        } catch (Exception e) {
            display("Failed to disconnect clients: " + e);
        }
    }  // end cleanup

    /*
     * Internal Classes
     * One instance of this will run for each client
     */
    class ClientThread extends Thread
    {
        private int threadId;
        private Socket socket;  // Socket to the server
        private ObjectInputStream iStream;
        private ObjectOutputStream oStream;
        private ChatDotUser user;
        private ChatDotMessage msg;
        private String date;

        /*
         * Constructors
         */
        ClientThread(Socket socket)
        {
            this.socket   = socket;
            this.threadId = ++uniqueId;
            String username = "";

            display("Setting up input/output streams.");
            try {
                // Output needs to be created first
                oStream  = new ObjectOutputStream(socket.getOutputStream());
                iStream  = new ObjectInputStream(socket.getInputStream());
                user = (ChatDotUser) iStream.readObject();
                if (user == null) {
                    display("Failed to get user");
                    return;
                }
            } catch (IOException e) {
                display("Failed to set up input/output streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
                // Have to catch this, but it's definitely a string
            }
            date = new Date().toString() + "\n";
        }  // end constructor

        /*
         * Public Methods
         */
        public void run()
        {
            // TODO: Notify other users of login
            // Keep running until LOGOUT is encountered
            boolean keepGoing = true;
            while (keepGoing) {
                // Read a string
                if (iStream == null || user == null) {
                    break;
                }
                try {
                    msg = (ChatDotMessage) iStream.readObject();
                } catch (IOException e) {
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }  // end try/catch

                switch (msg.getType())
                {
                    case MESSAGE:
                        if (msg.getRecipients() == null) {
                            broadcast(msg);
                        } else {
                            // Find the users that match
                            String timestamp = "[" + dateFormatter.format(new Date()) + "] ";
                            ArrayList<ChatDotUser> recipients = msg.getRecipients();
                            for (int i = recipients.size() - 1; i >= 0; --i) {
                                ChatDotUser recipient = recipients.get(i);
                                for (int j = clients.size() - 1; j >= 0; --j) {
                                    ClientThread thread = clients.get(j);
                                    // Found a match, send message
                                    // TODO: Log to chat history
                                    if (thread.getUsername() == recipient.getUsername()
                                        && !thread.sendMessage(timestamp + msg.getContent()))
                                    {
                                            clients.remove(i);
                                            display("Disconnected Client " + thread.getUsername()
                                                    + " removed from client list.");
                                    }
                                }  // end for j
                            } // end for i
                        }
                        break;
                    case LOGOUT:
                        keepGoing = false;
                        break;
                    case WHO:
                        for (int i = 0; i < clients.size(); ++i) {
                            sendMessage(clients.get(i).getUsername() + " is connected.");
                        }  // end for
                    default:
                        // Nothing
                }  // end switch
            }  // end while (true)
            // TODO: Notify other users of logout
            logout(threadId);
            close();
        }  // end run

        /*
         * Getters/Setters
         */
        public int getThreadId()
        {
            return this.threadId;
        }
        public String getUsername()
        {
            return user.getUsername();
        }
        public boolean isConnected()
        {
            if (this.socket != null) return socket.isConnected();
            return false;
        }

        /*
         * Private Methods
         */
        private void close()
        {
            try {
                if (oStream != null) oStream.close();
            } catch (IOException e) {}
            try {
                if (iStream != null) iStream.close();
            } catch (IOException e) {}
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {}
        }  // end disconnect

        private boolean sendMessage(String msg)
        {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                oStream.writeObject(msg);
            } catch (IOException e) {
                display("Error sending message to " + user.getUsername());
                display(e.toString());
            }  // end try/catch
            return true;
        }  // end sendMessage
    }  // end ClientThread
}  // end ChatDotServer
