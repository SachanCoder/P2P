package p2p.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CuteTextField extends JTextField {
    public CuteTextField() {
        setOpaque(false); // We paint background manually
        setForeground(Theme.TEXT_PRIMARY);
        setFont(Theme.FONT_REGULAR);
        setBorder(new EmptyBorder(10, 15, 10, 15)); // Padding
        setCaretColor(Theme.ACCENT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2.setColor(Color.WHITE); // Or Theme.PANEL_BG
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // Very Rounded

        // Border (Optional, maybe specific border color)
        g2.setColor(Theme.ACCENT_TRANSPARENT);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

        g2.dispose();

        super.paintComponent(g);
    }
}
