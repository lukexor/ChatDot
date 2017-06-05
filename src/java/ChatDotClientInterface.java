package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import main.ChatDotUser;
import main.ChatDotClient;
import main.ChatDotMessage;

enum PaneType {
    LOGIN, BUDDY, CHAT
}

public class ChatDotClientInterface extends JFrame implements ActionListener
{
    // Login Pane
    private JTextField usernameField;
    private JTextField passwordField;
    private JButton loginButton,
                    registerButton;

    // Buddy List Pane
    private JLabel currentUsernameLabel;
    private JButton broadcastButton,
                    logoutButton,
                    chatHistoryButton;

    // Chat Panes
    private JLabel recipientLabel;
    private JTextArea chatHistory;
    private JTextField messageField;
    private JButton sendMessageButton;

    // The client that communicates with the ChatDot server
    private ChatDotClient client;
    // Server details
    private int serverPort;
    private String serverHostname;
    // Whether we are connected to the server or not
    private boolean connected;

    // The currently logged in user
    private ChatDotUser user;

    // UI Panels
    private ChatDotClientInterface loginPane,
                                   buddyListPane;
    private ArrayList<ChatDotClientInterface> chatPanes;

    /*
     * Constructors
     */
    ChatDotClientInterface(PaneType paneType, String recipient)
    {
        super("ChatDot");
        switch (paneType) {
            case BUDDY:
                displayBuddyList();
                break;
            case CHAT:
                displayChatWindow(recipient);
                break;
            default:
                displayLogin("");
        }  // end switch (paneType)
    }  // end constructor

    /*
     * Main
     */
    public static void main(String[] args)
    {
        ChatDotClientInterface loginPane = new ChatDotClientInterface(PaneType.LOGIN, "");
    }  // end main

    /*
     * Public Methods
     */
    public void connectionFailed()
    {
        closeBuddyList();
        closeChatWindows();
        displayLogin("Disconnected from server.");
        connected = false;
    }  // end connectionFailed

    public void actionPerformed(ActionEvent event)
    {
        Object object = event.getSource();
        if (object == logout) {
            client.sendMessage(new ChatDotMessage(MessageType.LOGOUT, "", user));
            return;
        } else if (object == who) {
            client.sendMessage(new ChatDotMessage(MessageType.WHO, "", user));
            return;
        } else if (connected) {
            client.sendMessage(new ChatDotMessage(MessageType.MESSAGE,
                        textInputField.getText(), user));
            textInputField.setText("");
            return;
        } else if (object == login) {
            String username = textInputField.getText().trim();
            if (username.length() == 0) return;
            String hostname = serverHostnameField.getText().trim();
            if (hostname.length() == 0) return;
            String portString = serverPortField.getText().trim();
            if (portString.length() == 0) return;
            int port = 0;
            try {
                port = Integer.parseInt(portString);
            } catch (Exception e) {
                return;
            }

            user = new ChatDotUser(username);
            client = new ChatDotClient(user, hostname, port, 200, this);
            if (!client.start()) return;
            textInputField.setText("");
            label.setText("Enter your message:");
            connected = true;

            login.setEnabled(false);
            logout.setEnabled(true);
            who.setEnabled(true);
            serverHostnameField.setEditable(false);
            serverPortField.setEditable(false);
            textInputField.addActionListener(this);
        }
    }  // end actionPerformed

    /*
     * Unmodified Methods
     */
    void append(String str)
    {
        // chatHistory.append(str);
        // chatHistory.setCaretPosition(chatHistory.getText().length() - 1);
    }

    /*
     * Private Methods
     */
    private void displayLogin(String error)
    {
        setTitle("ChatDot Login");

        // Initialize panes
        buddyListPane = null;
        chatPanes     = new ArrayList<ChatDotClientInterface>();

        // Main Login Panel
        JPanel loginPanel = new JPanel(new GridLayout(5, 1));

        // Welcome text
        JLabel loginLabel = new JLabel("Welcome to ChatDot!",
                SwingConstants.CENTER);
        // Error text for failed logins, duplicate username, etc
        JLabel errorLabel = new JLabel("");
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginPanel.add(loginLabel);
        loginPanel.add(errorLabel);

        // Login Fields Panel
        // TODO: Lookup JFrame docs and format this better
        JPanel loginFieldsPanel = new JPanel(new GridLayout(2, 2));
        JLabel usernameLabel    = new JLabel("Username: ");
        JLabel passwordLabel    = new JLabel("Password: ");
        usernameField           = new JTextField("");
        passwordField           = new JTextField("");
        usernameField.setBackground(Color.WHITE);
        passwordField.setBackground(Color.WHITE);
        loginFieldsPanel.add(usernameLabel);
        loginFieldsPanel.add(usernameField);
        loginFieldsPanel.add(passwordLabel);
        loginFieldsPanel.add(passwordField);
        loginPanel.add(loginFieldsPanel);

        // Login Buttons Panel
        JPanel loginButtonsPanel = new JPanel(new GridLayout(2, 2));
        JButton loginButton      = new JButton("Login");
        JButton registerButton   = new JButton("Register");
        loginButton.addActionListener(this);
        registerButton.addActionListener(this);
        // Spacers
        loginButtonsPanel.add(new JPanel(new GridLayout(2, 2)));
        loginButtonsPanel.add(new JPanel(new GridLayout(2, 2)));
        loginButtonsPanel.add(loginButton);
        loginButtonsPanel.add(registerButton);
        loginPanel.add(loginButtonsPanel);

        // Add it to the frame
        add(loginPanel, BorderLayout.CENTER);

        // Put cursor in the login field
        loginButton.requestFocus();

        // Make it visible
        // TODO: Move frame to center of screen
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setVisible(true);

        // TODO: Move this instantiation to logging in, this displays
        // the Buddy List
        // buddyListPane = new ChatDotClientInterface(PaneType.BUDDY, "");
        // TODO: Add chatpanes
        // chatPanes.add(new ChatDotClientInterface(PaneType.CHAT, username);
    }  // end displayLogin

    private void displayBuddyList()
    {
        setTitle("Buddy List");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setVisible(true);
    }  // end displayBuddyList

    private void closeBuddyList()
    {
    }  // end closeBuddyList

    private void displayChatWindow(String recipient)
    {
        setTitle(recipient);
        setSize(400, 300);
        setVisible(true);
    }  // end displayChatWindow

    private void closeChatWindows()
    {
    }  // end closeChatWindows
}
