package cn.com.super_key.skconnect.common;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public interface NetRequest {

  @Nullable Response post(@NonNull Uri uri,@NonNull String str) throws IOException;
  @Nullable Response get(@NonNull Uri uri) throws IOException;
  void shutdown();

 class ResponseException extends IOException {
    final int responseCode;

    public ResponseException(String message, int responseCode) {
      super(message);
      this.responseCode = responseCode;
    }
  }

  class Response {
    final String  retString;
    final long contentLength;
    final String type;

    public Response(@NonNull String stream,long contentLength,String type) {
      if (stream == null) {
        throw new IllegalArgumentException("Stream may not be null.");
      }
      this.retString = stream;//inputStream2String(stream);
      this.contentLength = contentLength;
     // if(this.retString.length() != this.contentLength){
     //   throw new IllegalArgumentException("length error.orgleng="+contentLength+"content len="+this.retString.length());
    //  }
      this.type= type;
    }
    public   String inputStream2String(InputStream   in)
    {
      if(in == null)
        return "";
      StringBuffer out = new StringBuffer();
      try {
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) >0; ) {
          out.append(new String(b, 0, n));

        }
        in.close();
      }
      catch (Exception e){
        e.printStackTrace();
      }
      return   out.toString();
    }
    public String getRetString() {
      return retString;
    }
    public long getContentLength() {
      return contentLength;
    }
    public String getType() {
      return type;
    }
  }
}
