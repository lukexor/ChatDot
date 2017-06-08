package main;

import static org.junit.Assert.*;
import org.junit.Test;

import main.ChatDotServer;
import main.ChatDotMessage;
import main.ChatDotUser;

public class ChatDotServerTest
{
    private static final ChatDotServer server = new ChatDotServer(4444, 200, 100);
    private static final String username = "bross",
                                password = "R@nd0mP#sswotd3";

    @Test
    public void registerUsernameDoesntExistsShouldSucceed()
    {
        server.purgeUserDatabase();
        ChatDotUser user = new ChatDotUser(username);
        assertTrue("Attempt to register with new username",
                server.register(new ChatDotMessage(MessageType.REGISTER, "", user)));
    }

    @Test
    public void registerUsernameAlreadyExistsShouldError()
    {
        server.purgeUserDatabase();
        ChatDotUser user = new ChatDotUser(username);
        assertTrue("Attempt to register with new username",
                server.register(new ChatDotMessage(MessageType.REGISTER, "", user)));
        assertFalse("Attempt to register with duplicate username",
                server.register(new ChatDotMessage(MessageType.REGISTER, "", user)));
    }

    @Test
    public void registerBroadcastUsernameShouldUser()
    {
        server.purgeUserDatabase();
        ChatDotUser user = new ChatDotUser("Broadcast");
        assertFalse("Attempt to register with Broadcast username",
                server.register(new ChatDotMessage(MessageType.REGISTER, "", user)));
    }

    @Test
    public void loginUserDoesntExistShouldError()
    {
        server.purgeUserDatabase();
        ChatDotUser user = new ChatDotUser(username, password);
        assertFalse("Attempt to login with non-existing login",
                server.login(new ChatDotMessage(MessageType.LOGIN, "", user)));
    }

    @Test
    public void loginUserExistsAndIsValidShouldSucceed()
    {
        server.purgeUserDatabase();
        ChatDotUser user = new ChatDotUser(username, password);
        assertTrue("Attempt to register with a new username",
                server.register(new ChatDotMessage(MessageType.REGISTER, "", user)));
        assertTrue("Attempt to login with a valid login",
                server.login(new ChatDotMessage(MessageType.LOGIN, "", user)));
    }

    @Test
    public void loginUserExistsAndIsNotValidShouldError()
    {
        server.purgeUserDatabase();
        ChatDotUser user = new ChatDotUser(username, password);
        assertTrue("Attempt to register with a new username",
                server.register(new ChatDotMessage(MessageType.REGISTER, "", user)));
        user.setPassword("IncorrectPassword");
        assertFalse("Attempt to login with a invalid login",
                server.login(new ChatDotMessage(MessageType.LOGIN, "", user)));
    }
}  // end ChatDotServerTest
