/**
 *
 * @author arindam
 */
package com.mealapp.ui;

import com.mealapp.dao.UserDAO;
import com.mealapp.model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class LoginFrame extends JFrame {
    private final JTextField txtUser = new JTextField(20);
    private final JPasswordField txtPass = new JPasswordField(20);
    private final JButton btnLogin = new JButton("Login");

    // change this size to make fonts bigger/smaller
    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 16);

    public LoginFrame() {
        super("Meal Management - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Increased window size as requested
        setSize(1320, 720);
        setLocationRelativeTo(null);

        // Background panel that paints the image
        BackgroundPanel bgPanel = new BackgroundPanel("/images/background.png");
        bgPanel.setLayout(new GridBagLayout());

        // translucent form panel to keep fields readable
        JPanel form = createFormPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        bgPanel.add(form, gbc);

        setContentPane(bgPanel);

        // wiring
        btnLogin.addActionListener(this::onLogin);
        getRootPane().setDefaultButton(btnLogin);   // Enter triggers login
        SwingUtilities.invokeLater(() -> txtUser.requestFocusInWindow());
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout()) {
            // draw rounded translucent background
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                // translucent white (alpha between 0-255)
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(new Color(255, 255, 255, 200));
                // leave a small padding so rounded corners are visible
                g2.fillRoundRect(0, 0, w, h, 24, 24);
                g2.dispose();
                // do NOT call super.paintComponent(g) to avoid clearing the rounded rect
                // but ensure children are painted:
                super.paintChildren(g);
            }
        };
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.anchor = GridBagConstraints.WEST;

        JLabel lblUser = new JLabel("Username:");
        JLabel lblPass = new JLabel("Password:");

        lblUser.setFont(UI_FONT);
        lblPass.setFont(UI_FONT);
        txtUser.setFont(UI_FONT);
        txtPass.setFont(UI_FONT);
        btnLogin.setFont(UI_FONT.deriveFont(Font.BOLD));

        // Widen text fields slightly so the text doesn't look cramped
        Dimension tfSize = new Dimension(320, txtUser.getPreferredSize().height + 6);
        txtUser.setPreferredSize(tfSize);
        txtPass.setPreferredSize(tfSize);

        // Add components
        c.gridx = 0; c.gridy = 0;
        form.add(lblUser, c);

        c.gridx = 1;
        form.add(txtUser, c);

        c.gridx = 0; c.gridy = 1;
        form.add(lblPass, c);

        c.gridx = 1;
        form.add(txtPass, c);

        c.gridx = 1; c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        form.add(btnLogin, c);

        // Make sure labels don't have opaque backgrounds (so rounded panel shows)
        lblUser.setOpaque(false);
        lblPass.setOpaque(false);

        return form;
    }

    private void onLogin(ActionEvent e) {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password");
            return;
        }

        try {
            UserDAO dao = new UserDAO();
            User u = dao.findByUsername(user);
            if (u == null || !u.getPassword().equals(pass)) {
                JOptionPane.showMessageDialog(this, "Username or password wrong", "Login failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // success
            SwingUtilities.invokeLater(() -> {
                new MainFrame(u).setVisible(true);
                this.dispose();
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /**
     * BackgroundPanel — loads an image from the classpath and draws it scaled to cover the panel.
     * Expected location: resources/images/background.png (classpath resource "/images/background.png")
     */
    static class BackgroundPanel extends JPanel {
        private BufferedImage bg;

        public BackgroundPanel(String classpathResource) {
            try (InputStream is = getClass().getResourceAsStream(classpathResource)) {
                if (is != null) {
                    bg = ImageIO.read(is);
                } else {
                    System.err.println("Background resource not found on classpath: " + classpathResource);
                }
            } catch (Exception ex) {
                System.err.println("Failed to load background image: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) {
                int pw = getWidth(), ph = getHeight();
                int iw = bg.getWidth(), ih = bg.getHeight();

                double panelRatio = (double) pw / ph;
                double imageRatio = (double) iw / ih;

                int drawW, drawH;
                if (imageRatio > panelRatio) {
                    // image wider -> match height
                    drawH = ph;
                    drawW = (int) (ph * imageRatio);
                } else {
                    // image taller or similar -> match width
                    drawW = pw;
                    drawH = (int) (pw / imageRatio);
                }

                int x = (pw - drawW) / 2;
                int y = (ph - drawH) / 2;

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(bg, x, y, drawW, drawH, this);
                g2.dispose();
            }
        }
    }
}
