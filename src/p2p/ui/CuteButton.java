package p2p.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CuteButton extends JButton {
    private boolean isHovered = false;
    private Color normalColor;
    private Color hoverColor;

    public CuteButton(String text) {
        super(text);
        this.normalColor = Theme.ACCENT;
        this.hoverColor = Theme.ACCENT_HOVER;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(Theme.FONT_BOLD);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

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

    public void setColors(Color normal, Color hover) {
        this.normalColor = normal;
        this.hoverColor = hover;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Animation logic (simple instantaneous for now, can be timer based)
        int width = getWidth();
        int height = getHeight();

        if (isHovered) {
            g2.scale(1.05, 1.05); // Scale up
            g2.translate(-(width * 0.025), -(height * 0.025)); // Center
            g2.setColor(hoverColor);
        } else {
            g2.setColor(normalColor);
        }

        g2.fillRoundRect(0, 0, width, height, 30, 30); // Very round

        // Draw Text
        FontMetrics fm = g2.getFontMetrics();
        Rectangle stringBounds = fm.getStringBounds(getText(), g2).getBounds();
        int textX = (width - stringBounds.width) / 2;
        int textY = (height - stringBounds.height) / 2 + fm.getAscent();

        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
