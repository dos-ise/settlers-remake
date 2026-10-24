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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mg.protocol.ownership.Ownership;

/**
 * Ownership check against the Ubisoft Connect API.
 */
public class OwnershipService {

	private static final String SERVICE_NAME = "ownership_service";

	private final DemuxClient demuxClient;
	private int connectionId;

	public OwnershipService(DemuxClient demuxClient) {
		this.demuxClient = demuxClient;
	}

	public void connect() throws IOException {
		connectionId = demuxClient.openConnection(SERVICE_NAME);
	}

	/**
	 * Initializes the ownership service and returns the owned games.
	 */
	public List<Ownership.OwnedGame> getOwnedGames(String ticket, String sessionId) throws IOException {
		Ownership.Upstream request = Ownership.Upstream.newBuilder()
				.setRequest(Ownership.Req.newBuilder()
						.setRequestId(1)
						.setInitializeReq(Ownership.InitializeReq.newBuilder()
								.setGetAssociations(true)
								.setProtoVersion(7)
								.setUseStaging(false))
						.setUbiTicket(ticket)
						.setUbiSessionId(sessionId))
				.build();

		Ownership.Downstream response = demuxClient.sendServiceRequest(connectionId, request, Ownership.Downstream.parser());
		if (response == null || !response.hasResponse() || !response.getResponse().hasInitializeRsp()) {
			throw new UbisoftException("Ownership service initialization failed.");
		}

		Ownership.InitializeRsp initializeRsp = response.getResponse().getInitializeRsp();
		if (!initializeRsp.hasOwnedGames() || initializeRsp.getOwnedGames().getOwnedGamesCount() == 0) {
			return new ArrayList<>();
		}

		return new ArrayList<>(initializeRsp.getOwnedGames().getOwnedGamesList());
	}

	/**
	 * Requests an ownership token for the given product id. This token is required to initialize the
	 * download service.
	 */
	public OwnershipToken getOwnershipToken(int productId, String ticket, String sessionId) throws IOException {
		Ownership.Upstream request = Ownership.Upstream.newBuilder()
				.setRequest(Ownership.Req.newBuilder()
						.setRequestId(1)
						.setOwnershipTokenReq(Ownership.OwnershipTokenReq.newBuilder().setProductId(productId))
						.setUbiTicket(ticket)
						.setUbiSessionId(sessionId))
				.build();

		Ownership.Downstream response = demuxClient.sendServiceRequest(connectionId, request, Ownership.Downstream.parser());
		if (response == null || !response.hasResponse() || !response.getResponse().hasOwnershipTokenRsp()
				|| !response.getResponse().getOwnershipTokenRsp().getSuccess()) {
			throw new UbisoftException("Could not obtain an ownership token.");
		}

		Ownership.OwnershipTokenRsp rsp = response.getResponse().getOwnershipTokenRsp();
		return new OwnershipToken(rsp.getToken(), rsp.getExpiration());
	}

	/**
	 * An ownership token with its expiration time.
	 */
	public static class OwnershipToken {
		public final String token;
		public final long expiration;

		public OwnershipToken(String token, long expiration) {
			this.token = token;
			this.expiration = expiration;
		}
	}
}
