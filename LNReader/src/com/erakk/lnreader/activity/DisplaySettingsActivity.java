package com.erakk.lnreader.activity;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.service.MyScheduleReceiver;

public class DisplaySettingsActivity extends SherlockPreferenceActivity implements ICallbackNotifier{
	private static final String TAG = DisplaySettingsActivity.class.toString();
	private boolean isInverted;
	private ProgressDialog dialog = null;

	Context context;

	/**************************************************************
	 *	The onPreferenceTreeClick method's sole purpose is to deal with the known Android
	 *	bug that doesn't custom theme the child preference screen
	 *****************************************************************/
	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
    	if (preference!=null)
	    	if (preference instanceof PreferenceScreen)
	        	if (((PreferenceScreen)preference).getDialog()!=null)
	        		((PreferenceScreen)preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
    	return false;

	}


	@SuppressLint("SdCardPath")
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		context = this;
		UIHelper.SetTheme(this, null);
		super.onCreate(savedInstanceState);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

        //This man is deprecated but but we may want to be able to run on older API
        addPreferencesFromResource(R.xml.preferences);

        // Screen Orientation
        Preference lockHorizontal = findPreference(Constants.PREF_LOCK_HORIZONTAL);
        lockHorizontal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
        		if(p.getSharedPreferences().getBoolean(Constants.PREF_LOCK_HORIZONTAL, false)){
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        		}
        		else {
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        		}
        		return true;
            }
        });

        // Invert Color
        Preference invertColors = findPreference(Constants.PREF_INVERT_COLOR);
        invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	recreateUI();
                return true;
            }
        });

        /* A section to change Application Language
         *
         *  @freedomofkeima
         */
        final Preference changeLanguages = findPreference(Constants.PREF_LANGUAGE);
        final String[] languageSelectionArray = getResources().getStringArray(R.array.languageSelection);
        int languageSelectionValue = UIHelper.GetIntFromPreferences(Constants.PREF_LANGUAGE, 0);
        changeLanguages.setSummary(String.format(getResources().getString(R.string.selected_language), languageSelectionArray[languageSelectionValue]));

        changeLanguages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int languageSelectionValue = Util.tryParseInt(newValue.toString(), 0);
				UIHelper.setLanguage(context, languageSelectionValue);
				LNReaderApplication.getInstance().restartApplication();
		        return true;
			}
		});

        /* End of language section */
        
        /* A section to change Alternative Languages list
         * 
         *  @freedomofkeima
         */
        Preference selectAlternativeLanguage = findPreference("select_alternative_language");
        /* List of languages */
        final boolean indonesiaLanguage = PreferenceManager.getDefaultSharedPreferences(
				LNReaderApplication.getInstance().getApplicationContext())
				.getBoolean(Constants.LANG_BAHASA_INDONESIA, true);
        /* End of list of languages */
        selectAlternativeLanguage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	final boolean[] states = {indonesiaLanguage};
            	 /* Show checkBox to screen */
            	   AlertDialog.Builder builder = new AlertDialog.Builder(context);
            	    builder.setTitle(getResources().getString(R.string.alternative_language_title));
            	    builder.setMultiChoiceItems(Constants.languagelistNotDefault, states, new DialogInterface.OnMultiChoiceClickListener(){
            	        public void onClick(DialogInterface dialogInterface, int item, boolean state) {
            	        }
            	    });
            	    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int id) {
            	            SparseBooleanArray Checked = ((AlertDialog)dialog).getListView().getCheckedItemPositions();
            	            /* Save all choices to Shared Preferences */
            	            UIHelper.setAlternativeLanguagePreferences(context, Constants.LANG_BAHASA_INDONESIA, Checked.get(Checked.keyAt(0)));
            	            recreateUI();
            	        }
            	    });
            	    builder.setPositiveButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int id) {
            	             dialog.cancel();
            	        }
            	    });
            	    builder.create().show();
        		return true;
            }
        });       
        /* End of alternative languages list section */

        Preference clearDatabase = findPreference("clear_database");
        clearDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	clearDB();
        		return true;
            }
        });

        Preference backupDatabase = findPreference("backup_database");
        backupDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	try {
					copyDB(true);
				} catch (IOException e) {
					Log.e(TAG, "Error when backing up DB", e);
				}
        		return true;
            }
        });

        Preference restoreDatabase = findPreference("restore_database");
        restoreDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	try {
					copyDB(false);
				} catch (IOException e) {
					Log.e(TAG, "Error when restoring DB", e);
				}
        		return true;
            }
        });

        Preference clearImages = findPreference("clear_image_cache");
        clearImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
        		clearImages();
                return true;
            }
        });

        final Preference updatesInterval = findPreference(Constants.PREF_UPDATE_INTERVAL);
        final String[] updateIntervalArray = getResources().getStringArray(R.array.updateInterval);
        int updatesIntervalValue = UIHelper.GetIntFromPreferences(Constants.PREF_UPDATE_INTERVAL, 0);
        updatesInterval.setSummary(String.format(getResources().getString(R.string.update_interval_summary), updateIntervalArray[updatesIntervalValue]));
        updatesInterval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int updatesIntervalInt = Util.tryParseInt(newValue.toString(), 0);
				MyScheduleReceiver.reschedule(updatesIntervalInt);
				updatesInterval.setSummary(String.format(getResources().getString(R.string.update_interval_summary), updateIntervalArray[updatesIntervalInt]));
                return true;
			}
		});

        Preference runUpdates = findPreference(Constants.PREF_RUN_UPDATES);
        runUpdates.setSummary(String.format(getResources().getString(R.string.last_run)
        					, runUpdates.getSharedPreferences().getString(Constants.PREF_RUN_UPDATES, getResources().getString(R.string.none))
        					, runUpdates.getSharedPreferences().getString(Constants.PREF_RUN_UPDATES_STATUS, getResources().getString(R.string.unknown))
        					));
        runUpdates.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	runUpdate();
                return true;
            }
        });

        Preference appVersion = findPreference("app_version");
        String version = "N/A";
        try {
        	version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + ")";
		} catch (NameNotFoundException e) { }
        appVersion.setSummary(version);

        final Preference uiMode = findPreference("ui_selection");
        final String[] uiSelectionArray = getResources().getStringArray(R.array.uiSelection);
        int uiSelectionValue = UIHelper.GetIntFromPreferences(Constants.PREF_UI_SELECTION, 0);
        uiMode.setSummary(String.format(getResources().getString(R.string.selected_mode), uiSelectionArray[uiSelectionValue]));
        uiMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int uiSelectionValue = Util.tryParseInt(newValue.toString(), 0);
		        uiMode.setSummary(String.format(getResources().getString(R.string.selected_mode),uiSelectionArray[uiSelectionValue]));
		        return true;
			}
		});

        Preference defaultSaveLocation = findPreference("save_location");
        defaultSaveLocation.setSummary(String.format(getResources().getString(R.string.download_image_to), Constants.IMAGE_ROOT));

        Preference defaultDbLocation = findPreference("db_location");
        defaultDbLocation.setSummary(String.format(getResources().getString(R.string.novel_database_to), DBHelper.getDbPath(this)));

        Preference tos = findPreference("tos");
        tos.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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

        final Preference scrollingSize = findPreference(Constants.PREF_SCROLL_SIZE);
        int scrollingSizeValue = UIHelper.GetIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5);
        scrollingSize.setSummary(String.format(getResources().getString(R.string.scroll_size_summary2), scrollingSizeValue));
        scrollingSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int scrollingSizeValue =  Util.tryParseInt(newValue.toString(), 5);
				scrollingSize.setSummary(String.format(getResources().getString(R.string.scroll_size_summary2), scrollingSizeValue));
                return true;
			}
		});

        LNReaderApplication.getInstance().setUpdateServiceListener(this);
		isInverted = getColorPreferences();

        /************************************************************
         *  CSS Layout Behaviours
         *  1. When user's css sheet is used, disable the force justify, linespace and margin preferences
         *  2. When about to use user's css sheet, display a warning/message (NOT IMPLEMENTED)
         *  3. When linespace/margin is changed, update the summary text to reflect current value
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
        if(currUserCSS){
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
        lineSpacePref.setSummary( getResources().getString(R.string.line_spacing_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + currLineSpacing + "%");
        marginPref.setSummary( getResources().getString(R.string.margin_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + currMargin + "%");

        //Behaviour 1 (Updated Preference)
        user_cssPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
        	{

				public boolean onPreferenceChange(Preference preference, Object newValue) {
						Boolean set = (Boolean) newValue;

						if(set){
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
        	}
        );


        String customCssPath = customCssPathPref.getSharedPreferences().getString(Constants.PREF_CUSTOM_CSS_PATH, "/mnt/sdcard/custom.css");
        customCssPathPref.setSummary("Path: " + customCssPath);
        customCssPathPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {
				customCssPathPref.setSummary("Path: " + newValue.toString());
				return true;
			}
		});
        // Line Spacing Preference update for Screen
        lineSpacePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
        	{
				public boolean onPreferenceChange(Preference preference, Object newValue) {
						String set = (String) newValue;
						preference.setSummary(getResources().getString(R.string.line_spacing_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + set + "%");
					return true;
				}
        	}
        );

        marginPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
        		{
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						String set = (String) newValue;
						preference.setSummary(getResources().getString(R.string.margin_summary2) + " \n" + getResources().getString(R.string.current_value) + ": " + set + "%");
						return true;
					}
        		});
    }


	private void clearImages() {
		UIHelper.createYesNoDialog(this, getResources().getString(R.string.clear_image_question), getResources().getString(R.string.clear_image_question2) , new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE) {
					Toast.makeText(getApplicationContext(), "Clearing Images...", Toast.LENGTH_SHORT).show();
					DeleteRecursive(new File(Constants.IMAGE_ROOT));
					Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_SHORT).show();
				}
			}
		}).show();
	}

	private void clearDB() {
		UIHelper.createYesNoDialog(this, getResources().getString(R.string.clear_db_question), getResources().getString(R.string.clear_db_question2), new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE) {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.clear_database), Toast.LENGTH_SHORT).show();
					NovelsDao.getInstance(getApplicationContext()).deleteDB();
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.database_cleared), Toast.LENGTH_SHORT).show();
				}
			}
		}).show();
	}

	private void copyDB(boolean makeBackup) throws IOException {
		dialog = ProgressDialog.show(this, getResources().getString(R.string.database_manager), getResources().getString(R.string.database_backup_create) , true, true);
		String filePath = NovelsDao.getInstance(getApplicationContext()).copyDB(getApplicationContext(),makeBackup);
		if (filePath == "null") {
			Toast.makeText(getApplicationContext(),getResources().getString(R.string.database_not_found), Toast.LENGTH_SHORT).show();
		}
		else {
			if (makeBackup)
				Toast.makeText(getApplicationContext(), "Database backup created at " + filePath + "", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), "Database backup restored!", Toast.LENGTH_SHORT).show();
		}
		dialog.dismiss();
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
        if(isInverted != getColorPreferences()) {
        	UIHelper.Recreate(this);
        }
    }

	@Override
	protected void onStop(){
		super.onStop();
		LNReaderApplication.getInstance().setUpdateServiceListener(null);
	}

	private void DeleteRecursive(File fileOrDirectory) {
		// Skip Database
		if(fileOrDirectory.getAbsolutePath() == DBHelper.getDbPath(this)) return;
		if(fileOrDirectory.getAbsolutePath().contains("databases/pages.db")) {
			Log.d(TAG, "Skippin DB!");
			return;
		}

	    if (fileOrDirectory.isDirectory())
	    	Log.d(TAG, "Deleting Dir: " + fileOrDirectory.getAbsolutePath());
	    	File[] fileList = fileOrDirectory.listFiles();
	    	if(fileList == null || fileList.length == 0) return;

	        for (File child : fileList)
	            DeleteRecursive(child);

	    boolean result = fileOrDirectory.delete();
	    if(!result) Log.e(TAG, "Failed to delete: " + fileOrDirectory.getAbsolutePath());
	}

	@SuppressWarnings("deprecation")
	public void onCallback(ICallbackEventData message) {
		Preference runUpdates = findPreference(Constants.PREF_RUN_UPDATES);
		runUpdates.setSummary("Status: " + message.getMessage());
	}

	private void recreateUI() {
		UIHelper.Recreate(this);
	}

	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
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
