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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;

import jsettlers.gog.GogModels.GogDownlinkResponse;
import jsettlers.gog.GogModels.GogInstaller;
import jsettlers.gog.GogModels.GogOwnedProducts;
import jsettlers.gog.GogModels.GogProduct;
import jsettlers.gog.GogModels.GogProductSummary;

/**
 * Downloads the Settlers 3 game data from GOG. Downloads the "The Settlers 3 - Ultimate Collection"
 * installer and extracts the data directories needed by the game (GFX, SND, Map, Theme). Requires
 * the user to own the game on their GOG account.
 */
public class GogDownloader {

	// "The Settlers 3 - Ultimate Collection" on GOG.
	public static final String SETTLERS3_TITLE = "The Settlers 3 - Ultimate Collection";

	// Data directories needed by the game (relative to the install root).
	public static final String[] DATA_DIRECTORIES = { "GFX", "SND", "Map", "Theme" };

	/**
	 * Callback for download progress.
	 */
	public interface ProgressCallback {
		void onProgress(String status, int percent);
	}

	private static final Gson GSON = new Gson();

	private final String accessToken;
	private final File targetDirectory;

	public GogDownloader(String accessToken, File targetDirectory) {
		this.accessToken = accessToken;
		this.targetDirectory = targetDirectory;
	}

	/**
	 * Runs the whole download flow. Returns the number of files written.
	 */
	public int download(ProgressCallback progress) throws IOException {
		// Check ownership.
		progress.onProgress("Checking ownership...", 10);
		GogProductSummary settlers = findOwnedSettlers();
		if (settlers == null) {
			throw new GogException("You do not own " + SETTLERS3_TITLE + " on this account.");
		}

		// Get the downloads and pick the Windows installer.
		progress.onProgress("Getting download info...", 15);
		GogProduct product = getProduct(settlers.id);
		GogInstaller installer = pickInstaller(product);
		if (installer == null || installer.files == null || installer.files.length == 0) {
			throw new GogException("No Windows installer found for this game.");
		}

		// Generate a temporary download link.
		progress.onProgress("Preparing download...", 20);
		String downloadUrl = getDownlink(installer.files[0].downlink);

		// Download the installer to a temporary file.
		File installerFile = File.createTempFile("jsettlers-gog-", ".exe");
		try {
			downloadFile(downloadUrl, installerFile, progress, installer.name);

			// Extract the data directories.
			progress.onProgress("Extracting game data...", 90);
			int written = extractDataDirectories(installerFile, targetDirectory);
			if (written == 0) {
				throw new GogException("No game data files found in the installer.");
			}

			progress.onProgress("Download complete: " + written + " files.", 100);
			return written;
		} finally {
			installerFile.delete();
		}
	}

	/**
	 * Checks whether the user owns Settlers 3, by title or slug. Uses the getFilteredProducts
	 * endpoint which returns titles, so we can match by name.
	 */
	private GogProductSummary findOwnedSettlers() throws IOException {
		int page = 1;
		while (true) {
			GogOwnedProducts owned = getOwnedProducts(page);
			if (owned == null || owned.products == null || owned.products.length == 0) {
				return null;
			}
			for (GogProductSummary product : owned.products) {
				// Debug: log every owned game so we can identify the right product id.
				System.out.println("JSettlersGog owned: id=" + product.id + " title=" + product.title + " slug=" + product.slug);
				if (isSettlers3(product)) {
					return product;
				}
			}
			if (page >= owned.totalPages) {
				return null;
			}
			page++;
		}
	}

	private static boolean isSettlers3(GogProductSummary product) {
		if (product.title != null) {
			String title = product.title.toLowerCase(Locale.ROOT);
			if (title.contains("settlers 3") || title.contains("siedler 3")) {
				return true;
			}
		}
		if (product.slug != null) {
			String slug = product.slug.toLowerCase(Locale.ROOT);
			if (slug.contains("settlers_3") || slug.contains("siedler_3")) {
				return true;
			}
		}
		return false;
	}

	private GogOwnedProducts getOwnedProducts(int page) throws IOException {
		HttpURLConnection connection = GogLogin.openConnection("GET",
				GogLogin.EMBED_URL + "/account/getFilteredProducts?filter=owned&mediaType=1&page=" + page
						+ "&productsPerPage=100",
				accessToken);
		return getJson(connection, GogOwnedProducts.class, "Could not load the GOG library");
	}

	private GogProduct getProduct(long productId) throws IOException {
		HttpURLConnection connection = GogLogin.openApiConnection("GET",
				"/products/" + productId + "?expand=downloads", accessToken);
		return getJson(connection, GogProduct.class, "Could not load the game details");
	}

	private String getDownlink(String downlinkUrl) throws IOException {
		if (downlinkUrl == null || downlinkUrl.isEmpty()) {
			throw new GogException("No download link available.");
		}
		if (!downlinkUrl.startsWith("http")) {
			downlinkUrl = GogLogin.API_URL + downlinkUrl;
		}

		HttpURLConnection connection = GogLogin.openConnection("GET", downlinkUrl, accessToken);
		GogDownlinkResponse response = getJson(connection, GogDownlinkResponse.class, "Could not generate a download link");
		if (response == null) {
			throw new GogException("No download link received.");
		}
		String url = response.downlink != null ? response.downlink : response.url;
		if (url == null || url.isEmpty()) {
			throw new GogException("No download link received.");
		}
		return url;
	}

	private static <T> T getJson(HttpURLConnection connection, Class<T> type, String errorPrefix) throws IOException {
		int code = connection.getResponseCode();
		InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
		String body = stream != null ? new String(GogLogin.readAllBytes(stream), StandardCharsets.UTF_8) : "";
		if (code >= 200 && code < 300) {
			try {
				return GSON.fromJson(body, type);
			} catch (com.google.gson.JsonSyntaxException ex) {
				throw new GogException(errorPrefix + ": " + ex.getMessage() + " Body: " + body, ex);
			}
		}
		throw new GogException(errorPrefix + " (HTTP " + code + "): " + body);
	}

	/**
	 * Picks the Windows installer, preferring the device language.
	 */
	private static GogInstaller pickInstaller(GogProduct product) {
		if (product == null || product.downloads == null || product.downloads.installers == null) {
			return null;
		}

		String deviceLanguage = Locale.getDefault().getLanguage();
		GogInstaller fallback = null;
		for (GogInstaller installer : product.downloads.installers) {
			if (!"windows".equals(installer.os) || installer.files == null || installer.files.length == 0) {
				continue;
			}
			if (fallback == null) {
				fallback = installer;
			}
			if (installer.language != null && installer.language.equalsIgnoreCase(deviceLanguage)) {
				return installer;
			}
		}
		return fallback;
	}

	/**
	 * Downloads a file from the given URL to the target file, reporting progress.
	 */
	private static void downloadFile(String url, File target, ProgressCallback progress, String name) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setConnectTimeout(60000);
		connection.setReadTimeout(60000);
		connection.setInstanceFollowRedirects(true);

		int code = connection.getResponseCode();
		if (code < 200 || code >= 300) {
			throw new GogException("Download failed (HTTP " + code + ").");
		}

		long total = connection.getContentLength();
		try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(target)) {
			byte[] buffer = new byte[8192];
			long downloaded = 0;
			int n;
			while ((n = in.read(buffer)) != -1) {
				out.write(buffer, 0, n);
				downloaded += n;
				if (total > 0) {
					int percent = 20 + (int) (70.0 * downloaded / total);
					progress.onProgress("Downloading " + name + "...", percent);
				}
			}
		}
	}

	/**
	 * Extracts the data directories (GFX, SND, Map, Theme) from the installer into the target
	 * directory. GOG installers are zip archives with a self-extracting stub; {@link ZipFile} reads
	 * them by scanning for the end-of-central-directory record. Returns the number of files written.
	 */
	static int extractDataDirectories(File installer, File targetDirectory) throws IOException {
		int written = 0;
		try (ZipFile zip = new ZipFile(installer)) {
			for (String dataDir : DATA_DIRECTORIES) {
				String basePath = findBasePath(zip, dataDir);
				if (basePath == null) {
					continue;
				}

				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String path = entry.getName().replace('\\', '/');
					if (!path.toLowerCase(Locale.ROOT).startsWith(basePath.toLowerCase(Locale.ROOT))) {
						continue;
					}

					String relative = path.substring(basePath.length());
					File target = new File(targetDirectory, dataDir + File.separatorChar + relative.replace('/', File.separatorChar));
					if (entry.isDirectory()) {
						target.mkdirs();
					} else {
						target.getParentFile().mkdirs();
						try (InputStream in = zip.getInputStream(entry); OutputStream out = new FileOutputStream(target)) {
							byte[] buffer = new byte[8192];
							int n;
							while ((n = in.read(buffer)) != -1) {
								out.write(buffer, 0, n);
							}
						}
						written++;
					}
				}
			}
		}
		return written;
	}

	/**
	 * Finds the path prefix of the given data directory inside the installer (e.g. "GFX" or
	 * "app/GFX"), or null if the directory is not present.
	 */
	private static String findBasePath(ZipFile zip, String dataDir) throws IOException {
		String dataDirLower = dataDir.toLowerCase(Locale.ROOT);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			String path = entry.getName().replace('\\', '/');
			String lower = path.toLowerCase(Locale.ROOT);
			int index = findSegment(lower, dataDirLower);
			if (index >= 0) {
				return path.substring(0, index + dataDir.length());
			}
		}
		return null;
	}

	/**
	 * Returns the index of the given path segment in the path, or -1 if not present.
	 */
	private static int findSegment(String path, String segment) {
		String[] parts = path.split("/");
		int offset = 0;
		for (String part : parts) {
			if (part.equals(segment)) {
				return offset;
			}
			offset += part.length() + 1;
		}
		return -1;
	}
}
