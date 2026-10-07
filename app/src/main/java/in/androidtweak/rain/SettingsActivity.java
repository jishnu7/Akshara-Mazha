package in.androidtweak.rain;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.transition.MaterialSharedAxis;

/**
 * A live preview of the rain under a sheet of settings, which opens half way and can be
 * dragged down to see more of the rain. The preview follows every change.
 */
public class SettingsActivity extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_ENABLE_DEPTH = "enable_depth";
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_FALLING_SPEED = "falling_speed";
    /** The tail setting; stored under its old name, "number of bits" */
    public static final String KEY_TAIL = "num_bits";
    public static final String KEY_DENSITY = "density";
    public static final String KEY_BIT_COLOR = "bit_color";
    public static final String KEY_CHARACTER_SET_PREFS = "character_set_prefs";
    public static final String KEY_FONT_PREFS = "preference_font_name";
    public static final String KEY_FRAME_RATE = "frame_rate";

    /** Room left above the expanded sheet, so a strip of rain stays in view */
    private static final int EXPANDED_RAIN_DP = 48;
    /** The collapsed sheet: its drag handle and the set as wallpaper button */
    private static final int COLLAPSED_SHEET_DP = 112;

    private RainPreviewView preview;
    private BottomSheetBehavior<View> sheet;
    private View sheetHeader;
    private View setWallpaper;
    private TextView sheetTitle;
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsListener =
            (prefs, key) -> preview.refresh();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Material You: use the wallpaper-derived palette on Android 12+
        DynamicColors.applyToActivityIfAvailable(this);
        // The status bar is over the dark rain; the navigation bar is always over the sheet
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preview = findViewById(R.id.preview);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // The toolbar shows the app name itself; page titles go in the sheet's header
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        View sheetView = findViewById(R.id.settings_sheet);
        sheet = BottomSheetBehavior.from(sheetView);
        if (savedInstanceState == null) {
            sheet.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }
        sheetHeader = findViewById(R.id.sheet_header);
        setWallpaper = findViewById(R.id.set_wallpaper);
        setWallpaper.setOnClickListener(v -> setAsWallpaper());
        sheetTitle = findViewById(R.id.sheet_title);
        findViewById(R.id.sheet_back).setOnClickListener(v -> getSupportFragmentManager().popBackStack());

        applyInsets(toolbar, sheetView);

        FragmentManager fragments = getSupportFragmentManager();
        fragments.addOnBackStackChangedListener(this::updateSheetHeader);
        if (savedInstanceState == null) {
            fragments.beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        updateSheetHeader();
    }

    /** The rain runs edge to edge; the toolbar and sheet keep clear of the system bars */
    private void applyInsets(View toolbar, View sheetView) {
        float density = getResources().getDisplayMetrics().density;
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            toolbar.setPadding(bars.left, bars.top, bars.right, 0);
            ViewCompat.setPaddingRelative(sheetView, bars.left, 0, bars.right, 0);
            sheet.setExpandedOffset(bars.top + Math.round(EXPANDED_RAIN_DP * density));
            sheet.setPeekHeight(bars.bottom + Math.round(COLLAPSED_SHEET_DP * density));
            return insets;
        });
    }

    /** Opens a sub-page (a Preference with app:fragment) in the sheet, at full height */
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
        sheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        return true;
    }

    private void updateSheetHeader() {
        boolean onSubPage = getSupportFragmentManager().getBackStackEntryCount() > 0;
        sheetHeader.setVisibility(onSubPage ? View.VISIBLE : View.GONE);
        setWallpaper.setVisibility(onSubPage ? View.GONE : View.VISIBLE);
    }

    private void setAsWallpaper() {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(this, HackerWallpaperService.class));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Some devices don't support picking a specific live wallpaper
            startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
        }
    }

    /** Pages set their title as the activity title; it belongs in the sheet's header */
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (sheetTitle != null) {
            sheetTitle.setText(title);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(settingsListener);
        // Settings may have changed elsewhere, such as a reset while away
        preview.refresh();
        preview.setShown(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        preview.setShown(false);
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(settingsListener);
        HackerWallpaperService.reset();
    }
}
