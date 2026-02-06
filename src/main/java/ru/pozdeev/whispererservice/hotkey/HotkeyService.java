package ru.pozdeev.whispererservice.hotkey;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.pozdeev.whispererservice.speechrecognition.SpeechRecognitionService;
import ru.pozdeev.whispererservice.systemtray.SystemTrayIcon;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@Component
public class HotkeyService implements NativeKeyListener {
    @Value("${app.hotkey.windows}")
    private String windowsHotkey;

    @Value("${app.hotkey.mac}")
    private String macHotkey;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private boolean isRecording = false;
    private String activeHotkey;

    @Autowired
    private SpeechRecognitionService recognitionService;

    @Autowired
    private SystemTrayIcon trayIcon;

    @PostConstruct
    public void registerHook() {
        // Disable JNativeHook's default logger to prevent console spam
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);
        trayIcon.createAndShowGUI();
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // Определяем ОС и выбираем комбинацию клавиш
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            activeHotkey = macHotkey;
            log.info("Using Mac hotkey combination: {}", activeHotkey);
        } else {
            activeHotkey = windowsHotkey;
            log.info("Using Windows hotkey combination: {}", activeHotkey);
        }

        GlobalScreen.addNativeKeyListener(this);
        log.info("Global hotkey listener registered");
    }

    @PreDestroy
    public void unregisterHook() {
        try {
            GlobalScreen.unregisterNativeHook();
            GlobalScreen.removeNativeKeyListener(this);
            log.info("Global hotkey listener unregistered");
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();
        pressedKeys.add(keyCode);

        if (checkHotkeyCombination()) {
            if (!isRecording) {
                isRecording = true;
                log.info("Hotkey pressed - starting recording");
                recognitionService.startRecording();
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();
        pressedKeys.remove(keyCode);

        if (isRecording && !checkHotkeyCombination()) {
            isRecording = false;
            log.info("Hotkey released - stopping recording");
            recognitionService.stopRecording();
        }
    }

    private boolean checkHotkeyCombination() {
        Set<String> requiredKeys = Set.of(activeHotkey.split(" "));
        Set<String> pressedKeyNames = new HashSet<>();

        for (Integer keyCode : pressedKeys) {
            String keyName = NativeKeyEvent.getKeyText(keyCode).toLowerCase();
            pressedKeyNames.add(keyName);
        }

        return pressedKeyNames.containsAll(requiredKeys);
    }
}
