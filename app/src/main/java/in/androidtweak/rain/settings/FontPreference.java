package in.androidtweak.rain.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.androidtweak.rain.R;
import in.androidtweak.rain.Refreshable;

import java.util.Arrays;
import java.util.List;

public class FontPreference extends DialogPreference implements Refreshable {
    public static final String DEFAULT_FONT = "Meera";
    public static final String FONT_MEERA = "fonts/Meera.ttf";
    public static final String FONT_MANJARI = "fonts/Manjari-Regular.ttf";
    public static final String FONT_CHILANKA = "fonts/Chilanka-Regular.ttf";
    public static final String FONT_KERALEEYAM = "fonts/Keraleeyam.ttf";
    public static final String FONT_GAYATHRI = "fonts/Gayathri-Regular.ttf";
    public static final String FONT_ISHTIKA = "fonts/ishtika.ttf";
    public static final String FONT_DYUTHI = "fonts/Dyuthi-Regular.ttf";

    private Spinner spinner;

    public FontPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_dialog_font_names);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        SharedPreferences sp = getSharedPreferences();
        String fontName = sp.getString("preference_font_name", DEFAULT_FONT);

        Resources resources = view.getContext().getResources();
        List<String> characterSets = Arrays.asList(resources.getStringArray(R.array.font_names));

        spinner = (Spinner) view.findViewById(R.id.preference_font_name);
        spinner.setSelection(characterSets.indexOf(fontName));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void refresh(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        setSummary("ഇപ്പോള്\u200D ഉപയോഗിക്കുന്ന ഫോണ്ട്: " + sp.getString("preference_font_name", DEFAULT_FONT));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            String fontName = spinner.getSelectedItem().toString();
            editor.putString("preference_font_name", fontName);
            editor.commit();
            setSummary("ഇപ്പോള്\u200D ഉപയോഗിക്കുന്ന ഫോണ്ട്: " + fontName);
        }
    }
}
