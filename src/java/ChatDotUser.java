package main;

import java.util.UUID;
import java.io.Serializable;

public class ChatDotUser implements Serializable
{
    private UUID userId;
    private String username;

    public ChatDotUser(String username)
    {
        this.userId   = UUID.randomUUID();
        this.username = username;
    }  // end constructor

    public UUID getUserId() {
        return userId;
    }  // end getUserId

    public String getUsername() {
        return username;
    }  // end getUsername
}  // end ChatDotUser
