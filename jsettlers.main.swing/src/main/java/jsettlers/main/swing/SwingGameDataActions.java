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
package jsettlers.main.swing;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import jsettlers.gog.GogDownloader;
import jsettlers.gog.GogLogin;
import jsettlers.gog.GogModels.GogTokenResponse;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.menu.GameDataActions;
import jsettlers.ubisoft.Settlers3Downloader;
import jsettlers.ubisoft.UbisoftLogin;
import jsettlers.ubisoft.UbisoftModels.UbisoftLoginResult;

/**
 * Desktop implementation of the game data setup actions. Lets the user select the Settlers 3
 * installation folder or download the game data from GOG or Ubisoft Connect. The shared GL panel
 * triggers these actions; the result folder is handed back through the {@link CompletableFuture}.
 *
 * @author Copilot
 */
public class SwingGameDataActions implements GameDataActions {

	private static final String HELP_URL = "https://github.com/dos-ise/settlers-remake/blob/master/README.md";

	private final JSettlersFrame frame;
	private final CompletableFuture<File> folderFuture;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	public SwingGameDataActions(JSettlersFrame frame, CompletableFuture<File> folderFuture) {
		this.frame = frame;
		this.folderFuture = folderFuture;
	}

	@Override
	public void chooseDirectory() {
		SwingUtilities.invokeLater(() -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle(Labels.getString("game-data-choose-directory"));
			if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				folderFuture.complete(chooser.getSelectedFile());
			}
		});
	}

	@Override
	public void downloadFromGog() {
		executorService.execute(this::runGogDownload);
	}

	private void runGogDownload() {
		File targetDirectory = askForTargetDirectory();
		if (targetDirectory == null) {
			return;
		}

		openBrowser(GogLogin.AUTH_URL);
		String code = askForInput(Labels.getString("game-data-gog-code-title"), Labels.getString("game-data-gog-code-message"));
		if (code == null || code.trim().isEmpty()) {
			return;
		}

		try {
			GogTokenResponse token = GogLogin.exchangeCode(code.trim());
			if (token == null || token.accessToken == null || token.accessToken.isEmpty()) {
				showError(Labels.getString("game-data-login-failed"));
				return;
			}

			ProgressDialog progress = new ProgressDialog(Labels.getString("game-data-download-title"));
			try {
				GogDownloader downloader = new GogDownloader(token.accessToken, targetDirectory);
				downloader.download(progress::update);
			} finally {
				progress.close();
			}
			folderFuture.complete(targetDirectory);
		} catch (Exception ex) {
			showError(Labels.getString("game-data-download-failed") + " " + ex.getMessage());
		}
	}

	@Override
	public void downloadFromUbisoft() {
		executorService.execute(this::runUbisoftDownload);
	}

	private void runUbisoftDownload() {
		File targetDirectory = askForTargetDirectory();
		if (targetDirectory == null) {
			return;
		}

		String email = askForInput(Labels.getString("game-data-ubisoft-email"), Labels.getString("game-data-ubisoft-email"));
		if (email == null || email.trim().isEmpty()) {
			return;
		}
		String password = askForPassword(Labels.getString("game-data-ubisoft-password"));
		if (password == null || password.isEmpty()) {
			return;
		}

		try {
			UbisoftLoginResult loginResult = UbisoftLogin.login(email.trim(), password);
			if (loginResult.requiresTwoFactorAuthentication) {
				String code = askForInput(Labels.getString("game-data-ubisoft-2fa-code"), Labels.getString("game-data-ubisoft-2fa-code"));
				if (code == null || code.trim().isEmpty()) {
					return;
				}
				loginResult = UbisoftLogin.loginWithTwoFactorCode(loginResult.response.twoFactorAuthenticationTicket, code.trim());
			}
			if (!loginResult.success || loginResult.response == null) {
				String message = loginResult.errorMessage != null ? loginResult.errorMessage : "";
				showError(Labels.getString("game-data-login-failed") + " " + message);
				return;
			}

			ProgressDialog progress = new ProgressDialog(Labels.getString("game-data-download-title"));
			try {
				Settlers3Downloader downloader = new Settlers3Downloader(loginResult.response, targetDirectory);
				downloader.download(progress::update);
			} finally {
				progress.close();
			}
			folderFuture.complete(targetDirectory);
		} catch (Exception ex) {
			showError(Labels.getString("game-data-download-failed") + " " + ex.getMessage());
		}
	}

	@Override
	public void openHelp() {
		openBrowser(HELP_URL);
	}

	private File askForTargetDirectory() {
		final File[] result = new File[1];
		runOnEdt(() -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle(Labels.getString("game-data-download-target-title"));
			if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				result[0] = chooser.getSelectedFile();
			}
		});
		return result[0];
	}

	private String askForInput(String title, String message) {
		final String[] result = new String[1];
		runOnEdt(() -> result[0] = JOptionPane.showInputDialog(frame, message, title, JOptionPane.PLAIN_MESSAGE));
		return result[0];
	}

	private String askForPassword(String title) {
		final String[] result = new String[1];
		runOnEdt(() -> {
			JPasswordField passwordField = new JPasswordField();
			JPanel panel = new JPanel();
			panel.add(new JLabel(title + ":"));
			panel.add(passwordField);
			if (JOptionPane.showConfirmDialog(frame, panel, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				result[0] = new String(passwordField.getPassword());
			}
		});
		return result[0];
	}

	private void runOnEdt(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InterruptedException | InvocationTargetException ex) {
				// The dialog was interrupted; treat it as cancelled.
			}
		}
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(frame, message, Labels.getString("game-data-download-title"), JOptionPane.ERROR_MESSAGE);
	}

	private void openBrowser(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception ex) {
			showError(Labels.getString("error-open-url") + ": " + url);
		}
	}

	/**
	 * A simple modal progress dialog for the game data download.
	 */
	private static class ProgressDialog {
		private final javax.swing.JDialog dialog;
		private final JProgressBar progressBar;
		private final JLabel statusLabel;

		ProgressDialog(String title) {
			JPanel panel = new JPanel();
			panel.setLayout(new java.awt.BorderLayout(10, 10));
			statusLabel = new JLabel(" ");
			progressBar = new JProgressBar(0, 100);
			panel.add(statusLabel, java.awt.BorderLayout.NORTH);
			panel.add(progressBar, java.awt.BorderLayout.CENTER);
			panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

			dialog = new javax.swing.JDialog();
			dialog.setTitle(title);
			dialog.setContentPane(panel);
			dialog.setModal(true);
			dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
			dialog.setSize(400, 120);
			dialog.setLocationRelativeTo(null);
			SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		}

		void update(String status, int percent) {
			SwingUtilities.invokeLater(() -> {
				statusLabel.setText(status);
				progressBar.setValue(percent);
			});
		}

		void close() {
			SwingUtilities.invokeLater(() -> dialog.dispose());
		}
	}
}
