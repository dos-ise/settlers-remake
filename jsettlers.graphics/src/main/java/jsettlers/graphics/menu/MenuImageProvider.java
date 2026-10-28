/*******************************************************************************
 * Copyright (c) 2015
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

import java.nio.IntBuffer;

import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.ImageMetadata;
import go.graphics.ImageData;

/**
 * Provides the shared menu images (background, decorative leaves) that are used by the platform independent menus. The images are decoded lazily by a
 * platform specific {@link MenuImageDecoder}.
 */
public class MenuImageProvider {
	private static MenuImageProvider instance;

	private final MenuImageDecoder decoder;

	// The decoded images are cached statically so they are shared by all provider instances. The providers are
	// recreated on every menu navigation (each fragment calls setInstance), but the images must only be decoded once.
	private static Image background;
	private static Image leavesLeft;
	private static Image leavesRight;
	private static Image icon;
	private static Image border;

	public MenuImageProvider(MenuImageDecoder decoder) {
		this.decoder = decoder;
	}

	/**
	 * Sets the provider used by the shared menus. Must be called once at startup on each platform.
	 */
	public static void setInstance(MenuImageProvider provider) {
		instance = provider;
	}

	/**
	 * @return The current provider, or {@code null} if none was set.
	 */
	public static MenuImageProvider getInstance() {
		return instance;
	}

	/**
	 * @return The main menu background image.
	 */
	public Image getBackground() {
		if (background == null) {
			background = scaleDown(decoder.decode("jsettlers/graphics/menu/ui_background.jpg"), 800);
		}
		return background;
	}

	/**
	 * @return The decorative leaves image for the left side.
	 */
	public Image getLeavesLeft() {
		if (leavesLeft == null) {
			leavesLeft = decoder.decode("jsettlers/graphics/menu/ui_leafs-left.png");
		}
		return leavesLeft;
	}

	/**
	 * @return The decorative leaves image for the right side.
	 */
	public Image getLeavesRight() {
		if (leavesRight == null) {
			leavesRight = decoder.decode("jsettlers/graphics/menu/ui_leafs-right.png");
		}
		return leavesRight;
	}

	/**
	 * @return The game icon shown in the main menu when no sub menu is open.
	 */
	public Image getIcon() {
		if (icon == null) {
			icon = scaleDown(decoder.decode("jsettlers/graphics/menu/ui_icon.png"), 512);
		}
		return icon;
	}

	/**
	 * @return The gold-yellow border texture used for the menu frame.
	 */
	public Image getBorder() {
		if (border == null) {
			border = decoder.decode("jsettlers/graphics/menu/texture-border.png");
		}
		return border;
	}

	/**
	 * Scales an image down so that its larger side is at most {@code maxSize} pixels. Large images are slow to upload
	 * as textures on mobile GPUs, and the menu images are only ever drawn scaled to the screen anyway.
	 *
	 * @param image
	 *            The image to scale, or {@code null}.
	 * @param maxSize
	 *            The maximum size of the larger side in pixels.
	 * @return The scaled image, or the original if it is already small enough or {@code null}.
	 */
	private static Image scaleDown(Image image, int maxSize) {
		if (image == null || (image.getWidth() <= maxSize && image.getHeight() <= maxSize)) {
			return image;
		}
		float scale = Math.min((float) maxSize / image.getWidth(), (float) maxSize / image.getHeight());
		int newWidth = Math.max(1, Math.round(image.getWidth() * scale));
		int newHeight = Math.max(1, Math.round(image.getHeight() * scale));

		ImageData source = ((SingleImage) image).getData();
		IntBuffer src = source.getReadData32();
		int srcWidth = source.getWidth();
		int srcHeight = source.getHeight();
		int[] scaled = new int[newWidth * newHeight];
		for (int y = 0; y < newHeight; y++) {
			int srcY0 = y * srcHeight / newHeight;
			int srcY1 = Math.max(srcY0 + 1, (y + 1) * srcHeight / newHeight);
			for (int x = 0; x < newWidth; x++) {
				int srcX0 = x * srcWidth / newWidth;
				int srcX1 = Math.max(srcX0 + 1, (x + 1) * srcWidth / newWidth);
				long r = 0, g = 0, b = 0, a = 0;
				int count = 0;
				for (int sy = srcY0; sy < srcY1; sy++) {
					for (int sx = srcX0; sx < srcX1; sx++) {
						int pixel = src.get(sx + sy * srcWidth);
						r += (pixel >> 24) & 0xFF;
						g += (pixel >> 16) & 0xFF;
						b += (pixel >> 8) & 0xFF;
						a += pixel & 0xFF;
						count++;
					}
				}
				scaled[x + y * newWidth] = (int) (r / count) << 24 | (int) (g / count) << 16 | (int) (b / count) << 8 | (int) (a / count);
			}
		}
		ImageMetadata metadata = new ImageMetadata();
		metadata.width = newWidth;
		metadata.height = newHeight;
		metadata.offsetX = 0;
		metadata.offsetY = 0;
		return new SingleImage(metadata, scaled, "scaled-" + newWidth + "x" + newHeight);
	}
}
