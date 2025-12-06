package p2p.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CuteFileButton extends JButton {
    private boolean isHovered = false;

    public CuteFileButton() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(50, 40));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Hover Effect: Scale
        if (isHovered) {
            g2.scale(1.1, 1.1);
            g2.translate(-2, -2);
        }

        // Draw Paperclip "Character" background
        g2.setColor(Theme.SECONDARY_ACCENT); // Pastel Blue
        g2.fillRoundRect(5, 5, w - 10, h - 10, 15, 15);

        // Draw "Face"
        g2.setColor(Color.WHITE);
        // Eyes
        g2.fillOval(w / 2 - 8, h / 2 - 4, 4, 4);
        g2.fillOval(w / 2 + 4, h / 2 - 4, 4, 4);
        // Mouth (Smile)
        g2.setStroke(new BasicStroke(2));
        g2.drawArc(w / 2 - 5, h / 2 - 5, 10, 8, 0, -180);

        // Draw "Clip" shape hint (simple line)
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(8, 8, w - 16, h - 16, 12, 12);

        g2.dispose();
    }
}
