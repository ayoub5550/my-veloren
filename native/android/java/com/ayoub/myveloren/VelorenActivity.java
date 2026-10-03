package com.ayoub.myveloren;

import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.io.File;
import java.io.FileOutputStream;

/**
 * NativeActivity wrapper.
 *  - Marks Firebase Test Lab game-loop launches (file "autostart" = scenario number) so the
 *    Rust side autostarts and runs the scripted scenario.
 *  - vibrate(ms): haptic feedback, called from Rust over JNI (dev.3, 3.10).
 *  - finishLoop(): ends the current game-loop scenario (dev.3).
 */
public class VelorenActivity extends NativeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File marker = new File(getFilesDir(), "autostart");
        Intent intent = getIntent();
        boolean loop = intent != null && "com.google.intent.action.TEST_LOOP".equals(intent.getAction());
        try {
            if (loop) {
                int scenario = intent.getIntExtra("scenario", 1);
                FileOutputStream out = new FileOutputStream(marker);
                out.write(Integer.toString(scenario).getBytes());
                out.close();
            } else if (marker.exists()) {
                marker.delete();
            }
        } catch (Exception ignored) {
        }
        super.onCreate(savedInstanceState);
    }

    /** Called from Rust (JNI) on hits taken / dealt. */
    public void vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(Math.max(1, ms), VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }

    /** Called from Rust (JNI) when a Test Lab game-loop scenario is complete. */
    public void finishLoop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }
}
