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

import java.util.ArrayList;
import java.util.List;

/** A settings page that lets the user pick one option from a list of radio rows. */
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

    private final List<RadioButton> radios = new ArrayList<>();
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

        radios.clear();
        choices = getChoices();
        for (final Choice choice : choices) {
            View row = inflater.inflate(R.layout.item_choice, list, false);
            TextView title = row.findViewById(R.id.choice_title);
            TextView summary = row.findViewById(R.id.choice_summary);
            final RadioButton radio = row.findViewById(R.id.choice_radio);

            title.setText(choice.title);
            summary.setText(choice.summary);
            if (choice.typeface != null) {
                title.setTypeface(choice.typeface);
                summary.setTypeface(choice.typeface);
            }
            row.setOnClickListener(v -> select(choice.value));
            // Announce the whole row as a radio button
            ViewCompat.setAccessibilityDelegate(row, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(radio.isChecked());
                }
            });

            radios.add(radio);
            list.addView(row);
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
            radios.get(i).setChecked(choices.get(i).value.equals(value));
        }
    }
}
