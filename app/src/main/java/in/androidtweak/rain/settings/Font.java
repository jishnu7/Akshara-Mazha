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
    NUPURAM_CALLIGRAPHY("nupuram_calligraphy", R.font.nupuram_calligraphy, R.string.font_nupuram_calligraphy,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/Nupuram"),
    ANJALI_OLD_LIPI("anjali_old_lipi", R.font.anjali_old_lipi, R.string.font_anjali_old_lipi,
            "Kevin & Siji", License.OFL, "https://gitlab.com/smc/fonts/anjalioldlipi"),
    CHILANKA("chilanka", R.font.chilanka, R.string.font_chilanka,
            "Santhosh Thottingal", License.OFL, "https://gitlab.com/smc/fonts/chilanka"),
    DYUTHI("dyuthi", R.font.dyuthi, R.string.font_dyuthi,
            "Hiran Venugopalan, Hussain K H, Suresh P", License.OFL, "https://gitlab.com/smc/fonts/dyuthi"),
    KARUMBI("karumbi", R.font.karumbi, R.string.font_karumbi,
            "Kevin & Siji", License.OFL, "https://gitlab.com/smc/fonts/karumbi"),
    RAGHU_MALAYALAM("raghu_malayalam", R.font.raghu_malayalam, R.string.font_raghu_malayalam,
            "Prof. R. K. Joshi, Rajith Kumar K. M.", License.GPL_2, "https://gitlab.com/smc/fonts/raghumalayalamsans"),
    SURUMA("suruma", R.font.suruma, R.string.font_suruma,
            "Suresh P", License.GPL_3_FONT_EXCEPTION, "https://gitlab.com/smc/fonts/suruma"),
    RIT_RACHANA("rit_rachana", R.font.rit_rachana, R.string.font_rit_rachana,
            "Hussain K H", License.OFL, "https://gitlab.com/rit-fonts/RIT-Rachana"),
    RIT_MEERA_NEW("rit_meera_new", R.font.rit_meera_new, R.string.font_rit_meera_new,
            "Hussain K H", License.OFL, "https://gitlab.com/rit-fonts/MeeraNew"),
    RIT_PANMANA("rit_panmana", R.font.rit_panmana, R.string.font_rit_panmana,
            "Hussain K H", License.OFL, "https://gitlab.com/rit-fonts/Panmana"),
    RIT_TN_JOY("rit_tn_joy", R.font.rit_tn_joy, R.string.font_rit_tn_joy,
            "Hussain K H, P K Ashok Kumar", License.OFL, "https://gitlab.com/rit-fonts/tnjoy"),
    RIT_KERALEEYAM("rit_keraleeyam", R.font.rit_keraleeyam, R.string.font_rit_keraleeyam,
            "Hussain K H", License.OFL, "https://gitlab.com/rit-fonts/rit-keraleeyam"),
    RIT_SUNDAR("rit_sundar", R.font.rit_sundar, R.string.font_rit_sundar,
            "Hussain K H, Narayana Bhattathiri", License.OFL, "https://gitlab.com/rit-fonts/Sundar"),
    RIT_UROOB("rit_uroob", R.font.rit_uroob, R.string.font_rit_uroob,
            "Hussain K H", License.OFL, "https://gitlab.com/rit-fonts/rit-uroob"),
    RIT_BAHADUR("rit_bahadur", R.font.rit_bahadur, R.string.font_rit_bahadur,
            "Hussain K H", License.OFL, "https://gitlab.com/rit-fonts/bahadur"),
    RIT_KARUNA("rit_karuna", R.font.rit_karuna, R.string.font_rit_karuna,
            "Narayana Bhattathiri", License.OFL, "https://gitlab.com/rit-fonts/karuna"),
    RIT_CHINGAM("rit_chingam", R.font.rit_chingam, R.string.font_rit_chingam,
            "Narayana Bhattathiri", License.OFL, "https://gitlab.com/rit-fonts/chingam"),
    RIT_EZHUTHU("rit_ezhuthu", R.font.rit_ezhuthu, R.string.font_rit_ezhuthu,
            "Narayana Bhattathiri", License.OFL, "https://gitlab.com/rit-fonts/ezhuthu"),
    RIT_KUTTY("rit_kutty", R.font.rit_kutty, R.string.font_rit_kutty,
            "Kutty Kodungallur", License.OFL, "https://gitlab.com/rit-fonts/Kutty"),
    RIT_THAARA("rit_thaara", R.font.rit_thaara, R.string.font_rit_thaara,
            "Karambir Singh Rohilla", License.OFL, "https://gitlab.com/rit-fonts/Thaara"),
    RIT_LEKHA("rit_lekha", R.font.rit_lekha, R.string.font_rit_lekha,
            "Rahul Radhakrishnan", License.OFL, "https://gitlab.com/rit-fonts/Lekha"),
    RIT_LASYA("rit_lasya", R.font.rit_lasya, R.string.font_rit_lasya,
            "Krishnakumar P V", License.OFL, "https://gitlab.com/rit-fonts/Lasya"),
    RIT_ALA("rit_ala", R.font.rit_ala, R.string.font_rit_ala,
            "Radhakrishnan V N, Aswathy J", License.OFL, "https://gitlab.com/rit-fonts/Ala"),
    RIT_KERAM("rit_keram", R.font.rit_keram, R.string.font_rit_keram,
            "Sanesh M V", License.OFL, "https://gitlab.com/rit-fonts/Keram"),
    RIT_INDIRA("rit_indira", R.font.rit_indira, R.string.font_rit_indira,
            "Sudheer S", License.OFL, "https://gitlab.com/rit-fonts/Indira"),
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
