/*
 * Copyright (c) 2024
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
package jsettlers.main.android.mainmenu.ubisoft;

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
import android.webkit.ValueCallback;
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
import jsettlers.main.android.R;
import jsettlers.main.android.core.resources.scanner.AndroidResourcesLoader;
import jsettlers.ubisoft.Settlers3Downloader;
import jsettlers.ubisoft.UbisoftLogin;
import jsettlers.ubisoft.UbisoftModels.UbisoftLoginResponse;
import jsettlers.ubisoft.UbisoftModels.UbisoftLoginResult;

/**
 * Ubisoft Connect web login and game data download.
 * <p>
 * If the Settlers 3 game data is not present yet, shows the official Ubisoft Connect login page in a
 * full-screen WebView. After the user signs in, the session (ticket + session id) is read from the
 * page's localStorage, renewed under the app id and used to download the game data. No password is
 * ever typed into the app itself. On success the activity finishes with {@link Activity#RESULT_OK}
 * and the downloaded folder is stored as the resources directory.
 */
public class UbisoftLoginActivity extends AppCompatActivity {

	private static final String TAG = "JSettlersUbisoft";
	private static final String GAME_DATA_DIR = "JSettlers";

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	private WebView loginWebView;
	private ViewGroup loginLayout;
	private LinearLayout progressLayout;
	private ProgressBar progressBar;
	private TextView statusText;
	private LinearLayout retryLayout;
	private boolean pendingAllFilesAccessRequest;

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

		showUbisoftLogin();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (pendingAllFilesAccessRequest) {
			pendingAllFilesAccessRequest = false;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
				showUbisoftLogin();
			} else {
				Toast.makeText(this, R.string.ubisoft_storage_permission_needed, Toast.LENGTH_LONG).show();
				setResult(RESULT_CANCELED);
				finish();
			}
		}
	}

	private String getGameDataPath() {
		return new File(Environment.getExternalStorageDirectory(), GAME_DATA_DIR).getAbsolutePath();
	}

	/**
	 * Shows the Ubisoft Connect web login in a full-screen WebView. The user logs in on Ubisoft's
	 * official page; the session is then read from the page's localStorage and used for the download.
	 */
	private void showUbisoftLogin() {
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

		loginWebView.setWebViewClient(new UbisoftWebViewClient());
		loginWebView.loadUrl(UbisoftLogin.LOGIN_URL);
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
		retryButton.setText(R.string.ubisoft_retry);
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

	private void removeUbisoftLoginView() {
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
			removeUbisoftLoginView();
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		super.onBackPressed();
	}

	/**
	 * Called when the WebView login captured a session (ticket + session id) from the Ubisoft page.
	 * Renews the session under the app id and starts the download.
	 */
	private void onUbisoftWebLoginSuccess(String ticket, String sessionId) {
		removeUbisoftLoginView();
		showProgress(getString(R.string.ubisoft_logging_in), 0);

		executorService.execute(() -> {
			try {
				UbisoftLoginResult loginResult = UbisoftLogin.renewSession(ticket, sessionId);
				mainHandler.post(() -> {
					if (!loginResult.success) {
						showError(loginResult.errorMessage != null ? loginResult.errorMessage : getString(R.string.ubisoft_login_failed));
					} else {
						startUbisoftDownload(loginResult.response);
					}
				});
			} catch (Exception ex) {
				Log.e(TAG, "Renew session: " + ex);
				mainHandler.post(() -> showError(ex.getMessage()));
			}
		});
	}

	/**
	 * Downloads the Settlers 3 game data from Ubisoft Connect on a background thread.
	 */
	private void startUbisoftDownload(UbisoftLoginResponse login) {
		showProgress(getString(R.string.ubisoft_starting_download), 0);

		executorService.execute(() -> {
			try {
				Settlers3Downloader downloader = new Settlers3Downloader(login, new File(getGameDataPath()));
				int files = downloader.download((status, percent) -> mainHandler.post(() -> updateProgress(status, percent)));

				Log.d(TAG, "Download complete: " + files + " files.");
				PreferenceManager.getDefaultSharedPreferences(this)
						.edit()
						.putString(AndroidResourcesLoader.ORIGINAL_SETTLERS_FILES_PATH_SETTING_KEY, getGameDataPath())
						.apply();

				mainHandler.post(() -> {
					showProgress(getString(R.string.ubisoft_download_complete, files), 100);
					setResult(RESULT_OK);
					finish();
				});
			} catch (Exception ex) {
				Log.e(TAG, "Download: " + ex);
				mainHandler.post(() -> showError(getString(R.string.ubisoft_download_failed) + " " + ex.getMessage()));
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
		showUbisoftLogin();
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	private int dpToPx(int dp) {
		return (int) (dp * getResources().getDisplayMetrics().density);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mainHandler.removeCallbacksAndMessages(null);
		executorService.shutdownNow();
	}

	/**
	 * WebView client for the Ubisoft Connect web login. After the user logs in on Ubisoft's official
	 * page, the session (ticket + session id) is stored in the page's localStorage; this client polls
	 * for it and hands it back to the activity.
	 */
	private class UbisoftWebViewClient extends WebViewClient {
		private boolean checking;

		@Override
		public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			removeRetryOverlay();
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			checkForLogin();
		}

		@Override
		public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
			super.onReceivedError(view, request, error);
			if (request != null && request.isForMainFrame()) {
				Log.e(TAG, "WebView main frame error: " + error.getErrorCode() + " " + error.getDescription());
				showRetryOverlay(getString(R.string.ubisoft_network_error));
			}
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			Log.e(TAG, "WebView error: " + errorCode + " " + description + " (" + failingUrl + ")");
			showRetryOverlay(getString(R.string.ubisoft_network_error));
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
			Log.e(TAG, "WebView SSL error: " + error.getPrimaryError());
			showRetryOverlay(getString(R.string.ubisoft_ssl_error));
		}

		private void checkForLogin() {
			if (checking || loginWebView == null) {
				return;
			}
			checking = true;

			loginWebView.evaluateJavascript(CHECK_LOGIN_JS, new ValueCallback<String>() {
				@Override
				public void onReceiveValue(String result) {
					checking = false;
					if (loginWebView == null) {
						// The activity was closed while the check was running.
						return;
					}
					Log.d(TAG, "Web login check result: " + result);

					if (result != null && !result.isEmpty() && !"null".equals(result)) {
						String json = result;
						if (json.startsWith("\"") && json.endsWith("\"")) {
							json = json.substring(1, json.length() - 1).replace("\\\"", "\"");
						}

						UbisoftWebSession session = parseSession(json);
						if (session != null && session.ticket != null && !session.ticket.isEmpty()
								&& session.sessionId != null && !session.sessionId.isEmpty()) {
							Log.d(TAG, "Web login session captured.");
							mainHandler.removeCallbacksAndMessages(null);
							onUbisoftWebLoginSuccess(session.ticket, session.sessionId);
							return;
						}
					}

					mainHandler.postDelayed(UbisoftWebViewClient.this::checkForLogin, 500);
				}
			});
		}

		private UbisoftWebSession parseSession(String json) {
			try {
				// The JS returns lowercase property names; parse them manually to avoid a JSON
				// dependency in this activity.
				UbisoftWebSession session = new UbisoftWebSession();
				session.ticket = extractJsonString(json, "ticket");
				session.sessionId = extractJsonString(json, "sessionId");
				return session;
			} catch (Exception ex) {
				Log.e(TAG, "Web login parse error: " + ex.getMessage());
				return null;
			}
		}

		private String extractJsonString(String json, String key) {
			String pattern = "\"" + key + "\":\"";
			int start = json.indexOf(pattern);
			if (start < 0) {
				return null;
			}
			start += pattern.length();
			int end = json.indexOf('"', start);
			if (end < 0) {
				return null;
			}
			return json.substring(start, end);
		}
	}

	/**
	 * Session data captured from the Ubisoft Connect web login.
	 */
	private static class UbisoftWebSession {
		String ticket;
		String sessionId;
	}

	// Reads the Ubisoft Connect session from the page's localStorage. The session is stored under
	// "PRODloginData" after a successful login; other keys are scanned as a fallback.
	private static final String CHECK_LOGIN_JS = "(function() {"
			+ "function findSession() {"
			+ "var loginData = window.localStorage.getItem('PRODloginData');"
			+ "if (loginData) {"
			+ "try { var s = JSON.parse(loginData); if (s.ticket && s.sessionId) return s; } catch (e) {}"
			+ "}"
			+ "for (var i = 0; i < window.localStorage.length; i++) {"
			+ "var key = window.localStorage.key(i);"
			+ "try {"
			+ "var val = JSON.parse(window.localStorage.getItem(key));"
			+ "if (val && val.ticket && val.sessionId) return val;"
			+ "} catch (e) {}"
			+ "}"
			+ "return null;"
			+ "}"
			+ "var s = findSession();"
			+ "if (s) { return {ticket: s.ticket, sessionId: s.sessionId}; }"
			+ "return null;"
			+ "})();";
}
