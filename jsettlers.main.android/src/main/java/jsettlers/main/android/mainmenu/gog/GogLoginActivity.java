/*
 * Copyright (c) 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package jsettlers.main.android.mainmenu.gog;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import jsettlers.common.resources.SettlersFolderChecker;
import jsettlers.gog.GogDownloader;
import jsettlers.gog.GogLogin;
import jsettlers.gog.GogModels.GogTokenResponse;
import jsettlers.main.android.R;
import jsettlers.main.android.core.resources.scanner.AndroidResourcesLoader;

/**
 * GOG web login and game data download.
 * <p>
 * If the Settlers 3 game data is not present yet, shows the official GOG login page in a full-screen
 * WebView. After the user signs in, the OAuth authorization code is captured from the redirect, exchanged
 * for an access token and used to download the game data. No password is ever typed into the app itself.
 * On success the activity finishes with {@link Activity#RESULT_OK} and the downloaded folder is stored
 * as the resources directory.
 */
public class GogLoginActivity extends AppCompatActivity {

	private static final String TAG = "JSettlersGog";
	private static final String GAME_DATA_DIR = "JSettlers";
	private static final String REDIRECT_HOST = "embed.gog.com";

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	private WebView loginWebView;
	private ViewGroup loginLayout;
	private LinearLayout progressLayout;
	private ProgressBar progressBar;
	private TextView statusText;
	private LinearLayout retryLayout;
	private boolean pendingAllFilesAccessRequest;
	private boolean loginHandled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// If the game data is already present, go straight back.
		if (new File(getGameDataPath()).isDirectory() && SettlersFolderChecker.checkSettlersFolder(getGameDataPath()).isValidSettlersFolder()) {
			setResult(RESULT_OK);
			finish();
			return;
		}

		// Writing to /storage/emulated/0/JSettlers needs all-files access on Android 11+ (scoped storage).
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
			pendingAllFilesAccessRequest = true;
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			intent.setData(Uri.parse("package:" + getPackageName()));
			startActivity(intent);
			return;
		}

		showGogLogin();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (pendingAllFilesAccessRequest) {
			pendingAllFilesAccessRequest = false;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
				showGogLogin();
			} else {
				Toast.makeText(this, R.string.gog_storage_permission_needed, Toast.LENGTH_LONG).show();
				setResult(RESULT_CANCELED);
				finish();
			}
		}
	}

	private String getGameDataPath() {
		return new File(Environment.getExternalStorageDirectory(), GAME_DATA_DIR).getAbsolutePath();
	}

	/**
	 * Shows the GOG web login in a full-screen WebView. The user logs in on GOG's official page; the
	 * OAuth authorization code is then captured from the redirect and used for the download.
	 */
	private void showGogLogin() {
		loginWebView = new WebView(this);
		WebSettings settings = loginWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true);
		loginWebView.setFocusable(true);
		loginWebView.setFocusableInTouchMode(true);

		loginLayout = new FrameLayout(this);
		loginLayout.setBackgroundColor(Color.BLACK);
		loginLayout.addView(loginWebView, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		addContentView(loginLayout, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		loginWebView.setWebViewClient(new GogWebViewClient());
		loginWebView.loadUrl(GogLogin.AUTH_URL);
		loginWebView.requestFocus();
	}

	/**
	 * Shows a full-screen "no network" overlay with a retry button.
	 */
	private void showRetryOverlay(String message) {
		if (retryLayout != null) {
			return;
		}

		retryLayout = new LinearLayout(this);
		retryLayout.setOrientation(LinearLayout.VERTICAL);
		retryLayout.setGravity(Gravity.CENTER);
		retryLayout.setBackgroundColor(Color.parseColor("#1a1a2e"));

		TextView icon = new TextView(this);
		icon.setText("⚠");
		icon.setTextSize(48);
		icon.setGravity(Gravity.CENTER);
		icon.setTextColor(Color.parseColor("#ffcc00"));
		retryLayout.addView(icon);

		TextView text = new TextView(this);
		text.setText(message);
		text.setTextSize(16);
		text.setGravity(Gravity.CENTER);
		text.setTextColor(Color.parseColor("#dddddd"));
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		textParams.topMargin = dpToPx(16);
		textParams.leftMargin = dpToPx(24);
		textParams.rightMargin = dpToPx(24);
		retryLayout.addView(text, textParams);

		Button retryButton = new Button(this);
		retryButton.setText(R.string.gog_retry);
		retryButton.setTextSize(16);
		retryButton.setOnClickListener(v -> {
			removeRetryOverlay();
			loginWebView.reload();
		});
		LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		buttonParams.topMargin = dpToPx(24);
		retryLayout.addView(retryButton, buttonParams);

		addContentView(retryLayout, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	private void removeRetryOverlay() {
		if (retryLayout == null) {
			return;
		}
		((ViewGroup) retryLayout.getParent()).removeView(retryLayout);
		retryLayout = null;
	}

	private void removeGogLoginView() {
		if (loginWebView != null) {
			loginWebView.stopLoading();
			loginWebView.destroy();
			loginWebView = null;
		}
		if (loginLayout != null) {
			((ViewGroup) loginLayout.getParent()).removeView(loginLayout);
			loginLayout = null;
		}
	}

	@Override
	public void onBackPressed() {
		if (loginLayout != null) {
			removeGogLoginView();
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		super.onBackPressed();
	}

	/**
	 * Called when the WebView login captured an OAuth authorization code from the GOG redirect.
	 * Exchanges it for an access token and starts the download.
	 */
	private void onGogWebLoginSuccess(String code) {
		if (loginHandled) {
			return;
		}
		loginHandled = true;
		removeGogLoginView();
		showProgress(getString(R.string.gog_logging_in), 0);

		executorService.execute(() -> {
			try {
				GogTokenResponse token = GogLogin.exchangeCode(code);
				mainHandler.post(() -> {
					if (token == null || token.accessToken == null || token.accessToken.isEmpty()) {
						showError(getString(R.string.gog_login_failed));
					} else {
						startGogDownload(token.accessToken);
					}
				});
			} catch (Exception ex) {
				Log.e(TAG, "Exchange code", ex);
				mainHandler.post(() -> showError(ex.getMessage()));
			}
		});
	}

	/**
	 * Downloads the Settlers 3 game data from GOG on a background thread.
	 */
	private void startGogDownload(String accessToken) {
		showProgress(getString(R.string.gog_starting_download), 0);

		executorService.execute(() -> {
			try {
				GogDownloader downloader = new GogDownloader(accessToken, new File(getGameDataPath()));
				int files = downloader.download((status, percent) -> mainHandler.post(() -> updateProgress(status, percent)));

				Log.d(TAG, "Download complete: " + files + " files.");
				PreferenceManager.getDefaultSharedPreferences(this)
						.edit()
						.putString(AndroidResourcesLoader.ORIGINAL_SETTLERS_FILES_PATH_SETTING_KEY, getGameDataPath())
						.apply();

				mainHandler.post(() -> {
					showProgress(getString(R.string.gog_download_complete, files), 100);
					setResult(RESULT_OK);
					finish();
				});
			} catch (Exception ex) {
				Log.e(TAG, "Download: " + ex);
				mainHandler.post(() -> showError(getString(R.string.gog_download_failed) + " " + ex.getMessage()));
			}
		});
	}

	/**
	 * Shows a full-screen progress overlay (spinner + status text).
	 */
	private void showProgress(String status, int percent) {
		if (progressLayout == null) {
			progressLayout = new LinearLayout(this);
			progressLayout.setOrientation(LinearLayout.VERTICAL);
			progressLayout.setGravity(Gravity.CENTER);
			progressLayout.setBackgroundColor(Color.parseColor("#1a1a2e"));

			progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			progressBar.setMax(100);
			LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			barParams.leftMargin = dpToPx(32);
			barParams.rightMargin = dpToPx(32);
			progressLayout.addView(progressBar, barParams);

			statusText = new TextView(this);
			statusText.setTextSize(14);
			statusText.setGravity(Gravity.CENTER);
			statusText.setTextColor(Color.parseColor("#dddddd"));
			LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			statusParams.topMargin = dpToPx(16);
			progressLayout.addView(statusText, statusParams);

			addContentView(progressLayout, new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}

		progressBar.setProgress(percent);
		statusText.setText(status);
	}

	private void updateProgress(String status, int percent) {
		if (progressLayout == null) {
			return;
		}
		progressBar.setProgress(percent);
		statusText.setText(status);
	}

	private void showError(String message) {
		if (progressLayout != null) {
			((ViewGroup) progressLayout.getParent()).removeView(progressLayout);
			progressLayout = null;
		}
		showGogLogin();
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	private int dpToPx(int dp) {
		return (int) (dp * getResources().getDisplayMetrics().density);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		executorService.shutdownNow();
	}

	/**
	 * WebView client for the GOG web login. After the user logs in on GOG's official page, the OAuth
	 * authorization code arrives as a "code" query parameter on the redirect URL; this client captures
	 * it and hands it back to the activity.
	 */
	private class GogWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (handleRedirect(url)) {
				return true;
			}
			return super.shouldOverrideUrlLoading(view, url);
		}

		@Override
		public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			removeRetryOverlay();
			handleRedirect(url);
		}

		@Override
		public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
			super.onReceivedError(view, request, error);
			if (request != null && request.isForMainFrame()) {
				Log.e(TAG, "WebView main frame error: " + error.getErrorCode() + " " + error.getDescription());
				showRetryOverlay(getString(R.string.gog_network_error));
			}
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			Log.e(TAG, "WebView error: " + errorCode + " " + description + " (" + failingUrl + ")");
			showRetryOverlay(getString(R.string.gog_network_error));
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
			Log.e(TAG, "WebView SSL error: " + error.getPrimaryError());
			showRetryOverlay(getString(R.string.gog_ssl_error));
		}

		/**
		 * Checks whether the given URL is the GOG redirect with an authorization code. If so, captures
		 * the code and starts the download.
		 *
		 * @return true if the redirect was handled.
		 */
		private boolean handleRedirect(String url) {
			if (url == null || !url.startsWith("https://" + REDIRECT_HOST)) {
				return false;
			}

			String code = extractQueryParam(url, "code");
			if (code == null || code.isEmpty()) {
				return false;
			}

			Log.d(TAG, "OAuth code captured.");
			mainHandler.removeCallbacksAndMessages(null);
			onGogWebLoginSuccess(code);
			return true;
		}

		private String extractQueryParam(String url, String key) {
			try {
				return Uri.parse(url).getQueryParameter(key);
			} catch (Exception ex) {
				Log.e(TAG, "Query param parse error: " + ex.getMessage());
				return null;
			}
		}
	}
}
