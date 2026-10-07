package in.androidtweak.rain;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import in.androidtweak.rain.settings.CharacterSet;
import in.androidtweak.rain.settings.CharacterSetPreference;
import in.androidtweak.rain.settings.Font;

import java.util.Arrays;
import java.util.Random;

import static in.androidtweak.rain.SettingsActivity.KEY_BIT_COLOR;
import static in.androidtweak.rain.SettingsActivity.KEY_ENABLE_DEPTH;
import static in.androidtweak.rain.SettingsActivity.KEY_FALLING_SPEED;
import static in.androidtweak.rain.SettingsActivity.KEY_DENSITY;
import static in.androidtweak.rain.SettingsActivity.KEY_TAIL;
import static in.androidtweak.rain.SettingsActivity.KEY_TEXT_SIZE;

/**
 * The code rain, adapted from the "classic" mode of Rezmason's Matrix digital rain
 * (https://github.com/Rezmason/matrix). Glyphs sit still in a grid; raindrops are
 * waves of brightness travelling down each column, brightest at their leading
 * "cursor". Brightness is a function of time, so the very first frame is already
 * full of rain.
 * <p>
 * Not thread safe: the wallpaper's render thread owns it.
 */
final class CodeRain {

	// Classic mode's defaults, from Rezmason's js/config.js

	/**
	 * How fast raindrops progress, in rain time per second. Classic's fallSpeed is 0.3,
	 * which on a phone's small rows is too quick; 0.12 makes drops fall 6-12 rows a
	 * second, crossing a phone screen in about 5-11 seconds.
	 */
	private static final double FALL_SPEED = 0.12;
	/** Rain time between neighbouring rows */
	private static final double ROW_STEP = 0.01;
	/** Glyph changes per second per cell (cycleSpeed 0.03 per frame, at 60 fps) */
	private static final float CYCLES_PER_SECOND = 1.8f;
	/** baseContrast and baseBrightness: only the lower part of each raindrop is lit */
	private static final float BASE_CONTRAST = 1.1f;
	private static final float BASE_BRIGHTNESS = -0.5f;
	private static final float MAX_BASE = BASE_CONTRAST + BASE_BRIGHTNESS;

	/** raindropLength for each step of the tail setting; 10 would be classic's 0.75 */
	private static final double RAINDROP_LENGTH_PER_TAIL_STEP = 0.075;
	/** Brightest a trail glyph gets; the cursor is fully opaque */
	private static final int MAX_ALPHA = 240;
	/** Glyphs dimmer than this aren't worth drawing */
	private static final int MIN_ALPHA = 6;
	/** The cursor is the glyph color lightened toward white, over a softer glow */
	private static final float CURSOR_WHITENESS = 0.65f;
	private static final float GLOW_WHITENESS = 0.3f;
	private static final int GLOW_ALPHA = 200;
	/** Column spacing: this percentile of letter widths, times the gap */
	private static final float WIDE_LETTER_PERCENTILE = 0.9f;
	private static final float COLUMN_GAP = 1.2f;
	/** Where the baseline sits in a cell, as a fraction of its height */
	private static final float BASELINE = 0.8f;

	private static final double SQRT_2 = Math.sqrt(2);
	private static final double SQRT_5 = Math.sqrt(5);

	/** Pre-rendered glyphs, shared by every engine on the render thread */
	private static GlyphAtlas cachedAtlas;

	private final Random random = new Random();

	private final int columns;
	private final int rows;
	/** Row height: the text size */
	private final int cellSize;
	/** Column width: wide enough for the font's letters plus a gap */
	private final int columnWidth;

	/** The characters to rain; in exact text mode, the text's characters in order */
	private final String[] symbols;
	private final boolean exactText;

	private final double fallSpeed;
	private final double raindropLength;
	/** Fraction of raindrops that fall; fewer leave more of the background showing */
	private final double density;
	private final float cycleRate;

	private final double[] columnTimeOffset;
	private final double[] columnSpeed;
	/** Per column depth: blur strength and brightness, for a sense of distance */
	private final int[] columnBlur;
	private final float[] columnDim;

	private final int[] cellSymbol;
	private final float[] cellAge;
	/** Scratch: one column's brightness, from the row below the screen upwards */
	private final double[] brightness;
	/** Scratch: which raindrop each of those cells belongs to */
	private final long[] dropNumber;

	private final GlyphAtlas atlas;
	private final RectF glyphRect = new RectF();
	private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
	private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
	private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

	/** Seconds of rain so far; only advances while the wallpaper is visible */
	private double time;

	CodeRain(Context context, int width, int height) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Resources res = context.getResources();

		String charSetName = CharacterSetPreference.getSelectedValue(context);
		CharacterSet builtIn = CharacterSet.fromValue(context, charSetName);
		String characters;
		if (builtIn != null) {
			characters = context.getString(builtIn.charactersRes);
		} else if (charSetName.equals("Custom (exact text)")) {
			characters = prefs.getString("custom_character_string", "");
		} else {
			// Legacy custom sets from the original Hacker Live Wallpaper
			if (charSetName.equals("Custom")) {
				prefs.edit().putString(CharacterSetPreference.KEY_CHARACTER_SET_NAME,
						"Custom (random characters)").apply();
			} else if (!charSetName.equals("Custom (random characters)")) {
				throw new IllegalStateException("Invalid character set " + charSetName);
			}
			characters = prefs.getString("custom_character_set", "");
		}
		if (characters.isEmpty()) {
			throw new IllegalStateException("Character set can't be empty");
		}
		exactText = charSetName.equals("Custom (exact text)");
		symbols = exactText ? splitCharacters(characters) : characters.split(" ");

		int tail = prefs.getInt(KEY_TAIL, res.getInteger(R.integer.default_tail));
		raindropLength = tail * RAINDROP_LENGTH_PER_TAIL_STEP;
		density = prefs.getInt(KEY_DENSITY, res.getInteger(R.integer.default_density)) / 100.0;
		// One speed for everything, like classic's animationSpeed: falling and glyph changes
		double speed = prefs.getInt(KEY_FALLING_SPEED, res.getInteger(R.integer.default_falling_speed)) / 100.0;
		fallSpeed = FALL_SPEED * speed;
		cycleRate = exactText ? 0 : (float) (CYCLES_PER_SECOND * speed);
		cellSize = prefs.getInt(KEY_TEXT_SIZE, res.getInteger(R.integer.default_text_size));
		boolean depth = prefs.getBoolean(KEY_ENABLE_DEPTH, true);

		Integer imageColor = BackgroundImage.getRainColor(context);
		int color = imageColor != null ? imageColor
				: prefs.getInt(KEY_BIT_COLOR, ContextCompat.getColor(context, R.color.default_bit_color));
		trailPaint.setColor(color);
		cursorPaint.setColor(ColorUtils.blendARGB(color, Color.WHITE, CURSOR_WHITENESS));
		glowPaint.setColor(ColorUtils.blendARGB(color, Color.WHITE, GLOW_WHITENESS));

		Typeface typeface = Font.getSelected(context).getTypeface(context);
		// Rendering the atlas takes a moment, so keep it unless the glyphs changed
		if (cachedAtlas == null || !cachedAtlas.matches(typeface, cellSize, symbols)) {
			cachedAtlas = new GlyphAtlas(typeface, cellSize, symbols);
		}
		atlas = cachedAtlas;

		columnWidth = columnWidth(typeface, cellSize, symbols);
		columns = (width + columnWidth - 1) / columnWidth;
		rows = (height + cellSize - 1) / cellSize;

		columnTimeOffset = new double[columns];
		columnSpeed = new double[columns];
		columnBlur = new int[columns];
		columnDim = new float[columns];
		for (int c = 0; c < columns; c++) {
			// A random point in each column's rain, so the screen starts full
			columnTimeOffset[c] = random.nextDouble() * 1000;
			if (depth) {
				// Farther columns are blurrier, dimmer and slower
				double nearness = random.nextDouble();
				columnSpeed[c] = 0.5 + 0.5 * nearness;
				columnDim[c] = (float) (0.6 + 0.4 * nearness);
				columnBlur[c] = nearness > 0.65 ? GlyphAtlas.BLUR_NONE
						: nearness > 0.35 ? GlyphAtlas.BLUR_SLIGHT : GlyphAtlas.BLUR_STRONG;
			} else {
				columnSpeed[c] = 0.5 + 0.5 * random.nextDouble();
				columnDim[c] = 1;
				columnBlur[c] = GlyphAtlas.BLUR_NONE;
			}
		}

		cellSymbol = new int[columns * rows];
		cellAge = new float[columns * rows];
		for (int i = 0; i < cellSymbol.length; i++) {
			cellSymbol[i] = exactText ? (i % rows) % symbols.length : random.nextInt(symbols.length);
			cellAge[i] = random.nextFloat();
		}
		brightness = new double[rows + 1];
		dropNumber = new long[rows + 1];
	}

	/**
	 * Malayalam letters are often wider than they are tall, and how wide varies a lot
	 * by font, so space columns by the font's own letters: a gap past the 90th
	 * percentile width, so only the odd very wide letter touches a neighbour.
	 */
	private static int columnWidth(Typeface typeface, int textSize, String[] symbols) {
		Paint paint = new Paint();
		paint.setTypeface(typeface);
		paint.setTextSize(textSize);
		float[] widths = new float[symbols.length];
		for (int i = 0; i < symbols.length; i++) {
			widths[i] = paint.measureText(symbols[i]);
		}
		Arrays.sort(widths);
		float wide = widths[(int) (widths.length * WIDE_LETTER_PERCENTILE)];
		return Math.max(textSize, Math.round(wide * COLUMN_GAP));
	}

	private static String[] splitCharacters(String text) {
		String[] characters = new String[text.length()];
		for (int i = 0; i < characters.length; i++) {
			characters[i] = String.valueOf(text.charAt(i));
		}
		return characters;
	}

	private static double wobble(double x) {
		return x + 0.3 * Math.sin(SQRT_2 * x) + 0.2 * Math.sin(SQRT_5 * x);
	}

	/**
	 * Rain time of a cell; {@code rowUp} counts from the bottom row. Its fraction gives
	 * brightness, a sawtooth down the column, so glyphs get brighter toward each
	 * raindrop's bottom. Its whole part numbers the raindrops.
	 */
	private double rainTime(double columnTime, int rowUp) {
		return wobble((rowUp * ROW_STEP + columnTime) / raindropLength);
	}

	/** Whether a column's numbered raindrop falls, so roughly {@code density} of them do */
	private boolean dropFalls(int column, long drop) {
		if (density >= 1) {
			return true;
		}
		long h = (column + 1) * 0x9E3779B97F4A7C15L ^ drop * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h >>> 11) * 0x1.0p-53 < density;
	}

	/** Moves the rain forward and lets glyphs change */
	void advance(float seconds) {
		time += seconds;
		if (cycleRate == 0) {
			return;
		}
		float step = cycleRate * seconds;
		for (int i = 0; i < cellAge.length; i++) {
			float age = cellAge[i] + step;
			if (age >= 1) {
				cellSymbol[i] = random.nextInt(symbols.length);
				age -= (int) age;
			}
			cellAge[i] = age;
		}
	}

	void draw(Canvas canvas) {
		float baseline = cellSize * BASELINE;
		for (int c = 0; c < columns; c++) {
			double columnTime = columnTimeOffset[c] + time * fallSpeed * columnSpeed[c];
			for (int rowUp = -1; rowUp < rows; rowUp++) {
				double rainTime = rainTime(columnTime, rowUp);
				double whole = Math.floor(rainTime);
				brightness[rowUp + 1] = 1 - (rainTime - whole);
				dropNumber[rowUp + 1] = (long) whole;
			}
			int blur = columnBlur[c];
			float dim = columnDim[c];
			cursorPaint.setAlpha((int) (255 * dim));
			glowPaint.setAlpha((int) (GLOW_ALPHA * dim));
			float x = c * columnWidth + columnWidth / 2f;
			for (int row = 0; row < rows; row++) {
				int rowUp = rows - 1 - row;
				double bright = brightness[rowUp + 1];
				int symbol = cellSymbol[c * rows + row];
				float y = row * cellSize + baseline;
				// The cursor is the tip of a raindrop: brighter than the cell below it
				if (!dropFalls(c, dropNumber[rowUp + 1])) {
					continue;
				}
				if (bright > brightness[rowUp]) {
					drawGlyph(canvas, atlas.get(GlyphAtlas.BLUR_GLOW, symbol), x, y, glowPaint);
					drawGlyph(canvas, atlas.get(blur, symbol), x, y, cursorPaint);
					continue;
				}
				float base = (float) bright * BASE_CONTRAST + BASE_BRIGHTNESS;
				int alpha = (int) (Math.min(1, base / MAX_BASE) * MAX_ALPHA * dim);
				if (alpha < MIN_ALPHA) {
					continue;
				}
				trailPaint.setAlpha(alpha);
				drawGlyph(canvas, atlas.get(blur, symbol), x, y, trailPaint);
			}
		}
	}

	/** Draws a glyph from the atlas with its anchor at (x, y) */
	private void drawGlyph(Canvas canvas, GlyphAtlas.Glyph glyph, float x, float y, Paint paint) {
		float left = x - glyph.anchorX;
		float top = y - glyph.anchorY;
		glyphRect.set(left, top, left + glyph.src.width(), top + glyph.src.height());
		canvas.drawBitmap(atlas.bitmap, glyph.src, glyphRect, paint);
	}
}
