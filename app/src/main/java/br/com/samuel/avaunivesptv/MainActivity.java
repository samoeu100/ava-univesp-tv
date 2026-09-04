package br.com.samuel.avaunivesptv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String START_URL =
            "https://ava.univesp.br/mod/page/view.php?id=235976";
    private static final String PREFS = "ava_tv_preferences";
    private static final String PREF_MODE = "control_mode";
    private static final String MODE_MOUSE = "mouse";
    private static final String MODE_REMOTE = "remote";
    private static final int CURSOR_SIZE = 48;
    private static final int CURSOR_RADIUS = CURSOR_SIZE / 2;
    private static final int CURSOR_STEP = 25;

    private WebView webView;
    private FrameLayout root;
    private View cursor;
    private int cursorX = 320;
    private int cursorY = 180;
    private boolean mouseMode = true;
    private boolean centerLongPressHandled;
    private boolean modeDialogVisible;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportMultipleWindows(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setWebViewClient(new SafeWebViewClient());
        webView.setWebChromeClient(new TvChromeClient());
        webView.setDownloadListener(createDownloadListener());
        webView.requestFocus();

        cursor = createCursor();
        root.addView(cursor, new FrameLayout.LayoutParams(CURSOR_SIZE, CURSOR_SIZE));
        setContentView(root);

        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedMode = preferences.getString(PREF_MODE, null);
        if (MODE_REMOTE.equals(savedMode)) mouseMode = false;
        applyControlMode(false);
        updateCursorView();

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        if (savedMode == null) {
            root.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showControlModeDialog();
                }
            }, 500);
        }
    }

    private View createCursor() {
        View view = new View(this);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.rgb(85, 201, 243));
        background.setStroke(4, Color.WHITE);
        view.setBackground(background);
        view.setElevation(16f);
        view.setClickable(false);
        view.setFocusable(false);
        return view;
    }

    private void showControlModeDialog() {
        if (modeDialogVisible || isFinishing()) return;
        modeDialogVisible = true;
        String[] modes = {
                "modo mouse — mover a bolinha e clicar",
                "modo controle — passar pelos itens da página"
        };
        int selected = mouseMode ? 0 : 1;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("como você quer navegar?")
                .setSingleChoiceItems(modes, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface choiceDialog, int which) {
                        mouseMode = which == 0;
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit()
                                .putString(PREF_MODE, mouseMode ? MODE_MOUSE : MODE_REMOTE)
                                .apply();
                        applyControlMode(true);
                        choiceDialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface ignored) {
                        modeDialogVisible = false;
                        centerLongPressHandled = false;
                    }
                })
                .create();
        dialog.show();
    }

    private void applyControlMode(boolean showMessage) {
        cursor.setVisibility(mouseMode ? View.VISIBLE : View.GONE);
        webView.requestFocus();
        if (showMessage) {
            Toast.makeText(this, mouseMode ? "Modo mouse" : "Modo controle",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCursorView() {
        cursor.setX(cursorX - CURSOR_RADIUS);
        cursor.setY(cursorY - CURSOR_RADIUS);
    }

    private void moveCursor(int deltaX, int deltaY) {
        if (webView == null) return;

        int maxX = Math.max(CURSOR_RADIUS, webView.getWidth() - CURSOR_RADIUS);
        int maxY = Math.max(CURSOR_RADIUS, webView.getHeight() - CURSOR_RADIUS);
        int nextX = Math.max(CURSOR_RADIUS, Math.min(maxX, cursorX + deltaX));
        int nextY = cursorY + deltaY;

        if (nextY < CURSOR_RADIUS) {
            scrollAtCursor(-120);
            nextY = CURSOR_RADIUS;
        } else if (nextY > maxY) {
            scrollAtCursor(120);
            nextY = maxY;
        }

        cursorX = nextX;
        cursorY = nextY;
        updateCursorView();
    }

    private MotionEvent touchEvent(long downTime, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        return event;
    }

    private void clickCursor() {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = touchEvent(downTime, MotionEvent.ACTION_DOWN, cursorX, cursorY);
        webView.dispatchTouchEvent(down);
        down.recycle();

        MotionEvent up = touchEvent(downTime, MotionEvent.ACTION_UP, cursorX, cursorY);
        webView.dispatchTouchEvent(up);
        up.recycle();
    }

    private void scrollAtCursor(int amount) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = touchEvent(downTime, MotionEvent.ACTION_DOWN, cursorX, cursorY);
        webView.dispatchTouchEvent(down);
        down.recycle();

        MotionEvent move = touchEvent(
                downTime, MotionEvent.ACTION_MOVE, cursorX, cursorY - amount);
        webView.dispatchTouchEvent(move);
        move.recycle();

        MotionEvent up = touchEvent(
                downTime, MotionEvent.ACTION_UP, cursorX, cursorY - amount);
        webView.dispatchTouchEvent(up);
        up.recycle();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (fullscreenView != null || modeDialogVisible) {
            return super.dispatchKeyEvent(event);
        }

        int keyCode = event.getKeyCode();
        boolean center = keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER;

        if (center && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() > 0 && !centerLongPressHandled) {
            centerLongPressHandled = true;
            showControlModeDialog();
            return true;
        }
        if (center && event.getAction() == KeyEvent.ACTION_UP && centerLongPressHandled) {
            centerLongPressHandled = false;
            return true;
        }

        if (!mouseMode) return super.dispatchKeyEvent(event);

        boolean arrow = keyCode >= KeyEvent.KEYCODE_DPAD_UP
                && keyCode <= KeyEvent.KEYCODE_DPAD_CENTER;
        if (!center && !arrow) return super.dispatchKeyEvent(event);

        if (center) {
            if (event.getAction() == KeyEvent.ACTION_UP) clickCursor();
            return true;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) moveCursor(0, -CURSOR_STEP);
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) moveCursor(0, CURSOR_STEP);
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) moveCursor(-CURSOR_STEP, 0);
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) moveCursor(CURSOR_STEP, 0);
        return true;
    }

    private final class SafeWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if ("https".equalsIgnoreCase(uri.getScheme())) return false;
            Toast.makeText(MainActivity.this,
                    "Somente conexões seguras são permitidas.", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private final class TvChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (fullscreenView != null) {
                callback.onCustomViewHidden();
                return;
            }
            fullscreenView = view;
            fullscreenCallback = callback;
            webView.setVisibility(View.GONE);
            cursor.setVisibility(View.GONE);
            root.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            view.requestFocus();
        }

        @Override
        public void onHideCustomView() {
            closeFullscreenVideo();
        }
    }

    private DownloadListener createDownloadListener() {
        return new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                    String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request =
                            new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.addRequestHeader(
                            "Cookie", CookieManager.getInstance().getCookie(url));
                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, "arquivo-univesp");
                    DownloadManager manager = (DownloadManager)
                            getSystemService(Context.DOWNLOAD_SERVICE);
                    manager.enqueue(request);
                    Toast.makeText(MainActivity.this,
                            "Download iniciado.", Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    Toast.makeText(MainActivity.this,
                            "Não foi possível baixar o arquivo.", Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    private void closeFullscreenVideo() {
        if (fullscreenView == null) return;
        root.removeView(fullscreenView);
        fullscreenView = null;
        webView.setVisibility(View.VISIBLE);
        cursor.setVisibility(mouseMode ? View.VISIBLE : View.GONE);
        if (fullscreenCallback != null) {
            fullscreenCallback.onCustomViewHidden();
            fullscreenCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (fullscreenView != null) {
            closeFullscreenVideo();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
