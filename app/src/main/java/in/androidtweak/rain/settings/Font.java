package in.androidtweak.rain.settings;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.FontRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import in.androidtweak.rain.SettingsActivity;

public enum Font {
    MANJARI("manjari", R.font.manjari, R.string.font_manjari,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/manjari"),
    GAYATHRI("gayathri", R.font.gayathri, R.string.font_gayathri,
            "Binoy Dominic", License.OFL, "https://gitlab.com/smc/fonts/gayathri"),
    MALINI("malini", R.font.malini, R.string.font_malini,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/Malini"),
    NUPURAM("nupuram", R.font.nupuram, R.string.font_nupuram,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/Nupuram"),
    UROOB("uroob", R.font.uroob, R.string.font_uroob,
            "Hussain K H", License.OFL, "https://gitlab.com/smc/fonts/uroob"),
    NUPURAM_CALLIGRAPHY("nupuram_calligraphy", R.font.nupuram_calligraphy, R.string.font_nupuram_calligraphy,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/Nupuram"),
    KERALEEYAM("keraleeyam", R.font.keraleeyam, R.string.font_keraleeyam,
            "Hussain K H", License.OFL, "https://gitlab.com/smc/fonts/keraleeyam"),
    ANJALI_OLD_LIPI("anjali_old_lipi", R.font.anjali_old_lipi, R.string.font_anjali_old_lipi,
            "Kevin & Siji", License.OFL, "https://gitlab.com/smc/fonts/anjalioldlipi"),
    CHILANKA("chilanka", R.font.chilanka, R.string.font_chilanka,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/chilanka"),
    DYUTHI("dyuthi", R.font.dyuthi, R.string.font_dyuthi,
            "Hiran Venugopalan, Hussain K H, Suresh P", License.OFL, "https://gitlab.com/smc/fonts/dyuthi"),
    KARUMBI("karumbi", R.font.karumbi, R.string.font_karumbi,
            "Kevin & Siji", License.OFL, "https://gitlab.com/smc/fonts/karumbi"),
    MEERA("meera", R.font.meera, R.string.font_meera,
            "Hussain K H, Suresh P", License.OFL, "https://gitlab.com/smc/fonts/meera"),
    RACHANA("rachana", R.font.rachana, R.string.font_rachana,
            "Hussain K H", License.OFL, "https://gitlab.com/smc/fonts/rachana"),
    RAGHU_MALAYALAM("raghu_malayalam", R.font.raghu_malayalam, R.string.font_raghu_malayalam,
            "Prof. R. K. Joshi, Rajith Kumar K. M.", License.GPL_2, "https://gitlab.com/smc/fonts/raghumalayalamsans"),
    SURUMA("suruma", R.font.suruma, R.string.font_suruma,
            "Suresh P", License.GPL_3_FONT_EXCEPTION, "https://gitlab.com/smc/fonts/suruma"),
    ISHTIKA("ishtika", R.font.ishtika, R.string.font_ishtika,
            "Kailash Nadh", License.OFL, "https://nadh.in/code/ishtika");

    public static final Font DEFAULT = MANJARI;

    /** What's stored in the preference_font_name preference */
    public final String value;
    @FontRes
    public final int fontRes;
    @StringRes
    public final int labelRes;
    public final String designer;
    public final License license;
    public final String sourceUrl;

    Font(String value, @FontRes int fontRes, @StringRes int labelRes,
         String designer, License license, String sourceUrl) {
        this.value = value;
        this.fontRes = fontRes;
        this.labelRes = labelRes;
        this.designer = designer;
        this.license = license;
        this.sourceUrl = sourceUrl;
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
