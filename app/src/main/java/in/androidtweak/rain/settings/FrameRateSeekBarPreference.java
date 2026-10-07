package in.androidtweak.rain.settings;

import android.content.Context;
import android.util.AttributeSet;

import com.androidtweak.rain.R;

public class FrameRateSeekBarPreference extends SeekBarPreference {

	public FrameRateSeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected String transform(int value) {
		return getContext().getString(R.string.pref_frame_rate_value, value);
	}
}
