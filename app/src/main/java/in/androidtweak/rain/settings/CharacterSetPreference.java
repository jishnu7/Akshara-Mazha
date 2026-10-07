package in.androidtweak.rain.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.androidtweak.rain.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class CharacterSetPreference extends Preference {
    public static final String CHARSET_DEFAULT = "അക്ഷരങ്ങള്‍";
    public static final String ML_BINARY_CHAR_SET = "൦ ൧";
    public static final String ML_NUM_CHAR_SET = "൦ ൧ ൨ ൩ ൪ ൫ ൬ ൭ ൮ ൯";
    public static final String ML_CHAR_SET = "അ ആ ഇ ഉ ഋ ഌ എ ഏ ഒ ക ഖ ഗ ഘ ങ ച ഛ ജ ഝ ഞ ട ഠ ഡ ഢ ണ ത ധ ദ ഥ ന പ ഫ ബ ഭ മ യ ര ല വ ശ ഷ സ ഹ ള ഴ റ";

    private EditText editText;
    private AlertDialog dialog;
    private String selectedName;

    public CharacterSetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setSummaryProvider(new SummaryProvider<CharacterSetPreference>() {
            @Override
            public CharSequence provideSummary(@NonNull CharacterSetPreference preference) {
                return getContext().getString(R.string.pref_char_set_summary, getCharacterSetName());
            }
        });
    }

    private String getCharacterSetName() {
        return getSharedPreferences().getString("character_set_name", CHARSET_DEFAULT);
    }

    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        View view = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.preference_dialog_character_set, null);

        final String[] characterSets = getContext().getResources().getStringArray(R.array.character_sets);
        final MaterialAutoCompleteTextView nameView = view.findViewById(R.id.preference_character_set_name);
        nameView.setSimpleItems(characterSets);
        nameView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                updateEditText(characterSets[position]);
            }
        });

        editText = view.findViewById(R.id.preference_character_set);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                updatePositiveButton();
            }
        });

        dialog = builder.setTitle(getTitle())
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        save();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        updateEditText(getCharacterSetName());
        nameView.setText(selectedName, false);
    }

    private void updatePositiveButton() {
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(editText.length() > 0);
        }
    }

    private void updateEditText(String characterSetName) {
        String characterSet;

        if (characterSetName.equals("അക്ഷരങ്ങള്‍")) {
            characterSet = ML_CHAR_SET;
            editText.setEnabled(false);
        } else if (characterSetName.equals("അക്കങ്ങള്‍")) {
            characterSet = ML_NUM_CHAR_SET;
            editText.setEnabled(false);
        } else if (characterSetName.equals("ബൈനറി")) {
            characterSet = ML_BINARY_CHAR_SET;
            editText.setEnabled(false);
        } else if (characterSetName.equals("Custom (random characters)")) {
            editText.setEnabled(true);
            characterSet = getSharedPreferences().getString("custom_character_set", "");
        } else if (characterSetName.equals("Custom (exact text)")) {
            editText.setEnabled(true);
            characterSet = getSharedPreferences().getString("custom_character_string", "");
        } else {
            if (!characterSetName.equals("Custom")) { // Legacy charset name
                throw new RuntimeException("Invalid character set " + characterSetName);
            } else {
                getSharedPreferences().edit().putString("character_set_name", "Custom (random characters)")
                        .commit();
                characterSetName = "Custom (random characters)";
                editText.setEnabled(true);
                characterSet = getSharedPreferences().getString("custom_character_set", "");
            }
        }

        selectedName = characterSetName;
        editText.setText(characterSet);
        updatePositiveButton();
    }

    private void save() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString("character_set_name", selectedName);
        if (selectedName.equals("Custom (random characters)")) {
            editor.putString("custom_character_set", editText.getText().toString());
        } else if (selectedName.equals("Custom (exact text)")) {
            editor.putString("custom_character_string", editText.getText().toString());
        }
        editor.commit();
        notifyChanged();
    }
}
