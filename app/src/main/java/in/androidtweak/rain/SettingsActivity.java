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

    private static final int COLLAPSED_SHEET_DP = 112;

    private RainPreviewView preview;
    private BottomSheetBehavior<View> sheet;
    private View sheetHeader;
    private View setWallpaper;
    private View settingsContainer;
    private int navigationBarHeight;
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
        View header = findViewById(R.id.header);

        View sheetView = findViewById(R.id.settings_sheet);
        sheet = BottomSheetBehavior.from(sheetView);
        if (savedInstanceState == null) {
            sheet.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }
        settingsContainer = findViewById(R.id.settings_container);
        sheet.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                fitSettingsToSheet(bottomSheet);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                fitSettingsToSheet(bottomSheet);
            }
        });
        sheetView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                v.post(() -> fitSettingsToSheet(v)));
        sheetHeader = findViewById(R.id.sheet_header);
        setWallpaper = findViewById(R.id.set_wallpaper);
        setWallpaper.setOnClickListener(v -> setAsWallpaper());
        sheetTitle = findViewById(R.id.sheet_title);
        findViewById(R.id.sheet_back).setOnClickListener(v -> getSupportFragmentManager().popBackStack());

        applyInsets(header, sheetView);

        FragmentManager fragments = getSupportFragmentManager();
        fragments.addOnBackStackChangedListener(this::updateSheetHeader);
        if (savedInstanceState == null) {
            fragments.beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        updateSheetHeader();
    }

    private void fitSettingsToSheet(View sheetView) {
        int screenBottom = ((View) sheetView.getParent()).getHeight();
        int offScreen = Math.max(0, sheetView.getBottom() - screenBottom);
        int bottom = Math.max(0, offScreen + navigationBarHeight - sheetView.getPaddingBottom());
        if (settingsContainer.getPaddingBottom() != bottom) {
            settingsContainer.setPadding(0, 0, 0, bottom);
        }
    }

    private void applyInsets(View header, View sheetView) {
        float density = getResources().getDisplayMetrics().density;
        int headerStart = header.getPaddingStart();
        int headerEnd = header.getPaddingEnd();
        header.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                sheet.setExpandedOffset(v.getHeight()));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            header.setPaddingRelative(headerStart + bars.left, bars.top, headerEnd + bars.right, 0);
            ViewCompat.setPaddingRelative(sheetView, bars.left, 0, bars.right, 0);
            sheet.setPeekHeight(bars.bottom + Math.round(COLLAPSED_SHEET_DP * density));
            navigationBarHeight = bars.bottom;
            fitSettingsToSheet(sheetView);
            return insets;
        });
    }

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
