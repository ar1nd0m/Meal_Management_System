/**
 * Small shared styling helper for the Meal Management UI.
 *
 * Nothing here touches resource paths, packages, or DAO/model code —
 * it only centralizes colors, fonts, and a few component "stylers"
 * so LoginFrame and MainFrame can look consistent and modern without
 * duplicating styling code in every tab.
 */
package com.mealapp.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class UITheme {

    private UITheme() { }

    // ===== Palette =====
    static final Color PRIMARY        = new Color(0x2F6FED);
    static final Color PRIMARY_DARK   = new Color(0x1F4FBF);
    static final Color PRIMARY_LIGHT  = new Color(0x5B8DF5);
    static final Color DANGER         = new Color(0xE0473C);
    static final Color DANGER_DARK    = new Color(0xC13A30);
    static final Color TEXT_DARK      = new Color(0x1F2430);
    static final Color BORDER_GRAY    = new Color(0xD6DAE0);
    static final Color TABLE_STRIPE   = new Color(0xF3F6FC);
    static final Color CARD_BG        = new Color(255, 255, 255, 235);

    // ===== Fonts (sized for a full-screen app window — bump these here to resize everything at once) =====
    static final Font FONT_BASE    = new Font("SansSerif", Font.PLAIN, 32);
    static final Font FONT_BUTTON  = new Font("SansSerif", Font.BOLD, 28);
    static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 48);
    static final Font FONT_LABEL   = new Font("SansSerif", Font.PLAIN, 32);

    /**
     * Apply Nimbus (a much more modern-looking cross-platform L&F than
     * the default Metal one) plus a couple of accent-color overrides.
     * Call this ONCE, before any UI is created — LoginFrame.main() does this.
     */
    static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("nimbusBase", PRIMARY_DARK);
            UIManager.put("nimbusBlueGrey", new Color(0xDDE3EC));
            UIManager.put("control", new Color(0xF4F6FA));
            UIManager.put("nimbusFocus", PRIMARY_LIGHT);
            UIManager.put("nimbusSelectionBackground", PRIMARY);
            UIManager.put("text", TEXT_DARK);
            UIManager.put("Table.showGrid", Boolean.TRUE);
        } catch (Exception ex) {
            // Fall back silently to the default look & feel if Nimbus is unavailable.
            System.err.println("Could not set Nimbus look and feel: " + ex.getMessage());
        }
    }

    /** One button factory used everywhere — "Delete" gets a danger (red) style automatically. */
    static JButton button(String text) {
        boolean danger = text != null && text.trim().equalsIgnoreCase("Delete");
        Color base = danger ? DANGER : PRIMARY;
        Color hover = danger ? DANGER_DARK : PRIMARY_DARK;

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isRollover() || getModel().isPressed() ? hover : base;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(20, 44, 20, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Rounded, evenly-padded text field border so fields don't look cramped. */
    static void styleField(JTextField field) {
        field.setFont(FONT_BASE);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER_GRAY, 10),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));
    }

    /** Gives a table modern header colors, comfortable row height, and zebra striping. */
    static void styleTable(JTable table) {
        table.setFont(FONT_BASE);
        table.setRowHeight(64);
        table.setShowGrid(true);
        table.setGridColor(BORDER_GRAY);
        table.setSelectionBackground(PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(14, 10));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BUTTON);
        header.setBackground(PRIMARY_DARK);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 68));
        header.setReorderingAllowed(true);

        table.setDefaultRenderer(Object.class, new StripedCellRenderer());
    }

    /** Wraps content in a soft, rounded, translucent white "card" so it stays readable over the background image. */
    static JPanel card(LayoutManager lm) {
        JPanel card = new JPanel(lm) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        return card;
    }

    /** A translucent toolbar strip for the "add / filter" rows above each table. */
    static JPanel toolbar() {
        JPanel bar = card(new FlowLayout(FlowLayout.LEFT, 22, 18));
        return bar;
    }

    // ===== helpers =====

    private static class StripedCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_STRIPE);
                c.setForeground(TEXT_DARK);
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return c;
        }
    }

    static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 8, 4, 8);
        }
    }

    /** Adds a subtle hover-highlight to any button already carrying its own paint logic. */
    static void addHover(JButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });
    }
}
