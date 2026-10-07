package in.androidtweak.rain.settings;

import android.content.Context;

import com.androidtweak.rain.R;

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
        String preview = context.getString(CharacterSet.LETTERS.charactersRes);

        List<Choice> choices = new ArrayList<>();
        for (Font font : Font.values()) {
            choices.add(new Choice(font.name(), context.getString(font.labelRes), preview,
                    font.getTypeface(context)));
        }
        return choices;
    }

    @Override
    protected String getSelectedValue() {
        return Font.getSelected(requireContext()).name();
    }

    @Override
    protected void onChoiceSelected(String value) {
        Font.setSelected(requireContext(), Font.valueOf(value));
    }
}
