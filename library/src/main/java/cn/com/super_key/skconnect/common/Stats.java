package cn.com.super_key.skconnect.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

class Stats {

    private static final int DOWNLOAD_FINISHED = 4;

    private static final String STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats";

    final HandlerThread statsThread;
    final Handler handler;

    long totalDownloadSize;
    long averageDownloadSize;
    int downloadCount;

    Stats() {
        this.statsThread = new HandlerThread(STATS_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        this.statsThread.start();
        Utils.flushStackLocalLeaks(statsThread.getLooper());
        this.handler = new StatsHandler(statsThread.getLooper(), this);
    }

    void dispatchDownloadFinished(long size) {
        handler.sendMessage(handler.obtainMessage(DOWNLOAD_FINISHED, size));
    }

    void shutdown() {
        statsThread.quit();
    }


    void performDownloadFinished(Long size) {
        downloadCount++;
        totalDownloadSize += size;
        averageDownloadSize = getAverage(downloadCount, totalDownloadSize);
    }

    private static long getAverage(int count, long totalSize) {
        return totalSize / count;
    }

    private static class StatsHandler extends Handler {

        private final Stats stats;

        public StatsHandler(Looper looper, Stats stats) {
            super(looper);
            this.stats = stats;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case DOWNLOAD_FINISHED:
                    stats.performDownloadFinished((Long) msg.obj);
                    break;
                default:
                    SKConn.HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            throw new AssertionError("Unhandled stats message." + msg.what);
                        }
                    });
            }
        }
    }

    StatsSnapshot createSnapshot() {
        return new StatsSnapshot(
                totalDownloadSize, averageDownloadSize,
                downloadCount, System.currentTimeMillis());
    }

}