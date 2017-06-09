package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;

import main.ChatDotUser;
import main.ChatDotClient;
import main.ChatDotMessage;
import main.ChatDotChatWindow;

// interface ChatDotUICallback
// {

// }

public class ChatDotClientInterface extends JFrame implements ActionListener
{
    // Login Pane
    private JTextField usernameField;
    private JTextField passwordField;
    private JButton loginButton,
                    registerButton;

    // Buddy List Pane
    private JFrame buddyFrame,
                   chatHistoryFrame;
    private JPanel buddyList;
    private JScrollPane buddyScrollList;
    private JLabel currentUsernameLabel;
    private JButton broadcastButton,
                    logoutButton,
                    chatHistoryButton;
    private JTextArea chatHistoryArea;

    // Chat Panes
    private HashMap<String, ChatDotChatWindow> chatWindows;

    // The client that communicates with the ChatDot server
    private ChatDotClient client;
    // Whether we are connected to the server or not
    private boolean connected,
                    loggedIn;

    // The currently logged in user
    private ChatDotUser user;
    private HashMap<String, JButton> buddyButtons;
    private HashMap<String, ChatDotUser> buddies;

    /*
     * Constructors
     */
    ChatDotClientInterface()
    {
        super("ChatDot");
        buddies      = new HashMap<String, ChatDotUser>();
        chatWindows  = new HashMap<String, ChatDotChatWindow>();
        buddyButtons = new HashMap<String, JButton>();
        displayLogin();
    }  // end constructor

    /*
     * Main
     */
    public static void main(String[] args)
    {
        ChatDotClientInterface loginPane = new ChatDotClientInterface();
    }  // end main

    /*
     * Public Methods
     */
    public void connectionFailed()
    {
        closeBuddyList();
        closeChatWindows();
        setVisible(true);
        connected = false;
    }  // end connectionFailed

    public void actionPerformed(ActionEvent event)
    {
        Object object = event.getSource();

        // Get login values
        if (!loggedIn) {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.length() == 0 || password.length() == 0) {
                JOptionPane.showMessageDialog(this,
                    "Please enter both a username and a password.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Start the client
            user = new ChatDotUser(username, password);
            if (!connected) {
                client = new ChatDotClient(user, this);
                if (!client.start()) return;
                connected = true;
            }
        }

        if (object == registerButton) {
            try {
                if (!client.register()) {
                    displayError("Failed to register.");
                    return;
                }
            } catch (Exception e) {
                displayError("Failed to register.");
                return;
            }
        } else if (object == loginButton) {
            try {
                if (!client.login()) {
                    displayError("Failed to login.");
                    return;
                }
            } catch (Exception e) {
                displayError("Failed to login.");
                return;
            }
        } else if (object == logoutButton) {
            client.disconnect();
            logout();
        } else if (object == broadcastButton) {
            displayChatWindow(null);
        } else if (object == chatHistoryButton) {
            displayChatHistory();
        } else if (object instanceof JButton) {
            JButton button = (JButton) object;
            if (button.getActionCommand().equals("chat")) {
                String username = button.getText();
                displayChatWindow(username);
            }
        }

    }  // end actionPerformed

    public void sendMessage(String sender, String message)
    {
        ChatDotChatWindow window = chatWindows.get(sender);
        if (window == null) {
            window = new ChatDotChatWindow(sender, client, user, this);
            chatWindows.put(sender, window);
        } else {
            window.setVisible(true);
        }
        window.append(message);
    }  // end append

    public void updateChatHistory(String history)
    {
        if (chatHistoryArea != null) {
            chatHistoryArea.append(history + "\n");
            chatHistoryArea.setCaretPosition(chatHistoryArea.getText().length() - 1);
        }
    }

    /*
     * Unmodified Methods
     */
    void displayError(String msg)
    {
        JOptionPane.showMessageDialog(this,
            msg,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }  // end displayError

    void login()
    {
        setVisible(false);
        displayBuddyList();
        loggedIn = true;
    }  // end login

    void logout()
    {
        closeChatWindows();
        closeBuddyList();
        setVisible(true);
        loggedIn = false;
    }  // end logout

    void updateStatus(String username, String status, boolean isLogin)
    {
        if (user.getUsername().equals(username)) {
            loggedIn = true;
            return;
        }
        System.out.println(username + ": " + status);
        ChatDotUser buddy   = buddies.get(username);
        JButton buddyButton = buddyButtons.get(username);
        if (buddy == null) {
            buddy       = new ChatDotUser(username);
            buddyButton = new JButton(username);
            if (status.equals("Logged In")) {
                buddy.setOnline(true);
                buddyButton.setEnabled(true);
            } else {
                buddyButton.setEnabled(false);
            }
            buddyButton.addActionListener(this);
            buddyButton.setActionCommand("chat");
            buddyList.add(buddyButton);
            buddies.put(username, buddy);
            buddyButtons.put(username, buddyButton);
        } else if (username.equals(buddy.getUsername())) {
            if (status.equals("Logged In")) {
                buddy.setOnline(true);
                buddyButton.setEnabled(true);
            } else {
                buddy.setOnline(false);
                buddyButton.setEnabled(false);
            }
        }
        if (!isLogin) {
            JDialog notification = new JDialog();
            notification.setSize(150, 100);
            Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (int) dimension.getWidth() - notification.getWidth() - 15;
            int y = 15;
            notification.setLocation(x, y);
            JLabel statusLabel = new JLabel(username + ": " + status, JLabel.CENTER);
            notification.add(statusLabel);
            Timer timer = new Timer(2000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    notification.setVisible(false);
                    notification.dispose();
                }
            });
            timer.setRepeats(false);
            timer.start();
            notification.setVisible(true);
        }
        buddyList.revalidate();
        buddyList.repaint();
    }  // end method

    /*
     * Private Methods
     */
    private void displayLogin()
    {
        setTitle("ChatDot Login");

        // Main Login Panel
        JPanel loginPanel = new JPanel(new GridLayout(5, 1, 10, 10));

        // Welcome text
        JLabel loginLabel = new JLabel("Welcome to ChatDot!",
                SwingConstants.CENTER);
        loginPanel.add(loginLabel);

        // Login Fields Panel
        // TODO: Lookup JFrame docs and format this better
        JPanel loginFieldsPanel = new JPanel(new GridLayout(2, 2));
        JLabel usernameLabel    = new JLabel("Username: ");
        JLabel passwordLabel    = new JLabel("Password: ");
        usernameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        passwordLabel.setBorder(BorderFactory.createEmptyBorder(5, 20, 0, 0));
        usernameField           = new JTextField("");
        passwordField           = new JPasswordField("");
        usernameField.setBackground(Color.WHITE);
        passwordField.setBackground(Color.WHITE);
        loginFieldsPanel.add(usernameLabel);
        loginFieldsPanel.add(usernameField);
        loginFieldsPanel.add(passwordLabel);
        loginFieldsPanel.add(passwordField);
        loginPanel.add(loginFieldsPanel);

        // Login Buttons Panel
        JPanel loginButtonsPanel = new JPanel(new GridLayout(2, 2));
        loginButton              = new JButton("Login");
        registerButton           = new JButton("Register");
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
        usernameField.requestFocus();

        // Make it visible
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(400, 300);
        int x = (int) ((dimension.getWidth() - getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - getHeight()) / 2);
        setLocation(x, y);
        loginPanel.getRootPane().setDefaultButton(loginButton);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }  // end displayLogin

    private void displayBuddyList()
    {
        buddyFrame = new JFrame("Buddy List");
        buddyFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        buddyFrame.setSize(250, 650);
        buddyFrame.setLocation(15, 15);

        // Main Panel
        JPanel buddyPanel = new JPanel();
        buddyPanel.setLayout(new BoxLayout(buddyPanel, BoxLayout.Y_AXIS));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Current username
        currentUsernameLabel = new JLabel("Logged In User: " + user.getUsername());
        topPanel.add(currentUsernameLabel);

        // Buttons
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(this);
        broadcastButton = new JButton("Broadcast");
        broadcastButton.addActionListener(this);
        chatHistoryButton = new JButton("Chat History");
        chatHistoryButton.addActionListener(this);

        topPanel.add(logoutButton);
        topPanel.add(broadcastButton);
        topPanel.add(chatHistoryButton);

        buddyList = new JPanel();
        buddyList.setLayout(new BoxLayout(buddyList, BoxLayout.Y_AXIS));

        client.getBuddyStatus();

        buddyScrollList = new JScrollPane(buddyList);
        buddyPanel.add(topPanel);
        buddyPanel.add(buddyScrollList);
        buddyFrame.add(buddyPanel);
        buddyFrame.setVisible(true);
    }  // end displayBuddyList

    private void closeBuddyList()
    {
        if (buddyFrame != null) {
            buddyFrame.setVisible(false);
        }
    }  // end closeBuddyList

    private void displayChatWindow(String recipient)
    {
        if (recipient == null) {
            recipient = "Broadcast";
        }
        ChatDotChatWindow chatWindow = chatWindows.get(recipient);
        if (chatWindow == null) {
            chatWindow = new ChatDotChatWindow(recipient, client, user, this);
            chatWindows.put(recipient, chatWindow);
        } else {
            chatWindow.setVisible(true);
        }
    }  // end displayChatWindow

    private void displayChatHistory()
    {
        if (chatHistoryFrame == null) {
            chatHistoryFrame = new JFrame("Chat History");
            chatHistoryFrame.setDefaultCloseOperation(HIDE_ON_CLOSE);
            chatHistoryFrame.setSize(250, 650);

            JPanel chatPane = new JPanel();
            chatPane.setLayout(new BoxLayout(chatPane, BoxLayout.Y_AXIS));

            chatHistoryArea = new JTextArea(13, 20);
            JPanel chatAreaPane = new JPanel(new GridLayout(1, 1));
            chatAreaPane.add(new JScrollPane(chatHistoryArea));
            chatHistoryArea.setEditable(false);
            chatPane.add(chatAreaPane, BorderLayout.CENTER);

            client.getChatHistory();
            chatHistoryFrame.add(chatPane);
        }
        chatHistoryFrame.setVisible(true);
    }

    private void closeChatWindows()
    {
        for (int i = 0; i < chatWindows.size(); ++i) {
            ChatDotChatWindow window = chatWindows.get(i);
            if (window != null) {
                window.setVisible(false);
                window.dispose();
            }
            chatWindows.remove(i);
        }  // end for
    }  // end closeChatWindows
}
