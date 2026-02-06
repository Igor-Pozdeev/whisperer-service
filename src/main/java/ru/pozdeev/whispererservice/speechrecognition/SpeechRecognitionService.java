package ru.pozdeev.whispererservice.speechrecognition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class SpeechRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionService.class);

    @Autowired
    private ApplicationArguments args;

    @Value("${app.audio.sample-rate}")
    private float sampleRate;

    private Model model;
    private Recognizer recognizer;
    private TargetDataLine microphone;
    private boolean isRecording = false;
    private ByteArrayOutputStream audioBuffer;

    @Async
    public CompletableFuture<String> startRecording() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                initializeVosk();
                startAudioCapture();
                return processAudio();
            } catch (Exception e) {
                logger.error("Error during speech recognition", e);
                return "Error: " + e.getMessage();
            }
        });
    }

    public void stopRecording() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }

    private void initializeVosk() throws IOException {
        if (model == null) {
            String modelPath = args.getOptionValues("modelPath").get(0);
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            model = new Model(modelPath);
            recognizer = new Recognizer(model, sampleRate);
            recognizer.setMaxAlternatives(0);
            recognizer.setWords(true);
        }
    }

    private void startAudioCapture() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        audioBuffer = new ByteArrayOutputStream();
        isRecording = true;
        logger.info("Audio capture started");
    }

    private String processAudio() {
        byte[] buffer = new byte[4096];

        while (isRecording) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                // Отправляем данные в распознаватель
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String result = recognizer.getResult();
                    logger.debug("Partial result: {}", result);
                }

                // Сохраняем в буфер для возможного использования
                audioBuffer.write(buffer, 0, bytesRead);
            }
        }

        // Получаем финальный результат
        String finalResult = recognizer.getFinalResult();
        logger.info("Final recognition result: {}", finalResult);

        // Извлекаем текст из JSON-ответа
        String text = extractTextFromResult(finalResult);

        // Вводим текст в активное поле
        if (!text.isEmpty()) {
            try {
                typeText(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return text;
    }

    private String extractTextFromResult(String jsonResult) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResult);
            return root.get("text").asText();
        } catch (Exception e) {
            logger.error("Error extracting text from result", e);
        }
        return "";
    }

    private void typeText(String text) throws IOException {
        String[] cmd = {"cmd", "/c", "echo " + text + " | clip"};
        Runtime.getRuntime().exec(cmd);
    }
}