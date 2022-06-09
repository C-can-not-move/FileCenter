package com.swufe.czj.filecenter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

public class FileService extends Service {
    private Looper mLooper;
    private FileHandler mFileHandler;
    private ArrayList<String> mFileName = null;
    private ArrayList<String> mFilePaths = null;
    public static final String FILE_SEARCH_COMPLETED = "filecenter.file.FILE_SEARCH_COMPLETED";
    public static final String FILE_NOTIFICATION = "filecenter.file.FILE_NOTIFICATION";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //创建服务
    @Override
    public void onCreate(){
        super.onCreate();
        Log.i("FileService","文件查找服务已创建");
        //线程
        HandlerThread mHT = new HandlerThread("FileService",HandlerThread.NORM_PRIORITY);
        mHT.start();
        mLooper = mHT.getLooper();
        mFileHandler = new FileHandler(mLooper);
    }
    @Override
    public void onStart(Intent intent,int startId){
        super.onStart(intent,startId);
        Log.i("FileService","文件查找服务在启动");
        mFileName = new ArrayList<String>();
        mFilePaths = new ArrayList<String>();
        mFileHandler.sendEmptyMessage(0);
        fileSearchNotification();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        mNF.cancel(R.string.app_name);
    }

    class FileHandler extends Handler{
        public FileHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            Log.i("FileService","文件查找服务正在查找");
            //在指定范围搜索
            initFileArray(new File(SearchBroadCast.mServiceSearchPath));
            //当用户单击了取消搜索则不发生广播
            if(!MainActivity.isComeBackFromNotification==true){
                Intent intent = new Intent(FILE_SEARCH_COMPLETED);
                intent.putStringArrayListExtra("mFileNameList",mFileName);
                intent.putStringArrayListExtra("mFilePathsList",mFilePaths);
                //搜素完毕后携带数据并发送广播
                sendBroadcast(intent);
            }
        }
    }
    private int m = -1;
    /**
     * 可回调函数
     */
    private void initFileArray(File file){
        Log.i("FileService","文件查找服务正在查找"+file.getPath());
        if(file.canRead()){
            File[] mFileArray = file.listFiles();
            for(File currentArray:mFileArray){
                if(currentArray.getName().indexOf(SearchBroadCast.mServiceKeyword)!=-1){
                    if(m==-1){
                        m++;
                        //返回搜索之前的目录
                        mFileName.add("BacktoSearchBefore");
                        mFilePaths.add(MainActivity.mCurrentFilePath);
                    }
                    mFileName.add(currentArray.getName());
                    mFilePaths.add(currentArray.getPath());
                }
                //文件夹
                if(currentArray.exists()&&currentArray.isDirectory()){
                    //如果用户取消应该立即停止搜索
                    if(!MainActivity.isComeBackFromNotification==true){
                        return;
                    }
                    initFileArray(currentArray);
                }
            }
        }
    }
    NotificationManager mNF;
    /**
     * 通知
     */
    private void fileSearchNotification(){
        Intent intent = new Intent(FILE_NOTIFICATION);
        //点击notice
        intent.putExtra("notification","啦啦啦啦啦");
        PendingIntent mPI = PendingIntent.getBroadcast(this,0,intent,0);
        Notification notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle("title")
                .setContentText("搜索中。。。")
                .setContentIntent(mPI)
                .setSmallIcon(R.drawable.logo)
                .setWhen(System.currentTimeMillis())
                .build();
        if(mNF == null){
            mNF = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        }
        mNF.notify(R.string.app_name,notification);
    }
}
