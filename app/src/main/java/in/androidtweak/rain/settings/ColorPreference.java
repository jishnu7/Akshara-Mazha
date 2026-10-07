package in.androidtweak.rain.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.androidtweak.rain.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.margaritov.preference.colorpicker.ColorPickerPanelView;
import net.margaritov.preference.colorpicker.ColorPickerView;

/** A color preference showing a swatch, edited in a Material dialog with a color picker. */
public class ColorPreference extends Preference {

    private int color = Color.BLACK;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_color_swatch);
    }

    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        return a.getColor(index, Color.BLACK);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        int fallback = defaultValue != null ? (Integer) defaultValue : Color.BLACK;
        color = getPersistedInt(fallback);
        persistInt(color);
    }

    private void setColor(int newColor) {
        color = newColor;
        persistInt(color);
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View swatch = holder.findViewById(R.id.color_swatch);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        int strokeWidth = Math.round(getContext().getResources().getDisplayMetrics().density);
        circle.setStroke(strokeWidth, MaterialColors.getColor(swatch, com.google.android.material.R.attr.colorOutline));
        swatch.setBackground(circle);
    }

    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_color_picker, null);

        final ColorPickerView picker = view.findViewById(R.id.color_picker_view);
        ColorPickerPanelView oldPanel = view.findViewById(R.id.old_color_panel);
        final ColorPickerPanelView newPanel = view.findViewById(R.id.new_color_panel);

        oldPanel.setColor(color);
        newPanel.setColor(color);
        picker.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int c) {
                newPanel.setColor(c);
            }
        });
        picker.setColor(color);

        builder.setTitle(getTitle())
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int newColor = picker.getColor();
                        if (callChangeListener(newColor)) {
                            setColor(newColor);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
