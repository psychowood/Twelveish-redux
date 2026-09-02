package com.layoutxml.twelveish;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.CanvasType;
import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import com.layoutxml.twelveish.objects.TextGeneratorDataWrapper;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;

public class WatchFaceRenderer extends Renderer.CanvasRenderer {
    private static final String TAG = "WatchFaceRenderer";
    private static final Typeface DEFAULT_TYPEFACE = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;

    private final Context context;
    private final PreferenceManager preferenceManager;
    private final LanguageManager languageManager;
    private final ComplicationSlotsManager complicationSlotsManager;
    private final WatchState watchState;

    private final Paint secondaryTextPaint;
    private final Paint mainTextPaint;

    private String secondaryTextFirstLine = "";
    private String secondaryTextSecondLine = "";
    private String secondaryTextSecondLineCopy = "";
    private String mainText = "";
    private String dayOfTheWeek = "";
    private boolean fetchMainText = true;
    private float baseXCoordinate = 0;
    private float baseYCoordinate = 0;
    private int lastMainTextFetchTimeHours = -1;
    private int lastMainTextFetchTimeMinutes = -1;

    public WatchFaceRenderer(
            @NonNull Context context,
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull PreferenceManager preferenceManager,
            @NonNull LanguageManager languageManager
    ) {
        super(surfaceHolder, currentUserStyleRepository, watchState, CanvasType.SOFTWARE, 16L, false);
        Log.d(TAG, "Constructor");
        this.context = context;
        this.preferenceManager = preferenceManager;
        this.languageManager = languageManager;
        this.complicationSlotsManager = complicationSlotsManager;
        this.watchState = watchState;

        secondaryTextPaint = new Paint();
        secondaryTextPaint.setTypeface(DEFAULT_TYPEFACE);
        secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
        secondaryTextPaint.setColor(DEFAULT_TEXT_COLOR);

        mainTextPaint = new Paint();
        mainTextPaint.setTypeface(DEFAULT_TYPEFACE);
        mainTextPaint.setTextAlign(Paint.Align.CENTER);
        mainTextPaint.setColor(DEFAULT_TEXT_COLOR);
        
        adjustToPreferenceChanges();
    }

    public void adjustToPreferenceChanges() {
        mainTextPaint.setTypeface(preferenceManager.getFont());
        mainTextPaint.setTextAlign(Paint.Align.CENTER);
        mainTextPaint.setColor(preferenceManager.getMainTextColorActive());

        secondaryTextPaint.setTypeface(preferenceManager.getFont());
        secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
        secondaryTextPaint.setColor(preferenceManager.getSecondaryTextColorActive());
        secondaryTextPaint.setTextSize(context.getResources().getDisplayMetrics().heightPixels * 0.06f + preferenceManager.getSecondaryTextSizeOffset());
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        boolean ambientMode = watchState.isAmbient().getValue();
        setColors(canvas, ambientMode);

        // Convert ZonedDateTime to Calendar for compatibility with existing logic
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(zonedDateTime.toInstant().toEpochMilli());

        int seconds = calendar.get(Calendar.SECOND);
        int minutes = calendar.get(Calendar.MINUTE);
        
        if ((minutes % 5 == 0 || minutes == 1) && (seconds < 2)) {
            fetchMainText = true;
            updateDate(calendar, ambientMode);
        }
        
        int hourDigital = preferenceManager.isMilitaryFormatDigital() ? calendar.get(Calendar.HOUR_OF_DAY) : calendar.get(Calendar.HOUR);
        if (hourDigital == 0 && !preferenceManager.isMilitaryFormatDigital())
            hourDigital = 12;
            
        if (hourDigital - lastMainTextFetchTimeHours != 0 || minutes - lastMainTextFetchTimeMinutes > 5 || lastMainTextFetchTimeMinutes - minutes < -5) {
            fetchMainText = true;
            updateDate(calendar, ambientMode);
        }

        // Get digital clock
        String ampmSymbols = (!preferenceManager.isMilitaryFormatDigital()) ? (calendar.get(Calendar.HOUR_OF_DAY) >= 12 ? " pm" : " am") : "";
        String text = (ambientMode || !preferenceManager.isShowSeconds())
                ? String.format(Locale.UK, "%d:%02d" + ampmSymbols, hourDigital, minutes)
                : String.format(Locale.UK, "%d:%02d:%02d" + ampmSymbols, hourDigital, minutes, seconds);

        // Draw digital clock, date, battery percentage and day of the week
        float firstSeparator = 40.0f;
        if ((ambientMode && !preferenceManager.isShowSecondaryTextAmbient()) || (!ambientMode && !preferenceManager.isShowSecondaryTextActive())) {
            text = "";
        }
        
        if (!text.isEmpty() || !dayOfTheWeek.isEmpty()) {
            if (!text.isEmpty() && !dayOfTheWeek.isEmpty()) {
                canvas.drawText(text + " • " + dayOfTheWeek, bounds.width() / 2.0f, firstSeparator - secondaryTextPaint.ascent(), secondaryTextPaint);
                firstSeparator = 40 - secondaryTextPaint.ascent() + secondaryTextPaint.descent();
            } else if (!text.isEmpty()) {
                canvas.drawText(text, bounds.width() / 2.0f, firstSeparator - secondaryTextPaint.ascent(), secondaryTextPaint);
                firstSeparator = 40 - secondaryTextPaint.ascent() + secondaryTextPaint.descent();
            } else {
                canvas.drawText(dayOfTheWeek, bounds.width() / 2.0f, firstSeparator - secondaryTextPaint.ascent(), secondaryTextPaint);
                firstSeparator = 40 - secondaryTextPaint.ascent() + secondaryTextPaint.descent();
            }
        }
        
        if (!((ambientMode && preferenceManager.isShowSecondaryCalendarAmbient()) || (!ambientMode && preferenceManager.isShowSecondaryCalendarActive()))) {
            secondaryTextFirstLine = "";
        }
        
        if (!((ambientMode && preferenceManager.isShowBatteryAmbient()) || (!ambientMode && preferenceManager.isShowBatteryActive()))) {
            if (!secondaryTextSecondLine.isEmpty()) {
                secondaryTextSecondLineCopy = secondaryTextSecondLine;
            }
            secondaryTextSecondLine = "";
        } else if (secondaryTextSecondLine.isEmpty()) {
            secondaryTextSecondLine = secondaryTextSecondLineCopy;
        }
        
        if (!secondaryTextFirstLine.isEmpty() || !secondaryTextSecondLine.isEmpty()) {
            if (!secondaryTextFirstLine.isEmpty() && !secondaryTextSecondLine.isEmpty()) {
                canvas.drawText(secondaryTextFirstLine + " • " + secondaryTextSecondLine, bounds.width() / 2.0f, firstSeparator - secondaryTextPaint.ascent(), secondaryTextPaint);
                firstSeparator = firstSeparator - secondaryTextPaint.ascent() + secondaryTextPaint.descent();
            } else if (!secondaryTextFirstLine.isEmpty()) {
                canvas.drawText(secondaryTextFirstLine, bounds.width() / 2.0f, firstSeparator - secondaryTextPaint.ascent(), secondaryTextPaint);
                firstSeparator = firstSeparator - secondaryTextPaint.ascent() + secondaryTextPaint.descent();
            } else {
                canvas.drawText(secondaryTextSecondLine, bounds.width() / 2.0f, firstSeparator - secondaryTextPaint.ascent(), secondaryTextPaint);
                firstSeparator = firstSeparator - secondaryTextPaint.ascent() + secondaryTextPaint.descent();
            }
        }
        
        if (firstSeparator < bounds.height() / 4.0f)
            firstSeparator = bounds.height() / 4.0f;

        if (mainText.isEmpty())
            fetchMainText = true;
        if (baseYCoordinate == -1)
            fetchMainText = true;

        // Draw text clock
        if (fetchMainText) {
            lastMainTextFetchTimeMinutes = minutes;
            lastMainTextFetchTimeHours = hourDigital;
            
            TextGenerator generator = new TextGenerator(preferenceManager, languageManager, null, bounds.width(), bounds.height(), 0, firstSeparator);
            TextGeneratorDataWrapper data = generator.generateSync();
            setMainTextData(data);
            
            fetchMainText = false;
        }

        // Draw main text
        float t = baseYCoordinate;
        for (String line : mainText.split("\n")) {
            canvas.drawText(line, baseXCoordinate, t, mainTextPaint);
            t += mainTextPaint.descent() - mainTextPaint.ascent();
        }

        // Draw complications
        if ((ambientMode && preferenceManager.isShowComplicationAmbient()) || (!ambientMode && preferenceManager.isShowComplicationActive())) {
            for (ComplicationSlot slot : complicationSlotsManager.getComplicationSlots().values()) {
                if (slot.isEnabled()) {
                    slot.render(canvas, zonedDateTime, getRenderParameters());
                }
            }
        }
    }

    private void updateDate(Calendar calendar, boolean ambientMode) {
        int first, second, third;
        boolean fourFirst;
        switch (preferenceManager.getDateOrder()) {
            case DMY:
                first = calendar.get(Calendar.DAY_OF_MONTH);
                second = calendar.get(Calendar.MONTH) + 1;
                third = calendar.get(Calendar.YEAR);
                fourFirst = false;
                break;
            case YMD:
                first = calendar.get(Calendar.YEAR);
                second = calendar.get(Calendar.MONTH) + 1;
                third = calendar.get(Calendar.DAY_OF_MONTH);
                fourFirst = true;
                break;
            case YDM:
                first = calendar.get(Calendar.YEAR);
                second = calendar.get(Calendar.DAY_OF_MONTH);
                third = calendar.get(Calendar.MONTH) + 1;
                fourFirst = true;
                break;
            default:
                first = calendar.get(Calendar.MONTH) + 1;
                second = calendar.get(Calendar.DAY_OF_MONTH);
                third = calendar.get(Calendar.YEAR);
                fourFirst = false;
                break;
        }
        if (fourFirst)
            secondaryTextFirstLine = String.format(Locale.UK, "%04d" + preferenceManager.getDateSeparator() + "%02d" + preferenceManager.getDateSeparator() + "%02d", first, second, third);
        else
            secondaryTextFirstLine = String.format(Locale.UK, "%02d" + preferenceManager.getDateSeparator() + "%02d" + preferenceManager.getDateSeparator() + "%04d", first, second, third);


        // Get day of the week
        if ((ambientMode && preferenceManager.isShowDayAmbient()) || (!ambientMode && preferenceManager.isShowDayActive()))
            dayOfTheWeek = languageManager.getWeekday(calendar.get(Calendar.DAY_OF_WEEK) - 1);
        else
            dayOfTheWeek = "";
    }

    private void setColors(Canvas canvas, boolean ambientMode) {
        if (ambientMode) {
            canvas.drawColor(Color.BLACK);
            secondaryTextPaint.setColor(preferenceManager.getSecondaryTextColorAmbient());
            mainTextPaint.setColor(preferenceManager.getMainTextColorAmbient());
        } else {
            canvas.drawColor(preferenceManager.getBackgroundColor());
            secondaryTextPaint.setColor(preferenceManager.getSecondaryTextColorActive());
            mainTextPaint.setColor(preferenceManager.getMainTextColorActive());
        }
    }

    @Override
    public void renderHighlightLayer(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull ZonedDateTime zonedDateTime) {
        // Optional: Draw highlights for editing mode
    }
    
    public void setBatteryLevel(int level) {
        secondaryTextSecondLine = (level + "%");
        invalidate();
    }
    
    public void setMainTextData(TextGeneratorDataWrapper data) {
        mainText = data.getMainText();
        baseXCoordinate = data.getBaseXCoordinate();
        baseYCoordinate = data.getBaseYCoordinate();
        mainTextPaint.setTextSize(data.getTextSize() + preferenceManager.getMainTextSizeOffset());
        invalidate();
    }

    @Override
    public void onDump(@NonNull java.io.PrintWriter writer) {
        writer.println("WatchFaceRenderer");
        writer.println("mainText: " + mainText);
    }
}
