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
    private boolean isLogin;

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

    ChatDotMessage(MessageType type, String username, String status, boolean isLogin)
    {
        this(type, "", new ChatDotUser(username));
        this.status = status;
        this.isLogin = isLogin;
    }  // end constructor

    ChatDotMessage(MessageType type, String content, ChatDotUser sender)
    {
        this(type, content, sender, null);
    }  // end constructor

    ChatDotMessage(MessageType type, String content, ChatDotUser sender,
            ArrayList<ChatDotUser> recipients)
    {
        this.messageId  = UUID.randomUUID();
        this.content    = content;
        this.sender     = sender;
        this.recipients = recipients;
        this.timestamp  = date_formatter.format(new Date());
        this.type       = type;
        this.status     = "";
        this.isLogin    = false;
    }  // end constructor

    /*
     * Getters/Setters
     */
    public ChatDotUser getSender()
    {
        return sender;
    }  // end getSender
    public void setSender(ChatDotUser sender)
    {
        this.sender = sender;
    }  // end setSender
    public ArrayList<ChatDotUser> getRecipients()
    {
        return recipients;
    }  // end getRecipients
    public void setRecipients(ArrayList<ChatDotUser> recipients)
    {
        this.recipients = recipients;
    }  // end setRecipients
    public String getContent()
    {
        return content;
    }  // end getContent
    public void setContent(String content)
    {
        this.content = content;
    }  // end setContent
    public String getTimestamp()
    {
        return timestamp;
    }  // end getTimestamp
    public MessageType getType()
    {
        return type;
    }  // end getType
    public String getStatus()
    {
        return status;
    }  // end getStatus
    public boolean getIsLogin()
    {
        return isLogin;
    }  // end isLogin
}  // end ChatDotMessage
