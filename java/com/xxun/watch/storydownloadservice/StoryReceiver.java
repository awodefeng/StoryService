package com.xxun.watch.storydownloadservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by zhangjun5 on 2017/11/17.
 */

public class StoryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MyTag", "onclock......................");
        String action = intent.getAction();

        Log.e("storyreceiver broast:",action);
        switch(action){
            case Const.ACTION_BROAST_STORY_FINISH:
                Intent reqIntent = new Intent(Const.ACTION_BROAST_MEDIA_SONG_STATUS_REQ);
                reqIntent.putExtra(Const.MEDIA_STORY_STATUE_PLAY_INFO,"0");
                context.sendBroadcast(reqIntent);
                //Intent reqIntent1 = new Intent(Const.ACTION_BROAST_MEDIA_STATUS_PAUSE);
                //context.sendBroadcast(reqIntent1);
                Intent _intent3 = new Intent(context,DownloadService.class);
                context.stopService(_intent3);
                System.exit(0);
                break;
            case Const.ACTION_BROAST_STORY_BEGIN:
                Intent _intent2 = new Intent(context,DownloadService.class);
                _intent2.putExtra("start_type","0");
                context.startService(_intent2);
                break;
            case Const.ACTION_BROAST_LOGIN_OK:
            case Const.ACTION_BROAST_SESSION_OK:
                Intent _intent4 = new Intent(context,DownloadService.class);
                _intent4.putExtra("start_type","1");
                context.startService(_intent4);
                break;
            case Const.ACTION_BROAST_STORY_DOWNLOAD:
                Log.e("story receiver",intent.getStringExtra("data"));
                Intent _intent5 = new Intent(context,DownloadService.class);
                _intent5.putExtra("start_type","2");
                _intent5.putExtra("data",intent.getStringExtra("data"));
                context.startService(_intent5);
                break;
            case Const.ACTION_BROAST_MEDIA_STATUS_PLAY:
                String fileName = intent.getStringExtra(Const.MEDIA_STORY_STATUE_INTENT_DATA);
                Log.e("play data",fileName);
                Intent _intent6 = new Intent(context,DownloadService.class);
                _intent6.putExtra("start_type","3");
                _intent6.putExtra(Const.MEDIA_STORY_STATUE_INTENT_DATA,fileName);
                context.startService(_intent6);
                break;
            case Const.ACTION_BROAST_STORY_GET_LIST:
                Log.e("story receiver",intent.getStringExtra("data"));
                Intent list_intent = new Intent(context,DownloadService.class);
                list_intent.putExtra("start_type","4");
                list_intent.putExtra("data",intent.getStringExtra("data"));
                context.startService(list_intent);
                break;
            case Const.ACTION_BROAST_STORY_CHOOSE_DELETE:
                Log.e("story receiver",intent.getStringExtra("data"));
                Intent choose_delete_intent = new Intent(context,DownloadService.class);
                choose_delete_intent.putExtra("start_type","5");
                choose_delete_intent.putExtra("data",intent.getStringExtra("data"));
                context.startService(choose_delete_intent);
                break;
        }
    }
}
