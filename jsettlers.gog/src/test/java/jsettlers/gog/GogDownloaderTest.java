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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.Gson;

import jsettlers.gog.GogModels.GogDownlinkResponse;
import jsettlers.gog.GogModels.GogOwnedProducts;
import jsettlers.gog.GogModels.GogProduct;
import jsettlers.gog.GogModels.GogTokenResponse;
import jsettlers.gog.GogModels.GogUser;

/**
 * Tests for the GOG API models and the installer extraction.
 */
public class GogDownloaderTest {

	private static final Gson GSON = new Gson();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void parsesTokenResponse() {
		String json = "{\"access_token\":\"abc123\",\"refresh_token\":\"def456\",\"expires_in\":3600,"
				+ "\"token_type\":\"Bearer\",\"session_id\":\"sess1\"}";
		GogTokenResponse response = GSON.fromJson(json, GogTokenResponse.class);
		assertEquals("abc123", response.accessToken);
		assertEquals("def456", response.refreshToken);
		assertEquals(3600, response.expiresIn);
		assertEquals("Bearer", response.tokenType);
	}

	@Test
	public void parsesTokenError() {
		String json = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid credentials\"}";
		GogTokenResponse response = GSON.fromJson(json, GogTokenResponse.class);
		assertEquals("invalid_grant", response.error);
		assertEquals("Invalid credentials", response.errorDescription);
	}

	@Test
	public void parsesUser() {
		String json = "{\"id\":\"123456\",\"email\":\"user@example.com\",\"username\":\"player1\"}";
		GogUser user = GSON.fromJson(json, GogUser.class);
		assertEquals("123456", user.id);
		assertEquals("player1", user.username);
	}

	@Test
	public void parsesOwnedProducts() {
		String json = "{\"totalProducts\":2,\"page\":1,\"products\":["
				+ "{\"id\":1207659160,\"title\":\"The Settlers 3 - Ultimate Collection\"},"
				+ "{\"id\":123,\"title\":\"Some Other Game\"}]}";
		GogOwnedProducts products = GSON.fromJson(json, GogOwnedProducts.class);
		assertEquals(2, products.totalProducts);
		assertEquals(2, products.products.length);
		assertEquals(1207659160L, products.products[0].id);
		assertEquals("The Settlers 3 - Ultimate Collection", products.products[0].title);
	}

	@Test
	public void parsesProductDownloads() {
		String json = "{\"id\":1207659160,\"title\":\"The Settlers 3 - Ultimate Collection\",\"downloads\":["
				+ "{\"name\":\"setup.exe\",\"type\":\"installer\",\"os\":\"windows\",\"language\":\"en\","
				+ "\"size\":123456789,\"version\":\"2.0\",\"downlink\":{\"path\":\"/downlink/1207659160/setup.exe\"}},"
				+ "{\"name\":\"bonus.zip\",\"type\":\"bonus_content\",\"os\":\"windows\",\"language\":\"en\","
				+ "\"downlink\":{\"path\":\"/downlink/1207659160/bonus.zip\"}}]}";
		GogProduct product = GSON.fromJson(json, GogProduct.class);
		assertEquals(1207659160L, product.id);
		assertEquals(2, product.downloads.length);
		assertEquals("installer", product.downloads[0].type);
		assertEquals("windows", product.downloads[0].os);
		assertEquals("/downlink/1207659160/setup.exe", product.downloads[0].downlink.path);
	}

	@Test
	public void parsesDownlinkResponse() {
		String json = "{\"url\":\"https://download.gog.com/example\",\"expires_at\":\"2026-01-01T00:00:00Z\"}";
		GogDownlinkResponse response = GSON.fromJson(json, GogDownlinkResponse.class);
		assertEquals("https://download.gog.com/example", response.url);
	}

	@Test
	public void extractsDataDirectoriesFromStubbedInstaller() throws IOException {
		File installer = createStubbedInstaller();
		File target = temporaryFolder.newFolder("target");

		int written = GogDownloader.extractDataDirectories(installer, target);

		assertEquals(4, written);
		assertTrue(new File(target, "GFX/foo.bmp").isFile());
		assertTrue(new File(target, "SND/bar.wav").isFile());
		assertTrue(new File(target, "Map/baz.map").isFile());
		assertTrue(new File(target, "Theme/theme.txt").isFile());
		assertTrue(!new File(target, "README.txt").exists());
	}

	@Test
	public void extractsDataDirectoriesFromNestedInstaller() throws IOException {
		File installer = createNestedInstaller();
		File target = temporaryFolder.newFolder("target");

		int written = GogDownloader.extractDataDirectories(installer, target);

		assertEquals(2, written);
		assertTrue(new File(target, "GFX/foo.bmp").isFile());
		assertTrue(new File(target, "SND/bar.wav").isFile());
	}

	/**
	 * Creates a fake GOG installer: a zip with a self-extracting stub prepended, data directories at
	 * the root.
	 */
	private File createStubbedInstaller() throws IOException {
		File installer = temporaryFolder.newFile("setup.exe");
		try (FileOutputStream out = new FileOutputStream(installer)) {
			// Self-extracting stub (like a GOG installer).
			out.write("MZ fake stub".getBytes(StandardCharsets.UTF_8));
			try (ZipOutputStream zip = new ZipOutputStream(out)) {
				writeEntry(zip, "GFX/foo.bmp", "gfx-data");
				writeEntry(zip, "SND/bar.wav", "snd-data");
				writeEntry(zip, "Map/baz.map", "map-data");
				writeEntry(zip, "Theme/theme.txt", "theme-data");
				writeEntry(zip, "README.txt", "readme");
			}
		}
		return installer;
	}

	/**
	 * Creates a fake GOG installer with the data directories nested under an "app" folder.
	 */
	private File createNestedInstaller() throws IOException {
		File installer = temporaryFolder.newFile("setup2.exe");
		try (FileOutputStream out = new FileOutputStream(installer)) {
			out.write("MZ fake stub".getBytes(StandardCharsets.UTF_8));
			try (ZipOutputStream zip = new ZipOutputStream(out)) {
				writeEntry(zip, "app/GFX/foo.bmp", "gfx-data");
				writeEntry(zip, "app/SND/bar.wav", "snd-data");
				writeEntry(zip, "app/README.txt", "readme");
			}
		}
		return installer;
	}

	private static void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}
}
