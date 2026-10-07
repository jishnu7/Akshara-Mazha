package in.androidtweak.rain;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidtweak.rain.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        Preference setAsWallpaper = findPreference("set_as_wallpaper");
        setAsWallpaper.setOnPreferenceClickListener(pref -> {
            setAsWallpaper();
            return true;
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Material 3 lists separate sections with spacing, not dividers
        setDivider(null);

        // Let the list scroll behind the navigation bar but keep its last item reachable
        RecyclerView list = getListView();
        list.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(list, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ListPreference) {
            showListDialog((ListPreference) preference);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    /** Shows a ListPreference as a Material single-choice dialog */
    private void showListDialog(final ListPreference preference) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(preference.getTitle())
                .setSingleChoiceItems(preference.getEntries(),
                        preference.findIndexOfValue(preference.getValue()),
                        (dialog, which) -> {
                            String value = preference.getEntryValues()[which].toString();
                            if (preference.callChangeListener(value)) {
                                preference.setValue(value);
                            }
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setAsWallpaper() {
        Context context = requireContext();
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(context, HackerWallpaperService.class));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Some devices don't support picking a specific live wallpaper
            startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
        }
    }

    /** Restores every preference to its default value and rebuilds the screen */
    void resetToDefaults() {
        Context context = requireContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceManager.setDefaultValues(context, R.xml.prefs, true);
        onCreatePreferences(null, null);
    }
}
