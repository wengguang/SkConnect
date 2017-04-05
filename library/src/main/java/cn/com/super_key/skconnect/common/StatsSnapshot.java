package cn.com.super_key.skconnect.common;

import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;



public class StatsSnapshot {

  public final long totalDownloadSize;
  public final long averageDownloadSize;
  public final int downloadCount;
  public final long timeStamp;

  public StatsSnapshot(
      long totalDownloadSize,
      long averageDownloadSize,
      int downloadCount,  long timeStamp) {
    this.totalDownloadSize = totalDownloadSize;
    this.averageDownloadSize = averageDownloadSize;
    this.downloadCount = downloadCount;
    this.timeStamp = timeStamp;
  }

 public void dump() {
    StringWriter logWriter = new StringWriter();
    dump(new PrintWriter(logWriter));
  }

  /** Prints out this {@link StatsSnapshot} with the the provided {@link PrintWriter}. */
  public void dump(PrintWriter writer) {
    writer.println("===============BEGIN SKconn STATS ===============");

    writer.println("Network Stats");
    writer.print("  Download Count: ");
    writer.println(downloadCount);
    writer.print("  Total Download Size: ");
    writer.println(totalDownloadSize);
    writer.print("  Average Download Size: ");
    writer.println(averageDownloadSize);
    writer.println("===============END SKconn STATS ===============");
    writer.flush();
  }

  @Override public String toString() {
    return "StatsSnapshot{"
        +" downloadCount="
        + downloadCount
        + ", totalDownloadSize="
        + totalDownloadSize
        + ", averageDownloadSize="
        + averageDownloadSize
        + ", timeStamp="
        + timeStamp
        + '}';
  }
}
