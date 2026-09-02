package com.layoutxml.twelveish.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.wear.watchface.editor.ChosenComplicationDataSource;
import androidx.wear.watchface.editor.ListenableEditorSession;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.layoutxml.twelveish.ComplicationManager;
import com.layoutxml.twelveish.R;

public class ComplicationConfigActivity extends ComponentActivity implements View.OnClickListener {
    private static final String TAG = "ComplicationConfig";

    private int mLeftComplicationId;
    private int mRightComplicationId;
    private int mSelectedComplicationId;
    private ImageView mLeftComplicationBackground;
    private ImageButton mLeftComplication;
    private ImageView mRightComplicationBackground;
    private ImageButton mRightComplication;
    private ImageButton mBottomComplication;
    private Drawable mDefaultAddComplicationDrawable;
    private ListenableEditorSession editorSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);
        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication);
        mSelectedComplicationId = -1;

        mLeftComplicationId = ComplicationManager.LEFT_COMPLICATION_ID;
        mRightComplicationId = ComplicationManager.RIGHT_COMPLICATION_ID;

        mBottomComplication = findViewById(R.id.bottom_complication);
        mBottomComplication.setOnClickListener(this);
        mBottomComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        findViewById(R.id.bottom_complication_background).setVisibility(View.INVISIBLE);

        mLeftComplicationBackground = findViewById(R.id.left_complication_background);
        mLeftComplication = findViewById(R.id.left_complication);
        mLeftComplication.setOnClickListener(this);
        mLeftComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mLeftComplicationBackground.setVisibility(View.INVISIBLE);

        mRightComplicationBackground = findViewById(R.id.right_complication_background);
        mRightComplication = findViewById(R.id.right_complication);
        mRightComplication.setOnClickListener(this);
        mRightComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mRightComplicationBackground.setVisibility(View.INVISIBLE);

        updateComplicationViews();

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("androidx.wear.watchface.editor.extra.WATCH_FACE_COMPONENT")) {
            ListenableFuture<ListenableEditorSession> sessionFuture = ListenableEditorSession.listenableCreateOnWatchEditorSession(this);
            Futures.addCallback(sessionFuture, new FutureCallback<ListenableEditorSession>() {
                @Override
                public void onSuccess(ListenableEditorSession result) {
                    if (result != null) {
                        Log.d(TAG, "Editor session created");
                        editorSession = result;
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Failed to create editor session", t);
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            Log.d(TAG, "No editor session extras found");
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mBottomComplication)) {
            launchComplicationHelperActivity(ComplicationManager.ComplicationLocation.BOTTOM);
        } else if (view.equals(mLeftComplication)) {
            launchComplicationHelperActivity(ComplicationManager.ComplicationLocation.LEFT);
        } else if (view.equals(mRightComplication)) {
            launchComplicationHelperActivity(ComplicationManager.ComplicationLocation.RIGHT);
        }
    }

    private void launchComplicationHelperActivity(ComplicationManager.ComplicationLocation complicationLocation) {
        mSelectedComplicationId = ComplicationManager.getComplicationId(complicationLocation);
        if (mSelectedComplicationId >= 0 && editorSession != null) {
            Log.d(TAG, "Opening chooser for id: " + mSelectedComplicationId);
            ListenableFuture<ChosenComplicationDataSource> future = editorSession.listenableOpenComplicationDataSourceChooser(mSelectedComplicationId);
            Futures.addCallback(future, new FutureCallback<ChosenComplicationDataSource>() {
                @Override
                public void onSuccess(ChosenComplicationDataSource result) {
                    if (result != null) {
                        Log.d(TAG, "Complication set: " + result.getComplicationSlotId());
                        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        if (mSelectedComplicationId == mLeftComplicationId) {
                            prefs.edit().putBoolean(getString(R.string.complication_left_set), true).apply();
                        } else if (mSelectedComplicationId == mRightComplicationId) {
                            prefs.edit().putBoolean(getString(R.string.complication_right_set), true).apply();
                        }
                        runOnUiThread(() -> updateComplicationViews());
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Failed to choose complication", t);
                }
            }, ContextCompat.getMainExecutor(this));
        } else if (editorSession == null) {
            Toast.makeText(this, "Editor session not ready", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateComplicationViews() {
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        
        boolean leftSet = prefs.getBoolean(getString(R.string.complication_left_set), false);
        boolean rightSet = prefs.getBoolean(getString(R.string.complication_right_set), false);
        
        if (leftSet) {
            mLeftComplicationBackground.setVisibility(View.VISIBLE);
        } else {
            mLeftComplicationBackground.setVisibility(View.INVISIBLE);
        }
        
        if (rightSet) {
            mRightComplicationBackground.setVisibility(View.VISIBLE);
        } else {
            mRightComplicationBackground.setVisibility(View.INVISIBLE);
        }
    }
}
