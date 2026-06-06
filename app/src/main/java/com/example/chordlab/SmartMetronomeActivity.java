package com.example.chordlab;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartMetronomeActivity extends AppCompatActivity {

    private TextView tvBpm;
    private Button btnUploadFile;
    private SeekBar seekBarBpm;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<android.content.Intent> selectAudioLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                            processAudioUri(result.getData().getData());
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure this matches your XML file name
        setContentView(R.layout.activity_metronome);

        // Bind to the IDs from your XML layout
        tvBpm = findViewById(R.id.tvBpm);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        seekBarBpm = findViewById(R.id.seekBarBpm);

        btnUploadFile.setOnClickListener(v -> {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                selectAudioLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to open file manager.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processAudioUri(Uri uri) {
        if (uri == null) return;

        // Visual feedback while loading
        tvBpm.setText("--");
        Toast.makeText(this, "Analyzing audio...", Toast.LENGTH_SHORT).show();

        btnUploadFile.setEnabled(false);
        btnUploadFile.setText("⏳ Analyzing...");

        executorService.execute(() -> {
            File tempFile = null;

            try {
                tempFile = copyUriToTempFile(uri);

                double detectedBpm = detectBpm(tempFile);

                File finalTempFile = tempFile;

                mainHandler.post(() -> {
                    btnUploadFile.setEnabled(true);
                    btnUploadFile.setText("📁  Upload File"); // Reset button text

                    if (detectedBpm > 0) {
                        int finalBpmInt = (int) Math.round(detectedBpm);

                        // Update the large BPM Text
                        tvBpm.setText(String.valueOf(finalBpmInt));

                        // Sync the SeekBar with the new BPM
                        if (seekBarBpm != null) {
                            seekBarBpm.setProgress(finalBpmInt);
                        }

                        Toast.makeText(SmartMetronomeActivity.this, "BPM Detected: " + finalBpmInt, Toast.LENGTH_SHORT).show();
                    } else {
                        tvBpm.setText("120"); // Fallback to default
                        Toast.makeText(SmartMetronomeActivity.this, "Could not determine BPM.", Toast.LENGTH_LONG).show();
                    }

                    if (finalTempFile != null && finalTempFile.exists()) {
                        finalTempFile.delete();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                File finalTempFile = tempFile;

                mainHandler.post(() -> {
                    btnUploadFile.setEnabled(true);
                    btnUploadFile.setText("📁  Upload File");
                    tvBpm.setText("120"); // Fallback to default

                    Toast.makeText(
                            SmartMetronomeActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    if (finalTempFile != null && finalTempFile.exists()) {
                        finalTempFile.delete();
                    }
                });
            }
        });
    }

    private File copyUriToTempFile(Uri uri) throws Exception {

        File tempFile = File.createTempFile(
                "temp_audio",
                ".mp3",
                getCacheDir()
        );

        try (
                InputStream inputStream =
                        getContentResolver()
                                .openInputStream(uri);

                FileOutputStream outputStream =
                        new FileOutputStream(tempFile)
        ) {

            if (inputStream == null) {
                throw new Exception(
                        "Failed to open audio input stream"
                );
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }

        return tempFile;
    }

    private double detectBpm(File file) {
        try {
            android.media.MediaExtractor extractor = new android.media.MediaExtractor();
            extractor.setDataSource(file.getAbsolutePath());

            int trackIndex = -1;
            android.media.MediaFormat format = null;

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                format = extractor.getTrackFormat(i);
                String mime = format.getString(android.media.MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    trackIndex = i;
                    break;
                }
            }

            if (trackIndex == -1) return -1;

            extractor.selectTrack(trackIndex);

            // =========================================================
            // 1. SET SCAN WINDOW (60s to 120s)
            // =========================================================
            long startTimeUs = 60 * 1000000L;
            long durationUs = 60 * 1000000L;
            long endTimeUs = startTimeUs + durationUs;

            extractor.seekTo(startTimeUs, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            String mime = format.getString(android.media.MediaFormat.KEY_MIME);
            android.media.MediaCodec codec =
                    android.media.MediaCodec.createDecoderByType(mime);

            codec.configure(format, null, null, 0);
            codec.start();

            java.util.ArrayList<Float> energy = new java.util.ArrayList<>();
            android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();

            int sampleRate = format.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channelCount = format.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT) : 2;

            // 16-bit PCM uses 2 bytes per sample per channel
            double bytesPerSecond = sampleRate * channelCount * 2.0;
            double totalBytesProcessed = 0;

            // Keeps track of the raw absolute amplitude of the preceding window chunk
            float lastRawEnergy = 0f;

            boolean done = false;

            // =========================================================
            // 2. DECODING & ONSET FLUX EXTRACTION LOOP (THREAD-SAFE)
            // =========================================================
            while (!done) {
                // Use a 10ms timeout so we don't freeze the thread if the codec is busy
                int inputIndex = codec.dequeueInputBuffer(10000);

                if (inputIndex >= 0) {
                    java.nio.ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
                    long sampleTime = extractor.getSampleTime();

                    if (sampleTime >= endTimeUs || sampleTime < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0,
                                android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        int size = extractor.readSampleData(inputBuffer, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0,
                                    android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, sampleTime, 0);
                            extractor.advance();
                        }
                    }
                }

                // Check the output buffer with a 10ms timeout
                int outputIndex = codec.dequeueOutputBuffer(info, 10000);

                if (outputIndex == android.media.MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // The hardware codec is busy. Sleep for 5ms to prevent a CPU spike/infinite loop.
                    Thread.sleep(5);
                    continue;
                } else if (outputIndex >= 0) {
                    if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        done = true;
                    }

                    java.nio.ByteBuffer out = codec.getOutputBuffer(outputIndex);
                    byte[] chunk = new byte[info.size];
                    out.get(chunk);
                    out.clear();

                    if (chunk.length > 0) {
                        totalBytesProcessed += chunk.length;
                        long sum = 0;

                        for (int i = 0; i < chunk.length - 1; i += 2) {
                            short s = (short) ((chunk[i] & 0xFF) | (chunk[i + 1] << 8));
                            sum += Math.abs(s);
                        }

                        float currentRawEnergy = sum / (float) chunk.length;
                        float flux = currentRawEnergy - lastRawEnergy;
                        lastRawEnergy = currentRawEnergy;

                        if (flux > 0) {
                            energy.add(flux);
                        } else {
                            energy.add(0f);
                        }
                    }

                    // Release the buffer back to the hardware
                    codec.releaseOutputBuffer(outputIndex, false);
                }
            }

            // =========================================================
            // 3. AUTOCORRELATION ANALYSIS WITH LOCAL PEAK FILTERING
            // =========================================================
            java.util.List<Integer> topLags = new java.util.ArrayList<>();
            java.util.List<Double> topScores = new java.util.ArrayList<>();

            for (int lag = 15; lag < 250; lag++) {
                double score = 0;
                for (int i = 0; i < energy.size() - lag; i++) {
                    score += energy.get(i) * energy.get(i + lag);
                }

                // PEAK FILTER: Skip this lag if it's not a local maximum peak.
                // This prevents adjacent "bloat" values (like 40, 41, 42) from dominating your top 5 list.
                if (lag > 15 && lag < 249) {
                    double prevScore = 0;
                    double nextScore = 0;
                    for (int i = 0; i < energy.size() - (lag - 1); i++) prevScore += energy.get(i) * energy.get(i + lag - 1);
                    for (int i = 0; i < energy.size() - (lag + 1); i++) nextScore += energy.get(i) * energy.get(i + lag + 1);

                    if (score < prevScore || score < nextScore) {
                        continue;
                    }
                }

                if (topScores.size() < 5) {
                    topLags.add(lag);
                    topScores.add(score);
                } else {
                    int minIndex = 0;
                    for (int j = 1; j < topScores.size(); j++) {
                        if (topScores.get(j) < topScores.get(minIndex)) {
                            minIndex = j;
                        }
                    }
                    if (score > topScores.get(minIndex)) {
                        topScores.set(minIndex, score);
                        topLags.set(minIndex, lag);
                    }
                }
            }

            // =========================================================
            // 4. DYNAMIC FRAMERATE & ADJUSTED TUNED BOUNDS
            // =========================================================
            java.util.List<Double> bpms = new java.util.ArrayList<>();

            double averageChunkSize = totalBytesProcessed / energy.size();
            double frameRate = bytesPerSecond / averageChunkSize;

            for (int lag : topLags) {
                double secondsPerBeat = lag / frameRate;
                if (secondsPerBeat <= 0) continue;

                double bpm = 60.0 / secondsPerBeat;

                // Adjusted floor bounds up to 85 to ensure 140+ tracks scale correctly
                // instead of settling on sub-beat halves (70s).
                while (bpm < 85) bpm *= 2;
                while (bpm > 170) bpm /= 2;

                bpms.add(bpm);
            }

            if (bpms.isEmpty()) return -1;

            // =========================================================
            // 5. MEDIAN FILTER
            // =========================================================
            java.util.Collections.sort(bpms);

            double finalBpm;
            int n = bpms.size();
            if (n % 2 == 0) {
                finalBpm = (bpms.get(n / 2 - 1) + bpms.get(n / 2)) / 2.0;
            } else {
                finalBpm = bpms.get(n / 2);
            }

            return Math.round(finalBpm);

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}