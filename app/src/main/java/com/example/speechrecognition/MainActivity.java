package com.example.speechrecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO = 1;

    private AudioRecord recorder;
    private boolean isRecording = false;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);

        // Request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }

        // Set up GCP credential from res/raw
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.credentials);
            File outFile = new File(getFilesDir(), "credentials.json");

            try (OutputStream outputStream = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        startButton.setOnClickListener(v -> startMicCapture());
        stopButton.setOnClickListener(v -> stopMicCapture());
    }

    private void startMicCapture() {
        int sampleRate = 16000;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        isRecording = true;
        recorder.startRecording();

        new Thread(() -> {
            try {
                File outputFile = new File(getCacheDir(), "recorded_audio.wav");
                FileOutputStream fos = new FileOutputStream(outputFile);
                ByteArrayOutputStream rawPcm = new ByteArrayOutputStream();

                byte[] buffer = new byte[bufferSize];
                long startTime = System.currentTimeMillis();

                while (isRecording && (System.currentTimeMillis() - startTime < 5000)) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) rawPcm.write(buffer, 0, read);
                }

                recorder.stop();
                recorder.release();

                // Write WAV header and PCM data
                writeWavHeader(fos, rawPcm.size(), sampleRate, 1);
                rawPcm.writeTo(fos);
                fos.close();

                // Transcribe with helper
                TranscribeHelper.transcribeAudio(this, outputFile, new TranscribeHelper.TranscriptionCallback() {
                    @Override
                    public void onSuccess(String result) {
                        runOnUiThread(() -> textView.setText(result));
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> textView.setText("Error: " + error));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> textView.setText("Recording error: " + e.getMessage()));
            }
        }).start();
    }

    private void stopMicCapture() {
        isRecording = false;
    }

    private void writeWavHeader(FileOutputStream out, int pcmLength, int sampleRate, int channels) throws IOException {
        int byteRate = sampleRate * channels * 2;
        int totalLength = pcmLength + 36;

        byte[] header = new byte[44];

        System.arraycopy("RIFF".getBytes(), 0, header, 0, 4);
        header[4] = (byte) (totalLength & 0xff);
        header[5] = (byte) ((totalLength >> 8) & 0xff);
        header[6] = (byte) ((totalLength >> 16) & 0xff);
        header[7] = (byte) ((totalLength >> 24) & 0xff);
        System.arraycopy("WAVE".getBytes(), 0, header, 8, 4);
        System.arraycopy("fmt ".getBytes(), 0, header, 12, 4);
        header[16] = 16; header[20] = 1; header[22] = (byte) channels;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); // block align
        header[34] = 16; // bits per sample
        System.arraycopy("data".getBytes(), 0, header, 36, 4);
        header[40] = (byte) (pcmLength & 0xff);
        header[41] = (byte) ((pcmLength >> 8) & 0xff);
        header[42] = (byte) ((pcmLength >> 16) & 0xff);
        header[43] = (byte) ((pcmLength >> 24) & 0xff);

        out.write(header, 0, 44);
    }
}
