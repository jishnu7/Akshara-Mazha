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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static in.androidtweak.rain.SettingsActivity.KEY_BACKGROUND_COLOR;

public class HackerWallpaperService extends WallpaperService {

	private static final float FRAME_RATE = 30f;
	private static final long FRAME_INTERVAL_NANOS = (long) (TimeUnit.SECONDS.toNanos(1) / FRAME_RATE);
	private static final long FRAME_SLACK_NANOS = TimeUnit.MILLISECONDS.toNanos(2);
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

		private final List<BitSequence> sequences = new ArrayList<>();
		private Choreographer choreographer;
		private boolean visible;
		private boolean running;
		private long lastFrameNanos;
		private int width;
		private int backgroundColor;

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				// Lets variable refresh rate displays slow down to match
				holder.getSurface().setFrameRate(FRAME_RATE, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
			}
			renderHandler.post(() -> {
				this.width = width;
				BitSequence.setScreenDim(width, height);
				BitSequence.configure(getApplicationContext());
				resetSequences();
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

		private void start() {
			if (running || sequences.isEmpty()) {
				return;
			}
			running = true;
			long now = SystemClock.uptimeMillis();
			for (int i = 0; i < sequences.size(); i++) {
				sequences.get(i).unpause(now);
			}
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
			for (int i = 0; i < sequences.size(); i++) {
				sequences.get(i).pause();
			}
		}

		@Override
		public void doFrame(long frameTimeNanos) {
			if (!running) {
				return;
			}
			if (frameTimeNanos - lastFrameNanos < FRAME_INTERVAL_NANOS - FRAME_SLACK_NANOS) {
				scheduleFrame(0);
				return;
			}
			lastFrameNanos = frameTimeNanos;
			drawFrame();
			if (running) {
				long wakeAt = frameTimeNanos + FRAME_INTERVAL_NANOS - WAKE_EARLY_NANOS;
				scheduleFrame(Math.max(0, TimeUnit.NANOSECONDS.toMillis(wakeAt - System.nanoTime())));
			}
		}

		private void drawFrame() {
			if (previewReset && isPreview()) {
				previewReset = false;
				BitSequence.configure(getApplicationContext());
				resetSequences();
			} else if (reset && !isPreview()) {
				reset = false;
				BitSequence.configure(getApplicationContext());
				resetSequences();
			}

			long now = SystemClock.uptimeMillis();
			for (int i = 0; i < sequences.size(); i++) {
				sequences.get(i).update(now);
			}

			SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				// Draw on the GPU where available; the CPU canvas is much slower
				c = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
						? holder.lockHardwareCanvas() : holder.lockCanvas();
				if (c != null) {
					c.drawColor(backgroundColor);
					for (int i = 0; i < sequences.size(); i++) {
						sequences.get(i).draw(c);
					}
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

		private void resetSequences() {
			Context context = getApplicationContext();
			int color = PreferenceManager.getDefaultSharedPreferences(context)
					.getInt(KEY_BACKGROUND_COLOR, 0);
			backgroundColor = 0xFF000000 | color;

			boolean wasRunning = running;
			stop();
			sequences.clear();
			float columnWidth = BitSequence.getWidth(context);
			int numSequences = (int) (1.5 * width / columnWidth);
			long now = SystemClock.uptimeMillis();
			for (int i = 0; i < numSequences; i++) {
				sequences.add(new BitSequence((int) (i * columnWidth / 1.5), now));
			}
			if (wasRunning) {
				start();
			}
		}
	}
}
