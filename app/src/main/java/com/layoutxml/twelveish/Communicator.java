package com.layoutxml.twelveish;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import lombok.Getter;

public class Communicator implements DataClient.OnDataChangedListener{
    private static final String TAG = "Communicator";
    @Getter
    private final String path = "/twelveish";
    private final String DATA_KEY = "rokas-twelveish";
    // private final String HANDSHAKE_KEY = "rokas-twelveish-hs";
    private final String HANDSHAKE_REQUEST = "rokas-twelveish-hs-req";
    private final String HANDSHAKE_RESPONSE = "rokas-twelveish-hq-res";
    private final String GOODBYE_KEY = "rokas-twelveish-gb";
    private final String DATA_REQUEST_KEY = "rokas-twelveish-dr";
    private final String DATA_REQUEST_KEY2 = "rokas-twelveish-dr2";
    private final String CONFIG_REQUEST_KEY = "rokas-twelveish-cr";
    private final String CONFIG_REQUEST_KEY2 = "rokas-twelveish-cr2";
    private final String PREFERENCES_KEY = "rokas-twelveish-pr";
    private final String TIMESTAMP_KEY = "Timestamp";

    private final String PING = "rokas-twelveish-ping";
    private final String PING2 = "rokas-twelveish-ping2";

    private final Context context;
    private final PreferenceManager preferenceManager;
    private PutDataMapRequest dataMapRequest;

    Communicator(Context context, PreferenceManager preferenceManager) {
        this.context = context;
        this.preferenceManager = preferenceManager;
        dataMapRequest = PutDataMapRequest.create(path);
    }

    public void processData(DataItem dataItem) {
        DataMapItem mDataMapItem = DataMapItem.fromDataItem(dataItem);

        String[] array = mDataMapItem.getDataMap().getStringArray(DATA_KEY);
        if (array != null && array.length == 3) {
            savePreference(array);
        }

        boolean handshake = mDataMapItem.getDataMap().getBoolean(HANDSHAKE_REQUEST);
        if (handshake) {
            performHandshake();
        }

        boolean preferences = mDataMapItem.getDataMap().getBoolean(DATA_REQUEST_KEY);
        if (preferences) {
            sendCurrentPreferences();
        }

        boolean config = mDataMapItem.getDataMap().getBoolean(CONFIG_REQUEST_KEY);
        if (config) {
            sendConfigurationData();
        }

        boolean ping = mDataMapItem.getDataMap().getBoolean(PING);
        if(ping){
            echoPing();
        }
    }



    public void performHandshake() {
        final PutDataMapRequest mPutDataMapRequest = dataMapRequest;
        mPutDataMapRequest.getDataMap().putLong(TIMESTAMP_KEY, System.currentTimeMillis());
        mPutDataMapRequest.getDataMap().putBoolean(HANDSHAKE_REQUEST, false);
        mPutDataMapRequest.getDataMap().putBoolean(HANDSHAKE_RESPONSE, true);
        mPutDataMapRequest.setUrgent();
        PutDataRequest mPutDataRequest = mPutDataMapRequest.asPutDataRequest();
        Wearable.getDataClient(context).putDataItem(mPutDataRequest);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPutDataMapRequest.getDataMap().clear();
            }
        }, 5000);
    }

    private void echoPing() {
        final PutDataMapRequest mPutDataMapRequest = PutDataMapRequest.create(path);
        mPutDataMapRequest.getDataMap().putLong(TIMESTAMP_KEY, System.currentTimeMillis());
        mPutDataMapRequest.getDataMap().putBoolean(PING, false);
        mPutDataMapRequest.getDataMap().putBoolean(PING2, true);
        mPutDataMapRequest.setUrgent();
        PutDataRequest mPutDataRequest = mPutDataMapRequest.asPutDataRequest();
        Wearable.getDataClient(context).putDataItem(mPutDataRequest);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPutDataMapRequest.getDataMap().clear();
            }
        }, 5000);
    }

    public void disconnect() {
        final PutDataMapRequest mPutDataMapRequest = PutDataMapRequest.create(path);
        mPutDataMapRequest.getDataMap().putBoolean(GOODBYE_KEY, true);
        mPutDataMapRequest.setUrgent();
        PutDataRequest mPutDataRequest = mPutDataMapRequest.asPutDataRequest();
        Wearable.getDataClient(context).putDataItem(mPutDataRequest);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPutDataMapRequest.getDataMap().clear();
            }
        }, 5000);
    }

    private void savePreference(String[] preferenceArray) {
        // TODO: create a listener in WatchFace class to force refresh
        log("savePreference");

        for(int i = 0; i<preferenceArray.length; i+=3){
            switch(preferenceArray[i+2]){
                case "String":
                    preferenceManager.saveString(preferenceArray[i], preferenceArray[i+1]);
                    break;
                case "Integer":
                    try{
                        int value = Integer.parseInt(preferenceArray[i+1]);
                        preferenceManager.saveInt(preferenceArray[i], value);
                    } catch (NumberFormatException e){
                        log("Preference error");
                    }
                    break;
                case "Boolean":
                    boolean value = Boolean.parseBoolean(preferenceArray[i+1]);
                    preferenceManager.saveBoolean(preferenceArray[i], value);
                    break;
                default:
                    log("Unknown type in processData");
                    break;
            }

        }
    }

    private void sendCurrentPreferences() {
        final PutDataMapRequest mPutDataMapRequest = PutDataMapRequest.create(path);
        mPutDataMapRequest.getDataMap().putLong(TIMESTAMP_KEY, System.currentTimeMillis());
        mPutDataMapRequest.getDataMap().putStringArrayList(PREFERENCES_KEY, preferenceManager.getPreferencesList());
        mPutDataMapRequest.getDataMap().putBoolean(DATA_REQUEST_KEY, false);
        mPutDataMapRequest.getDataMap().putBoolean(DATA_REQUEST_KEY2, true);
        mPutDataMapRequest.setUrgent();
        PutDataRequest mPutDataRequest = mPutDataMapRequest.asPutDataRequest();
        Wearable.getDataClient(context).putDataItem(mPutDataRequest);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPutDataMapRequest.getDataMap().clear();
            }
        }, 5000);
    }

    private void sendConfigurationData() {
        String[] configToSend = new String[3];
        configToSend[0] = (int) 0 + ""; // TODO: set chin size
        configToSend[1] = Boolean.toString(preferenceManager.isComplicationLeftSet());
        configToSend[2] = Boolean.toString(preferenceManager.isComplicationRightSet());

        final PutDataMapRequest mPutDataMapRequest = PutDataMapRequest.create(path);
        mPutDataMapRequest.getDataMap().putLong(TIMESTAMP_KEY, System.currentTimeMillis());
        mPutDataMapRequest.getDataMap().putStringArray(PREFERENCES_KEY, configToSend);
        mPutDataMapRequest.getDataMap().putBoolean(CONFIG_REQUEST_KEY, false);
        mPutDataMapRequest.getDataMap().putBoolean(CONFIG_REQUEST_KEY2, true);
        mPutDataMapRequest.setUrgent();
        PutDataRequest mPutDataRequest = mPutDataMapRequest.asPutDataRequest();
        Wearable.getDataClient(context).putDataItem(mPutDataRequest);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPutDataMapRequest.getDataMap().clear();
            }
        }, 5000);
    }

    private void log(String message) {
        Log.d(TAG, message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath() != null && event.getDataItem().getUri().getPath().equals(path)) {
                DataItem dataItem = event.getDataItem();
                processData(dataItem);
            }
        }
    }
}
