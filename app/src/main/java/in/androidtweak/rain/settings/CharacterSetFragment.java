package in.androidtweak.rain.settings;

import android.content.Context;
import android.graphics.Typeface;

import com.androidtweak.rain.R;

import java.util.ArrayList;
import java.util.List;

/** Character set picker page; each set is previewed in the selected font. */
public class CharacterSetFragment extends ChoiceFragment {

    @Override
    protected int getTitleRes() {
        return R.string.pref_char_set;
    }

    @Override
    protected List<Choice> getChoices() {
        Context context = requireContext();
        Typeface font = FontPreference.getTypeface(context, FontPreference.getSelected(context));

        List<Choice> choices = new ArrayList<>();
        for (String name : context.getResources().getStringArray(R.array.character_sets)) {
            choices.add(new Choice(name, name, CharacterSetPreference.getCharacters(name), font));
        }
        return choices;
    }

    @Override
    protected String getSelectedValue() {
        return CharacterSetPreference.getSelected(requireContext());
    }

    @Override
    protected void onChoiceSelected(String value) {
        CharacterSetPreference.setSelected(requireContext(), value);
    }
}
