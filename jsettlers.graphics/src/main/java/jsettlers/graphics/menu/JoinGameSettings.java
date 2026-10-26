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

import jsettlers.common.menu.EPeaceTime;

/**
 * The game settings collected by the shared join game panel. The host converts them into a platform specific game
 * start (e.g. an {@code InitialGameState}).
 *
 * @author Copilot
 */
public class JoinGameSettings {
	private final JoinGamePlayerSlotData[] playerSlots;
	private final int startResourcesValue;
	private final EPeaceTime peaceTime;
	private final byte humanPlayerSlot;

	/**
	 * Creates a new settings object.
	 *
	 * @param playerSlots
	 *            One entry per map slot; entries with <code>null</code> player type are unavailable.
	 * @param startResourcesValue
	 *            The start resources value (1 = low, 2 = medium, 3 = high).
	 * @param peaceTime
	 *            The selected peace time.
	 * @param humanPlayerSlot
	 *            The slot the human player occupies.
	 */
	public JoinGameSettings(JoinGamePlayerSlotData[] playerSlots, int startResourcesValue, EPeaceTime peaceTime, byte humanPlayerSlot) {
		this.playerSlots = playerSlots;
		this.startResourcesValue = startResourcesValue;
		this.peaceTime = peaceTime;
		this.humanPlayerSlot = humanPlayerSlot;
	}

	public JoinGamePlayerSlotData[] getPlayerSlots() {
		return playerSlots;
	}

	public int getStartResourcesValue() {
		return startResourcesValue;
	}

	public EPeaceTime getPeaceTime() {
		return peaceTime;
	}

	public byte getHumanPlayerSlot() {
		return humanPlayerSlot;
	}
}
