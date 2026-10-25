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

/**
 * Download URL requests against the Ubisoft Connect API.
 */
public class DownloadService {

	private static final String SERVICE_NAME = "download_service";

	private final DemuxClient demuxClient;
	private int connectionId;
	private boolean initialized;

	public DownloadService(DemuxClient demuxClient) {
		this.demuxClient = demuxClient;
	}

	public void connect() throws IOException {
		connectionId = demuxClient.openConnection(SERVICE_NAME);
	}

	/**
	 * Initializes the download service with the ownership token obtained from the ownership service.
	 */
	public boolean initialize(String ownershipToken) throws IOException {
		mg.protocol.download_service.DownloadService.Upstream request = mg.protocol.download_service.DownloadService.Upstream.newBuilder()
				.setRequest(mg.protocol.download_service.DownloadService.Req.newBuilder()
						.setRequestId(1)
						.setInitializeReq(mg.protocol.download_service.DownloadService.InitializeReq.newBuilder()
								.setOwnershipToken(ownershipToken)
								.setNetworkId("")))
				.build();

		mg.protocol.download_service.DownloadService.Downstream response = demuxClient.sendServiceRequest(connectionId, request,
				mg.protocol.download_service.DownloadService.Downstream.parser());
		if (response == null || !response.hasResponse() || !response.getResponse().hasInitializeRsp()) {
			return false;
		}

		initialized = response.getResponse().getInitializeRsp().getOk();
		return initialized;
	}

	/**
	 * Returns the download URLs for the given relative file paths (e.g. "manifests/xxx.manifest" or
	 * "slices/abc123"). The result contains one entry per requested path, each with the mirror URLs for
	 * that file.
	 */
	public List<List<String>> getUrls(int productId, List<String> relativePaths) throws IOException {
		if (!initialized) {
			throw new UbisoftException("Download service is not initialized.");
		}

		mg.protocol.download_service.DownloadService.UrlReq.Request urlRequest = mg.protocol.download_service.DownloadService.UrlReq.Request.newBuilder()
				.setProductId(productId)
				.addAllRelativeFilePath(relativePaths)
				.build();

		mg.protocol.download_service.DownloadService.Upstream request = mg.protocol.download_service.DownloadService.Upstream.newBuilder()
				.setRequest(mg.protocol.download_service.DownloadService.Req.newBuilder()
						.setRequestId(1)
						.setUrlReq(mg.protocol.download_service.DownloadService.UrlReq.newBuilder().addUrlRequests(urlRequest)))
				.build();

		mg.protocol.download_service.DownloadService.Downstream response = demuxClient.sendServiceRequest(connectionId, request,
				mg.protocol.download_service.DownloadService.Downstream.parser());
		if (response == null || !response.hasResponse() || !response.getResponse().hasUrlRsp()
				|| response.getResponse().getUrlRsp().getUrlResponsesCount() == 0) {
			throw new UbisoftException("No download URLs received.");
		}

		mg.protocol.download_service.DownloadService.UrlRsp.UrlResponse urlResponse = response.getResponse().getUrlRsp().getUrlResponses(0);
		if (urlResponse.getResult() != mg.protocol.download_service.DownloadService.UrlRsp.Result.Result_Success
				|| urlResponse.getDownloadUrlsCount() == 0) {
			throw new UbisoftException("Download URLs were denied.");
		}

		List<List<String>> result = new ArrayList<>(urlResponse.getDownloadUrlsCount());
		for (mg.protocol.download_service.DownloadService.UrlRsp.DownloadUrls downloadUrls : urlResponse.getDownloadUrlsList()) {
			List<String> mirrors = new ArrayList<>();
			for (String url : downloadUrls.getUrlsList()) {
				if (!url.isEmpty()) {
					mirrors.add(url);
				}
			}
			result.add(mirrors);
		}

		return result;
	}
}
