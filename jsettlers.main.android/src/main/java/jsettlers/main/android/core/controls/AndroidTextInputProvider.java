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

package jsettlers.main.android.core.controls;

import java.util.function.Consumer;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import jsettlers.graphics.map.ITextInputProvider;
import jsettlers.main.android.R;

/**
 * Asks the user for a line of text with a dialog, e.g. for chat messages. This is the Android counterpart of the
 * Swing text input dialog on the desktop.
 */
public class AndroidTextInputProvider implements ITextInputProvider {
	private final Context context;

	public AndroidTextInputProvider(Context context) {
		this.context = context;
	}

	@Override
	public void requestText(String title, Consumer<String> onEntered) {
		View view = View.inflate(context, R.layout.dialog_edit_text, null);
		final EditText editText = view.findViewById(R.id.edit_text);

		Dialog dialog = new AlertDialog.Builder(context)
				.setTitle(title)
				.setView(view)
				.setPositiveButton(android.R.string.ok, (d, which) -> {
					String text = editText.getText().toString();
					if (!text.trim().isEmpty()) {
						onEntered.accept(text);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();

		dialog.setOnShowListener(d -> {
			editText.requestFocus();
			InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
			}
		});
		dialog.show();
	}
}
