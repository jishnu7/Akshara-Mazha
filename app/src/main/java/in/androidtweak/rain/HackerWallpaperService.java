package in.androidtweak.rain;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class HackerWallpaperService extends WallpaperService {

	private static volatile boolean reset = false;
	private static volatile boolean previewReset = false;

	public static void reset() {
		previewReset = true;
		reset = true;
	}

	/** Whether the settings changed since this kind of engine last looked */
	private static boolean consumeReset(boolean preview) {
		if (preview) {
			if (previewReset) {
				previewReset = false;
				return true;
			}
		} else if (reset) {
			reset = false;
			return true;
		}
		return false;
	}

	@Override
	public Engine onCreateEngine() {
		return new HackerWallpaperEngine();
	}

	public class HackerWallpaperEngine extends Engine {

		private RainRenderer renderer;

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			renderer = new RainRenderer(HackerWallpaperService.this, surfaceHolder,
					() -> consumeReset(isPreview()));
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			RainRenderer.renderThread().post(() -> renderer.onSurfaceChanged(width, height));
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);
			RainRenderer.renderThread().post(() -> renderer.setVisible(visible));
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			// The surface is invalid once this returns, so stop drawing first
			RainRenderer.runOnRenderThreadAndWait(renderer::onSurfaceDestroyed);
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onDestroy() {
			RainRenderer.runOnRenderThreadAndWait(renderer::onSurfaceDestroyed);
			super.onDestroy();
		}
	}
}
