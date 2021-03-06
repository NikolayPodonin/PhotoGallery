package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Ybr on 04.03.2018.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRE_DOWNLOAD = 1;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private LruCache<String, Bitmap> mLruCache;


    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T)msg.obj;
                    Log.i(TAG, "Got a request url: " + mRequestMap.get(target));
                    handleRequest(target);
                } else if (msg.what == MESSAGE_PRE_DOWNLOAD){
                    String url = (String)msg.obj;
                    Log.i(TAG, "handleMessage: pre-download");
                    try {
                        getBitmapWithLru(url);
                    } catch (IOException ioe){
                        Log.e(TAG, "Error download image", ioe);
                    }
                }
            }
        };
    }

    private void handleRequest(final T target) {
        try{
            final String url = mRequestMap.get(target);

            if(url == null){
                return;
            }

            final Bitmap bitmap = getBitmapWithLru(url);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url || mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException ioe){
            Log.e(TAG, "Error download image", ioe);
        }
    }

    private Bitmap getBitmapWithLru(final String url) throws IOException {
        if(mLruCache == null){
            mLruCache = new LruCache(50);
        }

        Bitmap bitMap = mLruCache.get(url);
        if(bitMap == null) {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            bitMap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mLruCache.put(url, bitMap);
        } else {
            Log.i(TAG, "Bitmap get from cash");
        }

        return bitMap;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnailPreDownload(String url){
        Log.i(TAG, "queueThumbnailPreDownload: pre-download url " + url);

        mRequestHandler.obtainMessage(MESSAGE_PRE_DOWNLOAD, url);
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a url: " + url);

        if(url == null){
            mRequestMap.remove(target);
        } else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }
}
