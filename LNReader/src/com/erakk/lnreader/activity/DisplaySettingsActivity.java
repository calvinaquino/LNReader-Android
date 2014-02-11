package com.erakk.lnreader.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.FileListAdapter;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.service.AutoBackupScheduleReceiver;
import com.erakk.lnreader.service.AutoBackupService;
import com.erakk.lnreader.service.UpdateScheduleReceiver;
import com.erakk.lnreader.task.CopyDBTask;
import com.erakk.lnreader.task.DeleteFilesTask;
import com.erakk.lnreader.task.RelinkImagesTask;
import com.erakk.lnreader.task.UnZipFilesTask;
import com.erakk.lnreader.task.ZipFilesTask;

public class DisplaySettingsActivity extends SherlockPreferenceActivity implements ICallbackNotifier {
	private static final String TAG = DisplaySettingsActivity.class.toString();
	private boolean isInverted;
	private DeleteFilesTask deleteTask;

	// Context context;

	/**************************************************************
	 * The onPreferenceTreeClick method's sole purpose is to deal with the known
	 * Android bug that doesn't custom theme the child preference screen
	 *****************************************************************/
	@Override
	@SuppressWarnings("deprecation")
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		if (preference != null)
			if (preference instanceof PreferenceScreen)
				if (((PreferenceScreen) preference).getDialog() != null) {
					/* If API Version >= 11 */
					try {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
							initializeActionBar((PreferenceScreen) preference);
						} else
							((PreferenceScreen) preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
					} catch (NullPointerException e) {
						Log.e(TAG, "Null Pointer Exception in PreferenceScreen Child.");
					}
				}

		return false;

	}

	/*
	 * Action Bar doesn't inherit parent (on nested Preference Tree)
	 * Reference :
	 * http://stackoverflow.com/questions/16374820/action-bar-home-button-not-functional-with-nested-preferencescreen
	 * Because of DialogInterface and View have OnClickListener, I've changed manually it to android.view.View
	 * (@freedomofkeima)
	 */
	/** Sets up the action bar for an {@link PreferenceScreen} */
	@SuppressLint("NewApi")
	public static void initializeActionBar(PreferenceScreen preferenceScreen) {
		final Dialog dialog = preferenceScreen.getDialog();

		if (dialog != null) {
			// Initialize the action bar
			dialog.getActionBar().setDisplayHomeAsUpEnabled(true);

			// Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are
			// dialogs which swallow events instead of passing to the activity
			// Related Issue: https://code.google.com/p/android/issues/detail?id=4611
			android.view.View homeBtn = dialog.findViewById(android.R.id.home);

			if (homeBtn != null) {
				android.view.View.OnClickListener dismissDialogClickListener = new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						dialog.dismiss();
					}
				};

				// Prepare yourselves for some hacky programming
				ViewParent homeBtnContainer = homeBtn.getParent();

				// The home button is an ImageView inside a FrameLayout
				if (homeBtnContainer instanceof FrameLayout) {
					ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();
					if (containerParent instanceof LinearLayout) {
						// This view also contains the title text, set the whole view as clickable
						((LinearLayout) containerParent).setOnClickListener(dismissDialogClickListener);
					} else {
						// Just set it on the home button
						((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
					}
				} else {
					// The 'If all else fails' default case
					homeBtn.setOnClickListener(dismissDialogClickListener);
				}
			}
		}
	}

	@Override
	@SuppressLint("SdCardPath")
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		// context = this;
		UIHelper.SetTheme(this, null);
		super.onCreate(savedInstanceState);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

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
		} catch (NameNotFoundException e) {
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
		isInverted = UIHelper.getColorPreferences(this);
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
		Preference invertColors = findPreference(Constants.PREF_INVERT_COLOR);
		invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference p) {
				recreateUI();
				return true;
			}
		});

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
		runUpdates.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

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
			Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
			intent.putExtra(EXTRA_SHOW_FRAGMENT, "com.android.settings.tts.TextToSpeechSettings");
			intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, intent.getExtras());
			startActivityForResult(intent, 0);
		} catch (Exception ex) {
			startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
		}
	}

	@SuppressWarnings("deprecation")
	private void storagePreferences() {
		final DisplaySettingsActivity dsa = this;
		// Clear DB
		Preference clearDatabase = findPreference("clear_database");
		clearDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

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
		restoreDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

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
		backupDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

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
				}
				else {
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
		clearImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

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
		if (ZipFilesTask.getInstance() != null && ZipFilesTask.getInstance().getStatus() == Status.RUNNING) {
			ZipFilesTask.getInstance().setCallback(this, Constants.PREF_BACKUP_THUMB_IMAGES);
		}

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
		if (UnZipFilesTask.getInstance() != null && UnZipFilesTask.getInstance().getStatus() == Status.RUNNING) {
			UnZipFilesTask.getInstance().setCallback(this, Constants.PREF_RESTORE_THUMB_IMAGES);
		}

		// relink thumbs
		Preference relinkThumbs = findPreference(Constants.PREF_RELINK_THUMB_IMAGES);
		relinkThumbs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				// Quick fix, please revise as seen fit.
				// Confirm task execution, useful during unintentional clicks.
				UIHelper.createYesNoDialog(
						dsa
						, getResources().getString(R.string.relink_question)
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
		if (RelinkImagesTask.getInstance() != null && RelinkImagesTask.getInstance().getStatus() == Status.RUNNING) {
			RelinkImagesTask.getInstance().setCallback(this, Constants.PREF_RELINK_THUMB_IMAGES);
		}
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
		String result = NovelsDao.getInstance(this).checkDB();
		Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
	}

	@SuppressLint({ "InlinedApi", "NewApi" })
	private void relinkThumbs() {
		String rootPath = UIHelper.getImageRoot(this);
		RelinkImagesTask task = RelinkImagesTask.getInstance(rootPath, this, Constants.PREF_RELINK_THUMB_IMAGES);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	@SuppressLint({ "InlinedApi", "NewApi" })
	private void restoreThumbs() {
		if (ZipFilesTask.getInstance() != null && ZipFilesTask.getInstance().getStatus() == Status.RUNNING) {
			Toast.makeText(this, "Please wait until all images are backed-up.", Toast.LENGTH_SHORT).show();
			return;
		}

		String zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_thumbs.zip";
		String thumbRootPath = UIHelper.getImageRoot(this) + "/project/images/thumb";

		if (getProcessAllImagesPreferences()) {
			zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_all_images.zip";
			thumbRootPath = UIHelper.getImageRoot(this) + "/project/images";
		}

		UnZipFilesTask task = UnZipFilesTask.getInstance(zipName, thumbRootPath, this, Constants.PREF_RESTORE_THUMB_IMAGES);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	private void showBackupsDB() {
		ArrayList<File> backups = AutoBackupService.getBackupFiles(this);
		final FileListAdapter adapter = new FileListAdapter(this, R.layout.file_list_item, backups);
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
		CopyDBTask task = new CopyDBTask(false, this, Constants.PREF_RESTORE_DB, filename);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	@SuppressLint("NewApi")
	private void backupDB() {
		CopyDBTask task = new CopyDBTask(true, this, Constants.PREF_BACKUP_DB, null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	@SuppressLint({ "InlinedApi", "NewApi" })
	private void backupThumbs() {
		if (UnZipFilesTask.getInstance() != null && UnZipFilesTask.getInstance().getStatus() == Status.RUNNING) {
			Toast.makeText(this, "Please wait until all images are restored.", Toast.LENGTH_SHORT).show();
			return;
		}

		String zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_thumbs.zip";
		String thumbRootPath = UIHelper.getImageRoot(this) + "/project/images/thumb";

		if (getProcessAllImagesPreferences()) {
			zipName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_all_images.zip";
			thumbRootPath = UIHelper.getImageRoot(this) + "/project/images";
		}

		ZipFilesTask task = ZipFilesTask.getInstance(zipName, thumbRootPath, this, Constants.PREF_BACKUP_THUMB_IMAGES);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	@SuppressLint("NewApi")
	private void deleteExternalTemp() {
		String filename = UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + "/wac/temp";
		deleteTask = new DeleteFilesTask(null, filename);
		String key = DeleteFilesTask.class.toString() + ":" + deleteTask;
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, deleteTask);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				deleteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				deleteTask.execute();
		}
		else {
			DeleteFilesTask tempTask = (DeleteFilesTask) LNReaderApplication.getInstance().getTask(key);
			if (tempTask != null) {
				deleteTask = tempTask;
				//deleteTask.owner = this;
			}
		}
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
		Iterator<Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
		int j = 0;
		while (it.hasNext()) {
			AlternativeLanguageInfo info = it.next().getValue();
			/* Default value of unregistered Alternative language = false (preventing too much tabs) */
			languageStatus[j] = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(info.getLanguage(), false);
			j++;
			it.remove();
		}
		/* End of list of languages */
		selectAlternativeLanguage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

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
		Iterator<Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
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
		builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int id) {
				setLanguageSelectionOKDialog(dialog);
			}
		});
		builder.setPositiveButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {

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
		Iterator<Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
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
	private void setCssPreferences() {
		/************************************************************
		 * CSS Layout Behaviours 1. When user's css sheet is used, disable the
		 * force justify, linespace and margin preferences 2. When about to use
		 * user's css sheet, display a warning/message (NOT IMPLEMENTED) 3. When
		 * linespace/margin is changed, update the summary text to reflect
		 * current value
		 ***************************************************************/

		final Preference user_cssPref = findPreference(Constants.PREF_USE_CUSTOM_CSS);
		final Preference lineSpacePref = findPreference(Constants.PREF_LINESPACING);
		final Preference justifyPref = findPreference(Constants.PREF_FORCE_JUSTIFIED);
		final Preference customCssPathPref = findPreference(Constants.PREF_CUSTOM_CSS_PATH);
		final Preference marginPref = findPreference(Constants.PREF_MARGINS);

		// Retrieve inital values stored
		Boolean currUserCSS = getPreferenceScreen().getSharedPreferences().getBoolean(Constants.PREF_USE_CUSTOM_CSS, false);
		String currLineSpacing = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_LINESPACING, "150");
		String currMargin = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_MARGINS, "5");

		// Behaviour 1 (Activity first loaded)
		if (currUserCSS) {
			marginPref.setEnabled(false);
			lineSpacePref.setEnabled(false);
			justifyPref.setEnabled(false);
			customCssPathPref.setEnabled(true);
		} else {
			marginPref.setEnabled(true);
			lineSpacePref.setEnabled(true);
			justifyPref.setEnabled(true);
			customCssPathPref.setEnabled(false);
		}

		// Behaviour 3 (Activity first loaded)
		lineSpacePref.setSummary(getResources().getString(R.string.line_spacing_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + currLineSpacing + "%");
		marginPref.setSummary(getResources().getString(R.string.margin_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + currMargin + "%");

		// Behaviour 1 (Updated Preference)
		user_cssPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean set = (Boolean) newValue;

				if (set) {
					marginPref.setEnabled(false);
					lineSpacePref.setEnabled(false);
					justifyPref.setEnabled(false);
					customCssPathPref.setEnabled(true);

				} else {
					marginPref.setEnabled(true);
					lineSpacePref.setEnabled(true);
					justifyPref.setEnabled(true);
					customCssPathPref.setEnabled(false);
				}
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
	}

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
		UIHelper.CheckScreenRotation(this);
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
					NovelsDao.getInstance(getApplicationContext()).deleteDB();
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
		if (isInverted != UIHelper.getColorPreferences(this)) {
			UIHelper.Recreate(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// relisting all handler
		LNReaderApplication.getInstance().setUpdateServiceListener(this);
		LNReaderApplication.getInstance().setAutoBackupServiceListener(this);
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
		CallbackEventData msg = (CallbackEventData) message;
		if (Util.isStringNullOrEmpty(msg.getSource())) {
			Preference runUpdates = findPreference(Constants.PREF_RUN_UPDATES);
			runUpdates.setSummary("Status: " + message.getMessage());
		} else {
			Preference pref = findPreference(msg.getSource());
			if (pref != null)
				pref.setSummary("Status: " + message.getMessage());
		}
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
}
