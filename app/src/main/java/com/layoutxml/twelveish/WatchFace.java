package com.layoutxml.twelveish;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.ListenableWatchFaceService;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.ComplicationSlotBounds;
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy;
import androidx.wear.watchface.complications.SystemDataSources;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.style.CurrentUserStyleRepository;
import androidx.wear.watchface.style.UserStyleSchema;
import androidx.wear.watchface.style.UserStyleSetting;
import androidx.wear.watchface.style.WatchFaceLayer;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.layoutxml.twelveish.objects.TextGeneratorDataWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WatchFace extends ListenableWatchFaceService implements DataClient.OnDataChangedListener, SharedPreferences.OnSharedPreferenceChangeListener, TextGeneratorListener {
    private static final String TAG = "WatchFaceService";
    
    private PreferenceManager preferenceManager;
    private LanguageManager languageManager;
    private Communicator communicator;
    private ComplicationManager complicationManager;
    private WatchFaceRenderer renderer;
    private int batteryLevel = 100;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryLevel = (int) (100 * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) / ((float) (intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1))));
            if (renderer != null) {
                renderer.setBatteryLevel(batteryLevel);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        preferenceManager = new PreferenceManager(getApplicationContext());
        languageManager = new LanguageManager(getApplicationContext());
        communicator = new Communicator(getApplicationContext(), preferenceManager);
        complicationManager = new ComplicationManager(getApplicationContext(), preferenceManager);
        
        complicationManager.initializeComplications();
        
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Wearable.getDataClient(this).addListener(this);
        
        preferenceManager.getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {}
        try {
            Wearable.getDataClient(this).removeListener(this);
        } catch (Exception ignored) {}
        if (preferenceManager != null) {
            preferenceManager.getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
        if (communicator != null) {
            communicator.disconnect();
        }
        super.onDestroy();
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged: " + key);
        forceRefresh();
    }

    @NonNull
    @Override
    protected ListenableFuture<androidx.wear.watchface.WatchFace> createWatchFaceFuture(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository
    ) {
        Log.d(TAG, "createWatchFaceFuture");
        if (preferenceManager == null) {
            preferenceManager = new PreferenceManager(getApplicationContext());
        }
        if (languageManager == null) {
            languageManager = new LanguageManager(getApplicationContext());
        }
        
        renderer = new WatchFaceRenderer(
                getApplicationContext(),
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                complicationSlotsManager,
                preferenceManager,
                languageManager
        );
        
        renderer.setBatteryLevel(batteryLevel);

        androidx.wear.watchface.WatchFace watchFace = new androidx.wear.watchface.WatchFace(
                WatchFaceType.DIGITAL,
                renderer
        );

        return Futures.immediateFuture(watchFace);
    }

    @NonNull
    @Override
    protected ComplicationSlotsManager createComplicationSlotsManager(@NonNull CurrentUserStyleRepository currentUserStyleRepository) {
        Log.d(TAG, "createComplicationSlotsManager");
        if (complicationManager == null) {
            complicationManager = new ComplicationManager(getApplicationContext(), new PreferenceManager(getApplicationContext()));
            complicationManager.initializeComplications();
        }
        
        ComplicationSlot bottomSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationManager.BOTTOM_COMPLICATION_ID,
                (watchState, listener) -> new androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable(
                        complicationManager.getDrawable(ComplicationManager.BOTTOM_COMPLICATION_ID),
                        watchState,
                        listener
                ),
                Arrays.asList(ComplicationType.RANGED_VALUE, ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT, ComplicationType.MONOCHROMATIC_IMAGE, ComplicationType.SMALL_IMAGE),
                new DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_WATCH_BATTERY, ComplicationType.RANGED_VALUE),
                new ComplicationSlotBounds(new RectF(0.25f, 0.75f, 0.75f, 0.95f))
        ).build();

        ComplicationSlot leftSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationManager.LEFT_COMPLICATION_ID,
                (watchState, listener) -> new androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable(
                        complicationManager.getDrawable(ComplicationManager.LEFT_COMPLICATION_ID),
                        watchState,
                        listener
                ),
                Arrays.asList(ComplicationType.RANGED_VALUE, ComplicationType.SHORT_TEXT, ComplicationType.MONOCHROMATIC_IMAGE, ComplicationType.SMALL_IMAGE),
                new DefaultComplicationDataSourcePolicy(),
                new ComplicationSlotBounds(new RectF(0f, 0.375f, 0.25f, 0.625f))
        ).build();

        ComplicationSlot rightSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationManager.RIGHT_COMPLICATION_ID,
                (watchState, listener) -> new androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable(
                        complicationManager.getDrawable(ComplicationManager.RIGHT_COMPLICATION_ID),
                        watchState,
                        listener
                ),
                Arrays.asList(ComplicationType.RANGED_VALUE, ComplicationType.SHORT_TEXT, ComplicationType.MONOCHROMATIC_IMAGE, ComplicationType.SMALL_IMAGE),
                new DefaultComplicationDataSourcePolicy(),
                new ComplicationSlotBounds(new RectF(0.75f, 0.375f, 1f, 0.625f))
        ).build();

        return new ComplicationSlotsManager(Arrays.asList(bottomSlot, leftSlot, rightSlot), currentUserStyleRepository);
    }

    @NonNull
    @Override
    protected UserStyleSchema createUserStyleSchema() {
        Log.d(TAG, "createUserStyleSchema");
        // Returning an empty schema for now to avoid restricted constructor issues in Java
        return new UserStyleSchema(Collections.emptyList());
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        if (communicator == null) return;
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath() != null && event.getDataItem().getUri().getPath().equals(communicator.getPath())) {
                communicator.processData(event.getDataItem());
                forceRefresh();
            }
        }
    }

    private void forceRefresh() {
        if (preferenceManager != null) preferenceManager.loadPreferences();
        if (languageManager != null) languageManager.loadPreferences();
        if (renderer != null) {
            renderer.adjustToPreferenceChanges();
        }
    }

    @Override
    public void textGeneratorListener(TextGeneratorDataWrapper textGeneratorDataWrapper) {
        if (renderer != null) {
            renderer.setMainTextData(textGeneratorDataWrapper);
        }
    }
}
