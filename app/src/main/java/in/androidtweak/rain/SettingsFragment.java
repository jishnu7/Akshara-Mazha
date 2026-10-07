package in.androidtweak.rain;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.androidtweak.rain.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import in.androidtweak.rain.settings.BackgroundImagePreference;
import in.androidtweak.rain.settings.Font;
import in.androidtweak.rain.settings.PreferenceCardDecoration;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final Executor imageExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onImagePicked);

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        Preference font = findPreference(SettingsActivity.KEY_FONT_PREFS);
        font.setSummaryProvider(pref -> getString(Font.getSelected(requireContext()).labelRes));

        Preference reset = findPreference("reset_to_defaults");
        reset.setOnPreferenceClickListener(pref -> {
            resetToDefaults();
            return true;
        });

        Preference image = findPreference(BackgroundImage.KEY_BACKGROUND_IMAGE);
        image.setOnPreferenceClickListener(pref -> {
            onBackgroundImageClicked();
            return true;
        });

        Preference colorFromImage = findPreference(BackgroundImage.KEY_RAIN_COLOR_FROM_IMAGE);
        colorFromImage.setOnPreferenceChangeListener((pref, newValue) -> {
            if ((Boolean) newValue) {
                // Pictures picked before this setting existed have no color worked out yet
                Context context = requireContext().getApplicationContext();
                imageExecutor.execute(() -> BackgroundImage.computeRainColorIfMissing(context));
            }
            updateBackgroundColorVisibility((Boolean) newValue);
            return true;
        });
        updateBackgroundColorVisibility();
    }

    private void updateBackgroundColorVisibility() {
        updateBackgroundColorVisibility(PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(BackgroundImage.KEY_RAIN_COLOR_FROM_IMAGE, true));
    }

    /**
     * The background is a color or a picture, so only offer the color without a picture.
     * With a picture, offer coloring the rain from it (which replaces the rain color),
     * and rain behind its subject.
     */
    private void updateBackgroundColorVisibility(boolean colorFromImage) {
        Context context = requireContext();
        boolean hasImage = BackgroundImage.get(context) != null;
        boolean canColorFromImage = hasImage && BackgroundImage.canColorRainFromImage();
        Preference color = findPreference(SettingsActivity.KEY_BACKGROUND_COLOR);
        if (color != null) {
            color.setVisible(!hasImage);
        }
        Preference fromImage = findPreference(BackgroundImage.KEY_RAIN_COLOR_FROM_IMAGE);
        if (fromImage != null) {
            fromImage.setVisible(canColorFromImage);
        }
        Preference rainColor = findPreference(SettingsActivity.KEY_BIT_COLOR);
        if (rainColor != null) {
            rainColor.setVisible(!(canColorFromImage && colorFromImage));
        }
        Preference behind = findPreference(BackgroundImage.KEY_RAIN_BEHIND_SUBJECT);
        if (behind != null) {
            behind.setVisible(hasImage);
        }
    }

    /** Without a picture, go straight to the photo picker; with one, offer to remove it too */
    private void onBackgroundImageClicked() {
        if (BackgroundImage.get(requireContext()) == null) {
            launchImagePicker();
            return;
        }
        CharSequence[] choices = {getString(R.string.bg_image_from_photos), getString(R.string.bg_image_remove)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_bg_image)
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        launchImagePicker();
                    } else {
                        BackgroundImage.clear(requireContext());
                        refreshBackgroundImage();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void launchImagePicker() {
        pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void onImagePicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        Context context = requireContext().getApplicationContext();
        boolean firstImage = BackgroundImage.get(context) == null;
        // A picture without a subject turns rain behind off, so the user hasn't chosen for it
        boolean hadSubject = BackgroundImage.getSubject(context) != null;
        BackgroundImagePreference preference = findPreference(BackgroundImage.KEY_BACKGROUND_IMAGE);
        if (preference != null) {
            preference.setBusy(true);
        }
        imageExecutor.execute(() -> {
            boolean saved;
            try {
                BackgroundImage.set(context, uri);
                saved = true;
            } catch (IOException | RuntimeException e) {
                saved = false;
            }
            boolean ok = saved;
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                if (!ok) {
                    Toast.makeText(context, R.string.bg_image_error, Toast.LENGTH_SHORT).show();
                } else {
                    // Going from a color to a picture starts with the picture's own look
                    if (firstImage) {
                        setChecked(BackgroundImage.KEY_RAIN_COLOR_FROM_IMAGE, true);
                    }
                    boolean hasSubject = BackgroundImage.getSubject(context) != null;
                    if (!hasSubject) {
                        setChecked(BackgroundImage.KEY_RAIN_BEHIND_SUBJECT, false);
                    } else if (firstImage || !hadSubject) {
                        setChecked(BackgroundImage.KEY_RAIN_BEHIND_SUBJECT, true);
                    }
                }
                BackgroundImagePreference image = findPreference(BackgroundImage.KEY_BACKGROUND_IMAGE);
                if (image != null) {
                    image.setBusy(false);
                }
                refreshBackgroundImage();
            });
        });
    }

    private void setChecked(String key, boolean checked) {
        SwitchPreferenceCompat preference = findPreference(key);
        if (preference != null) {
            preference.setChecked(checked);
        }
    }

    private void refreshBackgroundImage() {
        BackgroundImagePreference image = findPreference(BackgroundImage.KEY_BACKGROUND_IMAGE);
        if (image != null) {
            image.refresh();
        }
        updateBackgroundColorVisibility();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Material 3 lists separate sections with spacing, not dividers
        setDivider(null);

        RecyclerView list = getListView();
        list.setClipToPadding(false);
        list.addItemDecoration(new PreferenceCardDecoration(list, this::getPreferenceScreen));
        list.setPadding(list.getPaddingLeft(), list.getPaddingTop(), list.getPaddingRight(),
                getResources().getDimensionPixelSize(R.dimen.sheet_list_end_space));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.app_name);
    }

    /** Restores every preference to its default value and rebuilds the screen */
    private void resetToDefaults() {
        Context context = requireContext();
        BackgroundImage.clear(context);
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceManager.setDefaultValues(context, R.xml.prefs, true);
        onCreatePreferences(null, null);
    }
}
