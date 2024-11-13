import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

public class DocumentClient extends JFrame {
    private JTextArea documentArea;
    private JList<String> usersList;
    private DefaultListModel<String> usersListModel;
    private JButton connectButton;
    private JButton openDocButton;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Socket socket;
    private String username;
    private boolean isConnected = false;
    private boolean isUpdatingFromServer = false;
    private Thread messageListenerThread;
    private volatile boolean running = false;

    public DocumentClient() {
        setupUI();
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
        openDocButton = new JButton("Open Document");
        openDocButton.setEnabled(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(documentArea), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Active Users"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(200, getHeight()));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(openDocButton);

        mainPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.NORTH);

        add(mainPanel);

        connectButton.addActionListener(e -> handleConnect());
        openDocButton.addActionListener(e -> handleOpenDocument());

        documentArea.getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                if (!isUpdatingFromServer && isConnected) {
                    try {
                        String content = documentArea.getText();
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
                sendMessage(new Message(MessageType.OPEN_DOCUMENT, username, docId));
                documentArea.setEnabled(true);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DocumentClient().setVisible(true);
        });
    }
}