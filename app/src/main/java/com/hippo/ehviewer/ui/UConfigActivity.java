/*
 * Copyright 2018 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

/*
 * Created by Hippo on 2018/2/9.
 */

import static com.hippo.ehviewer.EhApplication.getEhProxySelector;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import com.google.android.material.snackbar.Snackbar;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhProxySelector;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.widget.DialogWebChromeClient;
import com.hippo.widget.ProgressView;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class UConfigActivity extends ToolbarActivity {

  private static final String TAG = UConfigActivity.class.getSimpleName();

  private WebView webView;
  private ProgressView progress;
  private String url;
  private boolean loaded;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
    CookieManager cookieManager = CookieManager.getInstance();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      cookieManager.flush();
      cookieManager.removeAllCookies(null);
      cookieManager.removeSessionCookies(null);
    } else {
      CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
      cookieSyncManager.startSync();
      cookieManager.removeAllCookie();
      cookieManager.removeSessionCookie();
      cookieSyncManager.stopSync();
    }

    // Copy cookies from okhttp cookie store to CookieManager
    url = EhUrl.getUConfigUrl();
    EhCookieStore store = EhApplication.getEhCookieStore(this);
    for (Cookie cookie : store.getCookies(HttpUrl.parse(url))) {
      cookieManager.setCookie(url, cookie.toString());
    }

    setContentView(R.layout.activity_u_config);
    setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    webView = (WebView) findViewById(R.id.webview);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebViewClient(new UConfigWebViewClient());
    webView.setWebChromeClient(new DialogWebChromeClient(this));
    if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
      int type = Settings.getProxyType();
      switch (type) {
        case EhProxySelector.TYPE_HTTP:
        case EhProxySelector.TYPE_SOCKS:
          String ip_host = Settings.getProxyIp();
          int port = Settings.getProxyPort();
          setProxy((type == EhProxySelector.TYPE_HTTP ? "HTTP" : "SOCKS") + "://" + ip_host + ":" + port);
          break;
        case EhProxySelector.TYPE_SYSTEM:
          try {
            ProxySelector proxySelector = getEhProxySelector(getApplication());
            List<Proxy> proxyList = proxySelector.select(new URI(url));
            if (proxyList != null && !proxyList.isEmpty()) {
              Proxy proxy = proxyList.get(0);
              InetSocketAddress address = (InetSocketAddress) proxy.address();
              String proxyUrl = proxy.type().name() + "://" + address.getHostString() + ":" + address.getPort();
              setProxy(proxyUrl);
              break;
            }
          } catch (URISyntaxException e) {
            e.printStackTrace();
          }
        case EhProxySelector.TYPE_DIRECT:
          setDirect();
      }
    }

    webView.loadUrl(url);
    progress = (ProgressView) findViewById(R.id.progress);

    Snackbar.make(webView, R.string.apply_tip, Snackbar.LENGTH_LONG).show();
  }

  private void setDirect() {
    Log.v(TAG, "Load UConfig via Direct");
    ProxyController.getInstance().clearProxyOverride(command -> {
    }, () -> {
    });
  }

  private void setProxy(String proxyUrl) {
    Log.v(TAG, "Load UConfig via " + proxyUrl);
    ProxyConfig proxyConfig = new ProxyConfig.Builder()
            .addProxyRule(proxyUrl)
            .build();
    ProxyController.getInstance().setProxyOverride(proxyConfig, command -> {
    }, () -> {
    });
  }

  private void apply() {
    webView.loadUrl("javascript:"
        + "(function() {\n"
        + "    var apply = document.getElementById(\"apply\").children[0];\n"
        + "    apply.click();\n"
        + "})();");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.activity_u_config, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.action_apply:
        if (loaded) {
          apply();
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private Cookie longLive(Cookie cookie) {
    return new Cookie.Builder()
        .name(cookie.name())
        .value(cookie.value())
        .domain(cookie.domain())
        .path(cookie.path())
        .expiresAt(Long.MAX_VALUE)
        .build();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    webView.destroy();

    // Put cookies back to okhttp cookie store
    CookieManager cookieManager = CookieManager.getInstance();
    String cookiesString = cookieManager.getCookie(url);

    if (cookiesString != null && !cookiesString.isEmpty()) {
      EhCookieStore store = EhApplication.getEhCookieStore(this);
      HttpUrl eUrl = HttpUrl.parse(EhUrl.HOST_E);
      HttpUrl exUrl = HttpUrl.parse(EhUrl.HOST_EX);

      // The cookies saved in the uconfig page should be shared between e and ex
      for (String header : cookiesString.split(";")) {
        Cookie eCookie = Cookie.parse(eUrl, header);
        if (eCookie != null) {
          store.addCookie(longLive(eCookie));
        }

        Cookie exCookie = Cookie.parse(exUrl, header);
        if (exCookie != null) {
          store.addCookie(longLive(exCookie));
        }
      }
    }
  }

  private class UConfigWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      // Never load other urls
      // allow 302 to login, etc.
      return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      progress.setVisibility(View.VISIBLE);
      loaded = false;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      progress.setVisibility(View.GONE);
      loaded = true;
    }
  }
}
