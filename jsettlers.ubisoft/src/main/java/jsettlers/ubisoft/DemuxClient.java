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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import mg.protocol.demux.Demux;

/**
 * TLS socket client for the Ubisoft Connect demux protocol. Implements the protobuf-based demux
 * protocol used by the Ubisoft Connect client.
 */
public class DemuxClient implements AutoCloseable {

	public static final String HOST = "dmx.upc.ubisoft.com";
	public static final int PORT = 443;
	public static final int CLIENT_VERSION = 12922;

	private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;

	private SSLSocket socket;
	private InputStream input;
	private OutputStream output;
	private int requestId = 1;

	/**
	 * Connects to the demux socket, checks the patch version and announces the client version.
	 */
	public void connect() throws IOException {
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		socket = (SSLSocket) factory.createSocket(HOST, PORT);
		socket.startHandshake();
		input = socket.getInputStream();
		output = socket.getOutputStream();

		versionCheck();
		pushVersion();
	}

	/**
	 * Authenticates the connection with the login ticket.
	 *
	 * @return true if the authentication succeeded.
	 */
	public boolean authenticate(String ticket) throws IOException {
		Demux.Req req = Demux.Req.newBuilder()
				.setRequestId(requestId++)
				.setAuthenticateReq(Demux.AuthenticateReq.newBuilder()
						.setClientId("uplay_pc")
						.setSendKeepAlive(false)
						.setToken(Demux.Token.newBuilder().setUbiTicket(ticket)))
				.build();

		Demux.Rsp rsp = sendRequest(req);
		return rsp != null && rsp.hasAuthenticateRsp() && rsp.getAuthenticateRsp().getSuccess();
	}

	/**
	 * Checks the current patch version (result is not enforced, matching the reference client).
	 */
	private void versionCheck() throws IOException {
		Demux.Req req = Demux.Req.newBuilder()
				.setRequestId(requestId++)
				.setGetPatchInfoReq(Demux.GetPatchInfoReq.newBuilder()
						.setPatchTrackId("DEFAULT")
						.setTestConfig(false)
						.setTrackType(0))
				.build();

		sendRequest(req);
	}

	/**
	 * Announces the client version to the server.
	 */
	private void pushVersion() throws IOException {
		Demux.Upstream upstream = Demux.Upstream.newBuilder()
				.setPush(Demux.Push.newBuilder()
						.setClientVersion(Demux.ClientVersionPush.newBuilder().setVersion(CLIENT_VERSION)))
				.build();

		writeFramed(upstream.toByteArray());
	}

	/**
	 * Opens a service connection (e.g. "ownership_service") and returns the connection id used for
	 * service requests.
	 */
	public int openConnection(String serviceName) throws IOException {
		Demux.Req req = Demux.Req.newBuilder()
				.setRequestId(requestId++)
				.setOpenConnectionReq(Demux.OpenConnectionReq.newBuilder().setServiceName(serviceName))
				.build();

		Demux.Rsp rsp = sendRequest(req);
		if (rsp == null || !rsp.hasOpenConnectionRsp() || !rsp.getOpenConnectionRsp().getSuccess()) {
			throw new UbisoftException("Failed to connect to service '" + serviceName + "'.");
		}

		return rsp.getOpenConnectionRsp().getConnectionId();
	}

	/**
	 * Sends a service-specific request on an open connection and returns the parsed response message.
	 */
	public <T extends Message> T sendServiceRequest(int connectionId, Message request, Parser<T> responseParser) throws IOException {
		Demux.Upstream upstream = Demux.Upstream.newBuilder()
				.setPush(Demux.Push.newBuilder()
						.setData(Demux.DataMessage.newBuilder()
								.setConnectionId(connectionId)
								.setData(ByteString.copyFrom(formatLengthPrefixed(request.toByteArray())))))
				.build();

		Demux.Downstream downstream = sendUpstream(upstream);
		if (downstream == null || !downstream.hasPush() || !downstream.getPush().hasData() || !downstream.getPush().getData().hasData()) {
			throw new UbisoftException("No response from service.");
		}

		byte[] data = downstream.getPush().getData().getData().toByteArray();
		if (data.length < 4) {
			throw new UbisoftException("Invalid service response.");
		}

		int length = readBigEndianUInt32(data, 0);
		if (length <= 0 || 4 + length > data.length) {
			throw new UbisoftException("Invalid service response length.");
		}

		return responseParser.parseFrom(data, 4, length);
	}

	/**
	 * Sends a demux request and returns the matching response.
	 */
	private Demux.Rsp sendRequest(Demux.Req req) throws IOException {
		Demux.Upstream upstream = Demux.Upstream.newBuilder().setRequest(req).build();
		Demux.Downstream downstream = sendUpstream(upstream);
		if (downstream == null || !downstream.hasResponse()) {
			return null;
		}
		return downstream.getResponse();
	}

	/**
	 * Sends an upstream message (4-byte big-endian length prefix) and reads the downstream response,
	 * skipping/handling any pushes that arrive in between.
	 */
	private Demux.Downstream sendUpstream(Demux.Upstream upstream) throws IOException {
		writeFramed(upstream.toByteArray());

		while (true) {
			Demux.Downstream downstream = readMessage();
			if (downstream == null) {
				continue;
			}

			// Demux-level responses (Authenticate, OpenConnection) arrive in Response; service
			// responses arrive in Push.Data.
			if (downstream.hasResponse() || (downstream.hasPush() && downstream.getPush().hasData())) {
				return downstream;
			}

			// Other pushes (e.g. keep-alive): answer keep-alive, ignore the rest.
			if (downstream.hasPush() && downstream.getPush().hasKeepAlive()) {
				sendKeepAlive();
			} else if (downstream.hasPush() && downstream.getPush().hasConnectionClosed()) {
				throw new UbisoftException("Connection closed by server.");
			}
		}
	}

	private void sendKeepAlive() throws IOException {
		Demux.Upstream upstream = Demux.Upstream.newBuilder()
				.setPush(Demux.Push.newBuilder().setKeepAlive(Demux.KeepAlivePush.getDefaultInstance()))
				.build();

		writeFramed(upstream.toByteArray());
	}

	/**
	 * Reads one demux message. Responses are length-prefixed; some pushes are sent as raw protobuf
	 * (first byte 0x12 = Push field tag).
	 */
	private Demux.Downstream readMessage() throws IOException {
		byte[] header = new byte[4];
		readExactly(header, 0, 4);

		if (header[0] == 0x12) {
			// Raw push without length prefix: 0x12 (Push field tag) followed by a varint length and
			// the Push payload.
			byte[] raw = readRawPush(header);
			return Demux.Downstream.parseFrom(raw);
		}

		int messageLength = readBigEndianUInt32(header, 0);
		if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
			throw new UbisoftException("Invalid server message length.");
		}

		byte[] message = new byte[messageLength];
		readExactly(message, 0, messageLength);
		return Demux.Downstream.parseFrom(message);
	}

	/**
	 * Reads a raw (non length-prefixed) push message. The first byte is the Push field tag (0x12); the
	 * varint length and payload follow.
	 */
	private byte[] readRawPush(byte[] firstBytes) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		buffer.write(firstBytes, 0, firstBytes.length);

		// Byte 0 is the tag (0x12). Parse the length varint starting at byte 1, using already-read
		// bytes first, then the stream.
		int offset = 1;
		int length = 0;
		int shift = 0;
		while (true) {
			int b;
			if (offset < firstBytes.length) {
				b = firstBytes[offset++] & 0xFF;
			} else {
				b = input.read();
				if (b < 0) {
					throw new UbisoftException("Connection closed unexpectedly.");
				}
				buffer.write(b);
			}
			length |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				break;
			}
			shift += 7;
			if (shift > 35) {
				throw new UbisoftException("Invalid server protobuf data.");
			}
		}

		if (length <= 0 || length > MAX_MESSAGE_SIZE) {
			throw new UbisoftException("Invalid server push length.");
		}

		// Some payload bytes may already be part of firstBytes.
		int alreadyHave = Math.max(0, firstBytes.length - offset);
		int toRead = length - alreadyHave;
		if (toRead > 0) {
			byte[] payload = new byte[toRead];
			readExactly(payload, 0, toRead);
			buffer.write(payload, 0, toRead);
		}

		return buffer.toByteArray();
	}

	private void writeFramed(byte[] payload) throws IOException {
		output.write(formatLengthPrefixed(payload));
		output.flush();
	}

	private static byte[] formatLengthPrefixed(byte[] payload) {
		byte[] framed = new byte[4 + payload.length];
		writeBigEndianUInt32(framed, 0, payload.length);
		System.arraycopy(payload, 0, framed, 4, payload.length);
		return framed;
	}

	private void readExactly(byte[] buffer, int offset, int count) throws IOException {
		int read = 0;
		while (read < count) {
			int n = input.read(buffer, offset + read, count - read);
			if (n <= 0) {
				throw new UbisoftException("Connection closed unexpectedly.");
			}
			read += n;
		}
	}

	private static int readBigEndianUInt32(byte[] buffer, int offset) {
		return ((buffer[offset] & 0xFF) << 24) | ((buffer[offset + 1] & 0xFF) << 16)
				| ((buffer[offset + 2] & 0xFF) << 8) | (buffer[offset + 3] & 0xFF);
	}

	private static void writeBigEndianUInt32(byte[] buffer, int offset, int value) {
		buffer[offset] = (byte) (value >> 24);
		buffer[offset + 1] = (byte) (value >> 16);
		buffer[offset + 2] = (byte) (value >> 8);
		buffer[offset + 3] = (byte) value;
	}

	@Override
	public void close() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}
}
