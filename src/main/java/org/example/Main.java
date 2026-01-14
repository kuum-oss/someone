package org.example;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import org.example.ui.BookLibraryGui;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Starting application...");
            
            // Подавляем предупреждения о Log4j2 и нативном доступе
            System.setProperty("log4j2.disable.jmx", "true");
            System.setProperty("log4j2.level", "ERROR");
            System.setProperty("apple.awt.application.appearance", "system");
            
            // Настройка темы в зависимости от системы (особенно важно для Mac M1)
            // Использование FlatPreferences для автоматического определения темы
            UIManager.put("FlatLaf.setPreferredAppearance", "system");
            
            if (isSystemDarkMode()) {
                FlatMacDarkLaf.setup();
            } else {
                FlatMacLightLaf.setup();
            }
            
            SwingUtilities.invokeLater(() -> {
                try {
                    BookLibraryGui gui = new BookLibraryGui();
                    gui.setVisible(true);
                    System.out.println("GUI is visible.");
                } catch (Exception e) {
                    System.err.println("Error during GUI creation: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Ошибка при запуске интерфейса: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (Exception e) {
            System.err.println("Fatal error during startup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isSystemDarkMode() {
        // FlatLaf 3.x+ умеет определять темную тему на macOS
        return com.formdev.flatlaf.util.SystemInfo.isMacOS && 
               com.formdev.flatlaf.FlatLaf.isLafDark();
    }
}
