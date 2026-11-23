package com.example.eventassistant;

import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Abstract base activity providing standardized top toolbar and back navigation behavior.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Initializes the toolbar header title and configures the back button click listener.
     * Must be called after {@code setContentView()} in child activities using {@code layout/view_back_header.xml}.
     *
     * @param titleRes string resource ID for the screen title
     */
    protected void setupToolbar(@StringRes int titleRes) {
        TextView title = findViewById(R.id.tvHeader);
        title.setText(titleRes);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
