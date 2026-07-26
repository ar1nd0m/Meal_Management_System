/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */


/**
 *
 * @author arindam
 */
package com.mealapp;

import com.mealapp.ui.LoginFrame;

import javax.swing.*;

public class app {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame f = new LoginFrame();
            f.setVisible(true);
        });
    }
}
