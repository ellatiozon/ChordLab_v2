package com.example.chordlab;

import android.content.Context;
import android.net.Uri;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoAudioSlicer {

    public interface SlicerCallback {
        void onProgressUpdate(String status);
        void onSliceReady(float[] audioChunk, float timestampSeconds);
        void onSlicingComplete();
        void onError(String error);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // 1. Core Mathematical Constants
    private static final int SAMPLE_RATE = 16000;
    private static final int WINDOW_SIZE = 16000; // Always 1 full second (Required by Neural Net)

    // NEW: The Micro-Buffer Step
    // 3200 samples = exactly 0.2 seconds. This means the AI makes 5 predictions every second.
    private static final int HOP_SIZE = 3200;

    public void processVideo(Context context, Uri videoUri, SlicerCallback callback) {
        callback.onProgressUpdate("Copying video securely...");

        executorService.execute(() -> {
            try {
                // 1. Copy URI to a temporary local file
                File inputVideo = new File(context.getCacheDir(), "input_video.mp4");
                InputStream is = context.getContentResolver().openInputStream(videoUri);
                FileOutputStream fos = new FileOutputStream(inputVideo);
                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
                is.close();

                // 2. FFmpeg Extraction Command
                callback.onProgressUpdate("Extracting raw audio...");
                File outputPcm = new File(context.getCacheDir(), "extracted_audio.pcm");
                if (outputPcm.exists()) outputPcm.delete();

                String ffmpegCommand = "-y -i " + inputVideo.getAbsolutePath() +
                        " -f s16le -acodec pcm_s16le -ar " + SAMPLE_RATE +
                        " -ac 1 " + outputPcm.getAbsolutePath();

                FFmpegSession session = FFmpegKit.execute(ffmpegCommand);

                if (ReturnCode.isSuccess(session.getReturnCode())) {
                    callback.onProgressUpdate("Slicing audio for AI...");
                    sliceAndEmit(outputPcm, callback);
                } else {
                    callback.onError("FFmpeg Extraction Failed.");
                }

                // Cleanup temporary files
                if (inputVideo.exists()) inputVideo.delete();
                if (outputPcm.exists()) outputPcm.delete();

            } catch (Exception e) {
                callback.onError("Processing failed: " + e.getMessage());
            }
        });
    }

    public void sliceAudioTrack(float[] audioTrack, SlicerCallback listener) {
        int totalSamples = audioTrack.length;

        // 2. The Micro-Sliding Loop
        for (int i = 0; i + WINDOW_SIZE <= totalSamples; i += HOP_SIZE) {
            float[] windowChunk = new float[WINDOW_SIZE];

            // Copy 16,000 samples starting from the current 'i' position
            System.arraycopy(audioTrack, i, windowChunk, 0, WINDOW_SIZE);

            // Calculate the exact timestamp for the START of this micro-buffer
            float timestampSeconds = (float) i / SAMPLE_RATE;

            // Fire the chunk to the AI
            listener.onSliceReady(windowChunk, timestampSeconds);
        }
    }

    private void sliceAndEmit(File pcmFile, SlicerCallback callback) {
        try {
            FileInputStream fis = new FileInputStream(pcmFile);
            byte[] pcmBytes = new byte[(int) pcmFile.length()];
            fis.read(pcmBytes);
            fis.close();

            ByteBuffer byteBuffer = ByteBuffer.wrap(pcmBytes);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            int totalSamples = pcmBytes.length / 2; // 2 bytes per 16-bit sample
            float[] allFloats = new float[totalSamples];

            // Convert to normalized floats
            for (int i = 0; i < totalSamples; i++) {
                allFloats[i] = byteBuffer.getShort() / 32768.0f;
            }

            // Slice into perfect 16,000 chunks
            for (int i = 0; i <= totalSamples - WINDOW_SIZE; i += HOP_SIZE) {
                float[] chunk = new float[WINDOW_SIZE];
                System.arraycopy(allFloats, i, chunk, 0, WINDOW_SIZE);

                float timestampSec = (float) i / SAMPLE_RATE;
                callback.onSliceReady(chunk, timestampSec);
            }

            callback.onSlicingComplete();

        } catch (Exception e) {
            callback.onError("Slicing failed: " + e.getMessage());
        }
    }
}