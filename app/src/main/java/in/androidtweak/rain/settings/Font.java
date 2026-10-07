package in.androidtweak.rain.settings;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.FontRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import in.androidtweak.rain.SettingsActivity;

/** The Malayalam fonts the wallpaper can use, in the order they're offered. */
public enum Font {
    MANJARI("manjari", R.font.manjari, R.string.font_manjari),
    MEERA("meera", R.font.meera, R.string.font_meera),
    RACHANA("rachana", R.font.rachana, R.string.font_rachana),
    GAYATHRI("gayathri", R.font.gayathri, R.string.font_gayathri),
    KERALEEYAM("keraleeyam", R.font.keraleeyam, R.string.font_keraleeyam),
    CHILANKA("chilanka", R.font.chilanka, R.string.font_chilanka),
    UROOB("uroob", R.font.uroob, R.string.font_uroob),
    DYUTHI("dyuthi", R.font.dyuthi, R.string.font_dyuthi),
    ISHTIKA("ishtika", R.font.ishtika, R.string.font_ishtika),
    KARUMBI("karumbi", R.font.karumbi, R.string.font_karumbi);

    public static final Font DEFAULT = MANJARI;

    /** What's stored in the preference_font_name preference */
    public final String value;
    @FontRes
    public final int fontRes;
    @StringRes
    public final int labelRes;

    Font(String value, @FontRes int fontRes, @StringRes int labelRes) {
        this.value = value;
        this.fontRes = fontRes;
        this.labelRes = labelRes;
    }

    public Typeface getTypeface(Context context) {
        return ResourcesCompat.getFont(context, fontRes);
    }

    /** The font for a stored value, falling back to the default */
    public static Font fromValue(String value) {
        for (Font font : values()) {
            if (font.value.equals(value)) {
                return font;
            }
        }
        return DEFAULT;
    }

    public static Font getSelected(Context context) {
        return fromValue(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_FONT_PREFS, DEFAULT.value));
    }

    public static void setSelected(Context context, Font font) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(SettingsActivity.KEY_FONT_PREFS, font.value)
                .apply();
    }
}
