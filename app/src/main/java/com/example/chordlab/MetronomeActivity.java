package com.example.chordlab;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MetronomeActivity extends AppCompatActivity {

    // ── UI ──────────────────────────────────────────────────────────────────
    private TextView tvBpm, tvTempoLabel;
    private SeekBar seekBarBpm;
    private Button btnDecrease, btnIncrease;
    private Button btnStart;
    private Button btnTapTempo;
    private Button btnSig44, btnSig34, btnSig68, btnSig24;
    private View[] beatViews;

    // ── PENDULUM FIELDS ─────────────────────────────────────────────────────
    private View pendulumDot;
    private boolean pendulumGoingRight = true;

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
        btnSig44      = findViewById(R.id.btnSig44);
        btnSig34      = findViewById(R.id.btnSig34);
        btnSig68      = findViewById(R.id.btnSig68);
        btnSig24      = findViewById(R.id.btnSig24);

        // Bind the pendulum dot
        pendulumDot   = findViewById(R.id.pendulumDot);

        beatViews = new View[]{
                findViewById(R.id.beat1),
                findViewById(R.id.beat2),
                findViewById(R.id.beat3),
                findViewById(R.id.beat4),
                findViewById(R.id.beat5),
                findViewById(R.id.beat6)
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
        resetPendulum(); // Reset the pendulum position when stopped
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

                double frequency = 1200.0; // Normal beat tone
                if (currentBeat == 0) {
                    frequency = 1800.0; // Primary accent (Beat 1)
                } else if (timeSignature == 6 && currentBeat == 3) {
                    frequency = 1500.0; // Secondary accent (Beat 4 in 6/8 time)
                }

                playClick(frequency);

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

    /**
     * Synthesises a short click tone and plays it immediately.
     */
    private void playClick(double frequency) {
        int durationMs   = 30;
        int numSamples   = (SAMPLE_RATE * durationMs) / 1000;

        short[] samples = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double angle    = 2.0 * Math.PI * i * frequency / SAMPLE_RATE;
            double envelope = 1.0 - (double) i / numSamples;   // linear fade-out
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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            track.stop();
            track.release();
        }, durationMs + 50);
    }

    // ── Beat indicator lights & Pendulum ─────────────────────────────────────
    private void highlightBeat(int beat) {

        // Trigger the pendulum sweep on every single beat!
        animatePendulum();

        for (int i = 0; i < beatViews.length; i++) {
            if (i < timeSignature) {
                beatViews[i].setVisibility(View.VISIBLE);
                beatViews[i].setBackgroundResource(
                        i == beat
                                ? R.drawable.metronome_bg_beat_active
                                : R.drawable.metronome_bg_beat_inactive);
            } else {
                beatViews[i].setVisibility(View.GONE);
            }
        }
    }

    private void updateBeatIndicators() {
        for (int i = 0; i < beatViews.length; i++) {
            if (i < timeSignature) {
                beatViews[i].setVisibility(View.VISIBLE);
                beatViews[i].setBackgroundResource(R.drawable.metronome_bg_beat_inactive);
            } else {
                beatViews[i].setVisibility(View.GONE);
            }
        }
    }

    // ── Pendulum Animation Logic ─────────────────────────────────────────────
    private void animatePendulum() {
        if (pendulumDot == null) return;

        pendulumDot.post(() -> {
            // Get parent width to know how far to slide
            int parentWidth = ((View) pendulumDot.getParent()).getWidth();
            int dotWidth    = pendulumDot.getWidth();

            if (parentWidth == 0) return; // Prevent layout issues before fully drawn

            float maxTransX = parentWidth - dotWidth;

            float targetX = pendulumGoingRight ? maxTransX : 0f;
            pendulumGoingRight = !pendulumGoingRight;

            long beatMs = 60_000L / bpm;

            pendulumDot.animate()
                    .translationX(targetX)
                    .setDuration(beatMs)
                    .setInterpolator(new LinearInterpolator()) // Keeps the sweep smooth
                    .start();
        });
    }

    private void resetPendulum() {
        if (pendulumDot == null) return;
        pendulumDot.post(() -> {
            pendulumGoingRight = true;
            pendulumDot.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .start();
        });
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

    // ── Back button ──────────────────────────────────────────────────────────
    private void setupBackButton() {
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            Intent intent = new Intent(MetronomeActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });
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
        handler.removeCallbacksAndMessages(null);
    }
}