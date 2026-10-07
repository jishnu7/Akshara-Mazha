package in.androidtweak.rain.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.androidtweak.rain.R;

import in.androidtweak.rain.BackgroundImage;

import java.io.File;

/** Settings row for the background image, with a thumbnail once one is chosen */
public class BackgroundImagePreference extends Preference {

    /** Thumbnails are tiny, so decode the image at a fraction of its size */
    private static final int THUMBNAIL_SAMPLE_SIZE = 8;

    /** True while a picked image is being prepared */
    private boolean busy;
    private File thumbnailFile;
    private Bitmap thumbnail;

    public BackgroundImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_image_thumbnail);
        setSummaryProvider(preference -> getContext().getString(busy ? R.string.pref_bg_image_busy
                : BackgroundImage.get(getContext()) == null ? R.string.pref_bg_image_none : R.string.pref_bg_image_set));
    }

    /** Call after the image changes */
    public void refresh() {
        notifyChanged();
    }

    /** Shows that a picked image is being prepared, which can take a few seconds */
    public void setBusy(boolean busy) {
        this.busy = busy;
        setEnabled(!busy);
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView view = (ImageView) holder.findViewById(R.id.image_thumbnail);
        File file = BackgroundImage.get(getContext());
        if (file == null) {
            view.setVisibility(View.GONE);
            return;
        }
        if (!file.equals(thumbnailFile)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = THUMBNAIL_SAMPLE_SIZE;
            thumbnail = BitmapFactory.decodeFile(file.getPath(), options);
            thumbnailFile = file;
        }
        view.setImageBitmap(thumbnail);
        view.setVisibility(View.VISIBLE);
    }
}
