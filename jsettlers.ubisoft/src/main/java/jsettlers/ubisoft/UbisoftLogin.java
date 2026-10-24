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
package jsettlers.ubisoft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import jsettlers.ubisoft.UbisoftModels.UbisoftLoginResponse;
import jsettlers.ubisoft.UbisoftModels.UbisoftLoginResult;

/**
 * Login against the Ubisoft Connect API. Uses the (unofficial) public Ubisoft services API that the
 * Ubisoft Connect client itself uses.
 */
public class UbisoftLogin {

	// App id used by the Ubisoft Connect downloader tooling (UplayDB). Required by the API for every request.
	public static final String APP_ID = "f68a4bb5-608a-4ff2-8123-be8ef797e0a6";
	public static final String USER_AGENT = "Massgate";
	public static final String SESSION_URL = "https://public-ubiservices.ubi.com/v3/profiles/sessions";

	// Web login (used by the Ubisoft Connect web app). After a successful login the session
	// (ticket + sessionId) is stored in the browser's localStorage under "PRODloginData" on the
	// connect.ubisoft.com domain.
	public static final String GENOME_ID = "954e66a0-be1b-4aa0-9690-fb75201e4e9e";
	public static final String LOGIN_URL = "https://connect.ubisoft.com/login?appId=" + APP_ID
			+ "&genomeId=" + GENOME_ID + "&lang=en-US&nextUrl=https:%2F%2Fconnect.ubisoft.com%2F";

	private static final Gson GSON = new Gson();

	private UbisoftLogin() {
	}

	/**
	 * First login step with email and password. If the account has two factor authentication enabled,
	 * the result contains a two factor authentication ticket and {@link UbisoftLoginResult#requiresTwoFactorAuthentication}
	 * is set; the caller must then call {@link #loginWithTwoFactorCode(String, String)}.
	 */
	public static UbisoftLoginResult login(String email, String password) throws IOException {
		if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
			return new UbisoftLoginResult(false, false, "Please enter your email and password.", null);
		}

		String basic = Base64Util.encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
		return postSession("Basic " + basic, null, null);
	}

	/**
	 * Second login step for accounts with two factor authentication.
	 */
	public static UbisoftLoginResult loginWithTwoFactorCode(String twoFactorTicket, String code) throws IOException {
		if (twoFactorTicket == null || twoFactorTicket.isEmpty() || code == null || code.isEmpty()) {
			return new UbisoftLoginResult(false, false, "Please enter your two-factor authentication code.", null);
		}

		return postSession("ubi_2fa_v1 t=" + twoFactorTicket, code, null);
	}

	/**
	 * Login with a previously stored remember-me ticket (no password needed).
	 */
	public static UbisoftLoginResult loginWithRememberMeTicket(String rememberMeTicket) throws IOException {
		if (rememberMeTicket == null || rememberMeTicket.isEmpty()) {
			return new UbisoftLoginResult(false, false, "No saved login found.", null);
		}

		return postSession("rm_v1 t=" + rememberMeTicket, null, null);
	}

	/**
	 * Renews a session captured from the web login (browser localStorage) under this app id. The browser
	 * ticket may be scoped to a different app id; renewing it makes it usable for the download API.
	 */
	public static UbisoftLoginResult renewSession(String ticket, String sessionId) throws IOException {
		if (ticket == null || ticket.isEmpty()) {
			return new UbisoftLoginResult(false, false, "No valid login found.", null);
		}

		HttpURLConnection connection = openConnection("PUT", SESSION_URL);
		connection.setRequestProperty("Authorization", "Ubi_v1 t=" + ticket);
		if (sessionId != null && !sessionId.isEmpty()) {
			connection.setRequestProperty("Ubi-SessionId", sessionId);
		}
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write("{}".getBytes(StandardCharsets.UTF_8));
		}
		return parseResponse(connection);
	}

	private static UbisoftLoginResult postSession(String authorization, String twoFactorCode, String rememberDeviceTicket) throws IOException {
		HttpURLConnection connection = openConnection("POST", SESSION_URL);
		// The Ubisoft API uses non-standard auth schemes (e.g. "ubi_2fa_v1 t=...") that
		// HttpURLConnection would reject if set as the standard Authorization header.
		connection.setRequestProperty("Authorization", authorization);
		if (twoFactorCode != null && !twoFactorCode.isEmpty()) {
			connection.setRequestProperty("Ubi-2FACode", twoFactorCode);
		}
		if (rememberDeviceTicket != null && !rememberDeviceTicket.isEmpty()) {
			connection.setRequestProperty("Ubi-RememberDeviceTicket", rememberDeviceTicket);
		}
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write("{\"rememberMe\": true}".getBytes(StandardCharsets.UTF_8));
		}
		return parseResponse(connection);
	}

	private static HttpURLConnection openConnection(String method, String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod(method);
		connection.setConnectTimeout(60000);
		connection.setReadTimeout(60000);
		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setRequestProperty("Ubi-AppId", APP_ID);
		connection.setRequestProperty("Ubi-RequestedPlatformType", "uplay");
		connection.setRequestProperty("Content-Type", "application/json");
		return connection;
	}

	private static UbisoftLoginResult parseResponse(HttpURLConnection connection) throws IOException {
		int code = connection.getResponseCode();
		InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
		String body = stream != null ? new String(readAllBytes(stream), StandardCharsets.UTF_8) : "";

		UbisoftLoginResponse response = null;
		try {
			response = GSON.fromJson(body, UbisoftLoginResponse.class);
		} catch (Exception ignored) {
			// Non-JSON error body; handled below.
		}

		if (code >= 200 && code < 300 && response != null && response.ticket != null && !response.ticket.isEmpty()) {
			return new UbisoftLoginResult(true, false, null, response);
		}

		if (response != null && response.requiresTwoFactorAuthentication()) {
			return new UbisoftLoginResult(false, true, null, response);
		}

		String message = response != null && response.message != null ? response.message : "Login failed (HTTP " + code + ").";
		return new UbisoftLoginResult(false, false, message, response);
	}

	private static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}
}
