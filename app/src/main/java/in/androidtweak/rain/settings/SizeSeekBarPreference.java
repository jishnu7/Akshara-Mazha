package in.androidtweak.rain.settings;

import android.content.Context;
import android.util.AttributeSet;

import com.androidtweak.rain.R;

public class SizeSeekBarPreference extends SeekBarPreference {

	private final String[] labels;

	public SizeSeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		labels = context.getResources().getStringArray(R.array.text_size_labels);
	}

	@Override
	protected String transform(int value) {
		int stop = stopIndex(value);
		return labels[Math.max(0, Math.min(labels.length - 1, stop))];
	}
}
