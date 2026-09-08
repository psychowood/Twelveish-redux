package com.layoutxml.twelveish;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.layoutxml.twelveish.objects.TextGeneratorDataWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class FuzzyTimeComparisonTest {

    private Context context;
    private LanguageManager languageManager;
    private PreferenceManager preferenceManager;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        preferenceManager = new PreferenceManager(context);
        languageManager = new LanguageManager(context);
    }

    @Test
    public void generateComparisonReport() throws Exception {
        String[] locales = {"en", "it"};
        File reportFile = new File("/Users/gg/dev/Twelveish-redux/fuzzy_time_matrix.artifact.md");
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("# Fuzzy Time Comparison Matrix\n\n");
            writer.write("Generated on: " + new Date() + "\n\n");

            for (String locale : locales) {
                writer.write("## Locale: " + locale + "\n\n");
                writer.write("| Time | Legacy Java Output | WFF XML Output | Match |\n");
                writer.write("| :--- | :--- | :--- | :--- |\n");

                setLocale(locale);
                WffModel wffModel = loadWffModel(locale);

                for (int h = 0; h < 24; h++) {
                    for (int m = 0; m < 60; m++) {
                        String legacyOutput = getLegacyOutput(h, m);
                        String wffOutput = getWffOutput(wffModel, h, m);
                        
                        String match = legacyOutput.replace("\n", " ").trim().equalsIgnoreCase(wffOutput.trim()) ? "✅" : "❌";
                        
                        writer.write(String.format("| %02d:%02d | `%s` | `%s` | %s |\n", 
                            h, m, legacyOutput.replace("\n", " "), wffOutput, match));
                    }
                }
                writer.write("\n");
            }
        }
        System.out.println("Report generated at: " + reportFile.getAbsolutePath());
    }

    private void setLocale(String locale) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        prefs.edit().putString(context.getString(R.string.preference_language), locale).commit();
        languageManager.loadPreferences();
    }

    private String getLegacyOutput(int h, int m) throws Exception {
        TextGenerator generator = new TextGenerator(preferenceManager, languageManager, null, 450, 450, 0f, 100f);
        
        Field calendarField = TextGenerator.class.getDeclaredField("calendar");
        calendarField.setAccessible(true);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, h);
        calendar.set(Calendar.MINUTE, m);
        calendarField.set(generator, calendar);
        
        Field minuteIndexField = TextGenerator.class.getDeclaredField("minuteIndex");
        minuteIndexField.setAccessible(true);
        minuteIndexField.set(generator, m / 5);
        
        Field roundTimeField = TextGenerator.class.getDeclaredField("roundTime");
        roundTimeField.setAccessible(true);
        roundTimeField.set(generator, m == 0);
        
        Method getHourIndexMethod = TextGenerator.class.getDeclaredMethod("getHourIndex");
        getHourIndexMethod.setAccessible(true);
        int hourIndex = (int) getHourIndexMethod.invoke(generator);
        
        Field hourIndexField = TextGenerator.class.getDeclaredField("hourIndex");
        hourIndexField.setAccessible(true);
        hourIndexField.set(generator, hourIndex);

        return generator.generateSync().getMainText();
    }

    private String getWffOutput(WffModel model, int h, int m) {
        int wffHour = (m >= 38) ? (h + 1) % 24 : h;
        
        String hourKey;
        if (wffHour == 0) hourKey = "hour_0";
        else if (wffHour == 12) hourKey = "hour_12";
        else hourKey = "hour_" + (wffHour % 12);
        
        String hourVal = model.strings.getOrDefault(hourKey, "");

        String prefixKey = null;
        if (m >= 10 && m < 20) prefixKey = "seg_2_prefix";
        else if (m >= 20 && m < 25) prefixKey = "seg_4_prefix";
        else if (m >= 25 && m < 30) prefixKey = "seg_5_prefix";
        else if (m >= 30 && m < 40) prefixKey = "seg_6_prefix";
        else if (m >= 40 && m < 50) prefixKey = "seg_8_prefix";
        else if (m >= 50 && m < 55) prefixKey = "seg_10_prefix";
        else if (m >= 55 && m < 60) prefixKey = "seg_11_prefix";
        
        String prefix = (prefixKey != null) ? model.strings.getOrDefault(prefixKey, "") : "";

        String suffixKey = null;
        if (m < 5) suffixKey = "seg_0_suffix";
        else if (m < 10) suffixKey = "seg_1_suffix";
        else if (m < 15) suffixKey = "seg_2_suffix";
        else if (m < 20) suffixKey = "seg_3_suffix";
        else if (m < 25) suffixKey = "seg_4_suffix";
        else if (m < 30) suffixKey = "seg_5_suffix";
        else if (m < 35) suffixKey = "seg_6_suffix";
        else if (m < 40) suffixKey = "seg_7_suffix";
        else if (m < 45) suffixKey = "seg_8_suffix";
        else if (m < 50) suffixKey = "seg_9_suffix";
        else if (m < 55) suffixKey = "seg_10_suffix";
        else if (m < 60) suffixKey = "seg_11_suffix";
        
        String suffix = (suffixKey != null) ? model.strings.getOrDefault(suffixKey, "") : "";

        StringBuilder res = new StringBuilder();
        if (!prefix.isEmpty()) res.append(prefix).append(" ");
        res.append(hourVal);
        if (!suffix.isEmpty()) res.append(" ").append(suffix);
        
        return res.toString().trim().replace("  ", " ");
    }

    private WffModel loadWffModel(String locale) throws Exception {
        Map<String, String> strings = new HashMap<>();
        String valuesDir = locale.equals("en") ? "values" : "values-" + locale;
        File stringsFile = new File("/Users/gg/dev/Twelveish-redux/watch-face/src/main/res/" + valuesDir + "/strings.xml");
        
        if (stringsFile.exists()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(stringsFile);
            NodeList nodes = doc.getElementsByTagName("string");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String name = node.getAttribute("name");
                String value = node.getTextContent();
                strings.put(name, value);
            }
        }
        
        return new WffModel(strings);
    }

    private static class WffModel {
        final Map<String, String> strings;
        WffModel(Map<String, String> strings) {
            this.strings = strings;
        }
    }
}
