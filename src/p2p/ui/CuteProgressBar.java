package p2p.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class CuteProgressBar extends JPanel {
    private int value = 0; // 0 to 100
    private Timer timer;
    private int animationOffset = 0;

    public CuteProgressBar() {
        setOpaque(false);
        setPreferredSize(new Dimension(200, 25));

        timer = new Timer(50, e -> {
            animationOffset = (animationOffset + 1) % 20;
            repaint();
        });
    }

    public void setValue(int v) {
        this.value = v;
        repaint();
    }

    public void startAnimation() {
        if (!timer.isRunning())
            timer.start();
    }

    public void stopAnimation() {
        timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background Track
        g2.setColor(new Color(230, 230, 230));
        g2.fillRoundRect(0, 0, w, h, h, h);

        // Progress Fill
        int fillWidth = (int) ((w * value) / 100.0);
        if (fillWidth > 0) {
            // Gradient Fill
            GradientPaint gp = new GradientPaint(0, 0, Theme.ACCENT, fillWidth, 0, Theme.ACCENT_HOVER);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, fillWidth, h, h, h);

            // Animated Stripes
            g2.setColor(new Color(255, 255, 255, 50));
            g2.setStroke(new BasicStroke(5));
            for (int i = -20; i < fillWidth; i += 20) {
                g2.drawLine(i + animationOffset, 0, i + animationOffset + 10, h);
            }
        }

        // Wobble Effect (Simulated by drawing a little "sparkle" at the end)
        if (timer.isRunning() && fillWidth > 5 && fillWidth < w) {
            g2.setColor(Color.WHITE);
            g2.fillOval(fillWidth - 8, 5, 4, 4);
        }

        g2.dispose();
    }
}
