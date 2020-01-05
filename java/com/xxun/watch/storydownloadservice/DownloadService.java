package com.xxun.watch.storydownloadservice;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.media.MediaScannerConnection;

import com.xiaoxun.statistics.XiaoXunStatisticsManager;

import android.app.Notification;
import android.app.PendingIntent;

import com.xiaoxun.sdk.ResponseData;
import com.xiaoxun.sdk.IResponseDataCallBack;
import com.xiaoxun.sdk.XiaoXunNetworkManager;
import com.xiaoxun.sdk.utils.CloudBridgeUtil;

import android.app.NotificationChannel;
import android.app.NotificationManager;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.provider.Settings;
import android.content.Context;

import com.xiaoxun.sdk.utils.Constant;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

    private XiaoXunNetworkManager nerservice;
    private DownloadManager downloadManager;
    private static File baseDir;
    private static File FileDir;
    private ArrayList<StoryBeanData> storyloadList = new ArrayList<>();//下载列表
    private ArrayList<String> storyPlayerList = new ArrayList<>();//播放列表
    private static File MusicDir;
    private boolean isSendStoryList = false;
    private boolean isPauseByPhoneOrAlarm = false;//story is pause by phone or alram or message
    private XiaoXunStatisticsManager statisticsManager;
    private String statisticsTime;
    private Notification notification;

    private MediaPlayer mediaPlayer = null;
    private AudioManager audioMgr = null; // Audio管理器，用了控制音量
    private String curPlaySong = null;
    private int maxVolume = 0;
    private int curVolume = 0;
    private int stepVolume = 0;
    private int curSongPosition = 0;
    private int downloadStoryCount = 0;
    private int storyLTEModileCount = 0;

    //story tell 是否打开
    private boolean storyTellAppIsOn = false;

    private Timer reqTimer = null;

    public DownloadService() {
    }

    private void registerSilenceSQL() {
        getContentResolver().registerContentObserver(Settings.System.getUriFor("SilenceList_result"), true, mSilenceResultObserver);
    }

    final private ContentObserver mSilenceResultObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            String result = Settings.System.getString(getContentResolver(), "SilenceList_result");
            boolean SilenceList_result = (result == null ? false : Boolean.parseBoolean(result));
            Log.i(TAG, "onChange: SilenceList_result " + SilenceList_result);
            if (SilenceList_result) {
                PauseSong();
                sendStatueToStoryTall();
            }
        }
    };

    private void initPlayer() {
        mediaPlayer = new MediaPlayer();
        audioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                curSongPosition = 0;
                Log.e(TAG, "onCompletion：listSize：loadData "+curPlaySong
                        + storyPlayerList.size() + storyCheckUtil.isCanLoadDataFromServer());
                if (storyPlayerList.size() == 0 || curPlaySong == null || !storyCheckUtil.isCanLoadDataFromServer()) {
                    PauseSong();
                    sendStatueToStoryTall();
                    return;
                }
                int position = 0;
                for (String fileName : storyPlayerList) {
                    if (fileName.equals(curPlaySong)) {
                        break;
                    }
                    position++;
                }
                if (position >= storyPlayerList.size() - 1) {
                    position = 0;
                } else {
                    position++;
                }

                PlayLocalFile(storyPlayerList.get(position));
                sendStatueToStoryTall();
            }
        });

//        curVolume = Integer.valueOf(getStringValue(Const.SHARE_PREF_MEDIA_VOLUME,"-1"));
        // 获取最大音乐音量
        maxVolume = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 每次调整的音量大概为最大音量的1/5
        stepVolume = maxVolume / 5;
        // 初始化音量大概为2*stepVolume
        Log.e("volume", curVolume + ":");
        if (curVolume == -1) {
            curVolume = stepVolume * 2;
        }
        curVolume = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        adjustVolume();
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            Log.e("down focus", focusChange + "");
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    isPauseByPhoneOrAlarm = true;
                }
                FocusLostPauseSong();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//                Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_FINISH);
//                sendBroadcast(finishIntent);
                // Stop playback
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    isPauseByPhoneOrAlarm = true;
                }
                FocusLostPauseSong();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    isPauseByPhoneOrAlarm = true;
                }
                FocusLostPauseSong();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback or Raise it back to normal
                if (isPauseByPhoneOrAlarm) {
                    PlayLocalFile(curPlaySong);
                    sendStatueToStoryTall();
                }
            }
        }
    };

    private boolean requestFocus() {
        // Request audio focus for playback
        int result = audioMgr.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void adjustVolume() {
        audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume,
                AudioManager.FLAG_PLAY_SOUND);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("service command", flags + ":" + START_STICKY);
        if (intent != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel("11", getText(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
                Notification.Builder notification1 = new Notification.Builder(this);
                notification1.setSmallIcon(R.mipmap.ic_launcher);
                notification1.setTicker("11");
                notification1.setChannelId("11");
                notification = notification1.build();

                Intent notificationIntent = new Intent(this, DownloadService.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                manager.notify(3, notification);
//                startForegroundService(notificationIntent);
            } else {
                notification = new Notification(R.mipmap.ic_launcher, getText(R.string.app_name),
                        System.currentTimeMillis());
                Intent notificationIntent = new Intent(this, DownloadService.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                startForeground(1, notification);
            }

            String state_type = intent.getStringExtra("start_type");
            if ("0".equals(state_type)) {
                storyTellAppIsOn = true;
            } else if (state_type.equals("1")) {
                getStoryListValue("000000");
            } else if (state_type.equals("2")) {
                String recvData = intent.getStringExtra("data");
                downloadStoryBroast(this, recvData);
            } else if (state_type.equals("3")) {
                String fileName = intent.getStringExtra(Const.MEDIA_STORY_STATUE_INTENT_DATA);
                PlayLocalFile(fileName);
            } else if (state_type.equals("4")) {
                String recvData = intent.getStringExtra("data");
                handStoryListData(this, recvData);
            } else if (state_type.equals("5")) {
                String recvData = intent.getStringExtra("data");
                handStoryChooseDelete(this, recvData);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("downloadStoryService", intent.getAction());
            switch (intent.getAction()) {
                case Constant.ACTION_NET_SWITCH_SUCC:
                    finishLteInface();
                    getStoryListValue("000000");
                    break;
                case Const.ACTION_BROAST_STORY_CHANGE_SOUND:
                    String volume = intent.getStringExtra(Const.MEDIA_STORY_STATUE_SOUND_CHANGE);
//                    setStringValue(Const.SHARE_PREF_MEDIA_VOLUME,String.valueOf(volume));
                    if (mediaPlayer != null) {
                        curVolume = Integer.valueOf(volume);
                        adjustVolume();
//                        float volumeSize = Float.valueOf(volume)/15;
//                        Log.e("volumeSize",volumeSize+"");
//                        mediaPlayer.setVolume(volumeSize,volumeSize);

                    }
                    break;
                case Const.ACTION_BROAST_STORY_PREVIEW_FINISH:
                    if ("StoryTell".equals(intent.getStringExtra("from"))) {
                        storyTellAppIsOn = false;
                    }
                    if (mediaPlayer != null && mediaPlayer.isPlaying() || storyTellAppIsOn) {

                    } else {
                        Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_FINISH);
                        sendBroadcast(finishIntent);
                        stopSelf();
                        System.exit(0);
                    }
                    break;
                case Const.ACTION_BROAST_STORY_PAUSE_PLAY:
                case Const.ACTION_BROAST_PHONE_OUTGOING_CALL:
                case Const.ACTION_BROAST_HAVE_NEW_MESSAGE:
                case Const.ACTION_BROAST_ALARM_ALERT:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        isPauseByPhoneOrAlarm = true;
                    }
                    FocusLostPauseSong();
                    break;
                case Const.ACTION_BROAST_PHONE_STATE:
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                    Log.e("download phone state", telephonyManager.getCallState() + ":" + TelephonyManager.CALL_STATE_RINGING
                            + ":" + TelephonyManager.CALL_STATE_IDLE);
                    switch (telephonyManager.getCallState()) {
                        case TelephonyManager.CALL_STATE_RINGING:
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                isPauseByPhoneOrAlarm = true;
                            }
                            PauseSong();
                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            if (isPauseByPhoneOrAlarm) {
                                PlayLocalFile(curPlaySong);
                            }
                            break;
                    }
                    break;
                case Const.ACTION_BROAST_MEDIA_STATUS_DELETE:
                    initStoryList();
                    break;
                case Const.ACTION_BROAST_STORY_RESUME_PLAY:
                case Const.ACTION_BROAST_ALARM_DONE:
                    if (isPauseByPhoneOrAlarm) {
                        PlayLocalFile(curPlaySong);
                    }
                    Log.e("broast", "alarm_done");
                case Const.ACTION_BROAST_MEDIA_SONG_STATUS:
                    Log.e("broast", "song_status");
                    sendStatueToStoryTall();
                    break;
//                case Const.ACTION_BROAST_MEDIA_STATUS_PLAY:
//                    String fileName = intent.getStringExtra(Const.MEDIA_STORY_STATUE_INTENT_DATA);
//                    PlayLocalFile(fileName);
//                    break;
                case Const.ACTION_BROAST_MEDIA_STATUS_PAUSE:
                    PauseSong();
                    break;
//                case Const.ACTION_BROAST_LOGIN_OK:
//                case Const.ACTION_BROAST_SESSION_OK:
//                    Log.e("receive broast:",intent.getAction());
//                    getStoryListValue("000000");
//                    break;
//                case Const.ACTION_BROAST_STORY_DOWNLOAD:
//                    String recvData = intent.getStringExtra("data");
//                    downloadStoryBroast(recvData);
//                    break;

                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                    long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    int[] ints = getBytesAndStatus(completeDownloadId);
                    StoryBeanData beanData = getBeanDateByDownloadId(completeDownloadId);
                    if (beanData == null) {
                        return;
                    }
                    //下载完成，处理网络相关操作（wifi关闭，2G-4G网络切换问题）
                    downloadStoryCount--;
                    if (downloadStoryCount <= 0) {
                        downloadStoryCount = 0;
                    }
                    Settings.Global.putInt(getContentResolver(), "story_downloading_flag", downloadStoryCount);
                    Log.e("downloadservice", "downloadstaus1:" + Settings.Global.getInt(getContentResolver(), "story_downloading_flag", 99));

                    if (!getNetStateIsWifi()) {
                        Log.e("downloadservice", "close LTEMODE");
                        finishLteInface();
                    }

                    if (ints[2] == DownloadManager.STATUS_SUCCESSFUL) {
                        //发送下载成功的通知
                        String fileName = getFileNameByBeanData(beanData);
                        String path = getMusicDir() + "/" + fileName;
                        Log.e("TAG", fileName + ":" + path);
                        scanFile(getApplicationContext(), path);
                        sendStoryStatueToService(beanData, 1, 110, true, "0");
                        initStoryList();
                        storyloadList.remove(beanData);

                        Intent _intent = new Intent("action.update.story.notice");
                        sendBroadcast(_intent);
                    } else if (ints[2] == DownloadManager.STATUS_FAILED) {
                        //发送下载失败的通知
                        storyloadList.remove(beanData);
                        sendStoryStatueToService(beanData, 1, 115, true, "5");
                    }

                    Log.e("downloadservice", "onreceive" + intent.getAction() + ":" + completeDownloadId);
                    break;
            }
        }
    };

    private void startTimer() {
        reqTimer = new Timer(300000, new Runnable() {
            public void run() {
                if (reqTimer != null) {
                    cancleLteInface();
                    reqTimer.restart();
                }
            }
        });
        reqTimer.start();
    }

    private void cancelTimer() {
        if (reqTimer != null) {
            reqTimer.stop();
            reqTimer = null;
        }
    }

    private boolean startLteInface() {
        boolean is4GTo2G = nerservice.requireLTEMode("com.xxun.watch.storydownloadservice");
        storyLTEModileCount++;
        setStringValue(Const.MEDIA_STORY_LTEMODILE_Count,
                String.valueOf(storyLTEModileCount));
        return is4GTo2G;
    }

    private void finishLteInface() {
        nerservice.releaseLTEMode("com.xxun.watch.storydownloadservice");
        if (storyLTEModileCount > 0) {
            storyLTEModileCount--;
        } else {
            storyLTEModileCount = 0;
        }
        setStringValue(Const.MEDIA_STORY_LTEMODILE_Count,
                String.valueOf(storyLTEModileCount));
    }

    private void cancleLteInface() {
        Log.e("downloadservice", "cancle Lte inface");
        int loadSize = getDownloadStatus(downloadManager);
        Log.e("downloadservice", loadSize + ":" + storyLTEModileCount + ":" + getStringValue(Const.MEDIA_STORY_LTEMODILE_Count, "0"));
        if (loadSize == 0) {
            if (storyLTEModileCount > 0) {
                for (int i = 0; i < storyLTEModileCount; i++) {
                    storyLTEModileCount--;
                    nerservice.releaseLTEMode("com.xxun.watch.storydownloadservice");
                }
                if (storyLTEModileCount <= 0) {
                    storyLTEModileCount = 0;
                }
                setStringValue(Const.MEDIA_STORY_LTEMODILE_Count,
                        String.valueOf(storyLTEModileCount));
            }
            downloadStoryCount = 0;
            Settings.Global.putInt(getContentResolver(), "story_downloading_flag", downloadStoryCount);
            Log.e("cancleLteInface", "cancleLteInface:" + Settings.Global.getInt(getContentResolver(), "story_downloading_flag", 99));

        }
    }

    //获取当前的下载任务    
    private int getDownloadStatus(DownloadManager dm) {
        int downloadCount = 0;
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor c = dm.query(query);
        if (c == null || !c.moveToFirst()) {
            // 无下载内容
            return downloadCount;
        }
        do {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                // 如果已经下载，返回状态，同时直接提示安装
                String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

            } else if (status == DownloadManager.STATUS_RUNNING
                    || status == DownloadManager.STATUS_PAUSED
                    || status == DownloadManager.STATUS_PENDING) {
                downloadCount++;
                Log.e("downmanage running", "running");
            } else {
                // 失败也视为可以再次下载
                if (title != null)
                    Log.e("downmanage fails", "fails");
            }

        } while (c.moveToNext());
        return downloadCount;
    }

    private void sendStatueToStoryTall() {
        Intent reqIntent = new Intent(Const.ACTION_BROAST_MEDIA_SONG_STATUS_REQ);
        if (curPlaySong != null && !curPlaySong.equals("")) {
            Log.e("req return", curPlaySong);
            reqIntent.putExtra(Const.MEDIA_STORY_STATUE_INTENT_DATA, curPlaySong);
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            reqIntent.putExtra(Const.MEDIA_STORY_STATUE_PLAY_INFO, "1");
        } else {
            reqIntent.putExtra(Const.MEDIA_STORY_STATUE_PLAY_INFO, "0");
        }
        sendBroadcast(reqIntent);
    }

    public void setStringValue(String key, String value) {
        final SharedPreferences preferences = getSharedPreferences(Const.SHARE_PREF_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String getStringValue(String key, String defValue) {
        String str = getSharedPreferences(Const.SHARE_PREF_NAME, Context.MODE_PRIVATE)
                .getString(key, defValue);
        return str;
    }

    private void scanFile(Context context, String path) {
        MediaScannerConnection.scanFile(context, new String[]{path},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String s, Uri uri) {
                        Log.e("TAG", "download Story onScanCompleted");
                    }
                });
    }

    private void handStoryListData(Context context, String recvData) {
        JSONObject recvJson = (JSONObject) JSONValue.parse(recvData);

        JSONObject pl = (JSONObject) recvJson.get("PL");
        String sEid = (String) pl.get("EID");
        Log.e("story list", sEid);
        JSONObject storyContent = new JSONObject();
        JSONArray jsonArray = getStoryNamesList();
        storyContent.put("sub_action", 114);
        storyContent.put("filelist", jsonArray.toJSONString());
        storyContent.put("timestamp", getTimeStamp());
        String sendData = obtainE2ECloudMsgContent(
                Const.CID_E2E, nerservice.getMsgSN(), sEid, nerservice.getSID(), storyContent).toJSONString();
        nerservice.sendJsonMessage(sendData,
                new StoryCallBack() {
                    @Override
                    public void onSuccess(ResponseData responseData) {
                        Log.e("handStoryListData e2e_resp", responseData.toString());
                        try {
                            Log.e("ResponSe1:", responseData.toString());

                            if (storyloadList.size() == 0) {
                                //Const.ACTION_BROAST_STORY_FINISH
                                //Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_PREVIEW_FINISH);
                                //sendBroadcast(finishIntent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(int i, String s) {
                        Log.e("handStoryListData", s);
                    }
                }
        );
    }

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    private void handStoryChooseDelete(Context context, String recvData) {
        JSONObject recvJson = (JSONObject) JSONValue.parse(recvData);
        JSONObject pl = (JSONObject) recvJson.get("PL");
        String sEid = (String) pl.get("EID");
        JSONArray fileArray = (JSONArray) JSONValue.parse((String) pl.get("filelist"));
        try {
            boolean isDeleCurSong = false;
            for (int i = 0; i < fileArray.size(); i++) {
                String fileName = (String) fileArray.get(i);
                Log.e("fileName", fileName + ":" + curPlaySong);
                if (fileName.equals(curPlaySong)) {
                    isDeleCurSong = true;
                }

                String filePath = getMusicDir().getPath() + "/" + fileName;
                if (deleteFile(filePath)) {
                    Log.e("delete", filePath + " delete success");
                }
            }
            initStoryList();
            if (isDeleCurSong) {
                if (storyPlayerList.size() == 0) {
                    curPlaySong = getString(R.string.no_play_name);
                    setStringValue(Const.MEDIA_STORY_CUR_PLAY_SONG, getString(R.string.no_play_name));
                    PauseSong();
                } else {
                    curPlaySong = storyPlayerList.get(0);
                    setStringValue(Const.MEDIA_STORY_CUR_PLAY_SONG, storyPlayerList.get(0));
                    if (mediaPlayer.isPlaying()) {
                        curSongPosition = 0;
                        PlayLocalFile(curPlaySong);
                    }
                }
                sendStatueToStoryTall();
            }
        } catch (Exception e) {
            Log.e("delete exception", e.toString());
        }

        JSONObject storyContent = new JSONObject();
        storyContent.put("sub_action", 115);
        storyContent.put("rc", 1);
        storyContent.put("timestamp", getTimeStamp());
        String sendData = obtainE2ECloudMsgContent(
                Const.CID_E2E, nerservice.getMsgSN(), sEid, nerservice.getSID(), storyContent).toJSONString();
        Log.e("sendData:", sendData);
        nerservice.sendJsonMessage(sendData,
                new StoryCallBack() {
                    @Override
                    public void onSuccess(ResponseData responseData) {
                        Log.e("handStoryListData e2e_resp", responseData.toString());
                        try {
                            Log.e("ResponSe1:", responseData.toString());

                            if (storyloadList.size() == 0) {
                                //Const.ACTION_BROAST_STORY_FINISH
                                Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_PREVIEW_FINISH);
                                sendBroadcast(finishIntent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(int i, String s) {
                        Log.e("handStoryListData", s);
                    }
                }
        );
    }

    private void downloadStoryBroast(Context context, String recvData) {
        JSONObject recvJson = (JSONObject) JSONValue.parse(recvData);
        JSONObject pl = (JSONObject) recvJson.get("PL");
        StoryBeanData storyBeanData = new StoryBeanData();
        storyCheckUtil.transJsonToStoryBean(pl, storyBeanData);
        long sdSize = storyCheckUtil.getAvailSpace(Environment.getExternalStorageDirectory().getAbsolutePath());//外部存储大小
        if (sdSize <= 1024 * 1024 * 20) {
            //发送下载失败的通知 内存不足提醒
            sendStoryStatueToService(storyBeanData, 1, 115, true, "2");
            return;
        }

        String storyOnlyWifi = android.provider.Settings.System.getString(getContentResolver(), Const.KEY_NAME_STORY_WIFI_ONLY);
        if (!"1".equals(storyOnlyWifi) || getNetStateIsWifi()) {
            if (!storyCheckUtil.checkStoryRepeat(storyloadList, storyPlayerList, storyBeanData)) {
                Log.e("aaaaaaa", "写入列表下载故事" + storyBeanData.getStoryFile());
                storyloadList.add(storyBeanData);

                downloadStoryControl(downloadManager, storyloadList, storyBeanData/*, storyOnlyWifi*/);
            } else {
                Log.e("aaaaaaa", "故事下载失败");
                //发送下载失败的通知  数据重复
                sendStoryStatueToService(storyBeanData, 1, 115, true, "3");
            }
        }
        //保存时间戳
        String updateTs = (String) pl.get("updateTS");
        if (updateTs != null && !updateTs.equals("")) {
//                            setValue(Const.KEY_NAME_LASTTS, getStoryLastTimeStamp());
        }
    }

    //play the song
    private void PlayLocalFile(String fileName) {
        requestFocus();//获取音频焦点
        try {
            String result = Settings.System.getString(getContentResolver(), "SilenceList_result");
            boolean SilenceList_result = (result == null ? false : Boolean.parseBoolean(result));
            Log.e("ContentObserver", "SilenceList_result" + SilenceList_result);
            if (SilenceList_result) {
                PauseSong();
                sendStatueToStoryTall();
                Toast.makeText(getApplicationContext(), getString(R.string.slient_time_story), Toast.LENGTH_SHORT).show();
                return;
            }

            if (mediaPlayer == null) {
                Log.e("mediaPlayer", "null");
                initPlayer();
                sendStatueToStoryTall();
                return;
            }

            curVolume = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
            adjustVolume();
            isPauseByPhoneOrAlarm = false;
            Log.e("curplay/filePath/curSongPosition", curPlaySong + "/" + fileName + "/" + curSongPosition);
            setStringValue(Const.MEDIA_STORY_CUR_PLAY_SONG, fileName);
            if (curPlaySong != null && curPlaySong.equals(fileName)) {
                curPlaySong = fileName;
                mediaPlayer.reset();
                String filePath = getMusicDir().getPath() + "/" + fileName;
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(curSongPosition);
                mediaPlayer.start();
            } else {
                curSongPosition = 0;
                curPlaySong = fileName;
                mediaPlayer.reset();
                String filePath = getMusicDir().getPath() + "/" + fileName;
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepare();
                mediaPlayer.start();
            }

            String curTimeStamp = getTimeStamp();
            if (statisticsTime != null) {
                long timeStampSec = storyCheckUtil.calcTwoTimeStampInterval(statisticsTime, curTimeStamp);
                Log.e("stepscount MainActivity", timeStampSec + "" + (int) timeStampSec + ":" + statisticsTime + ":" + curTimeStamp);
                statisticsManager.stats(XiaoXunStatisticsManager.STATS_STORY_TIME, (int) timeStampSec);
            }
            statisticsTime = curTimeStamp;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void PauseSong() {
        audioMgr.abandonAudioFocus(afChangeListener);//释放音频焦点
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (statisticsTime != null) {
                String curTimeStamp = getTimeStamp();
                long timeStampSec = storyCheckUtil.calcTwoTimeStampInterval(statisticsTime, curTimeStamp);
                Log.e("stepscount MainActivity", timeStampSec + "" + (int) timeStampSec + ":" + statisticsTime + ":" + curTimeStamp);
                statisticsManager.stats(XiaoXunStatisticsManager.STATS_STORY_TIME, (int) timeStampSec);
            }
            curSongPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    private void FocusLostPauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (statisticsTime != null) {
                String curTimeStamp = getTimeStamp();
                long timeStampSec = storyCheckUtil.calcTwoTimeStampInterval(statisticsTime, curTimeStamp);
                Log.e("stepscount MainActivity", timeStampSec + "" + (int) timeStampSec + ":" + statisticsTime + ":" + curTimeStamp);
                statisticsManager.stats(XiaoXunStatisticsManager.STATS_STORY_TIME, (int) timeStampSec);
            }
            curSongPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    private static String getFileNameByBeanData(StoryBeanData beanData) {
        String fileName;
        JSONObject jsonObject = (JSONObject) (JSONValue.parse(beanData.getStroyData()));
        String dataJson = jsonObject.get("track_id").toString();
        if (beanData.getStoryUrl().contains(".amr")) {
            fileName = beanData.getStoryFile() + "_" + dataJson + ".amr";
        } else if (beanData.getStoryUrl().contains(".mp3")) {
            fileName = beanData.getStoryFile() + "_" + dataJson + ".mp3";
        } else if (beanData.getStoryUrl().contains(".m4a")) {
            fileName = beanData.getStoryFile() + "_" + dataJson + ".m4a";
        } else {
            fileName = beanData.getStoryFile() + "_" + dataJson + ".mp3";
        }
        return fileName;
    }

    public void downloadStoryControl(DownloadManager downloadManager, ArrayList<StoryBeanData> storyloadList,
                                     StoryBeanData beanData/*, String onlyWifi*/) {
//        String apkUrl = "http://fdfs.xmcdn.com/group32/M08/46/20/wKgJS1mxL3-QuR-kABV9gmy4S-4916.mp3";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(beanData.getStoryUrl()));
        String fileName = getFileNameByBeanData(beanData);
        downloadStoryCount++;
        Settings.Global.putInt(getContentResolver(), "story_downloading_flag", downloadStoryCount);
        Log.e("downloadservice", "downloadstaus0:" + Settings.Global.getInt(getContentResolver(), "story_downloading_flag", 99));

//        Log.e("download onlyWif", onlyWifi + ":" + downloadStoryCount);
//        if (onlyWifi.equals("1")) {
//            if (getNetStateIsWifi()) {
//
//            } else {
//                Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_PREVIEW_FINISH);
//                sendBroadcast(finishIntent);
//                return;
//            }
//        } else {
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
//        }
        if (!getNetStateIsWifi()) {
            boolean is4GTo2G = startLteInface();
            Log.e("downloadservice", "openLTEMODE" + ":" + is4GTo2G);
            if (is4GTo2G) {
                storyloadList.remove(beanData);
                return;
            }
        }

        request.setDestinationInExternalPublicDir("Music", fileName);
        long downloadId = downloadManager.enqueue(request);
        beanData.setDownloadId(downloadId);
        storyloadList.remove(beanData);
        storyloadList.add(beanData);

        Log.e("downing story num:", "" + getDownloadStatus(downloadManager));

    }

    private boolean getNetStateIsWifi() {
        ConnectivityManager mConnect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnect.getActiveNetworkInfo();
        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    public void sendStoryStatueToService(final StoryBeanData storyBeanData, int newOptype, final int newStatus,
                                         final boolean isNoticeService, final String reason) {
        JSONObject storyContent = storyCheckUtil.getStoryListToJSONArray(storyBeanData, newOptype,
                newStatus, storyBeanData.getStoryUpdateTS(), reason);
        String sendData = obtainCloudMsgContent(
                Const.CID_STORY_STATUE, nerservice.getMsgSN(), nerservice.getSID(), storyContent).toJSONString();
        nerservice.sendJsonMessage(sendData,
                new StoryDownloadStatusCallBack(this, reason, newStatus, storyBeanData)
        );
    }

    public class StoryCallBack extends IResponseDataCallBack.Stub {
        @Override
        public void onSuccess(ResponseData responseData) {
        }

        @Override
        public void onError(int i, String s) {
        }
    }

    private class StoryDownloadStatusCallBack extends IResponseDataCallBack.Stub {

        private Context context;
        private String reason;
        private int newStatus;
        private StoryBeanData storyBeanData;

        public StoryDownloadStatusCallBack(Context context, String reason,
                                           int newStatus, StoryBeanData storyBeanData) {
            this.context = context;
            this.reason = reason;
            this.newStatus = newStatus;
            this.storyBeanData = storyBeanData;
        }

        @Override
        public void onSuccess(ResponseData responseData) {
            try {
                Log.e("ResponSe1:", responseData.toString());
                JSONObject jsonObject = (JSONObject) JSONValue.parse(responseData.getResponseData());
                int responRc = (int) jsonObject.get("RC");
                if (responRc == 1) {
                    JSONObject pl = (JSONObject) jsonObject.get("PL");
                    String updateTs = (String) pl.get(Const.KEY_NAME_UPDATETS);
                    if (true) {
                        if (newStatus == 110) {
                            JSONObject storyContent = storyCheckUtil.getStoryListToJSONArray(storyBeanData, 1, 100, updateTs, reason);
                            sendStoryNoticeMsgToService(storyContent);
                        } else {
                            JSONObject storyContent = storyCheckUtil.getStoryListToJSONArray(storyBeanData, 1, 105, updateTs, reason);
                            sendStoryNoticeMsgToService(storyContent);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(int i, String s) {
            Log.e("MyReceiver", "onError" + i + ":" + s);
        }

    }

    public void sendStoryNoticeMsgToService(JSONObject content) {
        JSONObject sendJson = new JSONObject();
        sendJson.put(Const.KEY_NAME_TGID, nerservice.getWatchGid());

        StringBuilder key = new StringBuilder("GP/");
        key.append(nerservice.getWatchGid());
        key.append("/MSG/");
        key.append(getReversedOrderTime(0));
        sendJson.put(Const.KEY_NAME_KEY, key.toString());

        JSONObject sendList = new JSONObject();
        sendList.put(Const.KEY_NAME_EID, nerservice.getWatchEid());
        sendList.put(Const.KEY_NAME_TYPE, "download");
        sendList.put(Const.KEY_NAME_CONTENT, content.toString());
        sendList.put(Const.KEY_NAME_DURATION, 100);
        sendJson.put("Value", sendList);
        String sendData = obtainCloudMsgContent(
                Const.CID_STORY_UPLOAD, nerservice.getMsgSN(), nerservice.getSID(), sendJson).toJSONString();
        nerservice.sendJsonMessage(sendData,
                new StoryNoticeMsgStatusCallBack(this)
        );
    }

    private class StoryNoticeMsgStatusCallBack extends IResponseDataCallBack.Stub {

        private Context context;

        public StoryNoticeMsgStatusCallBack(Context context) {
            this.context = context;
        }

        @Override
        public void onSuccess(ResponseData responseData) {
            try {
                Log.e("ResponSe1:", responseData.toString());

                if (storyloadList.size() == 0) {
                    //Const.ACTION_BROAST_STORY_FINISH
                    Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_PREVIEW_FINISH);
                    sendBroadcast(finishIntent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(int i, String s) {
            Log.e("MyReceiver", "onError" + i + ":" + s);
        }

    }

    public static String getReversedOrderTime(long time) {
        StringBuilder timeStamp = new StringBuilder();
        String test = null;
        if (time > 0) {
            test = getTimeStamp(time);
        } else {
            test = getTimeStamp();
        }

        timeStamp.append(String.format("%1$08d", Const.YMD_REVERSED_MASK_8 - Integer.parseInt(test.substring(0, 8))));
        timeStamp.append(String.format("%1$09d", Const.HMSS_REVERSED_MASK_9 - Integer.parseInt(test.substring(8, 17))));
        return timeStamp.toString();
    }

    public static String getTimeStamp(long time) {
        String timeStamp = null;
        if (time > 0) {
            Date d = new Date(time);
            DateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            timeStamp = format.format(d).toString();
        }
        return timeStamp;
    }

    public static String getTimeStamp() {
        Date d = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        return format.format(d).toString();
    }

    public static JSONObject obtainCloudMsgContent(int cid, int sn, String sid, Object pl) {
        JSONObject msg = new JSONObject();
        msg.put("Version", CloudBridgeUtil.PROTOCOL_VERSION);
        msg.put("CID", cid);
        if (sid != null) {
            msg.put(Const.KEY_NAME_SID, sid);
        }
        msg.put("SN", sn);
        if (pl != null) {
            msg.put("PL", pl);
        }
        return msg;
    }

    public static JSONObject obtainE2ECloudMsgContent(int cid, int sn, String tEid, String sid, Object pl) {
        JSONObject msg = new JSONObject();
        msg.put("Version", CloudBridgeUtil.PROTOCOL_VERSION);
        msg.put("CID", cid);
        if (tEid != null) {
            String[] teid = new String[1];
            teid[0] = tEid;
            msg.put("TEID", teid);
        }
        if (sid != null) {
            msg.put(Const.KEY_NAME_SID, sid);
        }
        msg.put("SN", sn);
        if (pl != null) {
            msg.put("PL", pl);
        }
        return msg;
    }

    public StoryBeanData getBeanDateByDownloadId(long downloadId) {
        StoryBeanData beanData = null;
        for (int i = 0; i < storyloadList.size(); i++) {
            StoryBeanData curBean = storyloadList.get(i);
            if (curBean.getDownloadId() == downloadId) {
                beanData = curBean;
                break;
            }
        }
        return beanData;
    }

    public int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{-1, -1, 0};
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                bytesAndStatus[0] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                bytesAndStatus[1] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                bytesAndStatus[2] = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return bytesAndStatus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("sercice", "onCreate");
        nerservice = (XiaoXunNetworkManager) getSystemService("xun.network.Service");
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        statisticsManager = (XiaoXunStatisticsManager) getSystemService("xun.statistics.service");

        initFileDirs();
        initStoryList();
        initPlayer();
        startTimer();
        curPlaySong = getStringValue(Const.MEDIA_STORY_CUR_PLAY_SONG, getString(R.string.no_play_name));
        storyLTEModileCount = Integer.valueOf(getStringValue(Const.MEDIA_STORY_LTEMODILE_Count, "0"));
        registerSilenceSQL();

//        getStoryListValue("000000");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        intentFilter.addAction(Const.ACTION_BROAST_MEDIA_STATUS_PLAY);
        intentFilter.addAction(Const.ACTION_BROAST_MEDIA_STATUS_PAUSE);
        intentFilter.addAction(Const.ACTION_BROAST_MEDIA_SONG_STATUS);
        intentFilter.addAction(Const.ACTION_BROAST_MEDIA_STATUS_DELETE);
        intentFilter.addAction(Const.ACTION_BROAST_HAVE_NEW_MESSAGE);
        intentFilter.addAction(Const.ACTION_BROAST_ALARM_ALERT);
        intentFilter.addAction(Const.ACTION_BROAST_ALARM_DONE);
        intentFilter.addAction(Const.ACTION_BROAST_STORY_RESUME_PLAY);
        intentFilter.addAction(Const.ACTION_BROAST_STORY_PAUSE_PLAY);
        intentFilter.addAction(Const.ACTION_BROAST_PHONE_OUTGOING_CALL);
        intentFilter.addAction(Const.ACTION_BROAST_PHONE_STATE);
        intentFilter.addAction(Const.ACTION_BROAST_STORY_PREVIEW_FINISH);
        intentFilter.addAction(Const.ACTION_BROAST_STORY_CHANGE_SOUND);
        intentFilter.addAction(Constant.ACTION_NET_SWITCH_SUCC);
        registerReceiver(broadcastReceiver, intentFilter);
        sendStatueToStoryTall();
        cancleLteInface();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            startForeground(1, notification);//service销毁时重新起来前台
            unregisterReceiver(broadcastReceiver);
            audioMgr.abandonAudioFocus(afChangeListener);
            getContentResolver().unregisterContentObserver(mSilenceResultObserver);
            cancelTimer();
            cancleLteInface();
        } catch (Exception e) {
            Log.e("exception", e.toString());
        }
    }

    private void getStoryListValue(String lastTs) {
        if (!isSendStoryList) {
            isSendStoryList = true;
        } else {
            return;
        }
        JSONObject pl = new JSONObject();
        pl.put(Const.KEY_NAME_EID, nerservice.getWatchEid());
        pl.put(Const.KEY_NAME_GID, nerservice.getWatchGid());
        pl.put(Const.KEY_NAME_LASTTS, lastTs);
//        JSONArray jsonArray = storyCheckUtil.getStoryListToJSONArray(storyloadList);
        JSONArray jsonArray = new JSONArray();
        pl.put(Const.KEY_NAME_ARRAY, jsonArray);
        String sendData = obtainCloudMsgContent(
                Const.CID_GETSTORY_LIST, nerservice.getMsgSN(), nerservice.getSID(), pl).toJSONString();
        Log.e("storyList:", "downloadService senddate:" + sendData);
        nerservice.sendJsonMessage(sendData,
                new StoryListMsgCallBack(this));
    }

    private class StoryListMsgCallBack extends IResponseDataCallBack.Stub {

        private Context context;

        public StoryListMsgCallBack(Context context) {
            this.context = context;
        }

        @Override
        public void onSuccess(ResponseData responseData) {
            try {
                isSendStoryList = false;
                Log.e("success:", "responseData:" + responseData.toString());
                JSONObject jsonObject = (JSONObject) JSONValue.parse(responseData.getResponseData());
                int responRc = (int) jsonObject.get("RC");
                if (responRc == 1) {
                    JSONObject pl = (JSONObject) jsonObject.get("PL");
                    JSONArray storyList = (JSONArray) pl.get("List");
                    Log.e("故事列表的数据为：", storyList.size() + "");
                    if (storyList.size() > 0) {
                        boolean is4GTo2G = startLteInface();
                        if (is4GTo2G) {
                            return;
                        } else {
                            finishLteInface();
                        }
                    }
                    for (int i = 0; i < storyList.size(); i++) {
                        JSONObject storyData = (JSONObject) storyList.get(i);
                        StoryBeanData storyBeanData = new StoryBeanData();
                        storyCheckUtil.transJsonToStoryBean(storyData, storyBeanData);
                        long sdSize = storyCheckUtil.getAvailSpace(Environment.getExternalStorageDirectory().getAbsolutePath());//外部存储大小
                        if (sdSize <= 1024 * 1024 * 50) {
                            //发送下载失败的通知 内存不足提醒
                            sendStoryStatueToService(storyBeanData, 1, 115, false, "2");
                        }

                        String storyOnlyWifi = android.provider.Settings.System.getString(getContentResolver(), Const.KEY_NAME_STORY_WIFI_ONLY);
                        if (!"1".equals(storyOnlyWifi) || getNetStateIsWifi()) {
                            if (!storyCheckUtil.checkStoryRepeat(storyloadList, storyPlayerList, storyBeanData)) {
                                Log.e("写入列表下载故事", storyBeanData.getStoryFile());
                                storyloadList.add(storyBeanData);
                                //开始下载故事
                                downloadStoryControl(downloadManager, storyloadList, storyBeanData/*, storyOnlyWifi*/);
                            } else {
                                Log.e("gushi:", storyBeanData.getStoryFile() + "故事下载失败");
                                //发送失败的推送  重复的故事
                                sendStoryStatueToService(storyBeanData, 1, 115, false, "3");
                            }
                        }
                    }
                    //保存时间戳
//                    String updateTs = (String) pl.get("updateTS");
//                    if (updateTs != null && !updateTs.equals("")) {
//                    }
                    if (storyloadList.size() == 0) {
                        Intent finishIntent = new Intent(Const.ACTION_BROAST_STORY_PREVIEW_FINISH);
                        sendBroadcast(finishIntent);
                    }
                } else {
                    Log.e("CallBack", "数据返回失败：" + responRc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(int i, String s) {
            Log.e("DownloadService", "onError" + i + ":" + s);
        }

    }

    public static File getFileDir() {
        if (!FileDir.isDirectory()) {
            FileDir.delete();
            FileDir.mkdirs();
        }
        return FileDir;
    }

    public static File getMusicDir() {
        if (!MusicDir.isDirectory()) {
            MusicDir.delete();
            MusicDir.mkdirs();
        }
        return MusicDir;
    }

    public void initStoryList() {
        storyPlayerList = new ArrayList<>();
        String filePath = getMusicDir().getPath();
        // 得到该路径文件夹下所有的文件
        File fileAll = new File(filePath);
        File[] files = fileAll.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.getName().contains(".mp3")
                    || file.getName().contains(".amr")
                    || file.getName().contains(".m4a")) {
                storyPlayerList.add(file.getName());
            }
        }
        curPlaySong = getStringValue(Const.MEDIA_STORY_CUR_PLAY_SONG, getString(R.string.no_play_name));
        if (curPlaySong.equals(getString(R.string.no_play_name)) && storyPlayerList.size() > 0) {
            setStringValue(Const.MEDIA_STORY_CUR_PLAY_SONG, storyPlayerList.get(0));
        }
    }

    private JSONArray getStoryNamesList() {
        JSONArray nameLists = new JSONArray();
        initStoryList();
        for (String fileName : storyPlayerList) {
            nameLists.add(fileName);
        }
        return nameLists;
    }

    private void initFileDirs() {
        Log.e("service:", Environment.MEDIA_MOUNTED + ":" + Environment.getExternalStorageState());
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (baseDir == null)
                baseDir = new File(Environment.getExternalStorageDirectory(), Const.MY_BASE_DIR);
            else
                baseDir = new File(baseDir.getPath());

            if (baseDir.exists() && !baseDir.isDirectory()) {
                baseDir.delete();
            }
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            FileDir = new File(baseDir, Const.MY_FILE_DIR);
            if (FileDir.exists() && !FileDir.isDirectory()) {
                FileDir.delete();
            }

            if (!FileDir.exists()) {
                FileDir.mkdir();
            }

            if (MusicDir == null) {
                MusicDir = new File(Environment.getExternalStorageDirectory(), Const.MY_MUSIC_DIR);
            } else
                MusicDir = new File(baseDir.getPath());

            if (MusicDir.exists() && !MusicDir.isDirectory()) {
                MusicDir.delete();
            }
            if (!MusicDir.exists()) {
                MusicDir.mkdirs();
            }
        }
    }


}
