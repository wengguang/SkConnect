package cn.com.super_key.skconnect.common;

import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.IOException;

import static cn.com.super_key.skconnect.common.NetRequest.Response;

class NetworkRequestHandler extends RequestHandler {

  static final int RETRY_COUNT = 2;
  private static final String SCHEME_HTTP = "http";
  private static final String SCHEME_HTTPS = "https";

  private final NetRequest downloader;
  private final Stats stats;

  public NetworkRequestHandler(NetRequest downloader, Stats stats) {
    this.downloader = downloader;
    this.stats = stats;
  }

  @Override public boolean canHandleRequest(Request data) {
    String scheme = data.uri.getScheme();
    return (SCHEME_HTTP.equals(scheme) || SCHEME_HTTPS.equals(scheme));
  }

  @Override
  @Nullable public String get(Request request) throws IOException {
    Response response = downloader.get(request.uri);
    if (response == null) {
      return null;
    }

    if ( response.getContentLength() == 0) {
      throw new ContentLengthException("Received response with 0 content-length header.");
    }
    if ( response.getContentLength() > 0) {
      stats.dispatchDownloadFinished(response.getContentLength());
    }

    return response.getRetString();
  }

  @Override
  @Nullable public String post(Request request,@NonNull String str) throws IOException {
    Response response = downloader.post(request.uri,str);
    if (response == null) {
      return null;
    }

    if ( response.getContentLength() == 0) {
      throw new ContentLengthException("Received response with 0 content-length header.");
    }
    if (response.getContentLength() > 0) {
      stats.dispatchDownloadFinished(response.getContentLength());
    }
    return response.getRetString();
  }

  @Override int getRetryCount() {
    return RETRY_COUNT;
  }

  @Override boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
    return info == null || info.isConnected();
  }

  @Override boolean supportsReplay() {
    return true;
  }

  static class ContentLengthException extends IOException {
    public ContentLengthException(String message) {
      super(message);
    }
  }
}
