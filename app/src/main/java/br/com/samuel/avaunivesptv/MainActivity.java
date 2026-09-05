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
    private static final long REMOTE_REPEAT_DELAY_MS = 110;
    private static final String REMOTE_NAVIGATION_SETUP =
            "(function(){"
            + "if(window.__avaTvMove)return;"
            + "var marker='data-ava-tv-focus';"
            + "var selector='a[href],button,input:not([type=hidden]),select,textarea,summary,"
            + "[role=button],[role=link],[tabindex]:not([tabindex=\\\"-1\\\"]),iframe,video';"
            + "if(!document.getElementById('ava-tv-focus-style')){"
            + "var style=document.createElement('style');style.id='ava-tv-focus-style';"
            + "style.textContent='['+marker+'=\\\"true\\\"]{outline:4px solid #35d8ff!important;"
            + "outline-offset:3px!important;box-shadow:0 0 0 7px rgba(0,90,255,.28)!important;}';"
            + "(document.head||document.documentElement).appendChild(style);}" 
            + "function visible(el){var r=el.getBoundingClientRect(),s=getComputedStyle(el);"
            + "return r.width>2&&r.height>2&&s.display!=='none'&&s.visibility!=='hidden'"
            + "&&Number(s.opacity)!==0&&!el.disabled;}"
            + "function items(){return Array.prototype.slice.call(document.querySelectorAll(selector)).filter(visible);}"
            + "function focus(el){var old=document.querySelector('['+marker+'=\\\"true\\\"]');"
            + "if(old&&old!==el)old.removeAttribute(marker);el.setAttribute(marker,'true');"
            + "try{el.focus({preventScroll:true});}catch(e){el.focus();}"
            + "el.scrollIntoView({block:'nearest',inline:'nearest',behavior:'smooth'});}"
            + "function scrollParent(el){for(var p=el&&el.parentElement;p;p=p.parentElement){"
            + "var s=getComputedStyle(p),o=s.overflowY;"
            + "if((o==='auto'||o==='scroll')&&p.scrollHeight>p.clientHeight+4)return p;}"
            + "return document.scrollingElement||document.documentElement;}"
            + "function scroll(el,dir){var p=scrollParent(el),vertical=dir==='up'||dir==='down';"
            + "var amount=vertical?Math.max(140,Math.round(innerHeight*.28)):Math.max(140,Math.round(innerWidth*.20));"
            + "var x=0,y=0;if(dir==='up')y=-amount;if(dir==='down')y=amount;"
            + "if(dir==='left')x=-amount;if(dir==='right')x=amount;"
            + "if(p===document.body||p===document.documentElement||p===document.scrollingElement)"
            + "window.scrollBy({left:x,top:y,behavior:'smooth'});else p.scrollBy({left:x,top:y,behavior:'smooth'});}"
            + "window.__avaTvMove=function(dir){var all=items();if(!all.length){scroll(null,dir);return false;}"
            + "var current=document.activeElement;"
            + "if(!current||current===document.body||all.indexOf(current)<0){"
            + "var first=all.filter(function(el){var r=el.getBoundingClientRect();"
            + "return r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth;})[0]||all[0];"
            + "focus(first);return true;}"
            + "var a=current.getBoundingClientRect(),ax=a.left+a.width/2,ay=a.top+a.height/2,best=null,bestScore=1e20;"
            + "all.forEach(function(el){if(el===current)return;var r=el.getBoundingClientRect();"
            + "var x=r.left+r.width/2,y=r.top+r.height/2,dx=x-ax,dy=y-ay;"
            + "var valid=(dir==='up'&&dy<-4)||(dir==='down'&&dy>4)||(dir==='left'&&dx<-4)||(dir==='right'&&dx>4);"
            + "if(!valid)return;var vertical=dir==='up'||dir==='down';"
            + "var primary=Math.abs(vertical?dy:dx),cross=Math.abs(vertical?dx:dy);"
            + "var overlap=vertical?Math.min(a.right,r.right)-Math.max(a.left,r.left):"
            + "Math.min(a.bottom,r.bottom)-Math.max(a.top,r.top);"
            + "var score=primary*4+cross*(overlap>0?.35:2.4);"
            + "if(r.bottom<0||r.top>innerHeight||r.right<0||r.left>innerWidth)score+=500;"
            + "if(score<bestScore){bestScore=score;best=el;}});"
            + "if(best){focus(best);return true;}scroll(current,dir);return false;};"
            + "})();";

    private WebView webView;
    private FrameLayout root;
    private View cursor;
    private int cursorX = 320;
    private int cursorY = 180;
    private boolean mouseMode = true;
    private boolean centerLongPressHandled;
    private boolean modeDialogVisible;
    private long lastRemoteNavigationAt;
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
        if (mouseMode) {
            webView.evaluateJavascript(
                    "document.querySelectorAll('[data-ava-tv-focus]').forEach(function(e){"
                    + "e.removeAttribute('data-ava-tv-focus');});", null);
        } else {
            installRemoteNavigation();
        }
        if (showMessage) {
            Toast.makeText(this, mouseMode ? "Modo mouse" : "Modo controle",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void installRemoteNavigation() {
        if (webView != null) webView.evaluateJavascript(REMOTE_NAVIGATION_SETUP, null);
    }

    private void navigateRemote(int keyCode) {
        String direction;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) direction = "up";
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) direction = "down";
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) direction = "left";
        else direction = "right";
        webView.evaluateJavascript(
                REMOTE_NAVIGATION_SETUP + "window.__avaTvMove('" + direction + "');", null);
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

        if (!mouseMode) {
            boolean direction = keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (!direction) return super.dispatchKeyEvent(event);
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                long now = event.getEventTime();
                if (event.getRepeatCount() == 0
                        || now - lastRemoteNavigationAt >= REMOTE_REPEAT_DELAY_MS) {
                    lastRemoteNavigationAt = now;
                    navigateRemote(keyCode);
                }
            }
            return true;
        }

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
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (!mouseMode) installRemoteNavigation();
        }

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
