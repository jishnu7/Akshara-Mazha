package in.androidtweak.rain;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.androidtweak.rain.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class SettingsSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

	/** Whether the current gesture started in the settings */
	private boolean inSettings;

	public SettingsSheetBehavior(@NonNull Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * The only scrolling inside the sheet is the settings; never let it move the sheet.
	 * (draggableOnNestedScroll isn't enough: it still moves at the end of the list.)
	 */
	@Override
	public boolean onStartNestedScroll(@NonNull CoordinatorLayout parent, @NonNull V child,
									   @NonNull View directTargetChild, @NonNull View target,
									   int axes, int type) {
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(@NonNull CoordinatorLayout parent, @NonNull V child,
										 @NonNull MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			View settings = child.findViewById(R.id.settings_container);
			inSettings = settings != null
					&& parent.isPointInChildBounds(settings, (int) event.getX(), (int) event.getY());
		}
		return !inSettings && super.onInterceptTouchEvent(parent, child, event);
	}

	@Override
	public boolean onTouchEvent(@NonNull CoordinatorLayout parent, @NonNull V child,
								@NonNull MotionEvent event) {
		return !inSettings && super.onTouchEvent(parent, child, event);
	}
}
