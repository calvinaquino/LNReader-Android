package com.erakk.lnreader.UI.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.FileListAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.service.AutoBackupScheduleReceiver;
import com.erakk.lnreader.service.AutoBackupService;
import com.erakk.lnreader.service.UpdateScheduleReceiver;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.CopyDBTask;
import com.erakk.lnreader.task.DeleteFilesTask;
import com.erakk.lnreader.task.RelinkImagesTask;
import com.erakk.lnreader.task.UnZipFilesTask;
import com.erakk.lnreader.task.ZipFilesTask;
import com.example.android.supportv7.app.AppCompatPreferenceActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//public class DisplaySettingsActivity extends PreferenceActivity implements IExtendedCallbackNotifier<AsyncTaskResult<?>> {
public class DisplaySettingsActivity extends AppCompatPreferenceActivity implements IExtendedCallbackNotifier<AsyncTaskResult<?>> {
    private static final String TAG = DisplaySettingsActivity.class.toString();

    private DeleteFilesTask deleteTask;
    private ZipFilesTask zipTask;
    private UnZipFilesTask unzipTask;
    private RelinkImagesTask relinkTask;
    private CopyDBTask copyDbTask;
    private CopyDBTask restoreDbTask;

    // Context context;

    /**
     * ***********************************************************
     * The onPreferenceTreeClick method's sole purpose is to deal with the known
     * Android bug that doesn't custom theme the child preference screen
     * ***************************************************************
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference != null)
            if (preference instanceof PreferenceScreen) {
                setUpNestedScreen((PreferenceScreen) preference);
            }
        return false;

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // HACK: Try to handle android.os.BadParcelableException: ClassNotFoundException when unmarshalling: android.support.v7.widget.Toolbar$SavedState
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to restore instance state.");
        }
    }

    /**
     * Enable toolbar on child screen
     * http://stackoverflow.com/a/27455330
     *
     * @param preferenceScreen
     */
    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();
        Toolbar bar = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                View tempView = dialog.findViewById(android.R.id.list);
                ViewParent viewParent = tempView.getParent();
                if (viewParent != null && viewParent instanceof LinearLayout) {
                    LinearLayout root = (LinearLayout) viewParent;
                    bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                    root.addView(bar, 0); // insert at top
                } else
                    Log.i(TAG, "setUpNestedScreen() using unknown Layout: " + viewParent.getClass().toString());
            } else {
                ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.content);
                ListView content = (ListView) root.getChildAt(0);

                root.removeAllViews();

                bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);

                int height;
                TypedValue tv = new TypedValue();
                if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                    height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
                } else {
                    height = bar.getHeight();
                }

                content.setPadding(0, height, 0, 0);

                root.addView(content);
                root.addView(bar);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to get Toolbar on Settings Page", ex);
        }

        if (bar != null) {
            bar.setTitle(preferenceScreen.getTitle());
            bar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
    }

    /**
     * Enable toolbar on pref screen
     * http://stackoverflow.com/a/30281205
     */
    private void setupActionBar() {
        Toolbar toolbar = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                ViewGroup root = (ViewGroup) findViewById(android.R.id.list).getParent().getParent().getParent();
                toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(toolbar, 0);
            } else {
                ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
                if (root.getChildAt(0) instanceof ListView) {
                    ListView content = (ListView) root.getChildAt(0);
                    root.removeAllViews();
                    toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                    int height;
                    TypedValue tv = new TypedValue();
                    if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                        height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
                    } else {
                        height = toolbar.getHeight();
                    }
                    content.setPadding(0, height, 0, 0);
                    root.addView(content);
                    root.addView(toolbar);
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to get Toolbar on Settings Page", ex);
        }
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    @SuppressLint("SdCardPath")
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        // This man is deprecated but but we may want to be able to run on older API
        addPreferencesFromResource(R.xml.preferences);

        generalPreferences();

        updatePreferences();

        readingPreferences();

        storagePreferences();

        maintenancePreferences();

        // TOS activity
        Preference tos = findPreference("tos");
        tos.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
                    intent.putExtra(Constants.EXTRA_PAGE, getResources().getString(R.string.copyright));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, getResources().getString(R.string.not_copyright), e);
                }
                return false;
            }
        });

        // App Version Activity
        Preference appVersion = findPreference("app_version");
        String version = "N/A";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot get version.", e);
        }
        appVersion.setSummary(version);
        appVersion.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(getApplicationContext(), DisplayChangelogActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, getResources().getString(R.string.title_activity_display_changelog), e);
                }
                return false;
            }
        });

        // Credits activity
        Preference credits = findPreference("credits");
        credits.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(getApplicationContext(), DisplayCreditActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, getResources().getString(R.string.title_activity_display_credit), e);
                }
                return false;
            }
        });

        // non preferences setup
        LNReaderApplication.getInstance().setUpdateServiceListener(this);
        LNReaderApplication.getInstance().setAutoBackupServiceListener(this);
    }

    @SuppressWarnings("deprecation")
    private void maintenancePreferences() {
        Preference findMissingChapter = findPreference(Constants.PREF_MISSING_CHAPTER);
        findMissingChapter.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(), FindMissingActivity.class);
                intent.putExtra(Constants.EXTRA_FIND_MISSING_MODE, Constants.PREF_MISSING_CHAPTER);
                startActivity(intent);
                return true;
            }
        });

        Preference findRedlinkChapter = findPreference(Constants.PREF_REDLINK_CHAPTER);
        findRedlinkChapter.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(), FindMissingActivity.class);
                intent.putExtra(Constants.EXTRA_FIND_MISSING_MODE, Constants.PREF_REDLINK_CHAPTER);
                startActivity(intent);
                return true;
            }
        });

        Preference findEmptyBook = findPreference(Constants.PREF_EMPTY_BOOK);
        findEmptyBook.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(), FindMissingActivity.class);
                intent.putExtra(Constants.EXTRA_FIND_MISSING_MODE, Constants.PREF_EMPTY_BOOK);
                startActivity(intent);
                return true;
            }
        });

        Preference findEmptyNovel = findPreference(Constants.PREF_EMPTY_NOVEL);
        findEmptyNovel.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(), FindMissingActivity.class);
                intent.putExtra(Constants.EXTRA_FIND_MISSING_MODE, Constants.PREF_EMPTY_NOVEL);
                startActivity(intent);
                return true;
            }
        });

        Preference cleanExternalTemp = findPreference(Constants.PREF_CLEAR_EXTERNAL_TEMP);
        cleanExternalTemp.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteExternalTemp();
                return false;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void generalPreferences() {
        // UI Selection
        final Preference uiMode = findPreference("ui_selection");
        final String[] uiSelectionArray = getResources().getStringArray(R.array.uiSelection);
        int uiSelectionValue = UIHelper.getIntFromPreferences(Constants.PREF_UI_SELECTION, 0);
        uiMode.setSummary(String.format(getResources().getString(R.string.selected_mode), uiSelectionArray[uiSelectionValue]));
        uiMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int uiSelectionValue = Util.tryParseInt(newValue.toString(), 0);
                uiMode.setSummary(String.format(getResources().getString(R.string.selected_mode), uiSelectionArray[uiSelectionValue]));
                return true;
            }
        });

        setApplicationLanguage();

        setAlternateLanguageList();

        // Invert Color
//		Preference invertColors = findPreference(Constants.PREF_INVERT_COLOR);
//		invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//
//			@Override
//			public boolean onPreferenceClick(Preference p) {
//				recreateUI();
//				return true;
//			}
//		});

        // Orientation Selection
        final Preference orientation = findPreference(Constants.PREF_ORIENTATION);
        final String[] orientationArray = getResources().getStringArray(R.array.orientationSelection);
        int orientationIntervalValue = UIHelper.getIntFromPreferences(Constants.PREF_ORIENTATION, 0);
        orientation.setSummary(String.format(getResources().getString(R.string.orientation_summary), orientationArray[orientationIntervalValue]));
        orientation.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int orientationIntervalValue = Util.tryParseInt(newValue.toString(), 0);
                // UpdateScheduleReceiver.reschedule(orientationIntervalValue);
                orientation.setSummary(String.format(getResources().getString(R.string.orientation_summary), orientationArray[orientationIntervalValue]));
                setOrientation();
                return true;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void updatePreferences() {
        // Update Interval
        final Preference updatesInterval = findPreference(Constants.PREF_UPDATE_INTERVAL);
        final String[] updateIntervalArray = getResources().getStringArray(R.array.updateInterval);
        int updatesIntervalValue = UIHelper.getIntFromPreferences(Constants.PREF_UPDATE_INTERVAL, 0);
        updatesInterval.setSummary(String.format(getResources().getString(R.string.update_interval_summary), updateIntervalArray[updatesIntervalValue]));
        updatesInterval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int updatesIntervalInt = Util.tryParseInt(newValue.toString(), 0);
                UpdateScheduleReceiver.reschedule(preference.getContext(), updatesIntervalInt);
                updatesInterval.setSummary(String.format(getResources().getString(R.string.update_interval_summary), updateIntervalArray[updatesIntervalInt]));
                return true;
            }
        });

        // Run Updates
        Preference runUpdates = findPreference(Constants.PREF_RUN_UPDATES);
        runUpdates.setSummary(String.format(getResources().getString(R.string.last_run), runUpdates.getSharedPreferences().getString(Constants.PREF_RUN_UPDATES, getResources().getString(R.string.none)), runUpdates.getSharedPreferences().getString(Constants.PREF_RUN_UPDATES_STATUS, getResources().getString(R.string.unknown))));
        runUpdates.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference p) {
                runUpdate();
                return true;
            }
        });
        // Time out
        final Preference timeout = findPreference(Constants.PREF_TIMEOUT);
        int timeoutValue = UIHelper.getIntFromPreferences(Constants.PREF_TIMEOUT, 60);
        timeout.setSummary(String.format(getResources().getString(R.string.pref_timeout_summary), timeoutValue));
        timeout.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int timeoutValue = Util.tryParseInt(newValue.toString(), 60);
                timeout.setSummary(String.format(getResources().getString(R.string.pref_timeout_summary), timeoutValue));
                return true;
            }
        });

        // Retry
        final Preference retry = findPreference(Constants.PREF_RETRY);
        int retryValue = UIHelper.getIntFromPreferences(Constants.PREF_RETRY, 3);
        retry.setSummary(String.format(getResources().getString(R.string.pref_retry_summary), retryValue));
        retry.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int retryValue = Util.tryParseInt(newValue.toString(), 3);
                retry.setSummary(String.format(getResources().getString(R.string.pref_retry_summary), retryValue));
                return true;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void readingPreferences() {

        // Scrolling Size
        final Preference scrollingSize = findPreference(Constants.PREF_SCROLL_SIZE);
        int scrollingSizeValue = UIHelper.getIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5);
        scrollingSize.setSummary(String.format(getResources().getString(R.string.scroll_size_summary2), scrollingSizeValue));
        scrollingSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int scrollingSizeValue = Util.tryParseInt(newValue.toString(), 5);
                scrollingSize.setSummary(String.format(getResources().getString(R.string.scroll_size_summary2), scrollingSizeValue));
                return true;
            }
        });

        // reset zoom
        final Preference resetZoom = findPreference(Constants.PREF_RESET_ZOOM);
        resetZoom.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    NovelsDao.getInstance().resetZoomLevel(null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed when resetting zoom level", e);
                }
                return true;
            }
        });


        setCssPreferences();

        setTtsPreferences();
    }

    @SuppressWarnings("deprecation")
    private void setTtsPreferences() {
        final Preference ttsEngine = findPreference(Constants.PREF_TTS_ENGINE);
        ttsEngine.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                loadTTSEngineSettings();
                return true;
            }
        });

        final Preference ttsPitch = findPreference(Constants.PREF_TTS_PITCH);
        float ttsPitchVal = UIHelper.getFloatFromPreferences(Constants.PREF_TTS_PITCH, 1.0f);
        ttsPitch.setSummary(getResources().getString(R.string.tts_pitch_summary, ttsPitchVal));
        ttsPitch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    float val = Float.parseFloat(newValue.toString());
                    ttsPitch.setSummary(getResources().getString(R.string.tts_pitch_summary, val));
                } catch (NumberFormatException ex) {
                    return false;
                }
                return true;
            }
        });

        final Preference ttsSpeechRate = findPreference(Constants.PREF_TTS_SPEECH_RATE);
        float ttsSpeechRateVal = UIHelper.getFloatFromPreferences(Constants.PREF_TTS_SPEECH_RATE, 1.0f);
        ttsSpeechRate.setSummary(getResources().getString(R.string.tts_reading_speed_summary, ttsSpeechRateVal));
        ttsSpeechRate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    float val = Float.parseFloat(newValue.toString());
                    ttsSpeechRate.setSummary(getResources().getString(R.string.tts_reading_speed_summary, val));
                } catch (NumberFormatException ex) {
                    return false;
                }
                return true;
            }
        });

        final Preference ttsDelay = findPreference(Constants.PREF_TTS_DELAY);
        float ttsDelayVal = UIHelper.getIntFromPreferences(Constants.PREF_TTS_DELAY, 500);
        ttsDelay.setSummary(getResources().getString(R.string.tts_whitespace_delay_summary, ttsDelayVal));
        ttsDelay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    int val = Integer.parseInt(newValue.toString());
                    ttsDelay.setSummary(getResources().getString(R.string.tts_whitespace_delay_summary, val));
                } catch (NumberFormatException ex) {
                    return false;
                }
                return true;
            }
        });
    }

    private void loadTTSEngineSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.putExtra(EXTRA_SHOW_FRAGMENT, "com.android.settings.tts.TextToSpeechSettings");
            intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, intent.getExtras());
            startActivityForResult(intent, 0);
        } catch (Exception ex) {
            startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
        }
    }

    @SuppressWarnings("deprecation")
    private void storagePreferences() {
        final DisplaySettingsActivity dsa = this;
        // Clear DB
        Preference clearDatabase = findPreference("clear_database");
        clearDatabase.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference p) {
                clearDB();
                return true;
            }
        });

        // DB Location
        Preference defaultDbLocation = findPreference("db_location");
        defaultDbLocation.setSummary(String.format(getResources().getString(R.string.novel_database_to), DBHelper.getDbPath(this)));
        defaultDbLocation.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                checkDB();
                return false;
            }
        });

        // Restore DB
        Preference restoreDatabase = findPreference(Constants.PREF_RESTORE_DB);
        restoreDatabase.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference p) {
                // Quick fix, please revise as seen fit.
                // Confirm task execution, useful during unintentional clicks.
                UIHelper.createYesNoDialog(
                        dsa
                        , getResources().getString(R.string.restore_db_question)
                        , getResources().getString(R.string.restore_db_question2)
                        , new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    showBackupsDB();
                                }
                            }
                        }).show();
                return true;
            }
        });

        // Backup DB
        Preference backupDatabase = findPreference(Constants.PREF_BACKUP_DB);
        backupDatabase.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference p) {
                // Quick fix, please revise as seen fit.
                // Confirm task execution, useful during unintentional clicks.
                UIHelper.createYesNoDialog(
                        dsa
                        , getResources().getString(R.string.backup_db_question)
                        , getResources().getString(R.string.backup_db_question2)
                        , new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    backupDB();
                                }
                            }
                        }).show();
                return true;
            }
        });

        // DB Backup Location
        final EditTextPreference backupLocation = (EditTextPreference) findPreference(Constants.PREF_BACKUP_LOCATION);
        backupLocation.setText(UIHelper.getBackupRoot(this));
        backupLocation.setSummary(getResources().getString(R.string.pref_db_backup_location_summary, UIHelper.getBackupRoot(this)));
        backupLocation.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newPath = (String) newValue;
                boolean result = checkBackupStoragePath(newPath);
                if (result)
                    backupLocation.setSummary(getResources().getString(R.string.pref_db_backup_location_summary, newPath));
                return result;
            }
        });

        // Auto Backup DB
        Preference autoBackup = findPreference(Constants.PREF_AUTO_BACKUP_ENABLED);
        autoBackup.setSummary(getResources().getString(R.string.pref_db_auto_backup_summary, new Date(autoBackup.getSharedPreferences().getLong(Constants.PREF_LAST_AUTO_BACKUP_TIME, 0))));
        autoBackup.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    runAutoBackupService();
                } else {
                    AutoBackupScheduleReceiver.removeSchedule(getApplicationContext());
                }
                return true;
            }
        });

        // Auto Backup DB
        final Preference autoBackupCount = findPreference(Constants.PREF_AUTO_BACKUP_COUNT);
        autoBackupCount.setSummary(getResources().getString(R.string.pref_db_auto_backup_count_summary, UIHelper.getIntFromPreferences(Constants.PREF_AUTO_BACKUP_COUNT, 0)));
        autoBackupCount.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                autoBackupCount.setSummary(getResources().getString(R.string.pref_db_auto_backup_count_summary, newValue));
                return true;
            }
        });

        // Clear Image
        Preference clearImages = findPreference("clear_image_cache");
        clearImages.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference p) {
                clearImages();
                return true;
            }
        });

        // Image Location
        final EditTextPreference defaultSaveLocation = (EditTextPreference) findPreference("save_location");
        defaultSaveLocation.setText(UIHelper.getImageRoot(this));
        defaultSaveLocation.setSummary(String.format(getResources().getString(R.string.download_image_to), UIHelper.getImageRoot(this)));
        defaultSaveLocation.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newPath = (String) newValue;
                boolean result = checkImageStoragePath(newPath);
                if (result)
                    defaultSaveLocation.setSummary(String.format(getResources().getString(R.string.download_image_to), newPath));
                return result;
            }
        });
        // Backup Thumbs
        Preference backupThumbs = findPreference(Constants.PREF_BACKUP_THUMB_IMAGES);
        backupThumbs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Quick fix, please revise as seen fit.
                // Confirm task execution, useful during unintentional clicks.
                UIHelper.createYesNoDialog(
                        dsa
                        , getResources().getString(R.string.backup_zip_question)
                        , getResources().getString(R.string.backup_zip_question2)
                        , new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    backupThumbs();
                                }
                            }
                        }).show();
                return true;
            }
        });

        // Restore Thumbs
        Preference restoreThumbs = findPreference(Constants.PREF_RESTORE_THUMB_IMAGES);
        restoreThumbs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Quick fix, please revise as seen fit.
                // Confirm task execution, useful during unintentional clicks.
                UIHelper.createYesNoDialog(
                        dsa
                        , getResources().getString(R.string.restore_zip_question)
                        , getResources().getString(R.string.restore_zip_question2)
                        , new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    restoreThumbs();
                                }
                            }
                        }).show();
                return true;
            }
        });

        // relink thumbs
        Preference relinkThumbs = findPreference(Constants.PREF_RELINK_THUMB_IMAGES);
        relinkThumbs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Quick fix, please revise as seen fit.
                // Confirm task execution, useful during unintentional clicks.
                UIHelper.createYesNoDialog(
                        dsa
                        , getResources().getString(R.string.relink_question, UIHelper.getImageRoot(LNReaderApplication.getInstance()))
                        , getResources().getString(R.string.relink_question2)
                        , new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    relinkThumbs();
                                }
                            }
                        }).show();
                return true;
            }
        });
    }

    private boolean checkBackupStoragePath(String newPath) {
        if (Util.isStringNullOrEmpty(newPath)) {
            newPath = UIHelper.getBackupRoot(this);
        }
        File dir = new File(newPath);
        if (!dir.exists()) {
            Log.e(TAG, String.format("Directory %s not exists, trying to create dir.", newPath));
            boolean result = dir.mkdirs();
            if (result) {
                Log.i(TAG, String.format("Directory %s created.", newPath));
                return true;
            } else {
                String message = String.format("Directory %s cannot be created.", newPath);
                Log.e(TAG, message);
                Toast.makeText(this, String.format("Directory %s cannot be created.", newPath), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            return true;
        }
    }

    private void runAutoBackupService() {
        LNReaderApplication.getInstance().runAutoBackupService(this);
    }

    private void checkDB() {
        String result = NovelsDao.getInstance().checkDB();
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint({"InlinedApi", "NewApi"})
    private void relinkThumbs() {
        if (RelinkImagesTask.getInstance() != null && RelinkImagesTask.getInstance().getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, "Please wait until relink process completed.", Toast.LENGTH_SHORT).show();
            return;
        }

        String rootPath = UIHelper.getImageRoot(this);
        relinkTask = RelinkImagesTask.getInstance(rootPath, this, Constants.PREF_RELINK_THUMB_IMAGES);
        String key = RelinkImagesTask.class.toString() + ":RelinkImage";
        relinkTask = setupTaskList(relinkTask, key);
        relinkTask.setCallback(this, Constants.PREF_RELINK_THUMB_IMAGES);
    }

    @SuppressLint({"InlinedApi", "NewApi"})
    private void restoreThumbs() {
        if (ZipFilesTask.getInstance() != null && ZipFilesTask.getInstance().getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, "Please wait until all images are backed-up.", Toast.LENGTH_SHORT).show();
            return;
        }

        String zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_thumbs.zip";
        String thumbRootPath = UIHelper.getImageRoot(this) + "/project/images/thumb";

        if (getProcessAllImagesPreferences()) {
            zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_all_images.zip";
            thumbRootPath = UIHelper.getImageRoot(this) + "/project/images";
        }

        unzipTask = UnZipFilesTask.getInstance(zipName, thumbRootPath, this, Constants.PREF_RESTORE_THUMB_IMAGES);
        String key = UnZipFilesTask.class.toString() + ":" + unzipTask;
        unzipTask = setupTaskList(unzipTask, key);
        unzipTask.setCallback(this, Constants.PREF_RESTORE_THUMB_IMAGES);
    }

    private void showBackupsDB() {
        ArrayList<File> backups = AutoBackupService.getBackupFiles(this);
        final FileListAdapter adapter = new FileListAdapter(this, R.layout.item_file, backups);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Backup File");
        builder.setAdapter(adapter, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File f = adapter.getItem(which);
                restoreDB(f.getAbsolutePath());
            }
        });
        builder.create().show();

    }

    @SuppressLint("NewApi")
    private void restoreDB(String filename) {
        copyDbTask = new CopyDBTask(false, this, Constants.PREF_RESTORE_DB, filename);
        String key = CopyDBTask.class.toString() + ":BackupDB";
        copyDbTask = setupTaskList(copyDbTask, key);
        copyDbTask.setCallbackNotifier(this);
    }

    @SuppressLint("NewApi")
    private void backupDB() {
        restoreDbTask = new CopyDBTask(true, this, Constants.PREF_BACKUP_DB, null);
        String key = CopyDBTask.class.toString() + ":RestoreDB";
        restoreDbTask = setupTaskList(restoreDbTask, key);
        restoreDbTask.setCallbackNotifier(this);
    }

    @SuppressLint({"InlinedApi", "NewApi"})
    private void backupThumbs() {
        if (ZipFilesTask.getInstance() != null && ZipFilesTask.getInstance().getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, "Please wait until all images are restored.", Toast.LENGTH_SHORT).show();
            return;
        }

        String zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_thumbs.zip";
        String thumbRootPath = UIHelper.getImageRoot(this) + "/project/images/thumb";

        if (getProcessAllImagesPreferences()) {
            zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_all_images.zip";
            thumbRootPath = UIHelper.getImageRoot(this) + "/project/images";
        }

        zipTask = ZipFilesTask.getInstance(zipName, thumbRootPath, this, Constants.PREF_BACKUP_THUMB_IMAGES);
        String key = ZipFilesTask.class.toString() + ":" + zipTask;
        zipTask = setupTaskList(zipTask, key);
        zipTask.setCallback(this, Constants.PREF_BACKUP_THUMB_IMAGES);
    }

    private void deleteExternalTemp() {
        String filename = UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + "/wac/temp";
        deleteTask = new DeleteFilesTask(this, filename, Constants.PREF_CLEAR_EXTERNAL_TEMP);
        String key = DeleteFilesTask.class.toString() + ":DeleteExternalTemp";
        deleteTask = setupTaskList(deleteTask, key);
        deleteTask.owner = this;
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NewApi")
    private <T extends AsyncTask<Void, ICallbackEventData, ?>> T setupTaskList(T task, String key) {

        boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
        if (isAdded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task.execute();
        } else {
            T tempTask = (T) LNReaderApplication.getInstance().getTask(key);
            if (tempTask != null) {
                task = tempTask;
            }
        }
        return task;
    }

    @SuppressWarnings("deprecation")
    private void setAlternateLanguageList() {
        /*
         * A section to change Alternative Languages list
		 *
		 * @freedomofkeima
		 */
        Preference selectAlternativeLanguage = findPreference("select_alternative_language");
        /* List of languages */
        final boolean[] languageStatus = new boolean[AlternativeLanguageInfo.getAlternativeLanguageInfo().size()];
        Iterator<Map.Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
        int j = 0;
        while (it.hasNext()) {
            AlternativeLanguageInfo info = it.next().getValue();
            /* Default value of unregistered Alternative language = false (preventing too much tabs) */
            languageStatus[j] = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(info.getLanguage(), false);
            j++;
            it.remove();
        }
		/* End of list of languages */
        selectAlternativeLanguage.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference p) {
                showLanguageSelection(languageStatus);
                return true;
            }
        });
		/* End of alternative languages list section */
    }

    private void showLanguageSelection(boolean[] languageStatus) {
        final String[] languageChoice = new String[AlternativeLanguageInfo.getAlternativeLanguageInfo().size()];
        Iterator<Map.Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
        int j = 0;
        while (it.hasNext()) {
            AlternativeLanguageInfo info = it.next().getValue();
            languageChoice[j] = info.getLanguage();
            j++;
            it.remove();
        }
		/* Show checkBox to screen */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.alternative_language_title));
        builder.setMultiChoiceItems(languageChoice, languageStatus, new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int item, boolean state) {
            }
        });
        builder.setNegativeButton("Ok", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                setLanguageSelectionOKDialog(dialog);
            }
        });
        builder.setPositiveButton(getResources().getString(R.string.cancel), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    private void setLanguageSelectionOKDialog(DialogInterface dialog) {
        SparseBooleanArray Checked = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
		/* Save all choices to Shared Preferences */
        Iterator<Map.Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
        int j = 0;
        while (it.hasNext()) {
            AlternativeLanguageInfo info = it.next().getValue();
            UIHelper.setAlternativeLanguagePreferences(this, info.getLanguage(), Checked.get(j));
            j++;
            it.remove();
        }
        recreateUI();
    }

    @SuppressWarnings("deprecation")
    private void setApplicationLanguage() {
		/*
		 * A section to change Application Language
		 *
		 * @freedomofkeima
		 */
        final Preference changeLanguages = findPreference(Constants.PREF_LANGUAGE);
        final String[] languageSelectionArray = getResources().getStringArray(R.array.languageSelection);
        final String[] localeArray = getResources().getStringArray(R.array.languageSelectionValues);
        String languageSelectionValue = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_LANGUAGE, "en");

        // construct the hash map with locale as the key and language as the
        // value
        final HashMap<String, String> langDict = new HashMap<String, String>();
        for (int i = 0; i < languageSelectionArray.length; i++) {
            langDict.put(localeArray[i], languageSelectionArray[i]);
        }

        // check if key exist, else fall back to en
        if (langDict.containsKey(languageSelectionValue)) {
            changeLanguages.setSummary(String.format(getResources().getString(R.string.selected_language), langDict.get(languageSelectionValue)));
        } else {
            changeLanguages.setSummary(String.format(getResources().getString(R.string.selected_language), langDict.get("en")));
        }

        changeLanguages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                handleLanguageChange(newValue);
                return true;
            }
        });

		/* End of language section */
    }

    private void handleLanguageChange(Object newValue) {
        String newLocale = newValue.toString();
        UIHelper.setLanguage(this, newLocale);
        LNReaderApplication.getInstance().restartApplication();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SdCardPath")
    /**
     * CSS Layout Behaviours
     * 1. When user's css sheet is used, disable the force justify, linespace and margin preferences
     * 2. When about to use user's css sheet, display a warning/message (NOT IMPLEMENTED)
     * 3. When linespace/margin is changed, update the summary text to reflect current value
     */
    private void setCssPreferences() {
        final Preference user_cssPref = findPreference(Constants.PREF_USE_CUSTOM_CSS);
        final Preference lineSpacePref = findPreference(Constants.PREF_LINESPACING);
        final Preference justifyPref = findPreference(Constants.PREF_FORCE_JUSTIFIED);
        final Preference customCssPathPref = findPreference(Constants.PREF_CUSTOM_CSS_PATH);
        final Preference marginPref = findPreference(Constants.PREF_MARGINS);
        final Preference headingFontPref = findPreference(Constants.PREF_HEADING_FONT);
        final Preference contentFontPref = findPreference(Constants.PREF_CONTENT_FONT);

        // Retrieve inital values stored
        Boolean currUserCSS = getPreferenceScreen().getSharedPreferences().getBoolean(Constants.PREF_USE_CUSTOM_CSS, false);
        String currLineSpacing = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_LINESPACING, "150");
        String currMargin = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_MARGINS, "5");
        String currHeadingFont = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_HEADING_FONT, "serif");
        String currContentFont = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_CONTENT_FONT, "sans-serif");

        // Behaviour 1 (Activity first loaded)
        marginPref.setEnabled(!currUserCSS);
        lineSpacePref.setEnabled(!currUserCSS);
        justifyPref.setEnabled(!currUserCSS);
        customCssPathPref.setEnabled(currUserCSS);
        headingFontPref.setEnabled(!currUserCSS);
        contentFontPref.setEnabled(!currUserCSS);

        // Behaviour 3 (Activity first loaded)
        lineSpacePref.setSummary(getResources().getString(R.string.line_spacing_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + currLineSpacing + "%");
        marginPref.setSummary(getResources().getString(R.string.margin_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + currMargin + "%");
        headingFontPref.setSummary(getResources().getString(R.string.pref_css_heading_fontface_summary) + currHeadingFont);
        contentFontPref.setSummary(getResources().getString(R.string.pref_css_content_fontface_summary) + currContentFont);

        // Behaviour 1 (Updated Preference)
        user_cssPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean set = (Boolean) newValue;

                marginPref.setEnabled(!set);
                lineSpacePref.setEnabled(!set);
                justifyPref.setEnabled(!set);
                customCssPathPref.setEnabled(set);
                headingFontPref.setEnabled(!set);
                contentFontPref.setEnabled(!set);
                return true;
            }
        });

        String customCssPath = customCssPathPref.getSharedPreferences().getString(Constants.PREF_CUSTOM_CSS_PATH, "/mnt/sdcard/custom.css");
        customCssPathPref.setSummary("Path: " + customCssPath);
        customCssPathPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                customCssPathPref.setSummary("Path: " + newValue.toString());
                return true;
            }
        });
        // Line Spacing Preference update for Screen
        lineSpacePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String set = (String) newValue;
                preference.setSummary(getResources().getString(R.string.line_spacing_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + set + "%");
                return true;
            }
        });

        marginPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String set = (String) newValue;
                preference.setSummary(getResources().getString(R.string.margin_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + set + "%");
                return true;
            }
        });

        headingFontPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String set = (String) newValue;
                preference.setSummary(getResources().getString(R.string.pref_css_heading_fontface_summary) + set);
                return true;
            }
        });

        contentFontPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String set = (String) newValue;
                preference.setSummary(getResources().getString(R.string.pref_css_content_fontface_summary) + set);
                return true;
            }
        });

        final Preference css_backColorPref = findPreference(Constants.PREF_CSS_BACKGROUND);
        final Preference css_foreColorPref = findPreference(Constants.PREF_CSS_FOREGROUND);
        final Preference css_linkColorPref = findPreference(Constants.PREF_CSS_LINK_COLOR);
        final Preference css_tableBorderColorPref = findPreference(Constants.PREF_CSS_TABLE_BORDER);
        final Preference css_tableBackPref = findPreference(Constants.PREF_CSS_TABLE_BACKGROUND);

        css_backColorPref.setSummary(UIHelper.getBackgroundColor(this));
        css_backColorPref.setOnPreferenceChangeListener(colorChangeListener);
        setColorIcon(css_backColorPref, UIHelper.getBackgroundColor(this));

        css_foreColorPref.setSummary(UIHelper.getForegroundColor(this));
        css_foreColorPref.setOnPreferenceChangeListener(colorChangeListener);
        setColorIcon(css_foreColorPref, UIHelper.getForegroundColor(this));

        css_linkColorPref.setSummary(UIHelper.getLinkColor(this));
        css_linkColorPref.setOnPreferenceChangeListener(colorChangeListener);
        setColorIcon(css_linkColorPref, UIHelper.getLinkColor(this));

        css_tableBorderColorPref.setSummary(UIHelper.getThumbBorderColor(this));
        css_tableBorderColorPref.setOnPreferenceChangeListener(colorChangeListener);
        setColorIcon(css_tableBorderColorPref, UIHelper.getThumbBorderColor(this));

        css_tableBackPref.setSummary(UIHelper.getThumbBackgroundColor(this));
        css_tableBackPref.setOnPreferenceChangeListener(colorChangeListener);
        setColorIcon(css_tableBackPref, UIHelper.getThumbBackgroundColor(this));
    }

    private void setColorIcon(Preference colorPref, String hexColor) {
        int c = Color.parseColor(hexColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Drawable d1 = getResources().getDrawable(R.drawable.ic_square);
            d1.mutate().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
            colorPref.setIcon(d1);
        } else {
            Spannable summary = new SpannableString(hexColor);
            summary.setSpan(new ForegroundColorSpan(c), 0, summary.length(), 0);
            colorPref.setSummary(summary);
        }
    }

    private final OnPreferenceChangeListener colorChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String set = (String) newValue;
            try {
                int c = Color.parseColor(set);
                preference.setSummary(set);
                Drawable d = getResources().getDrawable(R.drawable.ic_square);
                d.mutate().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
                preference.setIcon(d);
                return true;
            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_color, set), Toast.LENGTH_SHORT).show();
                return false;
            }

        }
    };

    protected boolean checkImageStoragePath(String newPath) {
        if (Util.isStringNullOrEmpty(newPath)) {
            newPath = UIHelper.getImageRoot(this);
        }
        File dir = new File(newPath);
        if (!dir.exists()) {
            Log.e(TAG, String.format("Directory %s not exists, trying to create dir.", newPath));
            boolean result = dir.mkdirs();
            if (result) {
                Log.i(TAG, String.format("Directory %s created.", newPath));
                return true;
            } else {
                String message = String.format("Directory %s cannot be created.", newPath);
                Log.e(TAG, message);
                Toast.makeText(this, String.format("Directory %s cannot be created.", newPath), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            return true;
        }
    }

    private void setOrientation() {
        UIHelper.Recreate(this);
    }

    private void clearImages() {
        final String imageRoot = UIHelper.getImageRoot(this);
        UIHelper.createYesNoDialog(this, getResources().getString(R.string.clear_image_question), getResources().getString(R.string.clear_image_question2), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    Toast.makeText(getApplicationContext(), "Clearing Images...", Toast.LENGTH_SHORT).show();
                    DeleteRecursive(new File(imageRoot));
                    Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_SHORT).show();
                }
            }
        }).show();
    }

    private void clearDB() {
        UIHelper.createYesNoDialog(this, getResources().getString(R.string.clear_db_question), getResources().getString(R.string.clear_db_question2), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.clear_database), Toast.LENGTH_SHORT).show();
                    NovelsDao.getInstance().deleteDB();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.database_cleared), Toast.LENGTH_SHORT).show();
                }
            }
        }).show();
    }

    @SuppressWarnings("deprecation")
    private void runUpdate() {
        LNReaderApplication.getInstance().runUpdateService(true, this);
        Preference runUpdates = findPreference(Constants.PREF_RUN_UPDATES);
        runUpdates.setSummary(getResources().getString(R.string.running));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // relisting all handler
        LNReaderApplication.getInstance().setUpdateServiceListener(this);
        LNReaderApplication.getInstance().setAutoBackupServiceListener(this);

        if (ZipFilesTask.getInstance() != null) {
            zipTask = ZipFilesTask.getInstance();
            if (zipTask.getStatus() == AsyncTask.Status.RUNNING) {
                zipTask.setCallback(this, Constants.PREF_BACKUP_THUMB_IMAGES);
            }
        }
        if (UnZipFilesTask.getInstance() != null) {
            unzipTask = UnZipFilesTask.getInstance();
            if (unzipTask.getStatus() == AsyncTask.Status.RUNNING) {
                unzipTask.setCallback(this, Constants.PREF_RESTORE_THUMB_IMAGES);
            }
        }

        if (RelinkImagesTask.getInstance() != null) {
            relinkTask = RelinkImagesTask.getInstance();
            if (relinkTask.getStatus() == AsyncTask.Status.RUNNING) {
                relinkTask.setCallback(this, Constants.PREF_RELINK_THUMB_IMAGES);
            }
        }

        String key = DeleteFilesTask.class.toString() + ":DeleteExternalTemp";
        deleteTask = (DeleteFilesTask) LNReaderApplication.getInstance().getTask(key);
        if (deleteTask != null)
            deleteTask.owner = this;

        key = CopyDBTask.class.toString() + ":BackupDB";
        copyDbTask = (CopyDBTask) LNReaderApplication.getInstance().getTask(key);
        if (copyDbTask != null)
            copyDbTask.setCallbackNotifier(this);

        key = CopyDBTask.class.toString() + ":RestoreDB";
        restoreDbTask = (CopyDBTask) LNReaderApplication.getInstance().getTask(key);
        if (restoreDbTask != null)
            restoreDbTask.setCallbackNotifier(this);

        UIHelper.CheckScreenRotation(this);
        UIHelper.CheckKeepAwake(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LNReaderApplication.getInstance().setUpdateServiceListener(null);
        LNReaderApplication.getInstance().setAutoBackupServiceListener(null);
    }

    private void DeleteRecursive(File fileOrDirectory) {
        // Skip Database
        if (fileOrDirectory.getAbsolutePath() == DBHelper.getDbPath(this))
            return;
        if (fileOrDirectory.getAbsolutePath().contains("databases/pages.db")) {
            Log.d(TAG, "Skippin DB!");
            return;
        }

        if (fileOrDirectory.isDirectory())
            Log.d(TAG, "Deleting Dir: " + fileOrDirectory.getAbsolutePath());
        File[] fileList = fileOrDirectory.listFiles();
        if (fileList == null || fileList.length == 0)
            return;

        for (File child : fileList)
            DeleteRecursive(child);

        boolean result = fileOrDirectory.delete();
        if (!result)
            Log.e(TAG, "Failed to delete: " + fileOrDirectory.getAbsolutePath());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onProgressCallback(ICallbackEventData message) {
        // default goes to update
        if (Util.isStringNullOrEmpty(message.getSource())) {
            Preference runUpdates = findPreference(Constants.PREF_RUN_UPDATES);
            runUpdates.setSummary("Status: " + message.getMessage());
        } else {
            Preference pref = findPreference(message.getSource());
            if (pref != null)
                pref.setSummary("Status: " + message.getMessage());
        }
    }

    @Override
    public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
        onProgressCallback(message);
    }

    private void recreateUI() {
        // UIHelper.Recreate(this);
        finish();
        startActivity(getIntent());
        UIHelper.CheckScreenRotation(this);
        UIHelper.CheckKeepAwake(this);
    }

    private boolean getProcessAllImagesPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_PROCESS_ALL_IMAGES, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
