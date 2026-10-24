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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;

import com.github.luben.zstd.Zstd;

import jsettlers.ubisoft.UbisoftModels.UbisoftLoginResponse;
import mg.protocol.download.Download;
import mg.protocol.ownership.Ownership;

/**
 * Downloads the Settlers 3 game data from Ubisoft Connect. Downloads the data directories needed by
 * the game (GFX, Map, SND, Theme) - not the whole game. Requires the user to own "The Settlers 3 -
 * History Edition" (product id 11784) on their Ubisoft Connect account.
 */
public class Settlers3Downloader {

	// "The Settlers 3 - History Edition" on Ubisoft Connect.
	public static final int SETTLERS3_HISTORY_EDITION_PRODUCT_ID = 11784;

	// Data directories needed by the game (relative to the install root).
	public static final String[] DATA_DIRECTORIES = { "GFX", "Map", "SND", "Theme" };

	/**
	 * Callback for download progress.
	 */
	public interface ProgressCallback {
		void onProgress(String status, int percent);
	}

	private final UbisoftLoginResponse login;
	private final File targetDirectory;

	public Settlers3Downloader(UbisoftLoginResponse login, File targetDirectory) {
		this.login = login;
		this.targetDirectory = targetDirectory;
	}

	/**
	 * Runs the whole download flow. Returns the number of files written.
	 */
	public int download(ProgressCallback progress) throws IOException {
		progress.onProgress("Connecting...", 5);
		try (DemuxClient demux = new DemuxClient()) {
			demux.connect();
			boolean authenticated = demux.authenticate(login.ticket);
			if (!authenticated) {
				throw new UbisoftException("Authentication failed.");
			}

			// Check ownership and get the ownership token.
			progress.onProgress("Checking ownership...", 10);
			OwnershipService ownership = new OwnershipService(demux);
			ownership.connect();
			List<Ownership.OwnedGame> ownedGames = ownership.getOwnedGames(login.ticket, login.sessionId);

			Ownership.OwnedGame settlers = null;
			for (Ownership.OwnedGame game : ownedGames) {
				if (game.getProductId() == SETTLERS3_HISTORY_EDITION_PRODUCT_ID) {
					settlers = game;
					break;
				}
			}

			if (settlers == null) {
				throw new UbisoftException("You do not own The Settlers 3 - History Edition on this account.");
			}

			if (!settlers.hasLatestManifest() || settlers.getLatestManifest().isEmpty()) {
				throw new UbisoftException("No manifest available for this game.");
			}

			OwnershipService.OwnershipToken ownershipToken = ownership.getOwnershipToken(
					SETTLERS3_HISTORY_EDITION_PRODUCT_ID, login.ticket, login.sessionId);

			// Initialize the download service.
			progress.onProgress("Initializing download...", 15);
			DownloadService download = new DownloadService(demux);
			download.connect();
			boolean initialized = download.initialize(ownershipToken.token);
			if (!initialized) {
				throw new UbisoftException("Download initialization failed.");
			}

			// Download and parse the manifest.
			progress.onProgress("Downloading manifest...", 20);
			byte[] manifestBytes = downloadManifest(download, settlers.getLatestManifest());
			Download.Manifest manifest = ManifestParser.parse(manifestBytes);

			// Find the data files.
			List<ManifestParser.ManifestFile> files = ManifestParser.findFilesByDirectory(manifest, DATA_DIRECTORIES);
			if (files.isEmpty()) {
				throw new UbisoftException("No game data files found in the manifest.");
			}

			// Download all files.
			int totalSlices = 0;
			for (ManifestParser.ManifestFile file : files) {
				totalSlices += file.slices.size();
			}

			int written = 0;
			int sliceIndex = 0;
			for (ManifestParser.ManifestFile file : files) {
				// Manifest paths may use "/" or "\" as separators.
				String relative = file.name.replace('\\', '/');
				File targetPath = new File(targetDirectory, relative.replace('/', File.separatorChar));
				File parent = targetPath.getParentFile();
				if (parent != null) {
					parent.mkdirs();
				}

				List<String> slicePaths = ManifestParser.getSlicePaths(manifest, file.slices);
				List<List<String>> urls = download.getUrls(SETTLERS3_HISTORY_EDITION_PRODUCT_ID, slicePaths);
				if (urls.size() != file.slices.size()) {
					throw new UbisoftException("Download URL count mismatch.");
				}

				try (OutputStream output = new FileOutputStream(targetPath)) {
					for (int i = 0; i < file.slices.size(); i++) {
						int percent = 20 + (int) (80.0 * sliceIndex / totalSlices);
						progress.onProgress("Downloading " + file.name + " (" + (i + 1) + "/" + file.slices.size() + ")...", percent);

						byte[] sliceBytes = downloadSlice(urls.get(i));
						byte[] decompressed = decompressSlice(manifest, sliceBytes, file.slices.get(i).getSize());
						output.write(decompressed);
						sliceIndex++;
					}
				}

				written++;
			}

			progress.onProgress("Download complete: " + written + " files.", 100);
			return written;
		}
	}

	private byte[] downloadManifest(DownloadService download, String manifestId) throws IOException {
		List<List<String>> urls = download.getUrls(SETTLERS3_HISTORY_EDITION_PRODUCT_ID,
				Collections.singletonList("manifests/" + manifestId + ".manifest"));
		if (urls.isEmpty() || urls.get(0).isEmpty()) {
			throw new UbisoftException("No manifest download URL.");
		}

		return downloadFromUrls(urls.get(0));
	}

	private byte[] downloadSlice(List<String> urls) throws IOException {
		return downloadFromUrls(urls);
	}

	/**
	 * Downloads from the first URL that works (the server provides multiple mirror URLs per file).
	 */
	private byte[] downloadFromUrls(List<String> urls) throws IOException {
		IOException lastError = null;
		for (String url : urls) {
			try {
				HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
				connection.setConnectTimeout(60000);
				connection.setReadTimeout(60000);
				connection.setInstanceFollowRedirects(true);

				int code = connection.getResponseCode();
				if (code >= 200 && code < 300) {
					try (InputStream in = connection.getInputStream()) {
						return readAllBytes(in);
					}
				} else {
					throw new IOException("HTTP " + code);
				}
			} catch (IOException ex) {
				lastError = ex;
			}
		}

		throw new UbisoftException("Download failed: " + (lastError != null ? lastError.getMessage() : "Unknown error."));
	}

	private static byte[] decompressSlice(Download.Manifest manifest, byte[] sliceBytes, int outputSize) throws IOException {
		if (!manifest.getIsCompressed()) {
			return sliceBytes;
		}

		String method = manifest.getCompressionMethod().name().toLowerCase();
		if (method.startsWith("compressionmethod_")) {
			method = method.substring("compressionmethod_".length());
		}

		switch (method) {
		case "zstd":
			return Zstd.decompress(sliceBytes, outputSize);
		case "deflate":
			try (InflaterInputStream zlib = new InflaterInputStream(new ByteArrayInputStream(sliceBytes))) {
				return readAllBytes(zlib);
			}
		case "lzham":
			throw new UbisoftException("LZHAM compression is not supported.");
		default:
			throw new UbisoftException("Unknown compression method: " + method);
		}
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
