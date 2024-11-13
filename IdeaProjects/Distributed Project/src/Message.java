// Message.java
import java.io.Serializable;

enum MessageType {
    CONNECT,
    CONNECT_ACK,
    OPEN_DOCUMENT,
    DOCUMENT_CONTENT,
    UPDATE_CONTENT,
    UPDATE_USERS,
    CURSOR_POSITION
}

class Message implements Serializable {
    private MessageType type;
    private String sender;
    private String content;
    private String fontFamily;
    private int fontSize;
    private long timestamp;
    private int cursorPosition;

    // Constructor for messages that don't need font information
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.fontFamily = "Arial";  // default font
        this.fontSize = 12;         // default size
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for messages that include font information
    public Message(MessageType type, String sender, String content, String fontFamily, int fontSize) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public MessageType getType() { return type; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getFontFamily() { return fontFamily; }
    public int getFontSize() { return fontSize; }
    public long getTimestamp() { return timestamp; }
    public int getCursorPosition() { return cursorPosition; }
    public void setCursorPosition(int position) { this.cursorPosition = position; }
}