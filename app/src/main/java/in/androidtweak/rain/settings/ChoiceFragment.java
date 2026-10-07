package in.androidtweak.rain.settings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;

import com.androidtweak.rain.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** A settings page that lets the user pick one option from a list of cards. */
public abstract class ChoiceFragment extends Fragment {

    protected static final class Choice {
        final String value;
        final CharSequence title;
        final CharSequence summary;
        final Typeface typeface;

        Choice(String value, CharSequence title, CharSequence summary, @Nullable Typeface typeface) {
            this.value = value;
            this.title = title;
            this.summary = summary;
            this.typeface = typeface;
        }
    }

    private final List<MaterialCardView> cards = new ArrayList<>();
    private List<Choice> choices;

    protected abstract int getTitleRes();

    protected abstract List<Choice> getChoices();

    protected abstract String getSelectedValue();

    protected abstract void onChoiceSelected(String value);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choice_list, container, false);
        LinearLayout list = view.findViewById(R.id.choice_list);

        cards.clear();
        choices = getChoices();
        for (final Choice choice : choices) {
            final MaterialCardView card = (MaterialCardView) inflater.inflate(R.layout.item_choice, list, false);
            TextView title = card.findViewById(R.id.choice_title);
            TextView summary = card.findViewById(R.id.choice_summary);

            title.setText(choice.title);
            summary.setText(choice.summary);
            if (choice.typeface != null) {
                title.setTypeface(choice.typeface);
                summary.setTypeface(choice.typeface);
            }
            card.setOnClickListener(v -> select(choice.value));
            // Only one option can be picked, so announce cards as radio buttons
            ViewCompat.setAccessibilityDelegate(card, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(card.isChecked());
                }
            });

            cards.add(card);
            list.addView(card);
        }
        updateChecked(getSelectedValue());

        // Let the list scroll behind the navigation bar but keep its last item reachable
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(getTitleRes());
    }

    private void select(String value) {
        onChoiceSelected(value);
        updateChecked(value);
    }

    private void updateChecked(String value) {
        for (int i = 0; i < choices.size(); i++) {
            cards.get(i).setChecked(choices.get(i).value.equals(value));
        }
    }
}
