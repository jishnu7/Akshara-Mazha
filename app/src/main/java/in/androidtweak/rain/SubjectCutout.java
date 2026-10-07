package in.androidtweak.rain;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.moduleinstall.InstallStatusListener;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse;
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds the main subject of a background picture (people, pets, objects) with ML Kit's
 * on-device subject segmentation, so the wallpaper can draw it in front of the rain.
 * The model comes from Google Play services; without them there's simply no cut-out.
 */
final class SubjectCutout {

	/** How long to wait for the model to download, or for segmentation to finish */
	private static final long TIMEOUT_SECONDS = 60;
	/** A cut-out covering less than this share of the picture is treated as no subject */
	private static final float MIN_COVERAGE = 0.005f;
	/** Check every n-th pixel when measuring coverage */
	private static final int COVERAGE_STEP = 4;

	private SubjectCutout() {
	}

	/** The picture's subject on a transparent background, or null when there isn't one */
	@WorkerThread
	@Nullable
	static Bitmap find(Context context, Bitmap picture) {
		SubjectSegmenter segmenter = SubjectSegmentation.getClient(
				new SubjectSegmenterOptions.Builder().enableForegroundBitmap().build());
		try {
			if (!ensureModel(context, segmenter)) {
				return null;
			}
			SubjectSegmentationResult result = Tasks.await(
					segmenter.process(InputImage.fromBitmap(picture, 0)), TIMEOUT_SECONDS, TimeUnit.SECONDS);
			Bitmap subject = result.getForegroundBitmap();
			return subject != null && coverage(subject) >= MIN_COVERAGE ? subject : null;
		} catch (ExecutionException | TimeoutException | RuntimeException e) {
			// No Play services, the model isn't available, or segmentation failed
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} finally {
			segmenter.close();
		}
	}

	/**
	 * Makes sure the segmentation model is on the device, downloading it if needed.
	 * The install request succeeds once accepted, so wait for the download to finish.
	 */
	private static boolean ensureModel(Context context, OptionalModuleApi api)
			throws ExecutionException, InterruptedException, TimeoutException {
		ModuleInstallClient client = ModuleInstall.getClient(context);
		if (Tasks.await(client.areModulesAvailable(api), TIMEOUT_SECONDS, TimeUnit.SECONDS).areModulesAvailable()) {
			return true;
		}
		CountDownLatch finished = new CountDownLatch(1);
		AtomicBoolean installed = new AtomicBoolean();
		InstallStatusListener listener = update -> {
			int state = update.getInstallState();
			if (state == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED) {
				installed.set(true);
				finished.countDown();
			} else if (state == ModuleInstallStatusUpdate.InstallState.STATE_FAILED
					|| state == ModuleInstallStatusUpdate.InstallState.STATE_CANCELED) {
				finished.countDown();
			}
		};
		try {
			ModuleInstallRequest request = ModuleInstallRequest.newBuilder()
					.addApi(api)
					.setListener(listener)
					.build();
			ModuleInstallResponse response = Tasks.await(client.installModules(request), TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (response.areModulesAlreadyInstalled()) {
				return true;
			}
			return finished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS) && installed.get();
		} finally {
			client.unregisterListener(listener);
		}
	}

	/** Share of the picture the cut-out covers */
	private static float coverage(Bitmap subject) {
		int width = subject.getWidth();
		int height = subject.getHeight();
		int[] row = new int[width];
		long covered = 0;
		long sampled = 0;
		for (int y = 0; y < height; y += COVERAGE_STEP) {
			subject.getPixels(row, 0, width, 0, y, width, 1);
			for (int x = 0; x < width; x += COVERAGE_STEP) {
				sampled++;
				if ((row[x] >>> 24) > 0) {
					covered++;
				}
			}
		}
		return sampled == 0 ? 0 : covered / (float) sampled;
	}
}
