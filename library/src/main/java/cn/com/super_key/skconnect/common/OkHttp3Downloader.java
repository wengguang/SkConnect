
package cn.com.super_key.skconnect.common;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


public final class OkHttp3Downloader implements NetRequest {

    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/x-json; charset=utf-8");

    private final Call.Factory client;
    private boolean sharedClient = true;

    public OkHttp3Downloader(final Context context) {
        this();
    }

    public OkHttp3Downloader() {
       //this.client = new OkHttpClient();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .build();

    }

    public OkHttp3Downloader(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public Response post(@NonNull Uri uri, @NonNull String str) throws IOException {

        Request request = new Request.Builder()
                .url(uri.toString())
                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, str))
                .build();

        okhttp3.Response response = client.newCall(request).execute();

        int responseCode = response.code();
        if (responseCode >= 300) {
            response.body().close();
            throw new ResponseException(responseCode + " " + response.message(),
                    responseCode);
        }
        ResponseBody responseBody = response.body();
        String type = response.header("type");
        return new Response(responseBody.string(), responseBody.contentLength(), type);
    }

    @Override
    public Response get(@NonNull Uri uri) throws IOException {
        okhttp3.Response response;
        Request request = new Request.Builder().url(uri.toString()).build();


        response = client.newCall(request).execute();

        int responseCode = response.code();
        if (responseCode >= 300) {
            response.body().close();
            throw new ResponseException(responseCode + " " + response.message(),
                    responseCode);
        }
        ResponseBody responseBody = response.body();
        String type = response.header("type");

        return new Response(responseBody.string(), responseBody.contentLength(), type);
    }


    @Override
    public void shutdown() {
    }

}
