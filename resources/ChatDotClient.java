System Overview
    Description
    Diagrams, Tables
System Architecture
    Subsystems, components, objects
    Dataflow Diagram Ch 12
Detailed System Design
    Subsystems, components, objects
    Class Diagram Ch 14.11


Nouns
    Message
    User
    Client
    Server
    History


Classes:
    User (Entity)
        attrs:
            int userId
            String email
            String password
            String name
            Enum status (Online, Offline)
            ChatHistory history
        methods:
            goOnline
            goOffline
    ChatHistory (Entity)
        attrs:
            Message[] messages
        methods:
            addEntry(Message message)
            save()
            load()
    Message (Entity)
        attrs:
            int messageId
            User sender
            User[] recipients
            String content
            DateTime timestamp
        methods:
    ChatClientInterface (Boundary)
        attrs:
        methods:
    ChatClient (Controller)
        attrs:
            int serverPort
            int userThrottle
            boolean connected
            Socket socket
            String serverHostname
            ChatProtocol protocol
            User loggedInUser
        methods:
            main()
            send(Message message, User user)
            updateStatus(User[] users)
            login(User user)
            logout(User user)
            isConnected()
            disconnect()
            toString()
    ChatServer (Controller)
        attrs:
            int listenPort
            int roomThrottle
            int maxConnections
            InetAddress hostAddress
            ServerSocket serverSocket
            Socket socket
            User[] loggedInUsers
        methods:
            main()
            run()
            send(Message message, User[] users)
            login(User user)
            logout(User user)
            register(User user)
