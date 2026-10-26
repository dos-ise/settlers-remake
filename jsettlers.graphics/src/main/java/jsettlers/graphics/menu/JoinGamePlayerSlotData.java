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

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.player.ECivilisation;

/**
 * The configuration of a single player slot in the shared join game panel. Fields may be <code>null</code> when the
 * map does not fix them (a <code>null</code> civilisation means "random").
 *
 * @author Copilot
 */
public class JoinGamePlayerSlotData {
	private final ECivilisation civilisation;
	private final EPlayerType playerType;
	private final Byte team;

	/**
	 * Creates a new player slot configuration.
	 *
	 * @param civilisation
	 *            The civilisation or <code>null</code> for random.
	 * @param playerType
	 *            The player type or <code>null</code> for an unavailable slot.
	 * @param team
	 *            The team or <code>null</code> for an unavailable slot.
	 */
	public JoinGamePlayerSlotData(ECivilisation civilisation, EPlayerType playerType, Byte team) {
		this.civilisation = civilisation;
		this.playerType = playerType;
		this.team = team;
	}

	public ECivilisation getCivilisation() {
		return civilisation;
	}

	public EPlayerType getPlayerType() {
		return playerType;
	}

	public Byte getTeam() {
		return team;
	}
}
