package p2p.ui;

import java.awt.Color;
import java.awt.Font;

public class Theme {
    // Pastel Palette
    public static final Color BACKGROUND = new Color(255, 253, 245); // Cream
    public static final Color PANEL_BG = new Color(255, 255, 255, 180); // Translucent White
    public static final Color TEXT_PRIMARY = new Color(80, 80, 80); // Soft Dark Grey
    public static final Color TEXT_SECONDARY = new Color(120, 120, 120);

    // Accents
    public static final Color ACCENT = new Color(255, 179, 186); // Pastel Pink
    public static final Color ACCENT_HOVER = new Color(255, 209, 220); // Lighter Pink
    public static final Color ACCENT_TRANSPARENT = new Color(255, 179, 186, 100);

    public static final Color SECONDARY_ACCENT = new Color(186, 225, 255); // Pastel Blue
    public static final Color SUCCESS = new Color(186, 255, 201); // Pastel Mint

    public static final Color BORDER = new Color(200, 200, 200, 50);
    public static final Color GLASS_HIGHLIGHT = new Color(255, 255, 255, 100);

    // Gradients
    public static final Color GRADIENT_START = new Color(255, 223, 186); // Peach
    public static final Color GRADIENT_END = new Color(255, 179, 186); // Pink

    // Fonts
    public static final Font FONT_REGULAR = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 24);
}
