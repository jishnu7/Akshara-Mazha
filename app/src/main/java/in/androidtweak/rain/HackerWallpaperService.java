package in.androidtweak.rain;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static in.androidtweak.rain.SettingsActivity.KEY_BACKGROUND_COLOR;

public class HackerWallpaperService extends WallpaperService {

	private static final long FRAME_SLACK_NANOS = TimeUnit.MILLISECONDS.toNanos(2);
	private static final long MAX_FRAME_STEP_MILLIS = 100;
	private static final long WAKE_EARLY_NANOS = TimeUnit.MILLISECONDS.toNanos(8);

	private static volatile boolean reset = false;
	private static volatile boolean previewReset = false;

	private HandlerThread renderThread;
	private Handler renderHandler;

	public static void reset() {
		previewReset = true;
		reset = true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		renderThread = new HandlerThread("MazhaRender");
		renderThread.start();
		renderHandler = new Handler(renderThread.getLooper());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		renderThread.quitSafely();
	}

	@Override
	public Engine onCreateEngine() {
		return new HackerWallpaperEngine();
	}

	private void runOnRenderThreadAndWait(Runnable task) {
		if (Looper.myLooper() == renderHandler.getLooper()) {
			task.run();
			return;
		}
		CountDownLatch done = new CountDownLatch(1);
		boolean posted = renderHandler.post(() -> {
			try {
				task.run();
			} finally {
				done.countDown();
			}
		});
		if (!posted) {
			return; // render thread already quit
		}
		try {
			done.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public class HackerWallpaperEngine extends Engine implements Choreographer.FrameCallback {

		private CodeRain rain;
		private Choreographer choreographer;
		private boolean visible;
		private boolean running;
		private long lastFrameNanos;
		/** Frames per second to draw, from the frame_rate setting */
		private int frameRate;
		private long frameIntervalNanos;
		/** When the last frame was drawn, in uptime ms; 0 when the rain has just (re)started */
		private long lastDrawMillis;
		private int width;
		private int height;
		private int backgroundColor;

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			renderHandler.post(() -> {
				this.width = width;
				this.height = height;
				loadSettings();
				resetRain();
				if (visible) {
					start();
				}
			});
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);
			renderHandler.post(() -> {
				this.visible = visible;
				if (visible) {
					start();
				} else {
					stop();
				}
			});
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			// The surface is invalid once this returns, so stop drawing first
			runOnRenderThreadAndWait(this::stop);
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onDestroy() {
			runOnRenderThreadAndWait(this::stop);
			super.onDestroy();
		}

		/** Reloads every setting; the frame rate also goes to the display as a hint */
		private void loadSettings() {
			Context context = getApplicationContext();
			frameRate = PreferenceManager.getDefaultSharedPreferences(context).getInt(
					SettingsActivity.KEY_FRAME_RATE, context.getResources().getInteger(R.integer.default_frame_rate));
			frameIntervalNanos = TimeUnit.SECONDS.toNanos(1) / frameRate;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				// Lets variable refresh rate displays switch to match
				try {
					getSurfaceHolder().getSurface().setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
				} catch (IllegalStateException | IllegalArgumentException e) {
					// No valid surface; onSurfaceChanged loads the settings again
				}
			}
		}

		private void start() {
			if (running || rain == null) {
				return;
			}
			running = true;
			// Time stands still while hidden, so the rain picks up where it left off
			lastDrawMillis = 0;
			if (choreographer == null) {
				choreographer = Choreographer.getInstance();
			}
			lastFrameNanos = 0;
			scheduleFrame(0);
		}

		private void scheduleFrame(long delayMillis) {
			choreographer.removeFrameCallback(this);
			choreographer.postFrameCallbackDelayed(this, delayMillis);
		}

		private void stop() {
			if (!running) {
				return;
			}
			running = false;
			choreographer.removeFrameCallback(this);
		}

		@Override
		public void doFrame(long frameTimeNanos) {
			if (!running) {
				return;
			}
			if (frameTimeNanos - lastFrameNanos < frameIntervalNanos - FRAME_SLACK_NANOS) {
				scheduleFrame(0);
				return;
			}
			lastFrameNanos = frameTimeNanos;
			drawFrame();
			if (running) {
				// At high frame rates the interval is shorter than WAKE_EARLY_NANOS
				long wakeEarly = Math.min(WAKE_EARLY_NANOS, frameIntervalNanos / 2);
				long wakeAt = frameTimeNanos + frameIntervalNanos - wakeEarly;
				scheduleFrame(Math.max(0, TimeUnit.NANOSECONDS.toMillis(wakeAt - System.nanoTime())));
			}
		}

		private void drawFrame() {
			if (previewReset && isPreview()) {
				previewReset = false;
				loadSettings();
				resetRain();
			} else if (reset && !isPreview()) {
				reset = false;
				loadSettings();
				resetRain();
			}

			// A long stall (a dropped frame, the device waking) shouldn't jump the rain ahead
			long now = SystemClock.uptimeMillis();
			long elapsed = lastDrawMillis == 0 ? 0 : Math.min(now - lastDrawMillis, MAX_FRAME_STEP_MILLIS);
			lastDrawMillis = now;
			rain.advance(elapsed / 1000f);

			SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				// Draw on the GPU where available; the CPU canvas is much slower
				c = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
						? holder.lockHardwareCanvas() : holder.lockCanvas();
				if (c != null) {
					c.drawColor(backgroundColor);
					rain.draw(c);
				}
			} catch (IllegalStateException | IllegalArgumentException e) {
				// The surface went away mid-frame; onSurfaceDestroyed stops the loop
			} finally {
				if (c != null) {
					try {
						holder.unlockCanvasAndPost(c);
					} catch (IllegalStateException | IllegalArgumentException e) {
						// Same as above
					}
				}
			}
		}

		private void resetRain() {
			Context context = getApplicationContext();
			int color = PreferenceManager.getDefaultSharedPreferences(context)
					.getInt(KEY_BACKGROUND_COLOR, 0);
			backgroundColor = 0xFF000000 | color;
			rain = new CodeRain(context, width, height);
		}
	}
}
