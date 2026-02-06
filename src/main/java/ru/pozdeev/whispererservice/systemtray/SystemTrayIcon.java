package ru.pozdeev.whispererservice.systemtray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

@Slf4j
@Component
public class SystemTrayIcon {

    private TrayIcon trayIcon;

    public void createAndShowGUI() {
        if (!SystemTray.isSupported()) {
            log.warn("!SystemTray.isSupported()={}", !SystemTray.isSupported());
            return;
        }

        PopupMenu popup = new PopupMenu();
        SystemTray tray = SystemTray.getSystemTray();

        ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
        Image image = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener((ActionEvent e) -> {
            System.exit(0);
        });

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.addActionListener((ActionEvent e) -> {
            showSettingsDialog();
        });

        popup.add(settingsItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "Voice Typer", popup);
        trayIcon.setImageAutoSize(true);

        try {
            tray.add(trayIcon);
            trayIcon.displayMessage("Voice Typer",
                    "Application started. Use Ctrl+Win to record.",
                    TrayIcon.MessageType.INFO);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void showSettingsDialog() {
        JFrame frame = new JFrame("Settings");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2));

        panel.add(new JLabel("Hotkey (Windows):"));
        JTextField windowsHotkey = new JTextField("Ctrl+Win");
        panel.add(windowsHotkey);

        panel.add(new JLabel("Hotkey (Mac):"));
        JTextField macHotkey = new JTextField("Cmd+Shift");
        panel.add(macHotkey);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            // Сохранение настроек
            frame.dispose();
        });

        panel.add(saveButton);

        frame.add(panel);
        frame.setVisible(true);
    }
}
