// Client.java
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client {
    private static JTextArea textArea = new JTextArea();
    private static DataOutputStream out;
    private static DataInputStream in;
    private static boolean isUpdating = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());

        try {
            Socket socket = new Socket("localhost", 12345); // Server IP and port
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            // Read the initial document content from the server
            String initialContent = in.readUTF();
            if (initialContent != null) {
                SwingUtilities.invokeLater(() -> {
                    isUpdating = true;
                    textArea.setText(initialContent);
                    isUpdating = false;
                });
            }

            // Listen for messages from the server
            new Thread(() -> {
                try {
                    String message;
                    while (true) {
                        message = in.readUTF();
                        String finalMessage = message;
                        SwingUtilities.invokeLater(() -> {
                            isUpdating = true;
                            textArea.setText(finalMessage);
                            isUpdating = false;
                        });
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Collaborative Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textArea.getDocument().addDocumentListener(docListener);
        frame.add(new JScrollPane(textArea));
        frame.setSize(500, 500);
        frame.setVisible(true);
    }

    // Document listener to detect changes
    private static javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener() {
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            sendText();
        }

        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            sendText();
        }

        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            sendText();
        }

        private void sendText() {
            if (out != null && !isUpdating) {
                try {
                    out.writeUTF(textArea.getText());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
