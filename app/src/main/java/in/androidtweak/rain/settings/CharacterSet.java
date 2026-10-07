package in.androidtweak.rain.settings;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.androidtweak.rain.R;

/** The built-in character sets the wallpaper rains. All text lives in strings.xml. */
public enum CharacterSet {
    LETTERS(R.string.charset_letters_value, R.string.charset_letters, R.string.charset_letters_characters),
    NUMBERS(R.string.charset_numbers_value, R.string.charset_numbers, R.string.charset_numbers_characters),
    BINARY(R.string.charset_binary_value, R.string.charset_binary, R.string.charset_binary_characters);

    public static final CharacterSet DEFAULT = LETTERS;

    /** What's stored in the character_set_name preference */
    @StringRes
    public final int valueRes;
    @StringRes
    public final int labelRes;
    /** Space separated characters to rain */
    @StringRes
    public final int charactersRes;

    CharacterSet(@StringRes int valueRes, @StringRes int labelRes, @StringRes int charactersRes) {
        this.valueRes = valueRes;
        this.labelRes = labelRes;
        this.charactersRes = charactersRes;
    }

    /** The set for a stored value, or null for a legacy custom set */
    @Nullable
    public static CharacterSet fromValue(Context context, String value) {
        for (CharacterSet set : values()) {
            if (context.getString(set.valueRes).equals(value)) {
                return set;
            }
        }
        return null;
    }
}
