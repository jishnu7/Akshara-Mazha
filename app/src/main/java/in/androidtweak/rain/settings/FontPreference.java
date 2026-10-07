package in.androidtweak.rain.settings;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.androidtweak.rain.R;

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
}
