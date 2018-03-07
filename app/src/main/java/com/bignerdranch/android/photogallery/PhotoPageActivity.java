package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

/**
 * Created by User on 06.03.2018.
 */

public class PhotoPageActivity extends SingleFragmentActivity {
    private PhotoPageFragment mPhotoPageFragment;

    public static Intent newIntent(Context context, Uri photoPageUri){
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(photoPageUri);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        mPhotoPageFragment = (PhotoPageFragment)PhotoPageFragment.newInstance(getIntent().getData());
        return mPhotoPageFragment;
    }

    @Override
    public void onBackPressed() {
        if( mPhotoPageFragment != null &&
                mPhotoPageFragment.getWebView() != null &&
                mPhotoPageFragment.getWebView().canGoBack()){
            mPhotoPageFragment.getWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }
}
