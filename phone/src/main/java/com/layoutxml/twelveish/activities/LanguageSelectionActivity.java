package com.layoutxml.twelveish.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.layoutxml.twelveish.R;
import com.layoutxml.twelveish.adapters.TextviewRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LanguageSelectionActivity extends AppCompatActivity implements TextviewRecyclerViewAdapter.ItemClickListener {

    private TextviewRecyclerViewAdapter adapter;
    private List<Pair<String, String>> settingOptions;
    private String settingsName = "languageSelectionList";
    private String[] availableLanguages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.color_selection_activity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        generateLanguageList();


        RecyclerView recyclerView = findViewById(R.id.colorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TextviewRecyclerViewAdapter(this, settingOptions, settingsName);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void generateLanguageList(){
        availableLanguages = getResources().getStringArray(R.array.AvailableLanguages);
        settingOptions = new ArrayList<>();
        for(String lang : availableLanguages){
            Locale mLocale = new Locale(lang);
            settingOptions.add(new Pair<String, String>(mLocale.getDisplayLanguage(mLocale), mLocale.getDisplayLanguage(new Locale("en"))));
        }
    }

    @Override
    public void onItemClick(View view, int position, String name) {
        Intent returnIntent = new Intent();
        String newLanguage = availableLanguages[position];
        returnIntent.putExtra("newLanguage", newLanguage);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
