package in.androidtweak.rain.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.androidtweak.rain.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

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

        setSwatch(holder.findViewById(R.id.color_swatch), color);
    }

    /** Draws a color as an outlined circle, so dark colors stay visible on dark surfaces */
    private static void setSwatch(View view, int color) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        int strokeWidth = Math.round(view.getResources().getDisplayMetrics().density);
        circle.setStroke(strokeWidth, MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutline));
        view.setBackground(circle);
    }

    private static boolean isNearBlack(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[2] < 0.05f;
    }

    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_color_picker, null);

        final ColorPickerView picker = view.findViewById(R.id.color_picker_view);
        final BrightnessSlideBar brightness = view.findViewById(R.id.brightness_slider);
        final View newColor = view.findViewById(R.id.new_color);

        setSwatch(view.findViewById(R.id.old_color), color);
        setSwatch(newColor, color);
        picker.attachBrightnessSlider(brightness);
        picker.setColorListener(new ColorEnvelopeListener() {
            @Override
            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                setSwatch(newColor, envelope.getColor());
            }
        });
        picker.setInitialColor(color);
        // Starting from black, picking a hue on the wheel would stay black until the
        // brightness slider is moved, so raise brightness on the first wheel touch
        picker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isNearBlack(picker.getColor())) {
                    brightness.setSelectorPosition(1f);
                }
                return false;
            }
        });

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
