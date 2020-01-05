package com.xxun.watch.storydownloadservice;



import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.xxun.watch.storydownloadservice.IMyAidlBinderStory;
import com.xxun.watch.storydownloadservice.*;
import android.content.IntentFilter;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.content.Context;

public class StoryBinderService extends Service{
    private StoryReceiver storyReceiver;
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("yanbing","StoryBinderService");

        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel channel = new NotificationChannel("11", getText(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            Notification.Builder notification1 = new Notification.Builder(this);
            notification1.setSmallIcon(R.mipmap.ic_launcher);
            notification1.setTicker("11");
            notification1.setChannelId("11");
            notification = notification1.build();
            manager.notify(3,notification);
//                startForegroundService(notificationIntent);
        }else {
            notification = new Notification(R.mipmap.ic_launcher, getText(R.string.app_name),
                    System.currentTimeMillis());
            Intent notificationIntent = new Intent(this, DownloadService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            startForeground(1, notification);
        }

        storyReceiver =new StoryReceiver();
        IntentFilter bindIntentFilter = new IntentFilter();
        bindIntentFilter.addAction("com.xiaoxun.xxun.story.start");
        bindIntentFilter.addAction("com.xiaoxun.xxun.story.finish");
        //登录广播 动态注册
        bindIntentFilter.addAction("com.xiaoxun.sdk.action.SESSION_OK");
        bindIntentFilter.addAction("com.xiaoxun.sdk.action.LOGIN_OK");
//        bindIntentFilter.addAction("brocast.action.story.download.noti");
        bindIntentFilter.addAction("brocast.action.media.status.play");
//        bindIntentFilter.addAction("brocast.action.story.list");
//        bindIntentFilter.addAction("brocast.action.story.choose.delete");
        registerReceiver(storyReceiver,bindIntentFilter);



    }

    public StoryBinderService(){
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    private IBinder mBinder = new IMyAidlBinderStory.Stub() {
        @Override
        public boolean startService(String broadcast) throws RemoteException {
            Log.d("yanbing","mBinder OK ");
            return true;
        }
    };

}