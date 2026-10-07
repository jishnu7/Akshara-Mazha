package in.androidtweak.rain;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.androidtweak.rain.R;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.transition.MaterialSharedAxis;

public class SettingsActivity extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_ENABLE_DEPTH = "enable_depth";
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_CHANGE_BIT_SPEED = "change_bit_speed";
    public static final String KEY_FALLING_SPEED = "falling_speed";
    public static final String KEY_NUM_BITS = "num_bits";
    public static final String KEY_BIT_COLOR = "bit_color";
    public static final String KEY_CHARACTER_SET_PREFS = "character_set_prefs";
    public static final String KEY_FONT_PREFS = "preference_font_name";
    public static final String KEY_FRAME_RATE = "frame_rate";

    private AppBarLayout appBar;
    private CollapsingToolbarLayout collapsingToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Material You: use the wallpaper-derived palette on Android 12+
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        appBar = findViewById(R.id.app_bar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Keep content clear of side system bars and cutouts in landscape
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, 0, bars.right, 0);
            return insets;
        });

        FragmentManager fragments = getSupportFragmentManager();
        fragments.addOnBackStackChangedListener(this::updateUpButton);
        if (savedInstanceState == null) {
            fragments.beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        updateUpButton();
    }

    /** Opens a sub-page (a Preference with app:fragment) with a Material shared axis transition */
    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller,
                                             @NonNull Preference pref) {
        FragmentManager fragments = getSupportFragmentManager();
        Fragment page = fragments.getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        page.setArguments(pref.getExtras());

        page.setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        page.setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        caller.setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        caller.setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

        fragments.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.settings_container, page)
                .addToBackStack(null)
                .commit();
        return true;
    }

    private void updateUpButton() {
        boolean onSubPage = getSupportFragmentManager().getBackStackEntryCount() > 0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(onSubPage);
        appBar.setExpanded(true, false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    /** The large collapsing title doesn't follow the toolbar once set, so update it directly */
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(title);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        HackerWallpaperService.reset();
    }
}
