package in.androidtweak.rain.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

/** Settings row for the character set; the choice itself is made on CharacterSetFragment. */
public class CharacterSetPreference extends Preference {
    public static final String KEY_CHARACTER_SET_NAME = "character_set_name";
    public static final String CHARSET_DEFAULT = "അക്ഷരങ്ങള്‍";
    public static final String ML_BINARY_CHAR_SET = "൦ ൧";
    public static final String ML_NUM_CHAR_SET = "൦ ൧ ൨ ൩ ൪ ൫ ൬ ൭ ൮ ൯";
    public static final String ML_CHAR_SET = "അ ആ ഇ ഉ ഋ ഌ എ ഏ ഒ ക ഖ ഗ ഘ ങ ച ഛ ജ ഝ ഞ ട ഠ ഡ ഢ ണ ത ധ ദ ഥ ന പ ഫ ബ ഭ മ യ ര ല വ ശ ഷ സ ഹ ള ഴ റ";

    public CharacterSetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setSummaryProvider(new SummaryProvider<CharacterSetPreference>() {
            @Override
            public CharSequence provideSummary(@NonNull CharacterSetPreference preference) {
                return getContext().getString(R.string.pref_char_set_summary, getSelected(getContext()));
            }
        });
    }

    /** The stored character set name, one of R.array.character_sets */
    public static String getSelected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_CHARACTER_SET_NAME, CHARSET_DEFAULT);
    }

    public static void setSelected(Context context, String name) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_CHARACTER_SET_NAME, name).apply();
    }

    /** The characters in one of the built-in sets */
    public static String getCharacters(String name) {
        if (name.equals("അക്കങ്ങള്‍")) {
            return ML_NUM_CHAR_SET;
        } else if (name.equals("ബൈനറി")) {
            return ML_BINARY_CHAR_SET;
        }
        return ML_CHAR_SET;
    }
}
