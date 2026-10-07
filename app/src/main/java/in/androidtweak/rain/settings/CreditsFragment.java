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

        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                getResources().getDimensionPixelSize(R.dimen.sheet_list_end_space));
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
