// Server.java
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static String documentContent = ""; // Variable to store the current document content
    private static final Object documentLock = new Object(); // Lock object for thread safety

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345); // Port number
        System.out.println("Server started...");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler clientThread = new ClientHandler(socket);
            clientHandlers.add(clientThread);
            clientThread.start();
        }
    }

    // Broadcast method to send updates to all clients
    public static void broadcast(String message, ClientHandler excludeUser) {
        synchronized (documentLock) {
            // Update the document content
            documentContent = message;
        }
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != excludeUser) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // ClientHandler class
    static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());

                // Send the current document content to the newly connected client
                String currentContent;
                synchronized (documentLock) {
                    currentContent = documentContent;
                }
                out.writeUTF(currentContent);
                out.flush();

                String message;
                while (true) {
                    // Read message from client
                    message = in.readUTF();

                    // Broadcast the received message
                    Server.broadcast(message, this);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected.");
            } finally {
                // Remove the client from the list
                clientHandlers.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Send message to the client
        void sendMessage(String message) {
            try {
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
