package com.layoutxml.twelveish;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.rendering.ComplicationDrawable;
import android.util.SparseArray;

import java.time.Instant;

public class ComplicationManager {
    public enum ComplicationLocation {
        BOTTOM,
        LEFT,
        RIGHT
    }
    public static final int BOTTOM_COMPLICATION_ID = 0;
    public static final int LEFT_COMPLICATION_ID = 1;
    public static final int RIGHT_COMPLICATION_ID = 2;
    private static final int[] COMPLICATION_IDS = {BOTTOM_COMPLICATION_ID, LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID};
    
    private final Context context;
    private final PreferenceManager preferenceManager;

    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    ComplicationManager(Context context, PreferenceManager preferenceManager) {
        this.context = context;
        this.preferenceManager = preferenceManager;
    }

    public static int getComplicationId(ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    public static int[] getSupportedComplicationTypes(ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case BOTTOM:
                return new int[]{
                        5, // RANGED_VALUE
                        6, // ICON
                        4, // LONG_TEXT
                        3, // SHORT_TEXT
                        7, // SMALL_IMAGE
                        8  // LARGE_IMAGE
                };
            case LEFT:
            case RIGHT:
                return new int[]{
                        5, // RANGED_VALUE
                        6, // ICON
                        3, // SHORT_TEXT
                        7  // SMALL_IMAGE
                };
            default:
                return new int[]{};
        }
    }

    public ComplicationDrawable getDrawable(int id) {
        if (mComplicationDrawableSparseArray != null) {
            return mComplicationDrawableSparseArray.get(id);
        }
        return null;
    }

    public void initializeComplications() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
        mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
        
        for (int complicationId : COMPLICATION_IDS) {
            ComplicationDrawable drawable = ComplicationDrawable.getDrawable(context, R.drawable.custom_complication_styles);
            if (drawable != null) {
                drawable.setContext(context);
                mComplicationDrawableSparseArray.put(complicationId, drawable);
                
                drawable.getActiveStyle().setBackgroundColor(preferenceManager.getBackgroundColor());
                drawable.getActiveStyle().setHighlightColor(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setHighlightColor(preferenceManager.getSecondaryTextColorAmbient());
                drawable.getActiveStyle().setIconColor(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setIconColor(preferenceManager.getSecondaryTextColorAmbient());
                drawable.getActiveStyle().setTextColor(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setTextColor(preferenceManager.getSecondaryTextColorAmbient());
                drawable.getActiveStyle().setRangedValuePrimaryColor(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setRangedValuePrimaryColor(preferenceManager.getSecondaryTextColorAmbient());
                drawable.getActiveStyle().setRangedValueSecondaryColor(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setRangedValueSecondaryColor(preferenceManager.getSecondaryTextColorAmbient());
                drawable.getActiveStyle().setRangedValueRingWidth(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setRangedValueRingWidth(preferenceManager.getSecondaryTextColorAmbient());
                drawable.getActiveStyle().setTitleColor(preferenceManager.getSecondaryTextColorActive());
                drawable.getAmbientStyle().setTitleColor(preferenceManager.getSecondaryTextColorAmbient());
            }
        }
    }

    public void drawComplications(Canvas canvas, long currentTimeMillis) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        for (int complicationId : COMPLICATION_IDS) {
            ComplicationDrawable complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
            if (complicationDrawable != null) {
                complicationDrawable.setCurrentTime(Instant.ofEpochMilli(currentTimeMillis));
                complicationDrawable.draw(canvas);
            }
        }
    }

    public void handleTap(int x, int y) {
        if (preferenceManager.isDisableComplicationTap() || android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        for (int complicationId : COMPLICATION_IDS) {
            ComplicationDrawable complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
            if (complicationDrawable != null && complicationDrawable.onTap(x, y)) {
                return;
            }
        }
    }

    public void changeAmbientMode(boolean isAmbient) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        for (int complicationId : COMPLICATION_IDS) {
            ComplicationDrawable complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
            if (complicationDrawable != null) {
                complicationDrawable.setInAmbientMode(isAmbient);
            }
        }
    }

    public void setBounds(int width, int height, float chinSize) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        Rect bottomBounds = new Rect(width / 2 - width / 4,
                (int) (height * 3 / 4),
                width / 2 + width / 4,
                (int) (height - chinSize));
        Rect leftBounds = new Rect(0,
                (int)(height * 3 / 8.0f),
                width / 4,
                (int)(height * 5 / 8.0f));
        Rect rightBounds = new Rect((int)(width * 3 / 4.0f),
                (int)(height * 3 / 8.0f),
                width,
                (int)(height * 5 / 8.0f));
        
        if (mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID) != null)
            mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID).setBounds(bottomBounds);
        if (mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID) != null)
            mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID).setBounds(leftBounds);
        if (mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID) != null)
            mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID).setBounds(rightBounds);
    }

    public void updateData(int id, ComplicationData data) {
        if (mActiveComplicationDataSparseArray != null)
            mActiveComplicationDataSparseArray.put(id, data);
        if (mComplicationDrawableSparseArray != null) {
            ComplicationDrawable complicationDrawable = mComplicationDrawableSparseArray.get(id);
            if (complicationDrawable != null) {
                complicationDrawable.setComplicationData(data, false);
            }
        }
    }
}
