import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

class Document {
    private String id;
    private StringBuilder content;
    private Set<String> activeUsers;
    private Set<ClientHandler> connectedClients;
    private ReentrantLock lock;
    private LinkedList<String> versionHistory; // To store versions of document
    private static final int MAX_VERSIONS = 10; // Maximum number of versions to keep

    public Document(String id) {
        this.id = id;
        this.content = new StringBuilder();
        this.activeUsers = ConcurrentHashMap.newKeySet();
        this.connectedClients = ConcurrentHashMap.newKeySet();
        this.lock = new ReentrantLock();
        this.versionHistory = new LinkedList<>();
    }

    public void updateContent(String newContent) {
        lock.lock();
        try {
            // Only save the current content if it differs from the new content
            if (!content.toString().isEmpty() && !content.toString().equals(newContent)) {
                if (versionHistory.size() == MAX_VERSIONS) {
                    versionHistory.removeFirst();
                }
                versionHistory.add(content.toString());
            }
            content = new StringBuilder(newContent); // Update the content
        } finally {
            lock.unlock();
        }
    }
    
    

    public String getContent() {
        lock.lock();
        try {
            return content.toString();
        } finally {
            lock.unlock();
        }
    }

    public List<String> getVersionHistory() {
        return Collections.unmodifiableList(versionHistory); // Return an unmodifiable list
    }

    public void addUser(String username, ClientHandler handler) {
        activeUsers.add(username);
        connectedClients.add(handler);
        broadcastActiveUsers();
    }

    public void removeUser(String username, ClientHandler handler) {
        activeUsers.remove(username);
        connectedClients.remove(handler);
        broadcastActiveUsers();
    }

    public Set<String> getActiveUsers() {
        return new HashSet<>(activeUsers);
    }

    public void broadcastUpdate(Message message, ClientHandler sender) {
        for (ClientHandler client : connectedClients) {
            if (client != sender) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void broadcastActiveUsers() {
        String userList = String.join(",", activeUsers);
        Message updateMessage = new Message(MessageType.UPDATE_USERS, "Server", userList);
        for (ClientHandler client : connectedClients) {
            try {
                client.sendMessage(updateMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Rollback to a specific version
    public void rollbackToVersion(int versionIndex) {
        lock.lock();
        try {
            if (versionIndex >= 0 && versionIndex < versionHistory.size()) {
                String versionContent= versionHistory.get(versionIndex);
                if(versionContent != null) {
                    content = new StringBuilder(versionContent); // Rollback to the specified version
                }
                
            } else {
                throw new IllegalArgumentException("Invalid version index");
            }
        } finally {
            lock.unlock();
        }
    }
}

public class DocumentServer {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, Document> documents;
    private ExecutorService executorService;

    public DocumentServer() {
        documents = new ConcurrentHashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                executorService.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Document getDocument(String docId) {
        return documents.computeIfAbsent(docId, id -> new Document(id));
    }

    public static void main(String[] args) {
        new DocumentServer().start();
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private DocumentServer server;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String username;
    private String currentDocId;
    private Document currentDoc;

    public ClientHandler(Socket socket, DocumentServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    public void sendMessage(Message message) throws IOException {
        synchronized (output) {
            output.writeObject(message);
            output.flush();
        }
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Message message = (Message) input.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            handleDisconnect();
        }
    }

    private void handleMessage(Message message) throws IOException {
        switch (message.getType()) {
            case CONNECT:
                username = message.getSender();
                sendMessage(new Message(MessageType.CONNECT_ACK, "Server", "Connected successfully"));
                break;

            case OPEN_DOCUMENT:
                currentDocId = message.getContent();
                currentDoc = server.getDocument(currentDocId);
                currentDoc.addUser(username, this);
                sendMessage(new Message(MessageType.DOCUMENT_CONTENT, "Server", currentDoc.getContent()));
                break;

            case UPDATE_CONTENT:
                if (currentDoc != null) {
                    currentDoc.updateContent(message.getContent());
                    currentDoc.broadcastUpdate(message, this);
                }
                break;

            case ROLLBACK_DOCUMENT:
                if (currentDoc != null) {
                    try {
                        int versionIndex = Integer.parseInt(message.getContent()); // Get the version index from the message

                        // Ensure versionIndex is valid
                        if (versionIndex >= 0 && versionIndex < currentDoc.getVersionHistory().size()) {
                            currentDoc.rollbackToVersion(versionIndex); // Perform the rollback
                            sendMessage(new Message(MessageType.DOCUMENT_CONTENT, "Server", currentDoc.getContent()));
                            currentDoc.broadcastUpdate(message, this); // Broadcast the rollback to other users
                        } else {
                            sendMessage(new Message(MessageType.ERROR, "Server", "Invalid version index"));
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(new Message(MessageType.ERROR, "Server", "Invalid version index format"));
                    }
                }
                break;

            case REMOVE_USER:
                if (currentDoc != null) {
                    currentDoc.removeUser(username, this);
                    currentDocId = null;
                    currentDoc = null;
                }
                break;
        }
    }

    private void handleDisconnect() {
        if (currentDoc != null) {
            currentDoc.removeUser(username, this);
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

