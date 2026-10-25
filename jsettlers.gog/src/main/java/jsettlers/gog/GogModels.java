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

import com.google.gson.annotations.SerializedName;

/**
 * JSON models for the GOG API.
 */
public class GogModels {

	/**
	 * Response of POST https://auth.gog.com/oauth/token
	 */
	public static class GogTokenResponse {
		@SerializedName("access_token")
		public String accessToken;
		@SerializedName("refresh_token")
		public String refreshToken;
		@SerializedName("expires_in")
		public long expiresIn;
		@SerializedName("token_type")
		public String tokenType;
		@SerializedName("session_id")
		public String sessionId;
		@SerializedName("error")
		public String error;
		@SerializedName("error_description")
		public String errorDescription;
	}

	/**
	 * Response of GET https://api.gog.com/users/me
	 */
	public static class GogUser {
		@SerializedName("id")
		public String id;
		@SerializedName("email")
		public String email;
		@SerializedName("username")
		public String username;
	}

	/**
	 * Response of GET https://api.gog.com/account/getFilteredProducts
	 */
	public static class GogOwnedProducts {
		@SerializedName("totalProducts")
		public int totalProducts;
		@SerializedName("page")
		public int page;
		@SerializedName("products")
		public GogProductSummary[] products;
	}

	/**
	 * A product entry in the owned products list.
	 */
	public static class GogProductSummary {
		@SerializedName("id")
		public long id;
		@SerializedName("title")
		public String title;
	}

	/**
	 * Response of GET https://api.gog.com/products/{id}?expand=downloads
	 */
	public static class GogProduct {
		@SerializedName("id")
		public long id;
		@SerializedName("title")
		public String title;
		@SerializedName("downloads")
		public GogDownload[] downloads;
	}

	/**
	 * A download entry of a product.
	 */
	public static class GogDownload {
		@SerializedName("name")
		public String name;
		@SerializedName("type")
		public String type;
		@SerializedName("os")
		public String os;
		@SerializedName("language")
		public String language;
		@SerializedName("size")
		public long size;
		@SerializedName("version")
		public String version;
		@SerializedName("downlink")
		public GogDownlink downlink;
	}

	/**
	 * The downlink path used to generate a temporary download URL.
	 */
	public static class GogDownlink {
		@SerializedName("path")
		public String path;
	}

	/**
	 * Response of GET https://api.gog.com/products/{id}/downlink
	 */
	public static class GogDownlinkResponse {
		@SerializedName("url")
		public String url;
		@SerializedName("expires_at")
		public String expiresAt;
	}
}
