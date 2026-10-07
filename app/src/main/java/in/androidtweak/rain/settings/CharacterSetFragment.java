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
        Typeface font = Font.getSelected(context).getTypeface(context);

        List<Choice> choices = new ArrayList<>();
        for (CharacterSet set : CharacterSet.values()) {
            choices.add(new Choice(set.name(), context.getString(set.labelRes),
                    context.getString(set.charactersRes), font));
        }
        return choices;
    }

    @Override
    protected String getSelectedValue() {
        Context context = requireContext();
        CharacterSet set = CharacterSet.fromValue(context, CharacterSetPreference.getSelectedValue(context));
        return set != null ? set.name() : "";
    }

    @Override
    protected void onChoiceSelected(String value) {
        CharacterSetPreference.setSelected(requireContext(), CharacterSet.valueOf(value));
    }
}
