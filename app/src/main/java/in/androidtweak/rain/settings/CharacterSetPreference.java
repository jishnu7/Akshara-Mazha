package in.androidtweak.rain.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

/** Settings row for the character set; the choice itself is made on CharacterSetFragment. */
public class CharacterSetPreference extends Preference {
    public static final String KEY_CHARACTER_SET_NAME = "character_set_name";

    public CharacterSetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setSummaryProvider(new SummaryProvider<CharacterSetPreference>() {
            @Override
            public CharSequence provideSummary(@NonNull CharacterSetPreference preference) {
                String value = getSelectedValue(getContext());
                CharacterSet set = CharacterSet.fromValue(getContext(), value);
                String label = set != null ? getContext().getString(set.labelRes) : value;
                return getContext().getString(R.string.pref_char_set_summary, label);
            }
        });
    }

    /** The stored character set value; a CharacterSet value, or a legacy custom set name */
    public static String getSelectedValue(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_CHARACTER_SET_NAME, context.getString(CharacterSet.DEFAULT.valueRes));
    }

    public static void setSelected(Context context, CharacterSet set) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(KEY_CHARACTER_SET_NAME, context.getString(set.valueRes))
                .apply();
    }
}
