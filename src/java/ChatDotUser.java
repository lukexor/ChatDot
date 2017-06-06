package main;

import java.util.UUID;
import java.io.Serializable;

public class ChatDotUser implements Serializable
{
    private UUID userId;
    private String username;
    private String password;
    private boolean online;

    public ChatDotUser(String username)
    {
        this(username, "");
    }  // end constructor

    public ChatDotUser(String username, String password)
    {
        this.userId   = UUID.randomUUID();
        this.username = username;
        this.password = password;
        this.online   = false;
    }  // end constructor

    public void setOnline(boolean status) {
        online = status;
    }  // end setOnline

    public boolean isOnline() {
        return online;
    }  // end isOnline

    public UUID getUserId() {
        return userId;
    }  // end getUserId

    public String getUsername() {
        return username;
    }  // end getUsername

    public String getPassword() {
        return password;
    }  // end getUsername
}  // end ChatDotUser
