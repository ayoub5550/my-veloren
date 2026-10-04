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
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Environment;
import java.io.OutputStream;
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

    // ---------------------------------------------------------- dev.6 text input (6.4) ---

    /** Registered from Rust with RegisterNatives (see android.rs native_text). */
    private static native void nativeText(String text, boolean ok);

    private AlertDialog textDialog;

    /**
     * Called from Rust when a game text field gets the focus (chat, search, character
     * name). Shows a dialog with a real EditText, so the system keyboard works with every
     * language (Arabic included), autocorrect and voice input. OK sends the whole text to
     * Rust, which types it into the focused field and presses Enter.
     * autoText (Test Lab only): type it and press OK after 1.5 s, so the video shows it.
     */
    public void showTextInput(final String title, final String initial, final String autoText) {
        showTextInput(title, initial, autoText, false);
    }

    /** dev.9: password = true hides the typed text (multiplayer account password). */
    public void showTextInput(final String title, final String initial, final String autoText,
                              final boolean password) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (textDialog != null && textDialog.isShowing()) return;
                    final EditText edit = new EditText(VelorenActivity.this);
                    edit.setSingleLine(true);
                    if (password) {
                        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    } else if (title != null && (title.startsWith("Username") || title.startsWith("Server"))) {
                        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                    }
                    edit.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);
                    edit.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
                    if (initial != null) {
                        edit.setText(initial);
                        edit.setSelection(edit.getText().length());
                    }
                    final boolean[] sent = new boolean[] { false };
                    AlertDialog.Builder b = new AlertDialog.Builder(VelorenActivity.this)
                        .setTitle(title)
                        .setView(edit)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (!sent[0]) {
                                    sent[0] = true;
                                    nativeText(edit.getText().toString(), true);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (!sent[0]) {
                                    sent[0] = true;
                                    nativeText(null, false);
                                }
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface d) {
                                if (!sent[0]) {
                                    sent[0] = true;
                                    nativeText(null, false);
                                }
                            }
                        });
                    textDialog = b.create();
                    edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent ev) {
                            if (actionId == EditorInfo.IME_ACTION_DONE && !sent[0]) {
                                sent[0] = true;
                                nativeText(edit.getText().toString(), true);
                                textDialog.dismiss();
                                return true;
                            }
                            return false;
                        }
                    });
                    textDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    textDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface d) {
                            hideSystemBars();
                        }
                    });
                    textDialog.show();
                    edit.requestFocus();
                    Log.i(TAG, "VEL-TEXT dialog shown: " + title);
                    if (autoText != null) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                edit.setText(autoText);
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (textDialog != null && textDialog.isShowing()) {
                                            textDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                                            Log.i(TAG, "VEL-TEXT auto text submitted");
                                        }
                                    }
                                }, 1200);
                            }
                        }, 1500);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "showTextInput: " + e);
                    nativeText(null, false);
                }
            }
        });
    }

    // ------------------------------------------------- dev.9 gamepad / keyboard / mouse ---

    /**
     * Mouse look: while the game holds the cursor, capture the pointer (hidden, relative
     * motion, no screen edges). Captured events still go through the NativeActivity input
     * queue, so Rust sees them as SOURCE_MOUSE_RELATIVE motion events.
     */
    public void pointerCapture(final boolean on) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View v = getWindow().getDecorView();
                    if (on) {
                        v.setFocusable(true);
                        v.setFocusableInTouchMode(true);
                        v.requestFocus();
                        v.requestPointerCapture();
                    } else {
                        v.releasePointerCapture();
                    }
                    Log.i(TAG, "VEL-INPUT pointer capture " + (on ? "requested" : "released")
                        + " hasCapture=" + v.hasPointerCapture());
                } catch (Exception e) {
                    Log.w(TAG, "pointerCapture: " + e);
                }
            }
        });
    }

    /** Open a web page (account registration for the official servers). */
    public void openUrl(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Log.w(TAG, "openUrl: " + e);
                    toast(url);
                }
            }
        });
    }

    private static java.lang.reflect.Method dispatchInput;
    private static Object dispatchTarget;
    private static boolean dispatchBlocked;

    /**
     * Test (scenario 12): feed an input event through this window's own input pipeline
     * (ViewRootImpl input stages -> the NativeActivity InputQueue -> android-activity ->
     * winit hook -> Rust), like a real Bluetooth device except for the system
     * InputDispatcher hop. Instrumentation needs INJECT_EVENTS. The ViewRootImpl entry
     * points are hidden APIs; when Android blocks all of them, keys still go through the
     * IME path (BaseInputConnection) and the Rust harness feeds motion values directly
     * to the hook handlers (it detects that a probe event never arrived).
     */
    private void inject(final android.view.InputEvent ev) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dispatchInput == null && !dispatchBlocked) {
                    Object root = getWindow().getDecorView().getParent();
                    Object[][] tries = {
                        { root, "dispatchInputEvent" },
                        { root, "enqueueInputEvent" },
                        { getWindow(), "injectInputEvent" },
                    };
                    StringBuilder why = new StringBuilder();
                    for (Object[] t : tries) {
                        Class<?> c = t[0].getClass();
                        while (c != null && dispatchInput == null) {
                            try {
                                java.lang.reflect.Method m = c.getDeclaredMethod((String) t[1], android.view.InputEvent.class);
                                m.setAccessible(true);
                                dispatchInput = m;
                                dispatchTarget = t[0];
                                Log.i(TAG, "VEL-INPUT inject via " + c.getName() + "." + t[1]);
                            } catch (Throwable e) {
                                c = c.getSuperclass();
                            }
                        }
                        if (dispatchInput != null) break;
                        why.append(t[0].getClass().getSimpleName()).append('.').append(t[1]).append(" hidden; ");
                    }
                    if (dispatchInput == null) {
                        dispatchBlocked = true;
                        Log.w(TAG, "VEL-INPUT window injection blocked (" + why + "); keys use the IME path");
                    }
                }
                if (dispatchInput != null) {
                    try {
                        dispatchInput.invoke(dispatchTarget, ev);
                        return;
                    } catch (Throwable e) {
                        Log.w(TAG, "VEL-INPUT inject failed: " + e);
                    }
                }
                if (ev instanceof KeyEvent) {
                    // fallback: the IME path (keeps the source)
                    new BaseInputConnection(getWindow().getDecorView(), false).sendKeyEvent((KeyEvent) ev);
                }
            }
        });
    }

    /** flags: KeyEvent.FLAG_* (the harness retries with FLAG_KEEP_TOUCH_MODE, see android.rs). */
    public void injectKey(int source, int code, boolean down, int flags) {
        long t = SystemClock.uptimeMillis();
        if (code >= KeyEvent.KEYCODE_DPAD_UP && code <= KeyEvent.KEYCODE_DPAD_RIGHT && down) {
            Log.i(TAG, "VEL-INPUT inject nav key " + code + " windowTouchMode="
                + getWindow().getDecorView().isInTouchMode() + " flags=" + flags);
        }
        inject(new KeyEvent(t, t, down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, code, 0, 0,
            -1, 0, flags, source));
    }

    /** lx, ly, rx, ry (Z / RZ), left / right trigger, hat x / y. */
    public void injectJoystick(float lx, float ly, float rx, float ry, float lt, float rt, float hx, float hy) {
        long t = SystemClock.uptimeMillis();
        android.view.MotionEvent.PointerProperties[] pp = { new android.view.MotionEvent.PointerProperties() };
        pp[0].id = 0;
        pp[0].toolType = android.view.MotionEvent.TOOL_TYPE_UNKNOWN;
        android.view.MotionEvent.PointerCoords[] pc = { new android.view.MotionEvent.PointerCoords() };
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_X, lx);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_Y, ly);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_Z, rx);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_RZ, ry);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_LTRIGGER, lt);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_RTRIGGER, rt);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_HAT_X, hx);
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_HAT_Y, hy);
        inject(android.view.MotionEvent.obtain(t, t, android.view.MotionEvent.ACTION_MOVE, 1, pp, pc,
            0, 0, 1f, 1f, -1, 0, android.view.InputDevice.SOURCE_JOYSTICK, 0));
    }

    /** A mouse event at (x, y) px: action = MotionEvent.ACTION_*, buttons = button state. */
    public void injectMouse(int action, float x, float y, int buttons, float vscroll) {
        long t = SystemClock.uptimeMillis();
        android.view.MotionEvent.PointerProperties[] pp = { new android.view.MotionEvent.PointerProperties() };
        pp[0].id = 0;
        pp[0].toolType = android.view.MotionEvent.TOOL_TYPE_MOUSE;
        android.view.MotionEvent.PointerCoords[] pc = { new android.view.MotionEvent.PointerCoords() };
        pc[0].x = x;
        pc[0].y = y;
        pc[0].setAxisValue(android.view.MotionEvent.AXIS_VSCROLL, vscroll);
        android.view.MotionEvent ev = android.view.MotionEvent.obtain(t, t, action, 1, pp, pc,
            0, buttons, 1f, 1f, -1, 0, android.view.InputDevice.SOURCE_MOUSE, 0);
        if (action == android.view.MotionEvent.ACTION_BUTTON_PRESS || action == android.view.MotionEvent.ACTION_BUTTON_RELEASE) {
            try {
                android.view.MotionEvent.class.getMethod("setActionButton", int.class).invoke(ev, buttons == 0 ? 1 : buttons);
            } catch (Throwable ignored) {
            }
        }
        inject(ev);
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

    // ------------------------------------------------- dev.8 save export / import (8.4) ---

    /** Registered from Rust with RegisterNatives (see android.rs native_file).
     *  op 1 = export finished, op 2 = import file picked (text = local copy path). */
    private static native void nativeFile(int op, String text, boolean ok);

    private static final int REQ_IMPORT = 4711;
    private String importDest;

    /** Called from Rust: show a short message. */
    public void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VelorenActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Called from Rust: copy the saves file made by the game (all singleplayer worlds,
     * their maps and characters) into the public Download folder, so it can be moved to
     * another phone or kept as a backup. API 29+: MediaStore (no permission needed);
     * API 26-28: app-specific external Download folder.
     */
    public void exportFile(final String path, final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String where;
                boolean ok = false;
                try {
                    File src = new File(path);
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues v = new ContentValues();
                        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                        v.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-tar");
                        v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                        ContentResolver cr = getContentResolver();
                        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                        if (uri == null) throw new Exception("MediaStore insert failed");
                        try (InputStream in = new FileInputStream(src);
                             OutputStream out = cr.openOutputStream(uri)) {
                            copy(in, out);
                        }
                        where = "Download/" + name;
                    } else {
                        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        File dst = new File(dir, name);
                        try (InputStream in = new FileInputStream(src);
                             OutputStream out = new FileOutputStream(dst)) {
                            copy(in, out);
                        }
                        where = dst.getAbsolutePath();
                    }
                    ok = true;
                    toast("Saves exported / تم تصدير الحفظ: " + where);
                } catch (Throwable e) {
                    where = "export failed: " + e;
                    toast("Export failed / فشل التصدير: " + e.getMessage());
                }
                Log.i(TAG, "dev8 export ok=" + ok + " " + where);
                try { nativeFile(1, where, ok); } catch (Throwable ignored) { }
            }
        }).start();
    }

    /** Called from Rust: let the player choose a saves file (system file picker). */
    public void pickImport(final String dest) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    importDest = dest;
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, REQ_IMPORT);
                } catch (Throwable e) {
                    try { nativeFile(2, "no file picker: " + e.getMessage(), false); } catch (Throwable ignored) { }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null || importDest == null) {
            try { nativeFile(2, "", false); } catch (Throwable ignored) { }
            return;
        }
        final Uri uri = data.getData();
        final String dest = importDest;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dst = new File(dest);
                    dst.getParentFile().mkdirs();
                    try (InputStream in = getContentResolver().openInputStream(uri);
                         OutputStream out = new FileOutputStream(dst)) {
                        copy(in, out);
                    }
                    Log.i(TAG, "dev8 import copied to " + dest);
                    nativeFile(2, dest, true);
                } catch (Throwable e) {
                    try { nativeFile(2, "import failed: " + e.getMessage(), false); } catch (Throwable ignored) { }
                }
            }
        }).start();
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        out.flush();
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
