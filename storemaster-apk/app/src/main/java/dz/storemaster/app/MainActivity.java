package dz.storemaster.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4817;
    private static final int CREATE_JSON_REQUEST = 4818;
    private WebView webView;
    private FrameLayout rootLayout;
    private View splashOverlay;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingJsonContent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17,17,17));
        getWindow().setNavigationBarColor(Color.rgb(17,17,17));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(244,244,241));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.addJavascriptInterface(new AndroidStoreMasterBridge(), "AndroidStoreMaster");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !(url.startsWith("file:///android_asset/") || url.startsWith("https://"));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "text/plain", "application/octet-stream"});
                try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
                catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "تعذر فتح مدير الملفات", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.rgb(11, 20, 42));
        rootLayout.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(rootLayout);
        webView.setInitialScale(75);
        if (state == null) webView.loadUrl("file:///android_asset/index.html");
        else webView.restoreState(state);
        if (state == null) showPersonalSplash();
    }

    private void showPersonalSplash() {
        FrameLayout splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.rgb(7, 15, 31));
        splashOverlay = splash;

        ImageView portrait = new ImageView(this);
        try {
            android.graphics.Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open("splash_photo.jpg"));
            portrait.setImageBitmap(bitmap);
        } catch (Exception ignored) {}
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
        portrait.setAlpha(0.0f);
        portrait.setScaleX(1.10f);
        portrait.setScaleY(1.10f);
        splash.addView(portrait, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB8001028, 0x44001028, 0xE6000818}));
        splash.addView(shade, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("STOREMASTER SYSTEM");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(24, 0, 0, 0xFFFFC857);
        title.setAlpha(0f);
        title.setScaleX(0.82f);
        title.setScaleY(0.82f);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        tp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        tp.leftMargin = 16; tp.rightMargin = 16; tp.topMargin = -18;
        splash.addView(title, tp);

        TextView manager = new TextView(this);
        manager.setText("Manager : BENAKRAB Ahcene");
        manager.setTextColor(0xFFFFD889);
        manager.setTextSize(18);
        manager.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        manager.setGravity(Gravity.CENTER);
        manager.setShadowLayer(20, 0, 0, 0xFF4AB7FF);
        manager.setAlpha(0f);
        manager.setScaleX(0.88f);
        manager.setScaleY(0.88f);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        mp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        mp.leftMargin = 12; mp.rightMargin = 12; mp.topMargin = 42;
        splash.addView(manager, mp);

        // Rocket and animated vapor trail.
        TextView rocket = new TextView(this);
        rocket.setText("🚀");
        rocket.setTextSize(54);
        rocket.setGravity(Gravity.CENTER);
        rocket.setAlpha(0f);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(120, 110);
        rp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        rp.bottomMargin = 24;
        splash.addView(rocket, rp);

        View vapor1 = makeVapor(0x99D7E8F5, 30);
        View vapor2 = makeVapor(0x77C4D9EA, 22);
        View vapor3 = makeVapor(0x66FFFFFF, 16);
        FrameLayout.LayoutParams vp1 = new FrameLayout.LayoutParams(30, 30);
        FrameLayout.LayoutParams vp2 = new FrameLayout.LayoutParams(22, 22);
        FrameLayout.LayoutParams vp3 = new FrameLayout.LayoutParams(16, 16);
        vp1.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; vp1.bottomMargin = 105; vp1.leftMargin = -22;
        vp2.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; vp2.bottomMargin = 92; vp2.leftMargin = 20;
        vp3.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; vp3.bottomMargin = 80; vp3.leftMargin = -5;
        vapor1.setAlpha(0f); vapor2.setAlpha(0f); vapor3.setAlpha(0f);
        splash.addView(vapor1, vp1); splash.addView(vapor2, vp2); splash.addView(vapor3, vp3);

        rootLayout.addView(splash, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        splash.bringToFront();
        splash.setElevation(1000f);

        // Cinematic entrance: slow portrait zoom, bright title and manager line.
        portrait.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(1100).setInterpolator(new DecelerateInterpolator()).start();
        title.animate().alpha(1f).scaleX(1.04f).scaleY(1.04f)
                .setStartDelay(450).setDuration(1000).setInterpolator(new DecelerateInterpolator()).start();
        manager.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(1050).setDuration(850).setInterpolator(new DecelerateInterpolator()).start();

        // Pulsing glow while the branding is visible.
        title.animate().alpha(0.72f).setStartDelay(1550).setDuration(420)
                .withEndAction(() -> title.animate().alpha(1f).setDuration(420).start()).start();
        manager.animate().alpha(0.72f).setStartDelay(1750).setDuration(420)
                .withEndAction(() -> manager.animate().alpha(1f).setDuration(420).start()).start();

        new Handler().postDelayed(() -> {
            title.animate().alpha(0f).translationY(-100f).scaleX(1.25f).scaleY(1.25f).setDuration(500).start();
            manager.animate().alpha(0f).translationY(-75f).setDuration(450).start();
            rocket.setAlpha(1f);
            vapor1.setAlpha(0.9f); vapor2.setAlpha(0.8f); vapor3.setAlpha(0.7f);
            vapor1.animate().translationY(-125f).translationX(-28f).alpha(0f).setDuration(850).start();
            vapor2.animate().translationY(-95f).translationX(25f).alpha(0f).setStartDelay(100).setDuration(800).start();
            vapor3.animate().translationY(-145f).translationX(4f).alpha(0f).setStartDelay(180).setDuration(900).start();
            rocket.animate().translationY(-1050f).scaleX(0.72f).scaleY(0.72f).alpha(0f).setDuration(1450)
                    .setInterpolator(new AccelerateInterpolator()).withEndAction(() -> {
                        if (rootLayout != null && splashOverlay != null) {
                            rootLayout.removeView(splashOverlay);
                            splashOverlay = null;
                        }
                    }).start();
        }, 3600);
    }

    private View makeVapor(int color, int size) {
        View puff = new View(this);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        puff.setBackground(shape);
        return puff;
    }

    private class AndroidStoreMasterBridge {
        @JavascriptInterface public void saveJson(String filename, String content) {
            runOnUiThread(() -> {
                pendingJsonContent = content == null ? "{}" : content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, (filename == null || filename.trim().isEmpty()) ? "storemaster-data.json" : filename);
                try { startActivityForResult(intent, CREATE_JSON_REQUEST); }
                catch (Exception e) {
                    pendingJsonContent = null;
                    Toast.makeText(MainActivity.this, "تعذر فتح نافذة حفظ الملف", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null && data.getData() != null) results = new Uri[]{data.getData()};
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        } else if (requestCode == CREATE_JSON_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingJsonContent != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new Exception("Impossible d’ouvrir le fichier");
                    out.write(pendingJsonContent.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Toast.makeText(this, "تم تصدير بيانات STOREMASTER بنجاح", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "فشل حفظ JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            pendingJsonContent = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else new AlertDialog.Builder(this).setMessage("هل تريد الخروج من STOREMASTER؟")
                .setNegativeButton("البقاء", (d,w) -> d.dismiss())
                .setPositiveButton("خروج", (d,w) -> finish()).show();
    }

    @Override protected void onDestroy() {
        if (filePathCallback != null) { filePathCallback.onReceiveValue(null); filePathCallback = null; }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
