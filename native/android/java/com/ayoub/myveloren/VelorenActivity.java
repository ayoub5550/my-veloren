package com.ayoub.myveloren;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ApplicationExitInfo;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.os.SystemClock;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * NativeActivity wrapper.
 *  - Marks Firebase Test Lab game-loop launches (file "autostart" = scenario number) so the
 *    Rust side autostarts and runs the scripted scenario.
 *  - vibrate(ms): haptic feedback, called from Rust over JNI (dev.3, 3.10).
 *  - finishLoop(): ends the current game-loop scenario (dev.3).
 *  - dev.5: keeps the screen on, immersive full screen (system bars hidden, display cutout
 *    left to the system so nothing is drawn under the notch), first-launch extraction
 *    progress dialog, crash reports from the previous launch (Rust panics + Android exit
 *    info) offered for sharing, and two test hooks: cycleBackground() and sendBack().
 */
public class VelorenActivity extends NativeActivity {
    private static final String TAG = "veloren";
    private boolean loop;
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File marker = new File(getFilesDir(), "autostart");
        Intent intent = getIntent();
        loop = intent != null && "com.google.intent.action.TEST_LOOP".equals(intent.getAction());
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
        // 5.3: keep the screen on while the game runs; landscape is fixed in the manifest.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            // Never draw under the notch / punch hole: the HUD has no safe-area support.
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            getWindow().setAttributes(lp);
        }
        super.onCreate(savedInstanceState);
        collectCrashReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        writeActivityFlags();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /** Read by the Test Lab check "activity_flags". */
    private void writeActivityFlags() {
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            boolean keepOn = (lp.flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
            int o = getRequestedOrientation();
            boolean landscape = o == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    || o == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    || o == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
            int cutout = Build.VERSION.SDK_INT >= 28 ? lp.layoutInDisplayCutoutMode : -1;
            String s = "keep_screen_on=" + (keepOn ? 1 : 0) + "\nlandscape=" + (landscape ? 1 : 0)
                    + "\ncutout_mode=" + cutout + "\nsdk=" + Build.VERSION.SDK_INT
                    + "\ndevice=" + Build.MANUFACTURER + " " + Build.MODEL + "\n";
            writeFile(new File(getFilesDir(), "activity_flags.txt"), s.getBytes("UTF-8"));
        } catch (Exception e) {
            Log.w(TAG, "activity flags: " + e);
        }
    }

    // ------------------------------------------------------------ crash reports (5.5) ---

    /**
     * Gather what the previous launches left behind: Rust panic reports
     * (files/userdata/crash/*.txt) and, on Android 11+, the system's record of how the
     * process ended (native crash, ANR, Java crash). The combined text goes to
     * files/crash_last_report.txt; outside Test Lab the player is asked whether to share it.
     */
    private void collectCrashReports() {
        try {
            StringBuilder sb = new StringBuilder();
            File dir = new File(getFilesDir(), "userdata/crash");
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    if (!f.getName().endsWith(".txt")) continue;
                    sb.append("==== ").append(f.getName()).append(" ====\n");
                    sb.append(new String(readFile(f, 64 * 1024), "UTF-8")).append("\n");
                    f.renameTo(new File(dir, f.getName() + ".reported"));
                }
            }
            if (Build.VERSION.SDK_INT >= 30) {
                appendExitInfo(sb);
            }
            if (sb.length() == 0) {
                return;
            }
            String header = "my-veloren " + getVersionName() + " on " + Build.MANUFACTURER + " "
                    + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ", SDK "
                    + Build.VERSION.SDK_INT + ")\n\n";
            final String report = header + sb;
            writeFile(new File(getFilesDir(), "crash_last_report.txt"), report.getBytes("UTF-8"));
            Log.i(TAG, "VEL-LIFE crash report collected: " + report.length() + " bytes");
            boolean realCrash = report.contains("kind: panic") || report.contains("exit_reason: CRASH")
                    || report.contains("exit_reason: ANR");
            if (!loop && realCrash) {
                offerShare(report);
            }
        } catch (Exception e) {
            Log.w(TAG, "crash report: " + e);
        }
    }

    private void appendExitInfo(StringBuilder sb) throws Exception {
        if (Build.VERSION.SDK_INT < 30) return;
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationExitInfo> infos = am.getHistoricalProcessExitReasons(getPackageName(), 0, 5);
        SharedPreferences prefs = getSharedPreferences("crash", MODE_PRIVATE);
        long seen = prefs.getLong("last_exit_ts", 0);
        long newest = seen;
        for (ApplicationExitInfo e : infos) {
            if (e.getTimestamp() <= seen) continue;
            newest = Math.max(newest, e.getTimestamp());
            String reason = exitReason(e.getReason());
            Log.i(TAG, "VEL-LIFE previous exit: " + reason + " status=" + e.getStatus() + " " + e.getDescription());
            sb.append("==== exit_reason: ").append(reason).append(" ====\n")
              .append("time: ").append(e.getTimestamp()).append(" status: ").append(e.getStatus())
              .append(" importance: ").append(e.getImportance())
              .append("\ndescription: ").append(e.getDescription()).append("\n");
            if (e.getReason() == ApplicationExitInfo.REASON_ANR) {
                InputStream in = e.getTraceInputStream();
                if (in != null) {
                    sb.append(new String(readAll(in, 64 * 1024), "UTF-8")).append("\n");
                }
            } else if (e.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                sb.append("(native tombstone: adb bugreport, or symbolize with the unstripped .so)\n");
            }
        }
        prefs.edit().putLong("last_exit_ts", newest).apply();
    }

    private static String exitReason(int r) {
        switch (r) {
            case ApplicationExitInfo.REASON_CRASH: return "CRASH";
            case ApplicationExitInfo.REASON_CRASH_NATIVE: return "CRASH_NATIVE";
            case ApplicationExitInfo.REASON_ANR: return "ANR";
            case ApplicationExitInfo.REASON_LOW_MEMORY: return "LOW_MEMORY";
            case ApplicationExitInfo.REASON_SIGNALED: return "SIGNALED";
            case ApplicationExitInfo.REASON_EXIT_SELF: return "EXIT_SELF";
            case ApplicationExitInfo.REASON_USER_REQUESTED: return "USER_REQUESTED";
            case ApplicationExitInfo.REASON_USER_STOPPED: return "USER_STOPPED";
            case ApplicationExitInfo.REASON_OTHER: return "OTHER";
            default: return "REASON_" + r;
        }
    }

    private void offerShare(final String report) {
        new AlertDialog.Builder(this)
                .setTitle("Veloren")
                .setMessage("The game closed unexpectedly last time. Share the crash report?\n"
                        + "أُغلقت اللعبة بشكل غير متوقع في المرة السابقة. هل تريد مشاركة تقرير الخطأ؟")
                .setPositiveButton("Share / مشاركة", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_SUBJECT, "my-veloren crash report");
                    send.putExtra(Intent.EXTRA_TEXT, report.length() > 90000 ? report.substring(0, 90000) : report);
                    startActivity(Intent.createChooser(send, "Share crash report"));
                    }
                })
                .setNegativeButton("Close / إغلاق", null)
                .show();
    }

    // --------------------------------------------------- asset extraction progress (5.4) ---

    /** Called from Rust (JNI) while assets.tar is unpacked on first launch. */
    public void extractProgress(final int pct) {
        final Context ctx = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            try {
                if (progressDialog == null) {
                    LinearLayout box = new LinearLayout(ctx);
                    box.setOrientation(LinearLayout.VERTICAL);
                    box.setPadding(48, 40, 48, 40);
                    box.setGravity(Gravity.CENTER_HORIZONTAL);
                    progressText = new TextView(ctx);
                    progressText.setTextSize(16);
                    progressBar = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
                    progressBar.setMax(100);
                    box.addView(progressText);
                    box.addView(progressBar, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    progressDialog = new AlertDialog.Builder(ctx).setView(box).setCancelable(false).create();
                    progressDialog.show();
                }
                progressBar.setProgress(pct);
                progressText.setText("Preparing game files (first launch)… " + pct + "%\n"
                        + "تجهيز ملفات اللعبة (أول تشغيل)… " + pct + "%");
            } catch (Exception e) {
                Log.w(TAG, "progress: " + e);
            }
            }
        });
    }

    /** Called from Rust (JNI) when extraction is complete. */
    public void extractDone() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
        });
    }

    // ---------------------------------------------------------------- test hooks (dev.5) ---

    /** Test: put an opaque activity on top for ms, so Android destroys our window (like Home). */
    public void cycleBackground(final int ms) {
        final Context ctx = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(ctx, CoverActivity.class);
                i.putExtra("ms", ms);
                startActivity(i);
            }
        });
    }

    /** Test: inject a real Back key press into this app (allowed for its own windows). */
    public void sendBack() {
        // Instrumentation.sendKeyDownUpSync needs INJECT_EVENTS (denied for normal apps,
        // vm6). Instead feed a KEYCODE_BACK through this window's own input pipeline the
        // way an IME does: ViewRootImpl -> NativePostImeInputStage -> the NativeActivity
        // InputQueue -> android-activity -> winit -> Rust. Only the system
        // InputDispatcher hop is skipped. No permission is needed for the own window.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View target = getWindow().getDecorView();
                    BaseInputConnection ic = new BaseInputConnection(target, false);
                    long t = SystemClock.uptimeMillis();
                    ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0));
                    ic.sendKeyEvent(new KeyEvent(t, t + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0));
                    Log.i(TAG, "sendBack: KEYCODE_BACK queued via window input pipeline");
                } catch (Exception e) {
                    Log.w(TAG, "sendBack: " + e);
                }
            }
        });
    }

    // ------------------------------------------------------------------- dev.3 calls ---

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

    // ---------------------------------------------------------------------- helpers ---

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private static byte[] readFile(File f, int max) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            return readAll(in, max);
        }
    }

    private static byte[] readAll(InputStream in, int max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0 && out.size() < max) {
            out.write(buf, 0, Math.min(n, max - out.size()));
        }
        in.close();
        return out.toByteArray();
    }

    private static void writeFile(File f, byte[] data) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }
}
