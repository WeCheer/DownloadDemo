package com.wyc.download.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.wyc.download.R;
import com.wyc.download.event.ProgressEvent;
import com.wyc.download.service.DownloadIntentService;
import com.wyc.download.util.ApkUtils;
import com.wyc.download.util.Constants;
import com.wyc.download.util.RxBus;
import com.wyc.download.util.SPUtil;
import com.wyc.download.widget.ProgressBarView;

import java.io.File;
import java.util.List;

import io.reactivex.functions.Consumer;

import static com.wyc.download.util.Constants.Extras.DOWNLOAD_FILE;
import static com.wyc.download.util.Constants.Extras.DOWNLOAD_ID;
import static com.wyc.download.util.Constants.Extras.DOWNLOAD_URL;


public class DownLoadActivity extends AppCompatActivity {
    public static final String TAG = "DownLoadActivity";
    private static final int DOWNLOADAPK_ID = 10;

    private String downloadUrl = "/qqmi/aphone_p2p/TencentVideo_V6.0.0.14297_848.apk";
    private String mDownloadFileName;
    private Dialog mDialog;
    private ProgressBarView mProgressBarView;
    private ImageView mCloseDialog;
    private TextView mStartDownLoad;
    private TextView mUpdateTip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxBus.getInstance().register(this);
        TextView tvDownload = findViewById(R.id.tv_download);
        mDownloadFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        tvDownload.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onClick(View v) {
                RxPermissions permissions = new RxPermissions(DownLoadActivity.this);
                permissions.setLogging(true);
                permissions.requestEachCombined(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(new Consumer<Permission>() {
                            @Override
                            public void accept(Permission permission) throws Exception {
                                if (permission.granted) {//全部同意后调用
                                    downloadFile();
                                } else if (permission.shouldShowRequestPermissionRationale) {//只要有一个选择：禁止，但没有选择“以后不再询问”，以后申请权限，会继续弹出提示
                                    Log.e(TAG, "checkPermissionRequestEachCombined--:" + "-READ_EXTERNAL_STORAGE-shouldShowRequestPermissionRationale:" + false);
                                } else {//只要有一个选择：禁止，但选择“以后不再询问”，以后申请权限，不会继续弹出提示
                                    ApkUtils.intentPermissionSetting(DownLoadActivity.this);
                                }
                            }
                        });
            }
        });
    }

    private void downloadFile() {
        Log.d(TAG, "下载文件");
        if (checkNotifySetting()) {
            downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            final File file = new File(Constants.APP_DOWNLOAD_DIR + mDownloadFileName);
            long range;
            if (file.exists()) {
                range = SPUtil.Companion.getInstance().get(downloadUrl, 0);
                if (range == file.length()) {
                    installProcess(file);
                } else {
                    createDownLoadDialog();
                }
            } else {
                createDownLoadDialog();
            }
        }
    }

    //安装应用的流程
    private void installProcess(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //先获取是否有安装未知来源应用的权限
            boolean haveInstallPermission = getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {//没有权限
                Log.d(TAG, "请求位置应用权限");
                new AlertDialog.Builder(this).setTitle("用户提示")
                        .setMessage("安装应用需要打开未知来源权限，请去设置中开启权限")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Uri packageURI = Uri.parse("package:" + getPackageName());
                                //注意这个是8.0新API
                                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                                startActivityForResult(intent, 10086);
                            }
                        }).show();
                return;
            }
        }
        //有权限，开始安装应用程序
        ApkUtils.installApp(DownLoadActivity.this, file);
    }

    private boolean checkNotifySetting() {
        Log.d(TAG, "开启通知权限");
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        // areNotificationsEnabled方法的有效性官方只最低支持到API 19，低于19的仍可调用此方法不过只会返回true，即默认为用户已经开启了通知。
        boolean isOpened = manager.areNotificationsEnabled();

        if (!isOpened) {
            try {
                // 根据isOpened结果，判断是否需要提醒用户跳转AppInfo页面，去打开App通知权限
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, getApplicationInfo().uid);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
                    intent.putExtra("app_package", getPackageName());
                    intent.putExtra("app_uid", getApplicationInfo().uid);
                }

                // 小米6 -MIUI9.6-8.0.0系统，是个特例，通知设置界面只能控制"允许使用通知圆点"——然而这个玩意并没有卵用，我想对雷布斯说：I'm not ok!!!
                //  if ("MI 6".equals(Build.MODEL)) {
                //      intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                //      Uri uri = Uri.fromParts("package", getPackageName(), null);
                //      intent.setData(uri);
                //      // intent.setAction("com.android.settings/.SubSettings");
                //  }
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                // 出现异常则跳转到应用设置界面：锤子坚果3——OC105 API25
                Intent intent = new Intent();

                //下面这种方案是直接跳转到当前应用的设置界面。
                //https://blog.csdn.net/ysy950803/article/details/71910806
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                return true;
            }
        }
        return isOpened;
    }

    @RxBus.Subscribe(scheduler = RxBus.RunningThreadType.io)
    public void onDownProgress(ProgressEvent event) {
        if (event.isCompleted()) {
            Log.d(TAG, "下载完成");
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }
        if (!TextUtils.isEmpty(event.getErrorMsg())) {
            Log.e(TAG, "下载文件出错 ：" + event.getErrorMsg());
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
            return;
        }
        if (mProgressBarView != null) {
            mProgressBarView.setProgress(event.getProgress());
            if (mStartDownLoad != null) {
                mStartDownLoad.setEnabled(false);
            }
        }
    }

    private void createDownLoadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_download_layout, null);
        builder.setView(view);
        mProgressBarView = view.findViewById(R.id.dialogDownLoadProgress);
        mStartDownLoad = view.findViewById(R.id.dialogStartBtn);
        mCloseDialog = view.findViewById(R.id.dialogClose);
        mUpdateTip = view.findViewById(R.id.dialogUpdateTip);
        mDialog = builder.create();
        mDialog.setCancelable(false);
        mStartDownLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning(DownloadIntentService.class.getName())) {
                    Toast.makeText(DownLoadActivity.this, "正在下载", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(DownLoadActivity.this, DownloadIntentService.class);
                Bundle bundle = new Bundle();
                bundle.putString(DOWNLOAD_URL, downloadUrl);
                bundle.putInt(DOWNLOAD_ID, DOWNLOADAPK_ID);
                bundle.putString(DOWNLOAD_FILE, mDownloadFileName);
                intent.putExtras(bundle);
                startService(intent);
                mCloseDialog.setVisibility(View.GONE);
                mStartDownLoad.setEnabled(false);
            }
        });
        mCloseDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
            }
        });
        mDialog.show();
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager windowManager = window.getWindowManager();
            Display mDisplay = windowManager.getDefaultDisplay();
            WindowManager.LayoutParams mParams = window.getAttributes();
            window.setGravity(Gravity.CENTER);//设置显示在底部
            mParams.width = (int) (mDisplay.getWidth() * 0.8);//设置Dialog的宽度为屏幕宽度
            window.setAttributes(mParams);
        }
    }


    /**
     * 用来判断服务是否运行.
     *
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    private boolean isServiceRunning(String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = null;
        if (activityManager != null) {
            serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        }
        if (serviceList != null) {
            if (serviceList.size() == 0) {
                return false;
            } else {
                for (int i = 0; i < serviceList.size(); i++) {
                    if (serviceList.get(i).service.getClassName().equals(className)) {
                        isRunning = true;
                        break;
                    }
                }
            }
        }
        return isRunning;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.getInstance().unregister(this);
    }
}
