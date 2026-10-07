package in.androidtweak.rain.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.androidtweak.rain.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

/** An integer preference edited with an inline Material slider. */
public abstract class SeekBarPreference extends Preference {

	private int value;
	private int minVal = 0;
	private int maxVal = 100;
	private int step = 1;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
		minVal = a.getInteger(R.styleable.SliderPreference_mymin, minVal);
		maxVal = a.getInteger(R.styleable.SliderPreference_mymax, maxVal);
		step = a.getInteger(R.styleable.SliderPreference_mystep, step);
		a.recycle();

		setLayoutResource(R.layout.preference_slider);
		setSelectable(false);
		setSummaryProvider(new SummaryProvider<SeekBarPreference>() {
			@Override
			public CharSequence provideSummary(@NonNull SeekBarPreference preference) {
				return transform(value);
			}
		});
	}

	protected abstract String transform(int value);

	@Override
	protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
		return a.getInteger(index, 0);
	}

	@Override
	protected void onSetInitialValue(@Nullable Object defaultValue) {
		int fallback = defaultValue != null ? (Integer) defaultValue : minVal;
		value = snap(getPersistedInt(fallback));
		persistInt(value);
	}

	private int snap(int v) {
		int clamped = Math.max(minVal, Math.min(maxVal, v));
		int snapped = minVal + Math.round((clamped - minVal) / (float) step) * step;
		return Math.min(snapped, maxVal);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		TextView titleView = (TextView) holder.findViewById(android.R.id.title);
		titleView.setTextColor(MaterialColors.getColor(titleView,
				com.google.android.material.R.attr.colorOnSurface));

		final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
		Slider slider = (Slider) holder.findViewById(R.id.preference_slider);
		slider.clearOnChangeListeners();
		slider.setValueFrom(minVal);
		slider.setValueTo(maxVal);
		slider.setStepSize(step);
		slider.setValue(value);
		slider.setLabelFormatter(new LabelFormatter() {
			@NonNull
			@Override
			public String getFormattedValue(float v) {
				return transform((int) v);
			}
		});
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider s, float v, boolean fromUser) {
				int newValue = (int) v;
				if (!fromUser || newValue == value) {
					return;
				}
				if (callChangeListener(newValue)) {
					value = newValue;
					persistInt(value);
					summaryView.setText(transform(value));
				} else {
					s.setValue(value);
				}
			}
		});
	}
}
