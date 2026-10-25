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
package jsettlers.gog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import jsettlers.gog.GogModels.GogTokenResponse;
import jsettlers.gog.GogModels.GogUser;

/**
 * Login against the GOG API. Uses the public OAuth2 API that the GOG Galaxy client uses (also used
 * by open-source tools like lgogdownloader).
 */
public class GogLogin {

	// Public OAuth2 client credentials of the GOG Galaxy client.
	public static final String CLIENT_ID = "46899977096215655";
	public static final String CLIENT_SECRET = "9d85c43b1482497dbbce61f4378764b1";

	// Redirect target of the web login. The authorization code arrives as a "code" query parameter
	// on this URL.
	public static final String REDIRECT_URI = "https://embed.gog.com/on_login_success?origin=client";

	public static final String AUTH_URL = "https://auth.gog.com/auth?client_id=" + CLIENT_ID
			+ "&redirect_uri=" + urlEncode(REDIRECT_URI) + "&response_type=code&layout=galaxy";
	public static final String TOKEN_URL = "https://auth.gog.com/oauth/token";
	public static final String API_URL = "https://api.gog.com";

	private static final Gson GSON = new Gson();

	private GogLogin() {
	}

	/**
	 * Exchanges an authorization code (captured from the web login redirect) for an access token.
	 */
	public static GogTokenResponse exchangeCode(String code) throws IOException {
		String body = "grant_type=authorization_code"
				+ "&client_id=" + CLIENT_ID
				+ "&client_secret=" + CLIENT_SECRET
				+ "&code=" + urlEncode(code)
				+ "&redirect_uri=" + urlEncode(REDIRECT_URI);
		return postToken(body);
	}

	/**
	 * Refreshes an expired access token.
	 */
	public static GogTokenResponse refreshToken(String refreshToken) throws IOException {
		String body = "grant_type=refresh_token"
				+ "&client_id=" + CLIENT_ID
				+ "&client_secret=" + CLIENT_SECRET
				+ "&refresh_token=" + urlEncode(refreshToken);
		return postToken(body);
	}

	private static GogTokenResponse postToken(String body) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(60000);
		connection.setReadTimeout(60000);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(body.getBytes(StandardCharsets.UTF_8));
		}

		int code = connection.getResponseCode();
		InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
		String responseBody = stream != null ? new String(readAllBytes(stream), StandardCharsets.UTF_8) : "";

		GogTokenResponse response = GSON.fromJson(responseBody, GogTokenResponse.class);
		if (code >= 200 && code < 300 && response != null && response.accessToken != null && !response.accessToken.isEmpty()) {
			return response;
		}

		String message = response != null && response.errorDescription != null
				? response.errorDescription
				: (response != null && response.error != null ? response.error : "Login failed (HTTP " + code + ").");
		throw new GogException(message);
	}

	/**
	 * Returns the user data for the given access token.
	 */
	public static GogUser getUser(String accessToken) throws IOException {
		HttpURLConnection connection = openApiConnection("GET", "/users/me", accessToken);
		int code = connection.getResponseCode();
		InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
		String body = stream != null ? new String(readAllBytes(stream), StandardCharsets.UTF_8) : "";
		if (code >= 200 && code < 300) {
			return GSON.fromJson(body, GogUser.class);
		}
		throw new GogException("Could not load the GOG user data (HTTP " + code + ").");
	}

	/**
	 * Opens a connection to the GOG API with the Bearer token.
	 */
	static HttpURLConnection openApiConnection(String method, String path, String accessToken) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(API_URL + path).openConnection();
		connection.setRequestMethod(method);
		connection.setConnectTimeout(60000);
		connection.setReadTimeout(60000);
		connection.setRequestProperty("Authorization", "Bearer " + accessToken);
		connection.setRequestProperty("Accept", "application/json");
		return connection;
	}

	static String urlEncode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new GogException("UTF-8 is not supported.", ex);
		}
	}

	static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}
}
