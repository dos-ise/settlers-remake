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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.InflaterInputStream;

import mg.protocol.download.Download;

/**
 * Parsing of Ubisoft Connect game manifests.
 */
public class ManifestParser {

	// The manifest file starts with a 356 byte header followed by a zlib-compressed protobuf Manifest message.
	private static final int MANIFEST_HEADER_SIZE = 356;

	private ManifestParser() {
	}

	public static Download.Manifest parse(byte[] manifestBytes) throws IOException {
		if (manifestBytes.length <= MANIFEST_HEADER_SIZE) {
			throw new UbisoftException("Manifest file is too short.");
		}

		try (InflaterInputStream decompressor = new InflaterInputStream(
				new ByteArrayInputStream(manifestBytes, MANIFEST_HEADER_SIZE, manifestBytes.length - MANIFEST_HEADER_SIZE))) {
			return Download.Manifest.parseFrom(decompressor);
		}
	}

	/**
	 * Finds the file entry with the given name (case-insensitive) in the manifest and returns its
	 * slices. Matches the exact name or a path ending with it (e.g. "loca/SPAE.PA").
	 */
	public static List<Download.Slice> findFileSlices(Download.Manifest manifest, String fileName) {
		for (Download.Chunk chunk : manifest.getChunksList()) {
			for (Download.File file : chunk.getFilesList()) {
				String name = file.getName();
				if (name.equalsIgnoreCase(fileName)
						|| name.toLowerCase(Locale.ENGLISH).endsWith("/" + fileName.toLowerCase(Locale.ENGLISH))
						|| name.toLowerCase(Locale.ENGLISH).endsWith("\\" + fileName.toLowerCase(Locale.ENGLISH))) {
					return new ArrayList<>(file.getSliceListList());
				}
			}
		}

		return null;
	}

	/**
	 * Finds all file entries whose path starts with one of the given directory prefixes
	 * (case-insensitive), e.g. "GFX", "Map", "SND".
	 */
	public static List<ManifestFile> findFilesByDirectory(Download.Manifest manifest, String... directoryPrefixes) {
		List<ManifestFile> result = new ArrayList<>();
		for (Download.Chunk chunk : manifest.getChunksList()) {
			for (Download.File file : chunk.getFilesList()) {
				String name = file.getName();
				for (String prefix : directoryPrefixes) {
					if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH) + "/")
							|| name.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH) + "\\")
							|| name.equalsIgnoreCase(prefix)) {
						result.add(new ManifestFile(name, new ArrayList<>(file.getSliceListList())));
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Builds the relative slice paths used to request download URLs. Version 3 manifests use a
	 * sub-directory layout.
	 */
	public static List<String> getSlicePaths(Download.Manifest manifest, List<Download.Slice> slices) {
		List<String> paths = new ArrayList<>(slices.size());
		int version = manifest.getVersion();

		for (Download.Slice slice : slices) {
			// The CDN paths use uppercase hex (matching the reference UplayKit implementation);
			// lowercase returns 404.
			String sliceId = toHexString(slice.getDownloadSha1().toByteArray());
			if (version == 3) {
				char dir = formatSliceHashChar(sliceId);
				paths.add("slices_v3/" + dir + "/" + sliceId);
			} else {
				paths.add("slices/" + sliceId);
			}
		}

		return paths;
	}

	/**
	 * Computes the sub-directory character for version 3 slice paths.
	 */
	private static char formatSliceHashChar(String sliceId) {
		final String base32 = "0123456789abcdefghijklmnopqrstuv";
		int reversedValue = Integer.parseInt("" + sliceId.charAt(1) + sliceId.charAt(0), 16);
		int offset = reversedValue / 16;
		int halfOffset = reversedValue % 2 == 0 ? 0 : 16;
		return base32.charAt(offset + halfOffset);
	}

	private static String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format(Locale.ENGLISH, "%02X", b));
		}
		return sb.toString();
	}

	/**
	 * A file entry from the manifest with its slices.
	 */
	public static class ManifestFile {
		public final String name;
		public final List<Download.Slice> slices;

		public ManifestFile(String name, List<Download.Slice> slices) {
			this.name = name;
			this.slices = slices;
		}
	}
}
