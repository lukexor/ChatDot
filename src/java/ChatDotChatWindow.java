package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import main.ChatDotClient;
import main.ChatDotClientInterface;

public class ChatDotChatWindow extends JFrame implements ActionListener
{
    private static final SimpleDateFormat dateFormatter
        = new SimpleDateFormat("HH:mm:ss");
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private ChatDotClientInterface clientInterface;
    private ChatDotClient client;
    private ChatDotUser user;

    ChatDotChatWindow(String title, ChatDotClient client, ChatDotUser user,
            ChatDotClientInterface clientInterface)
    {
        super(title);
        this.client          = client;
        this.user            = user;
        this.clientInterface = clientInterface;

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(400, 300);

        JPanel chatPane = new JPanel();
        chatPane.setLayout(new BoxLayout(chatPane, BoxLayout.Y_AXIS));

        chatArea = new JTextArea(13, 20);
        JPanel chatAreaPane = new JPanel(new GridLayout(1, 1));
        chatAreaPane.add(new JScrollPane(chatArea));
        chatArea.setEditable(false);
        chatPane.add(chatAreaPane, BorderLayout.CENTER);

        messageField = new JTextField();
        messageField.requestFocus();
        messageField.addActionListener(this);

        sendButton = new JButton("Send");
        sendButton.addActionListener(this);
        JPanel chatControls = new JPanel();
        chatControls.setLayout(new BoxLayout(chatControls, BoxLayout.Y_AXIS));
        chatControls.add(messageField);
        chatControls.add(sendButton);
        chatPane.add(chatControls);

        add(chatPane);
        setVisible(true);
    }  // end constructor

    public void actionPerformed(ActionEvent event)
    {
        Object object = event.getSource();

        if (messageField.getText().length() == 0) {
            return;
        }
        String timestamp = "[" + dateFormatter.format(new Date()) + "] ";
        String message = timestamp + user.getUsername() + ": " + messageField.getText();
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getText().length() - 1);
        ArrayList<ChatDotUser> recipients = new ArrayList<ChatDotUser>();
        recipients.add(new ChatDotUser(getTitle()));
        client.sendMessage(new ChatDotMessage(
            MessageType.MESSAGE,
            messageField.getText(),
            user,
            recipients));
        messageField.setText("");
    }  // end actionPerformed

    public void append(String message)
    {
        if (chatArea == null) return;
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getText().length() - 1);
    }  // end method
}  // end ChatDotChatWindow
