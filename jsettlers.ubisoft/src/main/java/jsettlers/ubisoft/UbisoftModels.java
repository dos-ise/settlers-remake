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

import com.google.gson.annotations.SerializedName;

/**
 * JSON models for the Ubisoft Connect API.
 */
public class UbisoftModels {

	/**
	 * Response of POST https://public-ubiservices.ubi.com/v3/profiles/sessions
	 */
	public static class UbisoftLoginResponse {
		@SerializedName("ticket")
		public String ticket;
		@SerializedName("sessionId")
		public String sessionId;
		@SerializedName("profileId")
		public String profileId;
		@SerializedName("userId")
		public String userId;
		@SerializedName("nameOnPlatform")
		public String nameOnPlatform;
		@SerializedName("rememberMeTicket")
		public String rememberMeTicket;
		@SerializedName("twoFactorAuthenticationTicket")
		public String twoFactorAuthenticationTicket;
		@SerializedName("environment")
		public String environment;
		@SerializedName("expiration")
		public String expiration;
		@SerializedName("spaceId")
		public String spaceId;
		@SerializedName("clientIp")
		public String clientIp;
		@SerializedName("serverTime")
		public String serverTime;
		@SerializedName("message")
		public String message;
		@SerializedName("httpCode")
		public Integer httpCode;

		/**
		 * @return true if the account requires two-factor authentication.
		 */
		public boolean requiresTwoFactorAuthentication() {
			return twoFactorAuthenticationTicket != null && !twoFactorAuthenticationTicket.isEmpty();
		}
	}

	/**
	 * Result of a login attempt.
	 */
	public static class UbisoftLoginResult {
		public final boolean success;
		public final boolean requiresTwoFactorAuthentication;
		public final String errorMessage;
		public final UbisoftLoginResponse response;

		public UbisoftLoginResult(boolean success, boolean requiresTwoFactorAuthentication, String errorMessage, UbisoftLoginResponse response) {
			this.success = success;
			this.requiresTwoFactorAuthentication = requiresTwoFactorAuthentication;
			this.errorMessage = errorMessage;
			this.response = response;
		}
	}
}
