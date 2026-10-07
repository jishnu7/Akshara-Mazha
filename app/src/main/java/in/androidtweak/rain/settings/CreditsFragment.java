package in.androidtweak.rain.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.androidtweak.rain.R;

/** Credits page: each bundled font with its designer and license, linking to its source. */
public class CreditsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choice_list, container, false);
        LinearLayout list = view.findViewById(R.id.choice_list);
        Context context = requireContext();

        for (Font font : Font.values()) {
            View card = inflater.inflate(R.layout.item_credit, list, false);
            TextView name = card.findViewById(R.id.credit_name);
            name.setText(font.labelRes);
            name.setTypeface(font.getTypeface(context));
            TextView designer = card.findViewById(R.id.credit_designer);
            designer.setText(getString(R.string.credits_designer, font.designer));
            TextView license = card.findViewById(R.id.credit_license);
            license.setText(getString(R.string.credits_license, font.license.label));
            card.setOnClickListener(v -> openSource(font));
            list.addView(card);
        }

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
        requireActivity().setTitle(R.string.pref_credits);
    }

    private void openSource(Font font) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(font.sourceUrl)));
        } catch (ActivityNotFoundException e) {
            // No browser; the license is still named on the card
        }
    }
}
