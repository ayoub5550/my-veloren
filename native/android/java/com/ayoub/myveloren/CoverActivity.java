package com.ayoub.myveloren;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

/**
 * dev.5 Test Lab helper: an opaque activity shown on top of the game for a few seconds.
 * The game activity is paused and stopped and its native window is destroyed, which is
 * the same path as Home / app switcher / screen off. Finishes itself after "ms".
 */
public class CoverActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("my-veloren dev.5: game in background");
        t.setTextColor(Color.WHITE);
        t.setBackgroundColor(Color.rgb(20, 24, 40));
        t.setGravity(Gravity.CENTER);
        t.setTextSize(22);
        setContentView(t);
        int ms = getIntent().getIntExtra("ms", 3000);
        Log.i("veloren", "VEL-LIFE cover activity shown for " + ms + " ms");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, ms);
    }
}
