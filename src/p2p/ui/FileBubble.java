package p2p.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class FileBubble extends JPanel {

    public FileBubble(String filename, String sender, boolean isMe) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);

        Color bgColor = isMe ? Theme.ACCENT : Theme.SECONDARY_ACCENT;

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Filename Header
        JLabel fileLabel = new JLabel("📄 " + filename);
        // Truncate long filename
        if (filename.length() > 25) {
            fileLabel.setText("📄 " + filename.substring(0, 22) + "...");
            fileLabel.setToolTipText(filename);
        }

        fileLabel.setFont(Theme.FONT_BOLD);
        fileLabel.setForeground(Theme.TEXT_PRIMARY);
        // Center text
        fileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(fileLabel);

        contentPanel.add(Box.createVerticalStrut(10));

        // Image Preview logic
        if (isImage(filename)) {
            JLabel imageLabel = new JLabel("Loading preview...");
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(200, 150));
            contentPanel.add(imageLabel);

            // Async load
            new Thread(() -> {
                try {
                    File imgFile = new File("download_" + filename);
                    if (imgFile.exists()) {
                        BufferedImage img = ImageIO.read(imgFile);
                        if (img != null) {
                            // Scale
                            Image scaled = getScaledImage(img, 200, 200);
                            SwingUtilities.invokeLater(() -> {
                                imageLabel.setText("");
                                imageLabel.setIcon(new ImageIcon(scaled));
                                imageLabel.setPreferredSize(null); // Let icon dictate, or keep constrained
                                contentPanel.revalidate();
                                contentPanel.repaint();
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> imageLabel.setText("(Preview unavailable)"));
                        }
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> imageLabel.setText("(Error loading preview)"));
                }
            }).start();
        }

        contentPanel.add(Box.createVerticalStrut(10));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        CuteButton openBtn = new CuteButton("Open");
        openBtn.setFont(Theme.FONT_SMALL);
        openBtn.setPreferredSize(new Dimension(70, 25));
        openBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File("download_" + filename));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage());
            }
        });

        btnPanel.add(openBtn);
        contentPanel.add(btnPanel);

        bubble.add(contentPanel);

        JPanel wrapper = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(bubble);

        add(wrapper, BorderLayout.CENTER);
    }

    private boolean isImage(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".bmp");
    }

    private Image getScaledImage(BufferedImage src, int w, int h) {
        int originalW = src.getWidth();
        int originalH = src.getHeight();
        int newW = w;
        int newH = h;

        // Maintain Aspect Ratio
        double aspect = (double) originalW / originalH;
        if (originalW > originalH) {
            newH = (int) (newW / aspect);
        } else {
            newW = (int) (newH * aspect);
        }

        return src.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
    }
}
