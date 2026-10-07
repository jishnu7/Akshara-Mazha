package in.androidtweak.rain;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.androidtweak.rain.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;

public class SettingsActivity extends AppCompatActivity {
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_ENABLE_DEPTH = "enable_depth";
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_CHANGE_BIT_SPEED = "change_bit_speed";
    public static final String KEY_FALLING_SPEED = "falling_speed";
    public static final String KEY_NUM_BITS = "num_bits";
    public static final String KEY_BIT_COLOR = "bit_color";
    public static final String KEY_CHARACTER_SET_PREFS = "character_set_prefs";
    public static final String KEY_FONT_PREFS = "preference_font_name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Material You: use the wallpaper-derived palette on Android 12+
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Keep content clear of side system bars and cutouts in landscape
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, 0, bars.right, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_reset_to_defaults) {
            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.settings_container);
            if (fragment != null) {
                fragment.resetToDefaults();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        BitSequence.configure(this);
        HackerWallpaperService.reset();
    }
}
