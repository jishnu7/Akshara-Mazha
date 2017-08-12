package in.androidtweak.rain.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.androidtweak.rain.R;
import in.androidtweak.rain.Refreshable;

import java.util.Arrays;
import java.util.List;

public class CharacterSetPreference extends DialogPreference implements Refreshable {
    public static final String CHARSET_DEFAULT = "അക്ഷരങ്ങള്\u200D";
    public static final String ML_BINARY_CHAR_SET = "൦ ൧";
    public static final String ML_NUM_CHAR_SET = "൦ ൧ ൨ ൩ ൪ ൫ ൬ ൭ ൮ ൯";
    public static final String ML_CHAR_SET = "അ ആ ഇ ഉ ഋ ഌ എ ഏ ഒ ക ഖ ഗ ഘ ങ ച ഛ ജ ഝ ഞ ട ഠ ഡ ഢ ണ ത ധ ദ ഥ ന പ ഫ ബ ഭ മ യ ര ല വ ശ ഷ സ ഹ ള ഴ റ";

    private EditText editText;
    private Spinner spinner;

    public CharacterSetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_dialog_character_set);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        String characterSetName = getSharedPreferences().getString("character_set_name", CHARSET_DEFAULT);
        updateEditText(characterSetName);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        SharedPreferences sp = getSharedPreferences();
        String characterSetName = sp.getString("character_set_name", CHARSET_DEFAULT);

        Resources resources = view.getContext().getResources();
        List<String> characterSets = Arrays.asList(resources.getStringArray(R.array.character_sets));

        editText = (EditText) view.findViewById(R.id.preference_character_set);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                disablePosButton(s.length() == 0);
            }
        });

        spinner = (Spinner) view.findViewById(R.id.preference_character_set_name);
        spinner.setSelection(characterSets.indexOf(characterSetName));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String characterSetName = spinner.getSelectedItem().toString();
                updateEditText(characterSetName);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void disablePosButton(boolean disable) {
        Button posButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        if (disable) {
            posButton.setEnabled(false);
        } else {
            posButton.setEnabled(true);
        }
    }

    private void updateEditText(String characterSetName) {
        String characterSet;

        if (characterSetName.equals("അക്ഷരങ്ങള്\u200D")) {
            characterSet = ML_CHAR_SET;
            editText.setEnabled(false);
        } else if (characterSetName.equals("അക്കങ്ങള്\u200D")) {
            characterSet = ML_NUM_CHAR_SET;
            editText.setEnabled(false);
        } else if (characterSetName.equals("ബൈനറി")) {
            characterSet = ML_BINARY_CHAR_SET;
            editText.setEnabled(false);
        } else if (characterSetName.equals("Custom (random characters)")) {
            editText.setEnabled(true);
            characterSet = getSharedPreferences().getString("custom_character_set", "");
            disablePosButton(characterSet.length() == 0);
        } else if (characterSetName.equals("Custom (exact text)")) {
            editText.setEnabled(true);
            characterSet = getSharedPreferences().getString("custom_character_string", "");
            disablePosButton(characterSet.length() == 0);
        } else {
            if (!characterSetName.equals("Custom")) { // Legacy charset name
                throw new RuntimeException("Invalid character set " + characterSetName);
            } else {
                getSharedPreferences().edit().putString("character_set_name", "Custom (random characters)")
                        .commit();
                editText.setEnabled(true);
                characterSet = getSharedPreferences().getString("custom_character_set", "");
                disablePosButton(characterSet.length() == 0);
            }
        }

        editText.setText(characterSet);
    }

    @Override
    public void refresh(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        setSummary("ഇപ്പോള്\u200D ഉപയോഗിക്കുന്ന ചിഹ്നങ്ങള്\u200D: " + sp.getString("character_set_name", CHARSET_DEFAULT));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            String characterSetName = spinner.getSelectedItem().toString();
            editor.putString("character_set_name", characterSetName);
            if (characterSetName.equals("Custom (random characters)")) {
                editor.putString("custom_character_set", editText.getText().toString());
            } else if (characterSetName.equals("Custom (exact text)")) {
                editor.putString("custom_character_string", editText.getText().toString());
            }
            editor.commit();
            setSummary("ഇപ്പോള്\u200D ഉപയോഗിക്കുന്ന ചിഹ്നങ്ങള്\u200D: " + characterSetName);
        }
    }
}
