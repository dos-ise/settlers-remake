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

/**
 * Minimal RFC 4648 base64 encoder. Used instead of {@code java.util.Base64} because the Android app
 * supports API level 19 where {@code java.util.Base64} is not available.
 */
final class Base64Util {

	private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	private Base64Util() {
	}

	static String encodeToString(byte[] data) {
		StringBuilder result = new StringBuilder(((data.length + 2) / 3) * 4);
		int i = 0;
		while (i < data.length) {
			int b0 = data[i++] & 0xFF;
			int b1 = i < data.length ? data[i++] & 0xFF : 0;
			int b2 = i < data.length ? data[i++] & 0xFF : 0;

			result.append(ALPHABET[b0 >>> 2]);
			result.append(ALPHABET[((b0 & 0x03) << 4) | (b1 >>> 4)]);
			result.append(i < data.length + 1 ? ALPHABET[((b1 & 0x0F) << 2) | (b2 >>> 6)] : '=');
			result.append(i < data.length + 2 ? ALPHABET[b2 & 0x3F] : '=');
		}
		return result.toString();
	}
}
