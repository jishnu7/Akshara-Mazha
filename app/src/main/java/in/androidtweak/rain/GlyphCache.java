package in.androidtweak.rain;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

final class GlyphCache {

	/** Blur strengths used for depth */
	static final int BLUR_NONE = 0;
	static final int BLUR_SLIGHT = 1;
	static final int BLUR_STRONG = 2;

	private static final BlurMaskFilter[] BLUR_FILTERS = {
			null,
			new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL),
			new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL),
	};

	/** Room around each glyph for the blur to spread into */
	private static final int PADDING = 8;

	static final class Glyph {
		final Bitmap mask;
		/** Where the glyph's anchor (horizontal center, baseline) sits inside the mask */
		final float anchorX;
		final float anchorY;

		Glyph(Bitmap mask, float anchorX, float anchorY) {
			this.mask = mask;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
		}
	}

	/** The glyphs for one text size and blur strength, rendered on first use */
	final class Bucket {
		private final Map<String, Glyph> glyphs = new HashMap<>();
		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		private Bucket(int textSize, int blur) {
			paint.setTypeface(typeface);
			paint.setTextSize(textSize);
			paint.setMaskFilter(BLUR_FILTERS[blur]);
		}

		Glyph get(String symbol) {
			Glyph glyph = glyphs.get(symbol);
			if (glyph == null) {
				glyph = render(symbol);
				glyphs.put(symbol, glyph);
			}
			return glyph;
		}

		private Glyph render(String symbol) {
			paint.getTextBounds(symbol, 0, symbol.length(), bounds);
			float advance = paint.measureText(symbol);
			// Ink can overhang the advance, so cover both
			float inkLeft = Math.min(bounds.left, 0);
			float inkRight = Math.max(bounds.right, advance);
			int width = Math.max(1, (int) Math.ceil(inkRight - inkLeft) + 2 * PADDING);
			int height = Math.max(1, bounds.height() + 2 * PADDING);

			float startX = PADDING - inkLeft;
			float baseline = PADDING - bounds.top;
			Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
			new Canvas(mask).drawText(symbol, startX, baseline, paint);
			mask.prepareToDraw();
			return new Glyph(mask, startX + advance / 2, baseline);
		}
	}

	private final Map<Integer, Bucket> buckets = new HashMap<>();
	private final Rect bounds = new Rect();
	private Typeface typeface;

	/** Drops every glyph; call when the font changes */
	void reset(Typeface typeface) {
		this.typeface = typeface;
		buckets.clear();
	}

	Bucket bucket(int textSize, int blur) {
		int key = textSize * BLUR_FILTERS.length + blur;
		Bucket bucket = buckets.get(key);
		if (bucket == null) {
			bucket = new Bucket(textSize, blur);
			buckets.put(key, bucket);
		}
		return bucket;
	}
}
