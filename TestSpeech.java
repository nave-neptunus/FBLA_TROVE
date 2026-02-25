import javax.sound.sampled.*;
import java.io.*;

public class TestSpeech {
    public static void main(String[] args) {
        try {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, true);
            TargetDataLine microphone;
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Microphone not supported");
                return;
            }
            System.out.println("Mic supported!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
