package com.wyc.download.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.wyc.download.App;
import com.wyc.download.R;
import com.wyc.download.callback.DownloadCallBack;
import com.wyc.download.event.ProgressEvent;
import com.wyc.download.http.RetrofitHttp;
import com.wyc.download.util.ApkUtils;
import com.wyc.download.util.Constants;
import com.wyc.download.util.RxBus;
import com.wyc.download.util.SPUtil;

import java.io.File;

import static com.wyc.download.util.Constants.Extras.DOWNLOAD_FILE;
import static com.wyc.download.util.Constants.Extras.DOWNLOAD_ID;
import static com.wyc.download.util.Constants.Extras.DOWNLOAD_URL;

public class DownloadIntentService extends IntentService {

    private static final String TAG = "DownloadIntentService";
    private NotificationManager mNotifyManager;
    private Notification mNotification;
    private Context mContext;

    private String mNotificationId = "下载测试";

    public DownloadIntentService() {
        super("InitializeService");
        initNotificationChannel(this);
        mContext = this;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        String downloadUrl = intent.getExtras().getString(DOWNLOAD_URL);
        final int downloadId = intent.getExtras().getInt(DOWNLOAD_ID);
        String mDownloadFileName = intent.getExtras().getString(DOWNLOAD_FILE);

        Log.d(TAG, "download_url --" + downloadUrl);
        Log.d(TAG, "download_file --" + mDownloadFileName);

        final File file = new File(Constants.APP_DOWNLOAD_DIR + mDownloadFileName);
        long range = 0;
        int progress = 0;
        if (file.exists()) {
            range = SPUtil.Companion.getInstance().get(downloadUrl, 0);
            progress = (int) (range * 100 / file.length());
//            if (range == file.length()) {
//                ApkUtils.installApp(mContext, file);
//                return;
//            }
        }

        Log.d(TAG, "range = " + range);

        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notify_download);
        remoteViews.setProgressBar(R.id.pb_progress, 100, progress, false);
        remoteViews.setTextViewText(R.id.tv_progress, "已下载" + progress + "%");

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, mNotificationId)
                .setContent(remoteViews)
                .setTicker("正在下载")
                .setSmallIcon(R.mipmap.ic_launcher);
        mNotification = builder.build();

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotifyManager != null) {
            mNotifyManager.notify(downloadId, mNotification);
        }

        RetrofitHttp.getInstance().downloadFile(range, downloadUrl, mDownloadFileName, new DownloadCallBack() {
            @Override
            public void onProgress(int progress) {
                remoteViews.setProgressBar(R.id.pb_progress, 100, progress, false);
                remoteViews.setTextViewText(R.id.tv_progress, "已下载" + progress + "%");
                mNotifyManager.notify(downloadId, mNotification);
                RxBus.getInstance().post(new ProgressEvent(progress));
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "下载完成");
                mNotifyManager.cancel(downloadId);
                RxBus.getInstance().post(new ProgressEvent(true));
                ApkUtils.installApp(mContext, file);
            }

            @Override
            public void onError(String msg) {
                mNotifyManager.cancel(downloadId);
                RxBus.getInstance().post(new ProgressEvent(msg));
                Log.d(TAG, "下载发生错误--" + msg);
            }
        });
    }

    private void initNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) App.context().getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(mNotificationId, mNotificationId, NotificationManager
                    .IMPORTANCE_HIGH);
            channel.setSound(null, null);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
