package com.layoutxml.twelveish;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
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
import java.util.TreeMap;

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
        String[] locales = {"en", "de", "el", "es", "fi", "fr", "hu", "it", "lt", "no", "nl", "pt", "ru", "sv"};
        File reportFile = new File("/Users/gg/dev/Twelveish-redux/fuzzy_time_matrix.artifact.md");
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("# Fuzzy Time Comparison Matrix\n\n");
            writer.write("Generated on: " + new Date() + "\n\n");

            for (String locale : locales) {
                writer.write("## Locale: " + locale + "\n\n");
                writer.write("| Time | Legacy Java Output | WFF XML Output | Match |\n");
                writer.write("| :--- | :--- | :--- | :--- |\n");

                setLocale(locale);
                // Map 'no' to 'nb' for WFF resources if necessary
                String wffLocale = locale.equals("no") ? "nb" : locale;
                WffModel wffModel = loadWffModel(wffLocale);

                String lastLegacy = "";
                String lastWff = "";

                for (int h = 0; h < 24; h++) {
                    for (int m = 0; m < 60; m++) {
                        String legacyOutput = getLegacyOutput(h, m);
                        String wffOutput = getWffOutput(wffModel, h, m, locale);
                        
                        String cleanLegacy = legacyOutput.replace("\n", " ").replaceAll("\\s+", " ").replace("\u00A0", " ").trim();
                        String cleanWff = wffOutput.replace("\n", " ").replaceAll("\\s+", " ").replace("\u00A0", " ").trim();
                        
                        String matchKeyLegacy = cleanLegacy.replace(" ", "").replace("'", "").replace("\\", "").toLowerCase();
                        String matchKeyWff = cleanWff.replace(" ", "").replace("'", "").replace("\\", "").toLowerCase();
                        
                        String prevMatchKeyLegacy = lastLegacy.replace("\n", " ").replace(" ", "").replace("\u00A0", "").replace("'", "").replace("\\", "").toLowerCase();
                        String prevMatchKeyWff = lastWff.replace("\n", " ").replace(" ", "").replace("\u00A0", "").replace("'", "").replace("\\", "").toLowerCase();

                        // Only print if different from last minute
                        if (!matchKeyLegacy.equals(prevMatchKeyLegacy) || !matchKeyWff.equals(prevMatchKeyWff)) {
                            String match = matchKeyLegacy.equals(matchKeyWff) ? "✅" : "❌";
                            writer.write(String.format("| %02d:%02d | `%s` | `%s` | %s |\n", 
                                h, m, cleanLegacy, cleanWff, match));
                            lastLegacy = legacyOutput;
                            lastWff = wffOutput;
                        }
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

    private String getWffOutput(WffModel model, int h, int m, String locale) {
        // Updated WFF Logic: Shift at 40 minutes (aligned with Legacy)
        int wffHour = (m >= 40) ? (h + 1) % 24 : h;
        
        String hourKey;
        if (wffHour == 0) hourKey = "hour_0";
        else if (wffHour == 12) hourKey = "hour_12";
        else hourKey = "hour_" + (wffHour % 12);
        
        String hourVal = model.strings.getOrDefault(hourKey, "");

        String prefixKey = null;
        if (m > 0 && m < 5) prefixKey = "seg_0_prefix";
        else if (m >= 5 && m < 10) prefixKey = "seg_1_prefix";
        else if (m >= 10 && m < 15) prefixKey = "seg_2_prefix";
        else if (m >= 15 && m < 20) prefixKey = "seg_3_prefix";
        else if (m >= 20 && m < 25) prefixKey = "seg_4_prefix";
        else if (m >= 25 && m < 30) prefixKey = "seg_5_prefix";
        else if (m >= 30 && m < 35) prefixKey = "seg_6_prefix";
        else if (m >= 35 && m < 40) prefixKey = "seg_7_prefix";
        else if (m >= 40 && m < 45) prefixKey = "seg_8_prefix";
        else if (m >= 45 && m < 50) prefixKey = "seg_9_prefix";
        else if (m >= 50 && m < 55) prefixKey = "seg_10_prefix";
        else if (m >= 55 && m < 60) prefixKey = "seg_11_prefix";
        
        String prefix = (prefixKey != null) ? model.strings.getOrDefault(prefixKey, "") : "";

        String suffixKey = null;
        if (m > 0 && m < 5) suffixKey = "seg_0_suffix"; 
        else if (m >= 5 && m < 10) suffixKey = "seg_1_suffix";
        else if (m >= 10 && m < 15) suffixKey = "seg_2_suffix";
        else if (m >= 15 && m < 20) suffixKey = "seg_3_suffix";
        else if (m >= 20 && m < 25) suffixKey = "seg_4_suffix";
        else if (m >= 25 && m < 30) suffixKey = "seg_5_suffix";
        else if (m >= 30 && m < 35) suffixKey = "seg_6_suffix";
        else if (m >= 35 && m < 40) suffixKey = "seg_7_suffix";
        else if (m >= 40 && m < 45) suffixKey = "seg_8_suffix";
        else if (m >= 45 && m < 50) suffixKey = "seg_9_suffix";
        else if (m >= 50 && m < 55) suffixKey = "seg_10_suffix";
        else if (m >= 55 && m < 60) suffixKey = "seg_11_suffix";
        
        String suffix = (suffixKey != null) ? model.strings.getOrDefault(suffixKey, "") : "";

        StringBuilder res = new StringBuilder();
        // Simulate vertical concatenation of the 3 groups in watchface.xml
        if (!prefix.isEmpty()) {
            res.append(prefix.trim());
        }
        if (!res.toString().isEmpty()) res.append("\n");
        res.append(hourVal.trim());
        if (!suffix.isEmpty()) {
            res.append("\n");
            res.append(suffix.trim());
        }
        
        return res.toString().trim();
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
                String value = node.getTextContent()
                    .replace("\\u00A0", "\u00A0")
                    .replace("\\'", "'");
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
