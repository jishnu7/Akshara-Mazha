package in.androidtweak.rain;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.Arrays;

/**
 * Every glyph the rain needs, rendered once into a single ALPHA_8 bitmap: each
 * symbol sharp, slightly and strongly blurred (for depth), and as a wide glow
 * (for cursors). One texture lets the GPU batch a frame's thousands of glyph
 * draws into a few calls, where separate bitmaps would cost a call each.
 * <p>
 * Drawing a glyph with a Paint tints it with the paint's color and alpha.
 */
final class GlyphAtlas {

	static final int BLUR_NONE = 0;
	static final int BLUR_SLIGHT = 1;
	static final int BLUR_STRONG = 2;
	static final int BLUR_GLOW = 3;
	private static final int VARIANTS = 4;

	/** Depth blur radii, in pixels */
	private static final float[] DEPTH_BLUR_RADII = {0, 2, 3};
	/** Glow blur radius, as a fraction of the text size */
	private static final float GLOW_RADIUS = 0.3f;
	/** Empty pixels between glyphs, so bitmap filtering doesn't bleed neighbours in */
	private static final int GAP = 1;
	private static final int MAX_WIDTH = 2048;

	static final class Glyph {
		/** Where the glyph sits in the atlas */
		final Rect src;
		/** Where the glyph's anchor (horizontal center, baseline) sits inside src */
		final float anchorX;
		final float anchorY;

		Glyph(Rect src, float anchorX, float anchorY) {
			this.src = src;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
		}
	}

	final Bitmap bitmap;
	private final Glyph[] glyphs;
	private final int symbolCount;

	private final Typeface typeface;
	private final int textSize;
	private final String[] symbols;

	GlyphAtlas(Typeface typeface, int textSize, String[] symbols) {
		this.typeface = typeface;
		this.textSize = textSize;
		this.symbols = symbols;
		symbolCount = symbols.length;
		glyphs = new Glyph[VARIANTS * symbolCount];

		Paint[] paints = new Paint[VARIANTS];
		int[] paddings = new int[VARIANTS];
		for (int v = 0; v < VARIANTS; v++) {
			float radius = v == BLUR_GLOW ? Math.max(1, textSize * GLOW_RADIUS) : DEPTH_BLUR_RADII[v];
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setTypeface(typeface);
			paint.setTextSize(textSize);
			if (radius > 0) {
				paint.setMaskFilter(new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL));
			}
			paints[v] = paint;
			// Room for the blur to spread into
			paddings[v] = (int) Math.ceil(2 * radius) + 2;
		}

		// Measure every glyph and pack them into rows
		Rect bounds = new Rect();
		float[] startX = new float[glyphs.length];
		float[] baseline = new float[glyphs.length];
		int x = 0;
		int y = 0;
		int rowHeight = 0;
		int atlasWidth = 0;
		for (int v = 0; v < VARIANTS; v++) {
			for (int s = 0; s < symbolCount; s++) {
				Paint paint = paints[v];
				int padding = paddings[v];
				String symbol = symbols[s];
				paint.getTextBounds(symbol, 0, symbol.length(), bounds);
				float advance = paint.measureText(symbol);
				// Ink can overhang the advance, so cover both
				float inkLeft = Math.min(bounds.left, 0);
				float inkRight = Math.max(bounds.right, advance);
				int width = Math.max(1, (int) Math.ceil(inkRight - inkLeft) + 2 * padding);
				int height = Math.max(1, bounds.height() + 2 * padding);
				if (x + width > MAX_WIDTH) {
					x = 0;
					y += rowHeight + GAP;
					rowHeight = 0;
				}
				int i = v * symbolCount + s;
				startX[i] = padding - inkLeft;
				baseline[i] = padding - bounds.top;
				glyphs[i] = new Glyph(new Rect(x, y, x + width, y + height),
						startX[i] + advance / 2, baseline[i]);
				x += width + GAP;
				rowHeight = Math.max(rowHeight, height);
				atlasWidth = Math.max(atlasWidth, x);
			}
		}

		bitmap = Bitmap.createBitmap(Math.max(1, atlasWidth), Math.max(1, y + rowHeight), Bitmap.Config.ALPHA_8);
		Canvas canvas = new Canvas(bitmap);
		for (int v = 0; v < VARIANTS; v++) {
			for (int s = 0; s < symbolCount; s++) {
				int i = v * symbolCount + s;
				Rect src = glyphs[i].src;
				canvas.drawText(symbols[s], src.left + startX[i], src.top + baseline[i], paints[v]);
			}
		}
		bitmap.prepareToDraw();
	}

	Glyph get(int variant, int symbol) {
		return glyphs[variant * symbolCount + symbol];
	}

	/** Whether this atlas already holds these glyphs, so it can be reused */
	boolean matches(Typeface typeface, int textSize, String[] symbols) {
		return this.typeface == typeface && this.textSize == textSize
				&& Arrays.equals(this.symbols, symbols);
	}
}
