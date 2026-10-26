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
package jsettlers.main.swing.menu;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.ImageMetadata;
import jsettlers.graphics.menu.MenuImageDecoder;

/**
 * Decodes the shared menu images on the desktop using {@link ImageIO}.
 *
 * @author Copilot
 */
public class SwingMenuImageDecoder implements MenuImageDecoder {

	@Override
	public Image decode(String resourcePath) {
		try (InputStream in = SwingMenuImageDecoder.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null) {
				System.err.println("Menu image not found: " + resourcePath);
				return null;
			}
			BufferedImage image = ImageIO.read(in);
			if (image == null) {
				System.err.println("Could not decode menu image: " + resourcePath);
				return null;
			}
			int width = image.getWidth();
			int height = image.getHeight();
			int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
			// The game's ImageData uses 0xRRGGBBAA (red in the highest byte), convert from the standard 0xAARRGGBB.
			for (int i = 0; i < argb.length; i++) {
				int pixel = argb[i];
				argb[i] = ((pixel >> 16) & 0xFF) << 24 | ((pixel >> 8) & 0xFF) << 16 | (pixel & 0xFF) << 8 | (pixel >>> 24) & 0xFF;
			}
			ImageMetadata metadata = new ImageMetadata();
			metadata.width = width;
			metadata.height = height;
			metadata.offsetX = 0;
			metadata.offsetY = 0;
			return new SingleImage(metadata, argb, resourcePath);
		} catch (IOException e) {
			System.err.println("Could not load menu image: " + resourcePath);
			e.printStackTrace();
			return null;
		}
	}
}
