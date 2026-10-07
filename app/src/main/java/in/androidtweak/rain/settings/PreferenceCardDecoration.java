package in.androidtweak.rain.settings;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Draws the rows of each preference group as one card, in the Android 16 settings style:
 * large outer corners, small inner corners and a thin gap between rows. Category headers
 * stay outside the cards.
 */
public class PreferenceCardDecoration extends RecyclerView.ItemDecoration {

    private final Supplier<PreferenceScreen> screen;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final float outerRadius;
    private final float innerRadius;
    private final int margin;
    private final int gap;

    public PreferenceCardDecoration(@NonNull RecyclerView list, @NonNull Supplier<PreferenceScreen> screen) {
        this.screen = screen;
        float density = list.getResources().getDisplayMetrics().density;
        outerRadius = 24 * density;
        innerRadius = 4 * density;
        margin = Math.round(16 * density);
        gap = Math.round(2 * density);
        paint.setColor(MaterialColors.getColor(list,
                com.google.android.material.R.attr.colorSurfaceContainer));
    }

    /** The rows in adapter order: each top-level preference, followed by a category's children */
    private List<Preference> flatten() {
        List<Preference> rows = new ArrayList<>();
        PreferenceScreen root = screen.get();
        if (root != null) {
            addVisible(root, rows);
        }
        return rows;
    }

    private static void addVisible(PreferenceGroup group, List<Preference> rows) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (!preference.isVisible()) {
                continue;
            }
            rows.add(preference);
            if (preference instanceof PreferenceGroup) {
                addVisible((PreferenceGroup) preference, rows);
            }
        }
    }

    private static boolean isCard(List<Preference> rows, int position) {
        return position >= 0 && position < rows.size() && !(rows.get(position) instanceof PreferenceCategory);
    }

    private static boolean sameCard(List<Preference> rows, int position, int other) {
        return isCard(rows, position) && isCard(rows, other)
                && rows.get(position).getParent() == rows.get(other).getParent();
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        List<Preference> rows = flatten();
        outRect.left = margin;
        outRect.right = margin;
        if (!isCard(rows, position)) {
            return;
        }
        final boolean first = !sameCard(rows, position, position - 1);
        final boolean last = !sameCard(rows, position, position + 1);
        if (!first) {
            outRect.top = gap;
        } else if (position == 0) {
            outRect.top = margin / 2;
        } else {
            // A category header already spaces cards apart; another card doesn't
            outRect.top = isCard(rows, position - 1) ? margin : 0;
        }
        outRect.bottom = position == rows.size() - 1 ? margin : 0;

        // Clip the row's ripple to the card's outer corners
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                int radius = Math.round(outerRadius);
                int top = first ? 0 : -radius;
                int bottom = last ? v.getHeight() : v.getHeight() + radius;
                outline.setRoundRect(0, top, v.getWidth(), bottom, outerRadius);
            }
        });
        view.setClipToOutline(true);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        List<Preference> rows = flatten();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (!isCard(rows, position)) {
                continue;
            }
            float top = sameCard(rows, position, position - 1) ? innerRadius : outerRadius;
            float bottom = sameCard(rows, position, position + 1) ? innerRadius : outerRadius;
            rect.set(child.getLeft(), child.getTop() + child.getTranslationY(),
                    child.getRight(), child.getBottom() + child.getTranslationY());
            path.reset();
            path.addRoundRect(rect, new float[]{top, top, top, top, bottom, bottom, bottom, bottom},
                    Path.Direction.CW);
            canvas.drawPath(path, paint);
        }
    }
}
