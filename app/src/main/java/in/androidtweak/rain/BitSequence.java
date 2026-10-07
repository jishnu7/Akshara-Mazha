package in.androidtweak.rain;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.preference.PreferenceManager;

import com.androidtweak.rain.R;

import in.androidtweak.rain.settings.CharacterSet;
import in.androidtweak.rain.settings.CharacterSetPreference;
import in.androidtweak.rain.settings.Font;
import in.androidtweak.rain.thirdparty.ArrayDeque;

import java.util.Random;

import static in.androidtweak.rain.SettingsActivity.KEY_BIT_COLOR;
import static in.androidtweak.rain.SettingsActivity.KEY_CHANGE_BIT_SPEED;
import static in.androidtweak.rain.SettingsActivity.KEY_ENABLE_DEPTH;
import static in.androidtweak.rain.SettingsActivity.KEY_FALLING_SPEED;
import static in.androidtweak.rain.SettingsActivity.KEY_NUM_BITS;
import static in.androidtweak.rain.SettingsActivity.KEY_TEXT_SIZE;

/**
 * A class that stores a list of bits. The first bit is removed and a new bit is
 * appended at a fixed interval. Calling the draw method of displays the bit
 * sequence vertically on the screen. Every time a bit is changed, the position
 * of the sequence on the screen will be shifted downward. Moving past the
 * bottom of the screen will cause the sequence to be placed above the screen
 *
 * @author Gulshan Singh
 */
public class BitSequence {

	/** Pre-rendered glyphs shared by every sequence */
	private static final GlyphCache glyphCache = new GlyphCache();

	/** The height of the screen */
	private static int HEIGHT;

	/** The bits this sequence stores */
	private ArrayDeque<String> bits = new ArrayDeque<>();

	/** A variable used for all operations needing random numbers */
	private Random r = new Random();

	/** Longest random wait, in ms, before a sequence starts falling */
	private static final int MAX_START_DELAY = 6000;

	/** When the next bit change and downward shift is due, in uptime milliseconds */
	private long nextTick;

	/** The position to draw the sequence at on the screen */
	float x, y;

	/** True when the BitSequence should be paused */
	private boolean pause = false;

	/** The characters to use in the sequence */
	private static String[] symbols = null;

	/** Describes the style of the sequence */
	private final Style style = new Style();
    private static String charSet;
    private static boolean isRandom = true;
    private int curChar = 0;

    public static class Style {
		/** The default speed at which bits should be changed */
		private static final int DEFAULT_CHANGE_BIT_SPEED = 100;

		/** The maximum alpha a bit can have */
		private static final int MAX_ALPHA = 240;

		private static int changeBitSpeed;
		private static int numBits;
		private static int color;
		private static int defaultTextSize;
		private static int defaultFallingSpeed;
		private static boolean depthEnabled;

		private static int alphaIncrement;
		private static int initialY;

		private int textSize;
		private int fallingSpeed;
		private int blur = GlyphCache.BLUR_NONE;
		private GlyphCache.Bucket glyphs;

		private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		private static Typeface tf;

		public static void initParameters(Context context) {
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(context);
            String charSetName = CharacterSetPreference.getSelectedValue(context);

            isRandom = true;
            CharacterSet builtIn = CharacterSet.fromValue(context, charSetName);
            if (builtIn != null) {
                charSet = context.getString(builtIn.charactersRes);
            } else if (charSetName.equals("Custom (random characters)")) {
                charSet = sp.getString("custom_character_set", "");
                if (charSet.length() == 0) {
                    throw new RuntimeException("Character set length can't be 0");
                }
            } else if (charSetName.equals("Custom (exact text)")) {
                isRandom = false;
                charSet = sp.getString("custom_character_string", "");
                if (charSet.length() == 0) {
                    throw new RuntimeException("Character set length can't be 0");
                }
            } else {
                if (!charSetName.equals("Custom")) { // Legacy character set
                    throw new RuntimeException("Invalid character set " + charSetName);
                } else {
                    sp.edit().putString("character_set_name", "Custom (random characters)")
                            .commit();
                    charSet = sp.getString("custom_character_set", "");
                    if (charSet.length() == 0) {
                        throw new RuntimeException("Character set length can't be 0");
                    }
                }
            }
            symbols = charSet.split(" ");

			PreferenceUtility preferences = new PreferenceUtility(context);

            if (isRandom) {
                numBits = preferences.getInt(KEY_NUM_BITS,
                        R.integer.default_num_bits);
            } else {
                numBits = charSet.length();
            }
			color = preferences
					.getInt(KEY_BIT_COLOR, R.color.default_bit_color);
			defaultTextSize = preferences.getInt(KEY_TEXT_SIZE,
					R.integer.default_text_size);

			double changeBitSpeedMultiplier = 100 / preferences.getDouble(
					KEY_CHANGE_BIT_SPEED, R.integer.default_change_bit_speed);
			double fallingSpeedMultiplier = preferences.getDouble(
					KEY_FALLING_SPEED, R.integer.default_falling_speed) / 100;

			changeBitSpeed = (int) (DEFAULT_CHANGE_BIT_SPEED * changeBitSpeedMultiplier);
			defaultFallingSpeed = (int) (defaultTextSize * fallingSpeedMultiplier);

			depthEnabled = preferences.getBoolean(KEY_ENABLE_DEPTH, true);

			alphaIncrement = MAX_ALPHA / numBits;
			initialY = -1 * defaultTextSize * numBits;

			tf = Font.getSelected(context).getTypeface(context);
			glyphCache.reset(tf);
		}

		public Style() {
			paint.setColor(color);
		}

		public void createPaint() {
			glyphs = glyphCache.bucket(textSize, blur);
		}

		private static class PreferenceUtility {
			private SharedPreferences preferences;
			private Resources res;

			public PreferenceUtility(Context context) {
				preferences = PreferenceManager
						.getDefaultSharedPreferences(context);
				res = context.getResources();
			}

			public int getInt(String key, int defaultId) {
				return preferences.getInt(key, res.getInteger(defaultId));
			}

			public double getDouble(String key, int defaultId) {
				return (double) preferences.getInt(key,
						res.getInteger(defaultId));
			}

			public boolean getBoolean(String key, boolean defaultVal) {
				return preferences.getBoolean(key, defaultVal);
			}
		}
	}

	/**
	 * Resets the sequence by repositioning it above the screen, resetting its
	 * visual characteristics, and waiting a random time before it falls again
	 */
	private void reset(long now) {
		y = Style.initialY;
		setDepth();
		style.createPaint();
		nextTick = now + r.nextInt(MAX_START_DELAY);
	}

	/**
	 * Changes a bit and moves the sequence down for each change-bit interval
	 * that has passed by {@code now}
	 */
	public void update(long now) {
		if (pause) {
			return;
		}
		while (now >= nextTick) {
			changeBit();
			y += style.fallingSpeed;
			if (y > HEIGHT) {
				reset(now);
				return;
			}
			nextTick += Style.changeBitSpeed;
		}
	}

	private void setDepth() {
		if (!Style.depthEnabled) {
			style.textSize = Style.defaultTextSize;
			style.fallingSpeed = Style.defaultFallingSpeed;
			style.blur = GlyphCache.BLUR_NONE;
		} else {
			double factor = r.nextDouble() * (1 - .8) + .8;
			style.textSize = (int) (Style.defaultTextSize * factor);
			style.fallingSpeed = (int) (Style.defaultFallingSpeed * Math.pow(
					factor, 4));

			if (factor > .93) {
				style.blur = GlyphCache.BLUR_NONE;
			} else if (factor <= .93 && factor >= .87) {
				style.blur = GlyphCache.BLUR_SLIGHT;
			} else {
				style.blur = GlyphCache.BLUR_STRONG;
			}
		}
	}

	/**
	 * Configures any BitSequences parameters requiring the application context
	 *
	 * @param context
	 *            the application context
	 */
	public static void configure(Context context) {
		Style.initParameters(context);
	}

	/**
	 * Configures the BitSequence based on the display
	 *
	 * @param width
	 *            the width of the screen
	 * @param height
	 *            the height of the screen
	 */
	public static void setScreenDim(int width, int height) {
		HEIGHT = height;
	}

	public BitSequence(int x, long now) {
        curChar = 0;
        for (int i = 0; i < Style.numBits; i++) {
            if (isRandom) {
                bits.add(getRandomBit(r));
            } else {
                // TODO: Disable numBits in settings if custom is selected
                bits.addFirst(getNextBit());
            }
		}
		this.x = x;
		reset(now);
	}

	/** Stops the sequence from changing until it's unpaused */
	public void pause() {
		pause = true;
	}

	/**
	 * Unpauses the BitSequence: sequences on the screen continue immediately,
	 * sequences off the screen start after a random delay
	 */
	public void unpause(long now) {
		if (pause) {
			if (y <= Style.initialY + style.textSize || y > HEIGHT) {
				nextTick = now + r.nextInt(MAX_START_DELAY);
			} else {
				nextTick = now;
			}
			pause = false;
		}
	}

	/** Shifts the bits back by one and adds a new bit to the end */
	private void changeBit() {
        if (isRandom) {
            bits.removeFirst();
            bits.addLast(getRandomBit(r));
        }
	}

    private String getNextBit() {
        String s = Character.toString(charSet.charAt(curChar));
        curChar = (curChar + 1) % charSet.length();
        return s;
    }

	/**
	 * Gets a new random bit
	 *
	 * @param r
	 *            the {@link Random} object to use
	 * @return A new random bit as a {@link String}
	 */
	private String getRandomBit(Random r) {
		return symbols[r.nextInt(symbols.length)];
	}

	/**
	 * Gets the width the BitSequence would be on the screen
	 *
	 * @return the width of the BitSequence
	 */
	public static float getWidth(Context context) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setTypeface(Style.tf);
		paint.setTextSize(Style.defaultTextSize);
		return paint.measureText("0");
	}

	/**
	 * Draws this BitSequence on the screen
	 *
	 * @param canvas
	 *            the {@link Canvas} on which to draw the BitSequence
	 */
	public void draw(Canvas canvas) {
		Paint paint = style.paint;
		int textSize = style.textSize;
		float bitY = y;
		for (int i = 0; i < bits.size(); i++, bitY += textSize) {
			// bitY is the baseline; skip rows entirely above or below the screen
			if (bitY < 0 || bitY - textSize > HEIGHT) {
				continue;
			}
			// Rows fade in from the top of the sequence
			paint.setAlpha((i + 1) * Style.alphaIncrement);
			GlyphCache.Glyph glyph = style.glyphs.get(bits.get(i));
			canvas.drawBitmap(glyph.mask, x - glyph.anchorX, bitY - glyph.anchorY, paint);
		}
	}
}
