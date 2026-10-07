package in.androidtweak.rain.settings;

import android.content.Context;

import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import in.androidtweak.rain.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

/** Font picker page; each option is previewed in its own font. */
public class FontFragment extends ChoiceFragment {

    @Override
    protected int getTitleRes() {
        return R.string.pref_font;
    }

    @Override
    protected List<Choice> getChoices() {
        Context context = requireContext();
        String[] names = context.getResources().getStringArray(R.array.font_names);
        String[] labels = context.getResources().getStringArray(R.array.font_name_labels);

        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            choices.add(new Choice(names[i], labels[i], CharacterSetPreference.ML_CHAR_SET,
                    FontPreference.getTypeface(context, names[i])));
        }
        return choices;
    }

    @Override
    protected String getSelectedValue() {
        return FontPreference.getSelected(requireContext());
    }

    @Override
    protected void onChoiceSelected(String value) {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                .putString(SettingsActivity.KEY_FONT_PREFS, value)
                .apply();
    }
}
