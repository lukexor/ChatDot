package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Date;

import main.ChatDotMessage;
import main.ChatDotUser;

public class ChatDotServer
{
    private static final String USER_RECORD_FILE = "userRecordFile.txt";
    private static final SimpleDateFormat dateFormatter
        = new SimpleDateFormat("HH:mm:ss");
    private static int uniqueId,
                       listenPort,
                       roomThrottle,
                       maxConnections;  // 0 is unlimited
    private ServerSocket serverSocket;
    private InetAddress hostAddress;
    private Socket socket;
    private ArrayList<ClientThread> clients;
    private boolean keepGoing;

    /*
     * Constructors
     */
    public ChatDotServer()
    {
        this(4444, 200, 100);
    }  // end constructor

    public ChatDotServer(int listenPort, int roomThrottle, int maxConnections)
    {
        this.uniqueId       = 0;
        this.listenPort     = listenPort;
        this.roomThrottle   = roomThrottle;
        this.maxConnections = maxConnections;
        this.clients        = new ArrayList<ClientThread>();

        try {
            hostAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            display("Could not get the host address.");
            return;
        }
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
        ChatDotUser sender = msg.getSender();
        String timestamp = "[" + dateFormatter.format(new Date()) + "] ";
        String message   = sender.getUsername() + ": " + msg.getContent();
        display(message);
        // Loop in reverse order in case clients have disconnected
        ChatDotUser broadcast = new ChatDotUser("Broadcast");
        String onlineUsersString = "";
        for (int i = clients.size() - 1; i >= 0; --i) {
            ClientThread thread = clients.get(i);
            if (thread.getUsername().equals(sender.getUsername())) continue;
            if (!thread.sendMessage(timestamp + message, MessageType.MESSAGE, broadcast)) {
                clients.remove(i);
                display("Disconnected Client " + thread.getUsername()
                        + " removed from client list.");
            } else {
                ChatDotUser recipient = new ChatDotUser(thread.getUsername());
                logChatHistory(recipient, broadcast, "IN", timestamp, "(From: " + sender.getUsername()
                        + ") " + msg.getContent());
                onlineUsersString += thread.getUsername();
                if (onlineUsersString.length() > 0 && i > 0) {
                    onlineUsersString += ", ";
                }
            }
        }  // end for
        logChatHistory(sender, broadcast, "OUT", timestamp, "(To: " + onlineUsersString
                + ") " + msg.getContent());
    }  // end broadcast

    public synchronized void sendUserMessage(ChatDotMessage msg)
    {
        // Find the users that match
        ChatDotUser sender = msg.getSender();
        String timestamp = "[" + dateFormatter.format(new Date()) + "] ";
        String message   = sender.getUsername() + ": " + msg.getContent();
        ArrayList<ChatDotUser> recipients = msg.getRecipients();
        for (int i = recipients.size() - 1; i >= 0; --i) {
            ChatDotUser recipient = recipients.get(i);
            for (int j = clients.size() - 1; j >= 0; --j) {
                ClientThread thread = clients.get(j);
                if (thread.getUsername().equals(sender.getUsername())) continue;
                // Found a match, send message
                logChatHistory(sender, recipient, "OUT", timestamp, msg.getContent());
                logChatHistory(recipient, sender, "IN", timestamp, msg.getContent());
                if (!thread.sendMessage(timestamp + message, MessageType.MESSAGE, sender))
                {
                        clients.remove(i);
                        display("Disconnected Client " + thread.getUsername()
                                + " removed from client list.");
                }
            }  // end for j
        } // end for i
    }  // end sendUserMessage

    public synchronized boolean register(ChatDotMessage msg)
    {
        ChatDotUser user = msg.getSender();
        String username  = user.getUsername();
        String password  = user.getPassword();

        // Check if exists
        if (username.equals("Broadcast")) {
            // Specially reserved name
            sendError(username, "Broadcast is reserved. Please choose another username.");
            return false;
        } else if (findRecord(username)) {
            sendError(username, username + " already exists.");
            return false;
        }

        // Write the new record
        BufferedWriter bWriter = null;
        FileWriter fWriter     = null;
        try {
            File file = new File(USER_RECORD_FILE);
            fWriter   = new FileWriter(file, true);
            bWriter   = new BufferedWriter(fWriter);
            bWriter.write(username + ":" + password + "\n");
            bWriter.flush();
            display(username + " registered.");
        } catch (Exception e) {
            sendError(username, "Failed to register.");
            return false;
        }
        return true;
    }  // end register

    public synchronized boolean login(ChatDotMessage msg)
    {
        ChatDotUser user = msg.getSender();
        String username  = user.getUsername();
        String password  = user.getPassword();

        // Check if exists
        if (!findRecord(username, password)) {
            sendError(username, "Invalid username/password.");
            return false;
        }
        // TODO: Prevent user from loggin in twice

        display(username + " logged in.");
        // Login and update users
        for (int i = 0; i < clients.size(); ++i) {
            ClientThread client = clients.get(i);
            client.sendStatus(username, "Logged In", false);
            client.setLoggedIn();
            if (client.getUsername().equals(username)) {
                client.sendMessage("", MessageType.LOGIN, null);
            }
        }  // end for
        return true;
    }  // end login

    void purgeUserDatabase()
    {
        BufferedWriter bWriter = null;
        FileWriter fWriter     = null;
        try {
            File file = new File(USER_RECORD_FILE);
            fWriter   = new FileWriter(file, false);
            bWriter   = new BufferedWriter(fWriter);
            bWriter.write("");
            bWriter.flush();
            display("Purged user database.");
        } catch (Exception e) {
            display("Failed to purge user database.");
        }
    }  // end method

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
        ClientThread thread = null;
        for (int i = 0; i < clients.size(); ++i) {
            thread = clients.get(i);
            if (thread.getThreadId() == threadId) {
                display(thread.getUsername() + " disconnected.");
                for (int j = 0; j < clients.size(); ++j) {
                    if (i == j) continue;
                    clients.get(j).sendStatus(thread.getUsername() ,"Logged Out", false);
                }  // end for
                thread.close();
                clients.remove(i);
                return;
            }
        }  // end for
    }  // end logout

    public String getChatHistory(ChatDotUser user)
    {
        BufferedReader bReader = null;
        FileReader fReader = null;
        String history = "";
        try {
            File file = new File("ChatHistory/" + user.getUsername() + ".txt");
            if (!file.exists()) {
                return history;
            }
            fReader = new FileReader(file);
            bReader = new BufferedReader(fReader);
            String currentLine;
            while ((currentLine = bReader.readLine()) != null) {
                history += currentLine + "\n";
            }
            return history;
        } catch (Exception e) {
            // No harm if we can't read
        }
        return history;
    }  // end getChatHistory

    /*
     * Private Methods
     */
    private void logChatHistory(ChatDotUser logUser, ChatDotUser toFromUser,
            String direction, String timestamp, String message)
    {
        BufferedWriter bWriter = null;
        FileWriter fWriter     = null;
        try {
            File file = new File("ChatHistory/" + logUser.getUsername() + ".txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            fWriter   = new FileWriter(file, true);
            bWriter   = new BufferedWriter(fWriter);
            if (direction.equals("OUT")) {
                bWriter.write(timestamp + logUser.getUsername() + " -> "
                    + toFromUser.getUsername() + ": " + message + "\n");
            } else {
                bWriter.write(timestamp + toFromUser.getUsername() + " -> "
                    + logUser.getUsername() + ": " + message + "\n");
            }
            bWriter.flush();
        } catch (Exception e) {
            display("Failed to log chat. " + e.toString());
        }
    }  // end logChatHistory

    private boolean findRecord(String username)
    {
        return findRecord(username, null);
    }  // end findRecord


    private boolean findRecord(String username, String password)
    {
        File file = new File(USER_RECORD_FILE);
        BufferedReader bReader = null;
        FileReader fReader = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fReader = new FileReader(file);
            bReader = new BufferedReader(fReader);

            String currentLine;
            String toMatch;
            if (password != null) {
                toMatch = username + ":" + password;
            } else {
                toMatch = username;
            }
            while ((currentLine = bReader.readLine()) != null) {
                if (password == null) {
                    currentLine = currentLine.split(":")[0];
                }
                if (toMatch.equals(currentLine)) {
                    return true;
                }
            }
        } catch (Exception e) {
            display("Failed to read " + USER_RECORD_FILE);
        }
        return false;
    }  // end findRecord

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

    private void sendError(String username, String msg)
    {
        for (int i = clients.size() - 1; i >= 0; --i) {
            ClientThread client = clients.get(i);
            if (client.getUsername().equals(username)) {
                client.sendMessage(msg, MessageType.ERROR, null);
            }
        }
    }  // end sendError

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
        private boolean loggedIn;

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
                            sendUserMessage(msg);
                        }
                        break;
                    case REGISTER:
                        if (register(msg)) {
                            login(msg);
                        } else {
                            // TODO: Error
                            display("Failed to register " + msg.getSender().getUsername());
                        }
                        break;
                    case LOGIN:
                        if (!login(msg)) {
                            // TODO: Error
                            display("Failed to login " + msg.getSender().getUsername());
                        }
                        break;
                    case LOGOUT:
                        keepGoing = false;
                        break;
                    case WHO:
                        HashMap<String, String> userStatus = new HashMap<String, String>();
                        File file = new File(USER_RECORD_FILE);
                        BufferedReader bReader = null;
                        FileReader fReader = null;
                        try {
                            if (!file.exists()) {
                                break;
                            }
                            fReader = new FileReader(file);
                            bReader = new BufferedReader(fReader);

                            String currentLine;
                            while ((currentLine = bReader.readLine()) != null) {
                                String username = currentLine.split(":")[0];
                                if (username.length() > 0) {
                                    userStatus.put(username, "Logged Out");
                                }
                            }
                        } catch (Exception e) {
                            // No harm if we couldn't read a file
                        }
                        for (int i = 0; i < clients.size(); ++i) {
                            userStatus.put(clients.get(i).getUsername(), "Logged In");
                        }  // end for

                        Iterator iter = userStatus.entrySet().iterator();
                        while (iter.hasNext()) {
                            Map.Entry pair = (Map.Entry) iter.next();
                            boolean isLogin = msg.getContent().equals("Login") ? true : false;
                            sendStatus((String)pair.getKey(), (String)pair.getValue(), isLogin);
                            iter.remove();
                        }  // end for
                    case HISTORY:
                        updateChatHistory(getChatHistory(msg.getSender()));
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
        public void setLoggedIn()
        {
            loggedIn = true;
        }
        public boolean isLoggedIn()
        {
            return loggedIn;
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

        private boolean sendMessage(String msg, MessageType type, ChatDotUser sender)
        {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                oStream.writeObject(new ChatDotMessage(type, msg, sender));
            } catch (IOException e) {
                display("Error sending message to " + user.getUsername());
                display(e.toString());
            }  // end try/catch
            return true;
        }  // end sendMessage

        private boolean sendStatus(String username, String status, boolean isLogin)
        {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                ChatDotMessage msg = new ChatDotMessage(MessageType.STATUS, username, status, isLogin);
                oStream.writeObject(msg);
            } catch (IOException e) {
                display("Error sending message to " + user.getUsername());
                display(e.toString());
            }  // end try/catch
            return true;
        }  // end sendStatus

        private boolean updateChatHistory(String history)
        {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                ChatDotMessage msg = new ChatDotMessage(MessageType.HISTORY, history);
                oStream.writeObject(msg);
            } catch (IOException e) {
                display(e.toString());
            }  // end try/catch
            return true;
        }  // end updateChatHistory
    }  // end ClientThread
}  // end ChatDotServer
