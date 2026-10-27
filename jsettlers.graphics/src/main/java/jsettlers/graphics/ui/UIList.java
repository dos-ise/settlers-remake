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
package jsettlers.graphics.ui;

import go.graphics.GLDrawContext;

import java.util.List;
import java.util.Optional;

import jsettlers.common.Color;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.position.FloatRectangle;
import jsettlers.common.action.Action;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.ImageMetadata;
import jsettlers.graphics.map.draw.ImageProvider;

public class UIList<T> implements UIElement {
	private static final OriginalImageLink SCROLLBAR_TOP =
			new OriginalImageLink(EImageLinkType.GUI, 2, 2, 0);
	private static final OriginalImageLink SCROLLBAR_MIDDLE =
			new OriginalImageLink(EImageLinkType.GUI, 2, 3, 0);
	private static final OriginalImageLink SCROLLBAR_BOTTOM =
			new OriginalImageLink(EImageLinkType.GUI, 2, 4, 0);

	/**
	 * A solid black background so the list entries are readable on the menu background.
	 */
	private final Image background = createBackground(new Color(0, 0, 0, 1), "list-background");

	/**
	 * Background of every second list entry so the touch area of each row is visible.
	 */
	private final Image evenItemBackground = createBackground(new Color(0.1f, 0.1f, 0.1f, 1), "list-item-even");

	/**
	 * Background of the other list entries.
	 */
	private final Image oddItemBackground = createBackground(new Color(0.16f, 0.16f, 0.16f, 1), "list-item-odd");

	/**
	 * Background of the currently selected list entry.
	 */
	private final Image selectedItemBackground = createBackground(new Color(0.25f, 0.3f, 0.4f, 1), "list-item-selected");

	private static Image createBackground(Color color, String name) {
		ImageMetadata metadata = new ImageMetadata();
		metadata.width = 1;
		metadata.height = 1;
		metadata.offsetX = 0;
		metadata.offsetY = 0;
		int[] rgba = new int[] { color.getRGBA() };
		return new SingleImage(metadata, rgba, name);
	}

	private static final float RIGHTBORDER = .97f;
	private static final float EDGEPART = .02f;
	private static final float SLIDER_MIN_HEIGHT = 0.1f;
	private final float itemheight;
	private FloatRectangle position;

	private float listoffset = 0; // relative in UI space
	private List<? extends T> items;
	private final Object itemsMutex = new Object();
	private T activeItem;
	private final ListItemGenerator<T> generator;

	private final class ScrollToExecutableAction extends ExecutableAction {
		private final int destinationOffset;

		private ScrollToExecutableAction(int destinationOffset) {
			this.destinationOffset = destinationOffset;
		}

		@Override
		public void execute() {
			scrollBy(destinationOffset);
		}
	}

	public interface ListItemGenerator<T> {
		UIListItem getItem(T item);
	}

	public UIList(List<? extends T> items, ListItemGenerator<T> generator, float itemheight) {
		setItems(items);
		this.generator = generator;
		this.itemheight = itemheight;
	}

	public void setItems(List<? extends T> list) {
		synchronized (itemsMutex) {
			this.items = list;

			if (!items.contains(activeItem)) {
				if (items.size() > 0) {
					activeItem = items.get(0);
				} else {
					activeItem = null;
				}
			}
		}
	}

	@Override
	public void setPosition(FloatRectangle position) {
		this.position = position;
	}

	@Override
	public void drawAt(GLDrawContext gl) {
		synchronized (itemsMutex) {
			float totallistheigt = itemheight * items.size(); // relative
			if (listoffset < 0) {
				listoffset = 0;
			} else if (listoffset + 1 > totallistheigt) {
				listoffset = Math.max(0, totallistheigt - 1);
			}

			float minY = position.getMinY();
			float minX = position.getMinX();

			float height = position.getHeight();
			float width = position.getWidth();

			background.drawImageAtRect(gl, minX, minY, width, height, 1);

			int startindex = (int) (listoffset / itemheight);

			int itemsperpage = (int) (1 / itemheight);

			for (int i = startindex; i < startindex + itemsperpage
					&& i < items.size(); i++) {
				T item = items.get(i);
				// TODO: Cache
				UIListItem listItem = generator.getItem(item);
				// relative
				float itemtop = 1 - i * itemheight + listoffset;
				float itembottom = itemtop - itemheight;

				Image itemBackground = activeItem == item ? selectedItemBackground
						: (i % 2 == 0 ? evenItemBackground : oddItemBackground);
				itemBackground.drawImageAtRect(gl, minX, minY + itembottom * height,
						RIGHTBORDER * width, itemheight * height, 1);

				listItem.setHighlighted(activeItem == item);
				listItem.setPosition(new FloatRectangle(minX, minY + itembottom
						* height, minX + RIGHTBORDER * width, minY + itemtop
						* height));
				listItem.drawAt(gl);
			}

			float sliderHeight =
					Math.min(1, Math.max(SLIDER_MIN_HEIGHT, 1 / totallistheigt));
			float slide =
					(1 - sliderHeight) * listoffset / (totallistheigt - 1);
			float sliderMax = 1 - slide;

			float sliderMin = sliderMax - sliderHeight;
			float edgeHeight = height * EDGEPART; // bottom / top edge at side.

			float sliderMinY = sliderMin * height + minY;
			float sliderMaxY = sliderMax * height + minY;

			// side
			ImageProvider provider = ImageProvider.getInstance();
			float scrollbarWidth = width * (1 - RIGHTBORDER);
			float scrollbarX = minX + width * RIGHTBORDER;
			provider.getImage(SCROLLBAR_TOP).drawImageAtRect(gl,
					scrollbarX, sliderMaxY - edgeHeight,
					scrollbarWidth, edgeHeight, 1);
			provider.getImage(SCROLLBAR_MIDDLE).drawImageAtRect(gl,
					scrollbarX, sliderMinY + edgeHeight,
					scrollbarWidth, Math.max(0, sliderMaxY - sliderMinY - 2 * edgeHeight), 1);
			provider.getImage(SCROLLBAR_BOTTOM).drawImageAtRect(gl,
					scrollbarX, sliderMinY,
					scrollbarWidth, edgeHeight, 1);
		}
	}

	@Override
	public Optional<Action> getAction(float relativex, float relativey) {
		synchronized (itemsMutex) {
			if (relativex < RIGHTBORDER) {
				float listy = 1 - relativey + listoffset;
				int itemIndex = (int) (listy / itemheight);
				if (itemIndex >= 0 && itemIndex < items.size()) {
					return Optional.of(new SelectAction(items.get(itemIndex)));
				} else {
					return Optional.empty();
				}
			} else {
				final float halfNumberOfDisplayedItems = (int) (0.5f / itemheight);
				final int destinationOffset = (int) ((1 - relativey) * items.size()
						- halfNumberOfDisplayedItems); // subtract this to get the center of the scrollbar where the player klicked.

				return Optional.of(new ScrollToExecutableAction(destinationOffset));
			}
		}
	}

	protected void scrollBy(int offset) {
		listoffset = itemheight * offset;
	}

	@Override
	public void handleZoom(float relativex, float relativey, float zoomFactor) {
		// The mouse wheel is converted to a zoom event. Scroll the list by one item per wheel step.
		scrollBy(zoomFactor > 1 ? -1 : 1);
	}

	private float panStartOffset = Float.NaN;

	@Override
	public void handlePanStart(float relativex, float relativey) {
		panStartOffset = listoffset;
	}

	@Override
	public void handlePan(float relativex, float relativey, float panDistanceX, float panDistanceY) {
		float height = position.getHeight();
		if (height > 0 && !Float.isNaN(panStartOffset)) {
			// Dragging up (positive y) scrolls down, so the offset grows with the pan distance.
			listoffset = panStartOffset + panDistanceY / height;
		}
	}

	@Override
	public void handlePanEnd(float relativex, float relativey) {
		panStartOffset = Float.NaN;
	}

	private class SelectAction extends ExecutableAction {
		private final T item;

		public SelectAction(T item) {
			this.item = item;
		}

		@Override
		public void execute() {
			// TODO: setActiveUIElement();
			setActiveItem(item);
		}
	}

	@Override
	public String getDescription(float relativex, float relativey) {
		return null;
	}

	public void setActiveItem(T activeItem) {
		synchronized (itemsMutex) {
			if (items.contains(activeItem)) {
				this.activeItem = activeItem;
			}
		}
	}

	public T getActiveItem() {
		return activeItem;
	}

	@Override
	public void onAttach() {
	}

	@Override
	public void onDetach() {
	}
}
