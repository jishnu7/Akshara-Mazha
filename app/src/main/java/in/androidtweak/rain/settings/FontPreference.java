package in.androidtweak.rain.settings;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import in.androidtweak.rain.SettingsActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FontPreference  {
    public static final String DEFAULT = "manjari";

    /** Font resources, keyed by the values stored for preference_font_name */
    private static final Map<String, Integer> FONTS = new HashMap<>();
    static {
        FONTS.put("manjari", R.font.manjari);
        FONTS.put("meera", R.font.meera);
        FONTS.put("rachana", R.font.rachana);
        FONTS.put("gayathri", R.font.gayathri);
        FONTS.put("keraleeyam", R.font.keraleeyam);
        FONTS.put("chilanka", R.font.chilanka);
        FONTS.put("uroob", R.font.uroob);
        FONTS.put("dyuthi", R.font.dyuthi);
        FONTS.put("ishtika", R.font.ishtika);
        FONTS.put("karumbi", R.font.karumbi);
    }

    public static Typeface getTypeface(Context context, String name) {
        Integer font = FONTS.get(name);
        return ResourcesCompat.getFont(context, font != null ? font : R.font.manjari);
    }

    /** The stored font name, e.g. "manjari" */
    public static String getSelected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_FONT_PREFS, DEFAULT);
    }

    /** The Malayalam label shown for a stored font name */
    public static String getDisplayName(Context context, String name) {
        String[] names = context.getResources().getStringArray(R.array.font_names);
        String[] labels = context.getResources().getStringArray(R.array.font_name_labels);
        int index = Arrays.asList(names).indexOf(name);
        return labels[index >= 0 ? index : 0];
    }
}
