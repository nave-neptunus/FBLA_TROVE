package com.fbla.businessboost.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.Random;

public class VoiceAssistant {

    private static boolean isRecording = false;
    private static Process currentSpeechProcess;

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
     * Simulates the start of microphone recording.
     */
    public static void startListening() {
        isRecording = true;
        // In a real implementation with a paid API, we would start capturing the
        // TargetDataLine here.
    }

    /**
     * Simulates stopping the microphone and transcribing the audio.
     * Returns a mockup response based on the demo context.
     * 
     * @param onComplete callback with the transcribed text (simulated user
     *                   question) and the AI's response
     */
    public static void stopListeningAndProcess(SpeechCallback onComplete) {
        if (!isRecording)
            return;
        isRecording = false;

        Task<Void> processTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Simulate network/transcription delay
                Thread.sleep(1500);

                // Simulated user inputs and AI Responses
                String[] mockUserInputs = {
                        "What is Commercio?",
                        "Can you help me find a highly rated restaurant?",
                        "What is my most favorited category?"
                };

                String[] mockAiResponses = {
                        "Commercio is an application designed to help you discover, evaluate, and save local businesses.",
                        "Of course. I recommend checking out the 'Sushi Spot'. It has a fantastic 4.8 star rating and great deals.",
                        "Based on your profile, your most favorited category is Restaurants. You seem to really enjoy food!"
                };

                int randomIndex = new Random().nextInt(mockUserInputs.length);
                String userText = mockUserInputs[randomIndex];
                String aiText = mockAiResponses[randomIndex];

                Platform.runLater(() -> {
                    onComplete.onResult(userText, aiText);
                    speak(aiText);
                });

                return null;
            }
        };
        new Thread(processTask).start();
    }

    public interface SpeechCallback {
        void onResult(String transcribedUserText, String aiResponse);
    }
}
