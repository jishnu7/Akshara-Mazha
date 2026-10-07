package in.androidtweak.rain;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The rain as it will look on the home screen, drawn by the same renderer as the
 * wallpaper. It runs only while shown, and {@link #refresh()} picks up new settings.
 */
public class RainPreviewView extends SurfaceView implements SurfaceHolder.Callback {

	private final RainRenderer renderer;

	public RainPreviewView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		renderer = new RainRenderer(context, getHolder(), null);
		getHolder().addCallback(this);
	}

	/** Starts or pauses the rain, to follow the screen being visible */
	public void setShown(boolean shown) {
		RainRenderer.renderThread().post(() -> renderer.setVisible(shown));
	}

	/** Redraws with the current settings */
	public void refresh() {
		renderer.requestReset();
	}

	@Override
	public void surfaceCreated(@NonNull SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
		RainRenderer.renderThread().post(() -> renderer.onSurfaceChanged(width, height));
	}

	@Override
	public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
		// The surface is invalid once this returns, so stop drawing first
		RainRenderer.runOnRenderThreadAndWait(renderer::onSurfaceDestroyed);
	}
}
