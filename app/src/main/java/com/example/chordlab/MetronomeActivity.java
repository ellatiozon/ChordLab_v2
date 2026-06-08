package com.example.chordlab;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MetronomeActivity extends AppCompatActivity {

    // ── UI ──────────────────────────────────────────────────────────────────
    private TextView tvBpm, tvTempoLabel;
    private SeekBar seekBarBpm;
    private Button btnDecrease, btnIncrease;
    private Button btnStart;
    private Button btnTapTempo;
    private Button btnUploadFile;
    private Button btnSig44, btnSig34, btnSig68, btnSig24;
    private View[] beatViews;

    // ── State ───────────────────────────────────────────────────────────────
    private int bpm = 120;
    private int timeSignature = 4;   // beats per bar
    private int currentBeat = 0;
    private boolean isRunning = false;

    // ── Tap tempo ───────────────────────────────────────────────────────────
    private final List<Long> tapTimes = new ArrayList<>();
    private static final long TAP_RESET_MS = 2500;

    // ── Tick engine ─────────────────────────────────────────────────────────
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Thread audioThread;
    private volatile boolean audioRunning = false;

    // Audio constants
    private static final int SAMPLE_RATE = 44100;

    // ── File Analysis Engine ────────────────────────────────────────────────
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> selectAudioLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            processAudioUri(result.getData().getData());
                        }
                    }
            );

    // ── Tempo name table ────────────────────────────────────────────────────
    private static final int[]    TEMPO_BPM    = {40, 60, 66, 76, 108, 120, 156, 168, 200, 220};
    private static final String[] TEMPO_NAMES  = {
            "Grave", "Largo", "Larghetto", "Adagio",
            "Andante", "Allegro", "Vivace", "Presto",
            "Prestissimo", "Prestissimo"
    };

    // ════════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);

        bindViews();
        setupSeekBar();
        setupBpmButtons();
        setupTimeSignatureButtons();
        setupStartButton();
        setupTapTempo();
        setupUploadButton();
        setupBackButton();

        updateBpmDisplay();
    }

    // ── Bind ────────────────────────────────────────────────────────────────
    private void bindViews() {
        tvBpm         = findViewById(R.id.tvBpm);
        tvTempoLabel  = findViewById(R.id.tvTempoLabel);
        seekBarBpm    = findViewById(R.id.seekBarBpm);
        btnDecrease   = findViewById(R.id.btnDecrease);
        btnIncrease   = findViewById(R.id.btnIncrease);
        btnStart      = findViewById(R.id.btnStart);
        btnTapTempo   = findViewById(R.id.btnTapTempo);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnSig44      = findViewById(R.id.btnSig44);
        btnSig34      = findViewById(R.id.btnSig34);
        btnSig68      = findViewById(R.id.btnSig68);
        btnSig24      = findViewById(R.id.btnSig24);

        beatViews = new View[]{
                findViewById(R.id.beat1),
                findViewById(R.id.beat2),
                findViewById(R.id.beat3),
                findViewById(R.id.beat4)
        };
    }

    // ── SeekBar (40–220 BPM, mapped from 0–180) ─────────────────────────────
    private void setupSeekBar() {
        seekBarBpm.setMax(180);
        seekBarBpm.setProgress(bpm - 40);

        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    bpm = progress + 40;
                    updateBpmDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                if (isRunning) restartTick();
            }
        });
    }

    // ── +/− buttons ─────────────────────────────────────────────────────────
    private void setupBpmButtons() {
        btnDecrease.setOnClickListener(v -> changeBpm(-1));
        btnIncrease.setOnClickListener(v -> changeBpm(+1));
    }

    private void changeBpm(int delta) {
        bpm = Math.max(40, Math.min(220, bpm + delta));
        seekBarBpm.setProgress(bpm - 40);
        updateBpmDisplay();
        if (isRunning) restartTick();
    }

    // ── Time signature ───────────────────────────────────────────────────────
    private void setupTimeSignatureButtons() {
        btnSig44.setOnClickListener(v -> selectTimeSignature(4, btnSig44));
        btnSig34.setOnClickListener(v -> selectTimeSignature(3, btnSig34));
        btnSig68.setOnClickListener(v -> selectTimeSignature(6, btnSig68));
        btnSig24.setOnClickListener(v -> selectTimeSignature(2, btnSig24));

        // default: 4/4
        selectTimeSignature(4, btnSig44);
    }

    private void selectTimeSignature(int beats, Button selected) {
        timeSignature = beats;
        currentBeat   = 0;

        Button[] allSig = {btnSig44, btnSig34, btnSig68, btnSig24};
        for (Button b : allSig) {
            b.setBackgroundResource(R.drawable.metronome_bg_sig_normal);
        }
        selected.setBackgroundResource(R.drawable.metronome_bg_sig_selected);

        updateBeatIndicators();
    }

    // ── Start / Stop ─────────────────────────────────────────────────────────
    private void setupStartButton() {
        btnStart.setOnClickListener(v -> {
            if (isRunning) stopMetronome();
            else           startMetronome();
        });
    }

    private void startMetronome() {
        isRunning    = true;
        currentBeat  = 0;
        btnStart.setText("⏹  STOP");
        startAudioThread();
    }

    private void stopMetronome() {
        isRunning    = false;
        audioRunning = false;
        btnStart.setText("▶  START");
        currentBeat = 0;
        updateBeatIndicators();
    }

    private void restartTick() {
        audioRunning = false;
        startAudioThread();
    }

    // ── Audio thread (generates click sound via AudioTrack) ──────────────────
    private void startAudioThread() {
        audioRunning = false;
        if (audioThread != null && audioThread.isAlive()) {
            audioThread.interrupt();
            try { audioThread.join(300); } catch (InterruptedException ignored) {}
        }

        audioRunning = true;
        audioThread  = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            while (audioRunning && isRunning) {
                long intervalMs = 60_000L / bpm;

                playClick(currentBeat == 0);

                final int displayBeat = currentBeat;
                handler.post(() -> {
                    highlightBeat(displayBeat);
                    currentBeat = (currentBeat + 1) % timeSignature;
                });

                try {
                    Thread.sleep(Math.max(10, intervalMs - 10));
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private void playClick(boolean accent) {
        int durationMs   = 30;
        int numSamples   = (SAMPLE_RATE * durationMs) / 1000;
        double frequency = accent ? 1800.0 : 1200.0;

        short[] samples = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double angle    = 2.0 * Math.PI * i * frequency / SAMPLE_RATE;
            double envelope = 1.0 - (double) i / numSamples;
            samples[i] = (short) (Math.sin(angle) * envelope * Short.MAX_VALUE * 0.8);
        }

        int minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(Math.max(minBufferSize, numSamples * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();

        track.write(samples, 0, numSamples);
        track.play();

        handler.postDelayed(() -> {
            try {
                track.stop();
                track.release();
            } catch (Exception ignored) {}
        }, durationMs + 50);
    }

    // ── Beat indicator lights ────────────────────────────────────────────────
    private void highlightBeat(int beat) {
        for (int i = 0; i < beatViews.length; i++) {
            if (i < timeSignature) {
                beatViews[i].setVisibility(View.VISIBLE);
                beatViews[i].setBackgroundResource(
                        i == beat
                                ? R.drawable.metronome_bg_beat_active
                                : R.drawable.metronome_bg_beat_inactive);
            } else {
                beatViews[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateBeatIndicators() {
        for (int i = 0; i < beatViews.length; i++) {
            if (i < timeSignature) {
                beatViews[i].setVisibility(View.VISIBLE);
                beatViews[i].setBackgroundResource(R.drawable.metronome_bg_beat_inactive);
            } else {
                beatViews[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    // ── Tap Tempo ────────────────────────────────────────────────────────────
    private void setupTapTempo() {
        btnTapTempo.setOnClickListener(v -> {
            long now = System.currentTimeMillis();

            if (!tapTimes.isEmpty() && now - tapTimes.get(tapTimes.size() - 1) > TAP_RESET_MS) {
                tapTimes.clear();
            }

            tapTimes.add(now);

            if (tapTimes.size() >= 2) {
                long total    = tapTimes.get(tapTimes.size() - 1) - tapTimes.get(0);
                double avgGap = (double) total / (tapTimes.size() - 1);
                int tapped    = (int) Math.round(60_000.0 / avgGap);
                bpm           = Math.max(40, Math.min(220, tapped));
                seekBarBpm.setProgress(bpm - 40);
                updateBpmDisplay();
                if (isRunning) restartTick();
            }

            if (tapTimes.size() > 8) tapTimes.remove(0);
        });
    }

    // ── Smart File Upload & Detection ───────────────────────────────────────
    private void setupUploadButton() {
        if (btnUploadFile == null) return;

        btnUploadFile.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                selectAudioLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to open file manager.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processAudioUri(Uri uri) {
        if (uri == null) return;

        if (isRunning) stopMetronome();

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
                    btnUploadFile.setText("📁  Upload File");

                    if (detectedBpm > 0) {
                        bpm = (int) Math.round(detectedBpm);
                        bpm = Math.max(40, Math.min(220, bpm));

                        updateBpmDisplay();
                        seekBarBpm.setProgress(bpm - 40);

                        Toast.makeText(MetronomeActivity.this, "BPM Detected: " + bpm, Toast.LENGTH_SHORT).show();
                    } else {
                        updateBpmDisplay();
                        Toast.makeText(MetronomeActivity.this, "Could not determine BPM.", Toast.LENGTH_LONG).show();
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
                    updateBpmDisplay();

                    Toast.makeText(MetronomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    if (finalTempFile != null && finalTempFile.exists()) {
                        finalTempFile.delete();
                    }
                });
            }
        });
    }

    private File copyUriToTempFile(Uri uri) throws Exception {
        File tempFile = File.createTempFile("temp_audio", ".mp3", getCacheDir());
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) throw new Exception("Failed to open audio input stream");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private double detectBpm(File file) {
        try {
            android.media.MediaExtractor extractor = new android.media.MediaExtractor();

            // Runtime Fix: Extract and pass the safe FileDescriptor directly
            try (FileInputStream fis = new FileInputStream(file)) {
                extractor.setDataSource(fis.getFD());
            }

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

            long startTimeUs = 60 * 1000000L;
            long durationUs = 60 * 1000000L;
            long endTimeUs = startTimeUs + durationUs;

            extractor.seekTo(startTimeUs, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            String mime = format.getString(android.media.MediaFormat.KEY_MIME);
            android.media.MediaCodec codec = android.media.MediaCodec.createDecoderByType(mime);

            codec.configure(format, null, null, 0);
            codec.start();

            ArrayList<Float> energy = new ArrayList<>();
            android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();

            int sampleRate = format.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channelCount = format.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT) : 2;

            double bytesPerSecond = sampleRate * channelCount * 2.0;
            double totalBytesProcessed = 0;
            float lastRawEnergy = 0f;
            boolean done = false;

            while (!done) {
                int inputIndex = codec.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
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

                int outputIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outputIndex == android.media.MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Thread.sleep(5);
                    continue;
                } else if (outputIndex == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ||
                           outputIndex == android.media.MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // Runtime Fix: Keep the loop moving forward safely on structural audio frame shifts
                    continue;
                } else if (outputIndex >= 0) {
                    if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        done = true;
                    }

                    ByteBuffer out = codec.getOutputBuffer(outputIndex);
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

                        energy.add(flux > 0 ? flux : 0f);
                    }
                    codec.releaseOutputBuffer(outputIndex, false);
                }
            }

            codec.stop();
            codec.release();
            extractor.release();

            // ── AUTOCORRELATION ──
            List<Integer> topLags = new ArrayList<>();
            List<Double> topScores = new ArrayList<>();

            for (int lag = 15; lag < 250; lag++) {
                double score = 0;
                for (int i = 0; i < energy.size() - lag; i++) {
                    score += energy.get(i) * energy.get(i + lag);
                }

                if (lag > 15 && lag < 249) {
                    double prevScore = 0;
                    double nextScore = 0;
                    for (int i = 0; i < energy.size() - (lag - 1); i++) prevScore += energy.get(i) * energy.get(i + lag - 1);
                    for (int i = 0; i < energy.size() - (lag + 1); i++) nextScore += energy.get(i) * energy.get(i + lag + 1);

                    if (score < prevScore || score < nextScore) continue;
                }

                if (topScores.size() < 5) {
                    topLags.add(lag);
                    topScores.add(score);
                } else {
                    int minIndex = 0;
                    for (int j = 1; j < topScores.size(); j++) {
                        if (topScores.get(j) < topScores.get(minIndex)) minIndex = j;
                    }
                    if (score > topScores.get(minIndex)) {
                        topScores.set(minIndex, score);
                        topLags.set(minIndex, lag);
                    }
                }
            }

            List<Double> bpms = new ArrayList<>();
            double averageChunkSize = totalBytesProcessed / energy.size();
            double frameRate = bytesPerSecond / averageChunkSize;

            for (int lag : topLags) {
                double secondsPerBeat = lag / frameRate;
                if (secondsPerBeat <= 0) continue;

                double calculatedBpm = 60.0 / secondsPerBeat;
                while (calculatedBpm < 85) calculatedBpm *= 2;
                while (calculatedBpm > 170) calculatedBpm /= 2;

                bpms.add(calculatedBpm);
            }

            if (bpms.isEmpty()) return -1;

            Collections.sort(bpms);
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

    // ── Back button ──────────────────────────────────────────────────────────
    private void setupBackButton() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private void updateBpmDisplay() {
        tvBpm.setText(String.valueOf(bpm));
        tvTempoLabel.setText(getTempoName(bpm));
    }

    private String getTempoName(int bpm) {
        for (int i = TEMPO_BPM.length - 1; i >= 0; i--) {
            if (bpm >= TEMPO_BPM[i]) return TEMPO_NAMES[i];
        }
        return "Grave";
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onPause() {
        super.onPause();
        if (isRunning) stopMetronome();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRunning = false;
        executorService.shutdown();
        handler.removeCallbacksAndMessages(null);
    }
}