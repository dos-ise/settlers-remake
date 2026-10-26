/*******************************************************************************
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
 *******************************************************************************/
package jsettlers.graphics.menu;

import jsettlers.common.Color;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.ImageMetadata;

/**
 * Converts the map preview image data (as provided by {@code IMapDefinition.getImage()}) into a shared
 * {@link Image} that can be drawn by the platform independent menus.
 *
 * @author Copilot
 */
public final class MapPreviewImage {
	private MapPreviewImage() {
	}

	/**
	 * Creates a shared image from the raw preview data of a map.
	 *
	 * @param data
	 *            The preview image data, a square array of 16 bit colors, or <code>null</code>.
	 * @return The image, or <code>null</code> if the data is missing.
	 */
	public static Image create(short[] data) {
		if (data == null || data.length == 0) {
			return null;
		}
		int size = (int) Math.sqrt(data.length);
		int[] rgba = new int[data.length];
		for (int i = 0; i < data.length; i++) {
			rgba[i] = Color.fromShort(data[i]).getRGBA();
		}
		ImageMetadata metadata = new ImageMetadata();
		metadata.width = size;
		metadata.height = size;
		metadata.offsetX = 0;
		metadata.offsetY = 0;
		return new SingleImage(metadata, rgba, "map-preview");
	}
}
