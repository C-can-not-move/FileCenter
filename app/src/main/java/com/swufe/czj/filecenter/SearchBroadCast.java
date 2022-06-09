package com.swufe.czj.filecenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SearchBroadCast extends BroadcastReceiver {
    public static String mServiceKeyword = "";//关键字
    public static String mServiceSearchPath = "";//搜索路径
    @Override
    public void onReceive(Context context, Intent intent) {
        String mAction = intent.getAction();
        if(MainActivity.KEYWORD_BROADCAST.equals(mAction)){
            mServiceKeyword = intent.getStringExtra("keyword");
            mServiceSearchPath = intent.getStringExtra("searchpath");
        }
    }
}
