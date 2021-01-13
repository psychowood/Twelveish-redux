package com.layoutxml.twelveish.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import com.layoutxml.twelveish.R;
import com.layoutxml.twelveish.adapters.TextviewRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LanguageSelectionActivity extends AppCompatActivity implements TextviewRecyclerViewAdapter.ItemClickListener {

    private TextviewRecyclerViewAdapter adapter;
    private List<Pair<String, String>> languageOptions;
    private String settingsName = "languageSelectionList";
    private String[] availableLanguages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.color_selection_activity);

        availableLanguages = getResources().getStringArray(R.array.AvailableLanguages);
        languageOptions = new ArrayList<>();
        for(String lang : availableLanguages){
            Locale mLocale = new Locale(lang);
            languageOptions.add(new Pair<String, String>(mLocale.getDisplayLanguage(mLocale), mLocale.getDisplayLanguage(new Locale ("en"))));
        }

        RecyclerView recyclerView = findViewById(R.id.colorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TextviewRecyclerViewAdapter(this, languageOptions, settingsName);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
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