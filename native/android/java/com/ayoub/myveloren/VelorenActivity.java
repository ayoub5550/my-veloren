package com.ayoub.myveloren;

import android.app.NativeActivity;
import android.content.Intent;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;

/** NativeActivity wrapper: marks Firebase Test Lab game-loop launches so the Rust side can autostart. */
public class VelorenActivity extends NativeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File marker = new File(getFilesDir(), "autostart");
        Intent intent = getIntent();
        boolean loop = intent != null && "com.google.intent.action.TEST_LOOP".equals(intent.getAction());
        try {
            if (loop) {
                FileOutputStream out = new FileOutputStream(marker);
                out.write("1".getBytes());
                out.close();
            } else if (marker.exists()) {
                marker.delete();
            }
        } catch (Exception ignored) {
        }
        super.onCreate(savedInstanceState);
    }
}
