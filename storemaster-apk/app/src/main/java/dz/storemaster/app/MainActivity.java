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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4817;
    private static final int CREATE_JSON_REQUEST = 4818;
    private WebView webView;
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
        setContentView(webView);
        webView.setInitialScale(75);
        if (state == null) webView.loadUrl("file:///android_asset/index.html");
        else webView.restoreState(state);
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
