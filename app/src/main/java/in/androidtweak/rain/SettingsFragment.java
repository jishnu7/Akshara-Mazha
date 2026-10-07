package in.androidtweak.rain;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidtweak.rain.R;

import in.androidtweak.rain.settings.FontPreference;
import in.androidtweak.rain.settings.PreferenceCardDecoration;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        Preference setAsWallpaper = findPreference("set_as_wallpaper");
        setAsWallpaper.setOnPreferenceClickListener(pref -> {
            setAsWallpaper();
            return true;
        });

        Preference font = findPreference(SettingsActivity.KEY_FONT_PREFS);
        font.setSummaryProvider(pref -> FontPreference.getDisplayName(requireContext(),
                FontPreference.getSelected(requireContext())));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Material 3 lists separate sections with spacing, not dividers
        setDivider(null);

        // Let the list scroll behind the navigation bar but keep its last item reachable
        RecyclerView list = getListView();
        list.setClipToPadding(false);
        list.addItemDecoration(new PreferenceCardDecoration(list, this::getPreferenceScreen));
        ViewCompat.setOnApplyWindowInsetsListener(list, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });

        // Reset applies to this page only, so the action lives with it
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.activity_settings, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_reset_to_defaults) {
                    resetToDefaults();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.app_name);
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
    private void resetToDefaults() {
        Context context = requireContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceManager.setDefaultValues(context, R.xml.prefs, true);
        onCreatePreferences(null, null);
    }
}
