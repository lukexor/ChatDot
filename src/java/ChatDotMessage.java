package main;

import java.util.UUID;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.Serializable;
import java.util.ArrayList;

import main.ChatDotUser;

enum MessageType {
    MESSAGE, ERROR, STATUS, LOGOUT, LOGIN, REGISTER, WHO, HISTORY
}

public class ChatDotMessage implements Serializable
{
    private static final SimpleDateFormat date_formatter
        = new SimpleDateFormat("HH:mm:ss");
    private UUID messageId;
    private ChatDotUser sender;
    private ArrayList<ChatDotUser> recipients;
    private String content,
                   timestamp,
                   status;
    private MessageType type;


    /*
     * Constructors
     */
    ChatDotMessage()
    {
        this(MessageType.MESSAGE, "");
    }  // end constructor

    ChatDotMessage(MessageType type, String content)
    {
        this(type, content, null, null);
    }  // end constructor

    ChatDotMessage(MessageType type, String username, String status)
    {
        this(type, "", new ChatDotUser(username));
        this.status = status;
    }  // end constructor

    ChatDotMessage(MessageType type, String content, ChatDotUser sender)
    {
        this(type, content, sender, null);
    }

    ChatDotMessage(MessageType type, String content, ChatDotUser sender,
            ArrayList<ChatDotUser> recipients)
    {
        this.messageId  = UUID.randomUUID();
        this.content    = content;
        this.sender     = sender;
        this.recipients = recipients;
        this.timestamp  = date_formatter.format(new Date());
        this.type       = type;
    }

    /*
     * Getters/Setters
     */
    public ChatDotUser getSender()
    {
        return sender;
    }
    public void setSender(ChatDotUser sender)
    {
        this.sender = sender;
    }
    public ArrayList<ChatDotUser> getRecipients()
    {
        return recipients;
    }
    public void setRecipients(ArrayList<ChatDotUser> recipients)
    {
        this.recipients = recipients;
    }
    public String getContent()
    {
        return content;
    }
    public void setContent(String content)
    {
        this.content = content;
    }
    public String getTimestamp()
    {
        return timestamp;
    }
    public MessageType getType()
    {
        return type;
    }
    public String getStatus()
    {
        return status;
    }  // end getStatus
}  // end ChatDotMessage
