package com.xxun.watch.storydownloadservice;

/**
 * Created by zhangjun5 on 2017/11/17.
 */

public class Const {
    public static final String MY_MUSIC_DIR = "Music";
    public static final String MY_BASE_DIR ="StoryTell";
    public static final String MY_LOG_DIR ="logs";
    public static final String MY_FILE_DIR ="files";

    public static final String SHARE_PREF_NAME = "story_service_share";

    public static final String KEY_NAME_SID = "SID";
    public static final String KEY_NAME_EID = "EID";
    public static final String KEY_NAME_GID = "GID";
    public static final String KEY_NAME_TYPE = "Type";
    public static final String KEY_NAME_CONTENT = "Content";
    public static final String KEY_NAME_DURATION = "Duration";
    public static final String KEY_NAME_TGID = "TGID";
    public static final String KEY_NAME_UPDATETS = "updateTS";
    public static final String KEY_NAME_LASTTS = "lastTS";
    public static final String KEY_NAME_ARRAY = "array";
    public static final String KEY_NAME_KEY = "Key";

    public static final String pref_file_name = "StoryFile";
    public static final String KEY_NAME_STORY_WIFI_ONLY ="story_dl_opt";

    public static final String ACTION_BROAST_BOOT_START = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_BROAST_STORY_DOWNLOAD = "brocast.action.story.download.noti";
    public static final String ACTION_BROAST_STORY_GET_LIST = "brocast.action.story.list";
    public static final String ACTION_BROAST_STORY_CHOOSE_DELETE = "brocast.action.story.choose.delete";
    public static final String ACTION_BROAST_SESSION_OK  = "com.xiaoxun.sdk.action.SESSION_OK";
    public static final String ACTION_BROAST_LOGIN_OK  = "com.xiaoxun.sdk.action.LOGIN_OK";
    public static final String ACTION_BROAST_ALARM_ALERT  = "com.xxun.xunalarm.ringtone.action.STARTALARM";
    public static final String ACTION_BROAST_ALARM_DONE  = "com.xxun.xunalarm.ringtone.action.FINISHALARM";
    public static final String ACTION_BROAST_HAVE_NEW_MESSAGE  = "com.broadcast.xxun.newMessage";
    public static final String ACTION_BROAST_PHONE_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";
    public static final String ACTION_BROAST_PHONE_STATE = "android.intent.action.PHONE_STATE";
    public static final String ACTION_BROAST_STORY_BEGIN = "com.xiaoxun.xxun.story.start";
    public static final String ACTION_BROAST_STORY_FINISH = "com.xiaoxun.xxun.story.finish";
    public static final String ACTION_BROAST_STORY_PREVIEW_FINISH = "com.xiaoxun.xxun.story.preview.finish";
    public static final String ACTION_BROAST_STORY_CHANGE_SOUND = "com.xiaoxun.xxun.story.change.sound";
    public static final String ACTION_BROAST_STORY_RESUME_PLAY = "com.xiaoxun.xxun.story.resume.play";
    public static final String ACTION_BROAST_STORY_PAUSE_PLAY = "com.xiaoxun.xxun.story.pause.play";

    public static final String ACTION_BROAST_MEDIA_STATUS_PLAY = "brocast.action.media.status.play";
    public static final String ACTION_BROAST_MEDIA_STATUS_PAUSE = "brocast.action.media.status.pause";
    public static final String ACTION_BROAST_MEDIA_STATUS_DELETE = "brocast.action.media.status.delete";
    public static final String ACTION_BROAST_MEDIA_SONG_STATUS = "brocast.action.media.song.status";
    public static final String ACTION_BROAST_MEDIA_SONG_STATUS_REQ = "brocast.action.media.song.status.req";

    public static final String MEDIA_STORY_STATUE_INTENT_DATA = "song_name";
    public static final String MEDIA_STORY_STATUE_PLAY_INFO = "is_play";
    public static final String MEDIA_STORY_STATUE_SOUND_CHANGE = "sound_change";
    public static final String SHARE_PREF_MEDIA_VOLUME = "share_pref_media_volume";
    public static final String MEDIA_STORY_CUR_PLAY_SONG = "cur_play_song";
    public static final String MEDIA_STORY_LTEMODILE_Count = "storyLTEModileCount";

    public static final int CID_STORY_STATUE = 70161;
    public static final int CID_GETSTORY_LIST = 70171;
    public static final int CID_STORY_UPLOAD = 70081;
    public static final int CID_E2E = 30011;
    public static final int CID_E2E_RES = 30012;

    public static final long YMD_REVERSED_MASK_8 = 99999999;
    public static final long HMSS_REVERSED_MASK_9 = 999999999;

}
