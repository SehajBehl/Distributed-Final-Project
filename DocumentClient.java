import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.LinkedList;

public class DocumentClient extends JFrame {
    private JTextArea documentArea;
    private JList<String> usersList;
    private DefaultListModel<String> usersListModel;
    private JButton connectButton;
    private JButton openDocButton;
    private JButton saveButton;
    private JButton loadButton;
    private JButton rollbackButton; // New rollback button
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Socket socket;
    private String username;
    private String currentDocId = null;
    private boolean isConnected = false;
    private boolean isUpdatingFromServer = false;
    private Thread messageListenerThread;
    private volatile boolean running = false;
    private LinkedList<String> documentHistory;  // To store document versions for rollback
    private int historyIndex;                    // To track the current version

    public DocumentClient() {
        setupUI();
        documentHistory = new LinkedList<>();
        historyIndex = -1;  // No history yet
    }

    private void setupUI() {
        setTitle("Collaborative Document Editor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        documentArea = new JTextArea();
        documentArea.setEnabled(false);
        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        connectButton = new JButton("Connect");
        openDocButton = new JButton("Create/Open Document");
        openDocButton.setEnabled(false);
        saveButton = new JButton("Save Document");
        saveButton.setEnabled(false);
        loadButton = new JButton("Load Document");
        rollbackButton = new JButton("Rollback");
        rollbackButton.setEnabled(false);  // Disable initially

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(documentArea), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Active Users"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(200, getHeight()));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(openDocButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(rollbackButton); // Add rollback button to panel

        mainPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.NORTH);

        add(mainPanel);

        connectButton.addActionListener(e -> handleConnect());
        openDocButton.addActionListener(e -> handleOpenDocument());
        saveButton.addActionListener(e -> handleSaveDocument());
        loadButton.addActionListener(e -> handleLoadDocument());
        rollbackButton.addActionListener(e -> handleRollback()); // Rollback button action

        documentArea.getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                if (!isUpdatingFromServer && isConnected) {
                    try {
                        String content = documentArea.getText();
                        // Save the current version to history
                        if (historyIndex == documentHistory.size() - 1) {
                            documentHistory.add(content);  // Add a new version
                        } else {
                            documentHistory.set(historyIndex + 1, content);  // Replace the version if we're in the middle
                        }
                        historyIndex++;
                        updateRollbackButtonState();  // Update the rollback button state
                        sendMessage(new Message(MessageType.UPDATE_CONTENT, username, content));
                    } catch (IOException e) {
                        e.printStackTrace();
                        handleConnectionError();
                    }
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) { handleChange(); }
            @Override
            public void removeUpdate(DocumentEvent e) { handleChange(); }
            @Override
            public void changedUpdate(DocumentEvent e) { handleChange(); }
        });
    }

    private void handleSaveDocument() {
        if (!documentArea.getText().isEmpty()) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(documentArea.getText());
                    JOptionPane.showMessageDialog(this, "Document saved successfully.");
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error saving document.");
                }
            }
        }
    }

    private void handleLoadDocument() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                isUpdatingFromServer = true; // Temporarily mark as updating to avoid sending updates to the server
                documentArea.setText(content.toString());
                isUpdatingFromServer = false; // Reset this flag to allow future edits to be sent
                documentArea.setEnabled(true); // Ensure the text area is enabled for editing
                JOptionPane.showMessageDialog(this, "Document loaded successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading document.");
            }
        }
    }

    private void setupNetworking() throws IOException {
        socket = new Socket("localhost", 5000);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
        running = true;

        messageListenerThread = new Thread(() -> {
            while (running) {
                try {
                    Message message = (Message) input.readObject();
                    handleServerMessage(message);
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        handleConnectionError();
                        break;
                    }
                }
            }
        });
        messageListenerThread.start();
    }

    private void handleConnect() {
        if (!isConnected) {
            String name = JOptionPane.showInputDialog("Enter your username:");
            if (name != null && !name.trim().isEmpty()) {
                username = name.trim();
                try {
                    setupNetworking();
                    sendMessage(new Message(MessageType.CONNECT, username, ""));
                    isConnected = true;
                    connectButton.setText("Disconnect");
                    openDocButton.setEnabled(true);
                    setTitle("Collaborative Document Editor - " + username);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Could not connect to server");
                    handleConnectionError();
                }
            }
        } else {
            handleDisconnect();
        }
    }

    private void handleOpenDocument() {
        String docId = JOptionPane.showInputDialog("Enter document ID:");
        if (docId != null && !docId.trim().isEmpty()) {
            try {
                // Remove user from the current document's active user list if switching to a new document
                if (currentDocId != null && !currentDocId.equals(docId)) {
                    sendMessage(new Message(MessageType.REMOVE_USER, username, currentDocId));
                }
                sendMessage(new Message(MessageType.OPEN_DOCUMENT, username, docId));
                currentDocId = docId;
                documentArea.setEnabled(true);
                saveButton.setEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
                handleConnectionError();
            }
        }
    }

    private void sendMessage(Message message) throws IOException {
        synchronized (output) {
            output.writeObject(message);
            output.flush();
        }
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            try {
                switch (message.getType()) {
                    case CONNECT_ACK:
                        JOptionPane.showMessageDialog(this, "Connected to server");
                        break;

                    case DOCUMENT_CONTENT:
                    case UPDATE_CONTENT:
                        isUpdatingFromServer = true;
                        documentArea.setText(message.getContent());
                        isUpdatingFromServer = false;
                        break;

                    case UPDATE_USERS:
                        updateUsersList(message.getContent().split(","));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleConnectionError() {
        SwingUtilities.invokeLater(() -> {
            if (isConnected) {
                JOptionPane.showMessageDialog(this, "Lost connection to server");
                handleDisconnect();
            }
        });
    }

    private void handleDisconnect() {
        running = false;
        isConnected = false;

        if (messageListenerThread != null) {
            messageListenerThread.interrupt();
        }

        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        output = null;
        input = null;
        socket = null;
        messageListenerThread = null;

        SwingUtilities.invokeLater(() -> {
            connectButton.setText("Connect");
            openDocButton.setEnabled(false);
            documentArea.setEnabled(false);
            documentArea.setText("");
            usersListModel.clear();
            setTitle("Collaborative Document Editor");
        });
    }

    private void updateUsersList(String[] users) {
        usersListModel.clear();
        for (String user : users) {
            if (!user.trim().isEmpty()) {
                usersListModel.addElement(user);
            }
        }
    }

    // Rollback feature
    private void handleRollback() {
        if (historyIndex > 0) {
            historyIndex--;  // Go to the previous version
            String previousVersion = documentHistory.get(historyIndex);
            documentArea.setText(previousVersion);  // Revert to the previous version
            isUpdatingFromServer = false;  // Reset this flag
        } else {
            JOptionPane.showMessageDialog(this, "No previous version to rollback to.");
        }
    }

    private void updateRollbackButtonState() {
        rollbackButton.setEnabled(historyIndex > 0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DocumentClient client = new DocumentClient();
            client.setVisible(true);
        });
    }
}


