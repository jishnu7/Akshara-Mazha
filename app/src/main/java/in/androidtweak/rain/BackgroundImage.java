package in.androidtweak.rain;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.exifinterface.media.ExifInterface;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * The user's own picture behind the rain. A picked photo is copied into app storage,
 * since links from the photo picker don't last, turned upright and shrunk to about
 * the screen's size so the wallpaper can keep it in memory cheaply.
 */
public final class BackgroundImage {

	/** Stores the file name, which changes with each pick so the wallpaper reloads it */
	public static final String KEY_BACKGROUND_IMAGE = "background_image";
	/** Whether to draw the picture's subject in front of the rain */
	public static final String KEY_RAIN_BEHIND_SUBJECT = "rain_behind_subject";
	/** How bright to draw the picture, in percent; lower darkens it behind the rain */
	public static final String KEY_BRIGHTNESS = "background_image_brightness";
	private static final String FILE_PREFIX = "background_";
	private static final String PICTURE_SUFFIX = ".jpg";
	/** The subject cut-out sits next to its picture: background_123.jpg, background_123_subject.png */
	private static final String SUBJECT_SUFFIX = "_subject.png";
	private static final int JPEG_QUALITY = 90;

	private BackgroundImage() {
	}

	/** The current image file, or null when the background is a plain color */
	@Nullable
	public static File get(Context context) {
		String name = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(KEY_BACKGROUND_IMAGE, null);
		if (name == null) {
			return null;
		}
		File file = new File(context.getFilesDir(), name);
		return file.exists() ? file : null;
	}

	/** The current picture's subject cut-out, or null if it has none */
	@Nullable
	public static File getSubject(Context context) {
		File picture = get(context);
		if (picture == null) {
			return null;
		}
		File subject = new File(picture.getParentFile(), subjectName(picture.getName()));
		return subject.exists() ? subject : null;
	}

	private static String subjectName(String pictureName) {
		return pictureName.substring(0, pictureName.length() - PICTURE_SUFFIX.length()) + SUBJECT_SUFFIX;
	}

	/**
	 * Copies a picked image into app storage and makes it the background. Also looks
	 * for its subject, so the rain can fall behind it; that part is best effort.
	 */
	@WorkerThread
	public static void set(Context context, Uri uri) throws IOException {
		DisplayMetrics screen = context.getResources().getDisplayMetrics();
		Bitmap bitmap = decodeUpright(context.getContentResolver(), uri,
				Math.max(screen.widthPixels, screen.heightPixels));

		File file = new File(context.getFilesDir(), FILE_PREFIX + System.currentTimeMillis() + PICTURE_SUFFIX);
		try (OutputStream out = new FileOutputStream(file)) {
			if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
				throw new IOException("Couldn't save the image");
			}
		}

		Bitmap subject = SubjectCutout.find(context, bitmap);
		bitmap.recycle();
		File subjectFile = new File(context.getFilesDir(), subjectName(file.getName()));
		if (subject != null) {
			// PNG, since the cut-out needs its transparency
			try (OutputStream out = new FileOutputStream(subjectFile)) {
				if (!subject.compress(Bitmap.CompressFormat.PNG, 100, out)) {
					//noinspection ResultOfMethodCallIgnored
					subjectFile.delete();
				}
			} catch (IOException e) {
				//noinspection ResultOfMethodCallIgnored
				subjectFile.delete();
			}
			subject.recycle();
		}

		deleteFiles(context, file.getName(), subjectFile.getName());
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putString(KEY_BACKGROUND_IMAGE, file.getName())
				.apply();
	}

	/** Goes back to a plain color background */
	public static void clear(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.remove(KEY_BACKGROUND_IMAGE)
				.apply();
		deleteFiles(context);
	}

	/** Deletes saved images and cut-outs, except the files named in {@code keep} */
	private static void deleteFiles(Context context, String... keep) {
		File[] files = context.getFilesDir().listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.getName().startsWith(FILE_PREFIX) && !Arrays.asList(keep).contains(file.getName())) {
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
		}
	}

	/** Loads the image for drawing, shrunk so it's no bigger than needed to cover the screen */
	@Nullable
	static Bitmap load(File file, int width, int height) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getPath(), options);
		options.inSampleSize = sampleSize(options.outWidth, options.outHeight, Math.max(width, height));
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
		if (bitmap != null) {
			bitmap.prepareToDraw();
		}
		return bitmap;
	}

	/** The part of an image that fills a screen of this size without stretching */
	static Rect centerCrop(int imageWidth, int imageHeight, int width, int height) {
		float scale = Math.max(width / (float) imageWidth, height / (float) imageHeight);
		int cropWidth = Math.round(width / scale);
		int cropHeight = Math.round(height / scale);
		int left = (imageWidth - cropWidth) / 2;
		int top = (imageHeight - cropHeight) / 2;
		return new Rect(left, top, left + cropWidth, top + cropHeight);
	}

	/** Decodes an image the right way up, with its longer side at most about {@code maxSize} */
	private static Bitmap decodeUpright(ContentResolver resolver, Uri uri, int maxSize) throws IOException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			// ImageDecoder already applies the photo's rotation
			return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri), (decoder, info, source) -> {
				int longest = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
				if (longest > maxSize) {
					float scale = maxSize / (float) longest;
					decoder.setTargetSize(Math.round(info.getSize().getWidth() * scale),
							Math.round(info.getSize().getHeight() * scale));
				}
				decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
			});
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		try (InputStream in = resolver.openInputStream(uri)) {
			BitmapFactory.decodeStream(in, null, options);
		}
		options.inSampleSize = sampleSize(options.outWidth, options.outHeight, maxSize);
		options.inJustDecodeBounds = false;
		Bitmap bitmap;
		try (InputStream in = resolver.openInputStream(uri)) {
			bitmap = BitmapFactory.decodeStream(in, null, options);
		}
		if (bitmap == null) {
			throw new IOException("Not an image");
		}
		int degrees;
		try (InputStream in = resolver.openInputStream(uri)) {
			degrees = in == null ? 0 : new ExifInterface(in).getRotationDegrees();
		}
		if (degrees == 0) {
			return bitmap;
		}
		Matrix rotation = new Matrix();
		rotation.postRotate(degrees);
		Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
		bitmap.recycle();
		return upright;
	}

	/** Largest power of two that keeps the longer side at least {@code maxSize} */
	private static int sampleSize(int width, int height, int maxSize) {
		int sample = 1;
		while (Math.max(width, height) / (sample * 2) >= maxSize) {
			sample *= 2;
		}
		return sample;
	}
}
