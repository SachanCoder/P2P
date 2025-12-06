package p2p.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatBubblePanel extends JPanel {
    private final String content;
    private final boolean isMe;
    private final String sender;
    private final String timestamp;

    private static final int MAX_WIDTH = 280;

    public ChatBubblePanel(String sender, String content, boolean isMe) {
        this.sender = sender;
        this.content = content;
        this.isMe = isMe;
        this.timestamp = new SimpleDateFormat("HH:mm").format(new Date());

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isMe) {
                    g2.setColor(Theme.ACCENT);
                } else if (sender.equals("GLOBAL")) {
                    g2.setColor(new Color(255, 160, 122));
                } else {
                    g2.setColor(Theme.SECONDARY_ACCENT);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));

        // 1. Username Badge (Only for others)
        if (!isMe && !sender.equals("GLOBAL")) {
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            header.setOpaque(false);

            JLabel nameLabel = new JLabel(" " + sender + " ");
            nameLabel.setFont(Theme.FONT_SMALL); // Use plain or bold small font
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setOpaque(true);
            nameLabel.setBackground(getUserColor(sender));

            // Rounded badge border effect?
            // Simple approach: standard opaque label

            header.add(nameLabel);
            header.setAlignmentX(0.0f); // Left align in BoxLayout
            bubble.add(header);
            bubble.add(Box.createVerticalStrut(4));
        }

        // 2. Message Body
        JTextArea textArea = new JTextArea(content);
        textArea.setFont(Theme.FONT_REGULAR);
        textArea.setForeground(Color.WHITE);
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setAlignmentX(0.0f);

        // Calculate Size for wrapping
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        String[] lines = content.split("\n");
        int maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(line));
        }
        int targetWidth = Math.min(Math.max(maxLineWidth, 60) + 20, MAX_WIDTH);

        textArea.setSize(new Dimension(targetWidth, Short.MAX_VALUE));
        textArea.setPreferredSize(new Dimension(targetWidth, textArea.getPreferredSize().height));

        bubble.add(textArea);

        // 3. Timestamp
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(255, 255, 255, 180)); // Translucent white
        footer.add(timeLabel);
        footer.setAlignmentX(0.0f); // Match alignment logic
        // But FlowLayout creates its own alignment inside the panel

        bubble.add(Box.createVerticalStrut(2));
        bubble.add(footer);

        JPanel wrapper = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(bubble);

        add(wrapper, BorderLayout.CENTER);
    }

    private Color getUserColor(String s) {
        int hash = s.hashCode();
        float hue = (Math.abs(hash) % 360) / 360f;
        // High saturation, mid-low brightness for contrast with white text
        return Color.getHSBColor(hue, 0.7f, 0.7f);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
