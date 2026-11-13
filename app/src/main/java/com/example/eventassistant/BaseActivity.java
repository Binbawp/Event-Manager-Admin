package com.example.eventassistant;

import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

/** Shared back-arrow header behaviour for every screen that is not a main tab. */
public abstract class BaseActivity extends AppCompatActivity {

    /** Call after setContentView(). The layout must include layout/view_back_header.xml. */
    protected void setupToolbar(@StringRes int titleRes) {
        TextView title = findViewById(R.id.tvHeader);
        title.setText(titleRes);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
