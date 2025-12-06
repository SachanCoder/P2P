package p2p.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class GlassPanel extends JPanel {
    private final int radius = 20;

    public GlassPanel() {
        setOpaque(false);
        setBackground(Theme.PANEL_BG);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background with translucency
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

        // Add subtle highlight for "glass" reflection edge
        g2.setColor(Theme.GLASS_HIGHLIGHT);
        g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, radius, radius);

        g2.dispose();
        super.paintComponent(g);
    }
}
