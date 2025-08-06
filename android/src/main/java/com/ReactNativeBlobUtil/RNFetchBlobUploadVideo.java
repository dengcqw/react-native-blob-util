package com.ReactNativeBlobUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.RNFetchBlob.videoupload.TXUGCPublish;
import com.RNFetchBlob.videoupload.TXUGCPublishTypeDef;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class RNFetchBlobUploadVideo extends BroadcastReceiver implements Runnable {

    public static HashMap<String, TXUGCPublish> taskTable = new HashMap<>();
    ReadableMap options;
    String taskId;
    Callback callback;

    public RNFetchBlobUploadVideo(ReadableMap options, String taskId, final Callback callback) {
        this.options = options;
        this.taskId = taskId;
        this.callback = callback;
    }


    @Override
    public void run() {
        uploadFile(this.options);
    }

    private void releaseTaskResource() {
        this.callback = null;
        if(RNFetchBlobUploadVideo.taskTable.containsKey(taskId)) {
            TXUGCPublish publish = RNFetchBlobUploadVideo.taskTable.get(taskId);
            publish.canclePublish();
            RNFetchBlobUploadVideo.taskTable.remove(taskId);
        }
        if(ReactNativeBlobUtilReq.uploadProgressReport.containsKey(taskId)) {
            ReactNativeBlobUtilReq.uploadProgressReport.remove(taskId);
        }
    }

    public void uploadFile(ReadableMap map) {
        TXUGCPublishTypeDef.TXPublishParam param = new TXUGCPublishTypeDef.TXPublishParam();
        param.signature = map.getString("sign");
        param.videoPath = map.getString("fileURL");
        String  = map.getString("userID");

        WeakReference<RNFetchBlobUploadVideo> thisRef = new WeakReference<RNFetchBlobUploadVideo>(this);
        TXUGCPublish mVideoPublish = new TXUGCPublish(ReactNativeBlobUtilImpl.RCTContext, userID);
        RNFetchBlobUploadVideo.taskTable.put(taskId, mVideoPublish);
        mVideoPublish.setListener(new TXUGCPublishTypeDef.ITXVideoPublishListener() {
            @Override
            public void onPublishProgress(long uploadBytes, long totalBytes) {
                Log.d("upload video", "onPublishProgress: " + uploadBytes + "/" + totalBytes);
                ReactNativeBlobUtilProgressConfig reportConfig = ReactNativeBlobUtilReq.getReportUploadProgress(thisRef.get().taskId);

                double progress = uploadBytes * 1.0 / totalBytes;

                if (reportConfig != null && reportConfig.shouldReport((float) progress)) {
                    WritableMap args = Arguments.createMap();
                    args.putString("taskId", taskId);
                    args.putString("written", String.valueOf(uploadBytes));
                    args.putString("total", String.valueOf(totalBytes));
                    args.putDouble("percent", progress);
                    ReactNativeBlobUtilImpl.RCTContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit(ReactNativeBlobUtilConst.EVENT_UPLOAD_PROGRESS, args);
                }
            }
            @Override
            public void onPublishComplete(TXUGCPublishTypeDef.TXPublishResult result) {
                Log.d("upload video", "onPublishComplete: " + result.retCode + " Msg:" + (result.retCode == 0 ? result.videoURL : result.descMsg));
                if (thisRef.get().callback == null) return;
                if (result.retCode == 0) {
                    WritableMap map = Arguments.createMap();
                    map.putString("videoURL", result.videoURL);
                    map.putString("videoId", result.videoId);
                    thisRef.get().callback.invoke(map, null);
                } else {
                    thisRef.get().callback.invoke(null, result.descMsg);
                }
                thisRef.get().releaseTaskResource();
            }
        });

        mVideoPublish.publishVideo(param);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}

