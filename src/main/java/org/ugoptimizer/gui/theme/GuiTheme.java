package org.ugoptimizer.gui.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Central visual language for the UG Campus Security control room.
 *
 * <p>The palette is deliberately restrained: a dark operational shell with a
 * light workspace, status colours that communicate urgency without relying on
 * colour alone, and typography sized for dense, readable data tables.</p>
 */
public final class GuiTheme {

    private GuiTheme() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    // Shell (header, sidebar, status bar)
    public static final Color SHELL_BACKGROUND = new Color(0x16, 0x1B, 0x2A);
    public static final Color SHELL_BACKGROUND_ALT = new Color(0x1F, 0x27, 0x3A);
    public static final Color SHELL_BORDER = new Color(0x0F, 0x13, 0x1F);

    // Workspace
    public static final Color WORKSPACE_BACKGROUND = new Color(0xF2, 0xF4, 0xF8);
    public static final Color PANEL_BACKGROUND = Color.WHITE;
    public static final Color PANEL_BORDER = new Color(0xDC, 0xE2, 0xEC);
    public static final Color TABLE_STRIPE = new Color(0xF7, 0xF9, 0xFC);
    public static final Color TABLE_SELECTION = new Color(0xC9, 0xE2, 0xF9);

    // Text
    public static final Color TEXT_PRIMARY = new Color(0x1A, 0x20, 0x2E);
    public static final Color TEXT_SECONDARY = new Color(0x5B, 0x64, 0x75);
    public static final Color TEXT_MUTED = new Color(0x8A, 0x93, 0xA6);
    public static final Color TEXT_ON_DARK = new Color(0xE8, 0xEC, 0xF4);
    public static final Color TEXT_ON_DARK_MUTED = new Color(0x9A, 0xA5, 0xB8);

    // Brand accent
    public static final Color ACCENT = new Color(0x1F, 0x6F, 0xEB);
    public static final Color ACCENT_DARK = new Color(0x16, 0x54, 0xB4);
    public static final Color ACCENT_SOFT = new Color(0xE3, 0xEE, 0xFD);

    // Status colours
    public static final Color STATUS_OK = new Color(0x2E, 0x8B, 0x57);
    public static final Color STATUS_OK_SOFT = new Color(0xE6, 0xF4, 0xEC);
    public static final Color STATUS_WARN = new Color(0xB5, 0x79, 0x0A);
    public static final Color STATUS_WARN_SOFT = new Color(0xFC, 0xF3, 0xE3);
    public static final Color STATUS_DANGER = new Color(0xC0, 0x38, 0x2D);
    public static final Color STATUS_DANGER_SOFT = new Color(0xFB, 0xE9, 0xE7);
    public static final Color STATUS_NEUTRAL = new Color(0x4A, 0x55, 0x68);
    public static final Color STATUS_NEUTRAL_SOFT = new Color(0xED, 0xF0, 0xF5);
    public static final Color STATUS_INFO = new Color(0x1F, 0x6F, 0xEB);
    public static final Color STATUS_INFO_SOFT = new Color(0xE3, 0xEE, 0xFD);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
}
