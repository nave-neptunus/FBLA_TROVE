package com.fbla.businessboost.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class VoiceAssistant {

    private static boolean isRecording = false;
    private static Process currentSpeechProcess;

    private static TargetDataLine targetDataLine;
    private static ByteArrayOutputStream audioOutputStream;

    private static final String GOOGLE_SPEECH_API_URL = "http://www.google.com/speech-api/v2/recognize?client=chromium&lang=en-US&key=REDACTED_GOOGLE_API_KEY&pFilter=0";

    /**
     * Speaks the given text aloud using the macOS 'say' command.
     */
    public static void speak(String text) {
        stopSpeaking(); // Stop any ongoing speech
        new Thread(() -> {
            try {
                // Ensure text doesn't break the shell command
                String safeText = text.replace("\"", "").replace("'", "");
                currentSpeechProcess = Runtime.getRuntime().exec(new String[] { "say", safeText });
                currentSpeechProcess.waitFor();
            } catch (Exception e) {
                System.err.println("Error playing TTS: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Stops any currently playing speech.
     */
    public static void stopSpeaking() {
        if (currentSpeechProcess != null && currentSpeechProcess.isAlive()) {
            currentSpeechProcess.destroy();
        }
    }

    /**
     * Starts microphone recording in a background thread.
     */
    public static void startListening() {
        if (isRecording)
            return;

        try {
            // 16kHz, 16 bit, Mono, Signed, Little-Endian
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone not supported!");
                return;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            isRecording = true;
            audioOutputStream = new ByteArrayOutputStream();

            new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }).start();

        } catch (LineUnavailableException e) {
            System.err.println("Could not open microphone: " + e.getMessage());
        }
    }

    /**
     * Stops the microphone and transcribes the audio using Google Web Speech API.
     * Generates a contextual AI response based on keywords.
     * 
     * @param onComplete callback with the transcribed text and the AI's response
     */
    public static void stopListeningAndProcess(SpeechCallback onComplete) {
        if (!isRecording)
            return;
        isRecording = false;

        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }

        Task<Void> processTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                byte[] audioData = audioOutputStream.toByteArray();
                String transcribedText = "Could not understand audio.";
                String aiResponse = "I'm sorry, I couldn't hear that clearly. Could you please try again?";

                try {
                    // Send to Google Web Speech API
                    URL url = URI.create(GOOGLE_SPEECH_API_URL).toURL();
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "audio/l16; rate=16000");
                    con.setDoOutput(true);

                    try (OutputStream os = con.getOutputStream()) {
                        os.write(audioData);
                        os.flush();
                    }

                    if (con.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // Parse JSON line by line as Google can sometimes return multiple JSON objects
                        // separated by newlines
                        String rawResponse = response.toString();
                        String finalJsonStr = "";

                        // Find the last valid JSON block that has a result
                        String[] jsonBlocks = rawResponse.split("\n");
                        for (String block : jsonBlocks) {
                            if (block.contains("\"transcript\"")) {
                                finalJsonStr = block;
                            }
                        }

                        if (!finalJsonStr.isEmpty()) {
                            JSONObject root = new JSONObject(finalJsonStr);
                            if (root.has("result")) {
                                JSONArray results = root.getJSONArray("result");
                                if (results.length() > 0) {
                                    JSONObject firstResult = results.getJSONObject(0);
                                    if (firstResult.has("alternative")) {
                                        JSONArray alternatives = firstResult.getJSONArray("alternative");
                                        if (alternatives.length() > 0) {
                                            transcribedText = alternatives.getJSONObject(0).getString("transcript");
                                            aiResponse = generateResponse(transcribedText);
                                        }
                                    }
                                }
                            }
                        } else {
                            // If we didn't get any confident transcription
                            aiResponse = "I didn't pick up any speech. Please try moving closer to the microphone.";
                        }
                    } else {
                        System.err.println("Speech API Error: HTTP " + con.getResponseCode());
                        aiResponse = "I'm having trouble connecting to my speech recognition service right now.";
                    }
                } catch (Exception e) {
                    System.err.println("Speech API Error: " + e.getMessage());
                    aiResponse = "There was an unexpected error processing your voice.";
                }

                // Final values need to be effectively final for lambda
                final String finalTranscribed = transcribedText;
                final String finalAiResponse = aiResponse;

                Platform.runLater(() -> {
                    onComplete.onResult(finalTranscribed, finalAiResponse);
                    speak(finalAiResponse);
                });

                return null;
            }
        };
        new Thread(processTask).start();
    }

    /**
     * Generates a context-aware response based on keywords in the transcript.
     */
    private static String generateResponse(String transcript) {
        String lower = transcript.toLowerCase();

        if (lower.contains("commercio") || lower.contains("app") || lower.contains("what is this")) {
            return "Commercio is an application designed to help you discover, evaluate, and save local businesses.";
        } else if (lower.contains("restaurant") || lower.contains("food") || lower.contains("hungry")) {
            return "I can help with that. Try searching for food in the Smart Search tab to see top-rated local restaurants around you.";
        } else if (lower.contains("favorite") || lower.contains("saved") || lower.contains("profile")) {
            return "You can view all your saved businesses in the Favorites tab. Based on your activity, you seem to enjoy high quality retail stores.";
        } else if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return "Hi there! I am the Commercio Voice Assistant. Hold the microphone button to ask me a question.";
        } else if (lower.contains("search") || lower.contains("find")) {
            return "You can type a city or zip code in the Smart Search tab to find local small businesses in any area.";
        } else if (lower.contains("settings") || lower.contains("dark mode") || lower.contains("light mode")) {
            return "You can toggle the application theme between light and dark mode securely from the Settings tab.";
        }

        return "I heard you say: " + transcript
                + ". I'm an offline assistant right now, so I only know how to answer basic questions about Commercio.";
    }

    public interface SpeechCallback {
        void onResult(String transcribedUserText, String aiResponse);
    }
}
