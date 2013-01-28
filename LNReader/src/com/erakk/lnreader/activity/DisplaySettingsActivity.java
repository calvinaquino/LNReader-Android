package com.erakk.lnreader.activity;

import java.io.File;
import java.io.IOException;

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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.service.MyScheduleReceiver;

public class DisplaySettingsActivity extends PreferenceActivity implements ICallbackNotifier{
	private static final String TAG = DisplaySettingsActivity.class.toString();
	private boolean isInverted;
	private ProgressDialog dialog = null;
	Context context;
	
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
        Preference lockHorizontal = (Preference)  findPreference(Constants.PREF_LOCK_HORIZONTAL);
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
        Preference invertColors = (Preference)  findPreference(Constants.PREF_INVERT_COLOR);
        invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	recreateUI();
                return true;
            }
        });
        
        
        Preference clearDatabase = (Preference)  findPreference("clear_database");
        clearDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	clearDB();            	
        		return true;
            }
        });
        
        Preference backupDatabase = (Preference)  findPreference("backup_database");
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
        
        Preference restoreDatabase = (Preference)  findPreference("restore_database");
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
        
        Preference clearImages = (Preference)  findPreference("clear_image_cache");
        clearImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
        		clearImages();
                return true;
            }
        });
        
        final Preference updatesInterval = (Preference)  findPreference(Constants.PREF_UPDATE_INTERVAL);
        final String[] updateIntervalArray = getResources().getStringArray(R.array.updateInterval);
        int updatesIntervalValue = UIHelper.GetIntFromPreferences(Constants.PREF_UPDATE_INTERVAL, 0);
        updatesInterval.setSummary("Define how often updates will be verified (" + updateIntervalArray[updatesIntervalValue] + ")");
        updatesInterval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int updatesIntervalInt = Integer.parseInt(newValue.toString());	
				MyScheduleReceiver.reschedule(updatesIntervalInt);
				updatesInterval.setSummary("Define how often updates will be verified (" + updateIntervalArray[updatesIntervalInt] + ")");
                return true;
			}
		});
        
        Preference runUpdates = (Preference) findPreference(Constants.PREF_RUN_UPDATES);
        runUpdates.setSummary("Last Run: " + runUpdates.getSharedPreferences().getString(Constants.PREF_RUN_UPDATES, "None") + 
        					  "\nStatus: " + runUpdates.getSharedPreferences().getString(Constants.PREF_RUN_UPDATES_STATUS, "Unknown"));
        runUpdates.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
            	runUpdate();
                return true;
            }
        });

        Preference appVersion = (Preference) findPreference("app_version");
        try {
			appVersion.setSummary(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			appVersion.setSummary("N/A");
		}
        
        Preference defaultSaveLocation = (Preference) findPreference("save_location");
        defaultSaveLocation.setSummary("Downloaded images saved to: " + Constants.IMAGE_ROOT);
        
        Preference defaultDbLocation = (Preference) findPreference("db_location");
        defaultDbLocation.setSummary("Novel Database saved to: " + DBHelper.getDbPath(this));
                        
        Preference tos = (Preference) findPreference("tos");
        tos.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				try {
					Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
			        intent.putExtra(Constants.EXTRA_PAGE, "Baka-Tsuki:Copyrights");
			        startActivity(intent);
				} catch (Exception e) {
					Log.e(TAG, "Cannot get copyright page.", e);
				}				
				return false;
			}
		});
        
        final Preference scrollingSize = (Preference)  findPreference(Constants.PREF_SCROLL_SIZE);
        int scrollingSizeValue = UIHelper.GetIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5);
        scrollingSize.setSummary("Scrolling size for volumer rocker (" + scrollingSizeValue + ")");
        scrollingSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int scrollingSizeValue = Integer.parseInt(newValue.toString());	
				scrollingSize.setSummary("Scrolling size for volumer rocker (" + scrollingSizeValue + ")");
                return true;
			}
		});
        
        LNReaderApplication.getInstance().setUpdateServiceListener(this);
        isInverted = getColorPreferences();        

        /************************************************************
         *  CSS Layout Behaviours
         *  1. When user's css sheet is used, disable the force justify and linespace preferences
         *  2. When about to use user's css sheet, display a warning/message
         *  3. When linespace is changed, update the summary text to reflect current value
         ***************************************************************/
        
        final Preference user_cssPref = (Preference) findPreference(Constants.PREF_USE_CUSTOM_CSS);
        final Preference lineSpacePref = (Preference) findPreference(Constants.PREF_LINESPACING);
        final Preference justifyPref = (Preference) findPreference(Constants.PREF_FORCE_JUSTIFIED);
        final Preference customCssPathPref = (Preference) findPreference(Constants.PREF_CUSTOM_CSS_PATH);
        
        // Retrieve inital values stored
        Boolean currUserCSS = getPreferenceScreen().getSharedPreferences().getBoolean(Constants.PREF_USE_CUSTOM_CSS, false);
        String currLineSpacing = getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_LINESPACING, "150");
        
        // Behaviour 1 (Activity first loaded)
        if(currUserCSS){
        	lineSpacePref.setEnabled(false);
        	justifyPref.setEnabled(false);
        	customCssPathPref.setEnabled(true);
        } else {
        	lineSpacePref.setEnabled(true);
        	justifyPref.setEnabled(true);
        	customCssPathPref.setEnabled(false);       	
        }
        
        // Behaviour 3 (Activity first loaded)
        lineSpacePref.setSummary("Increases the space between lines. The greater the number, the more padding it has. \nCurrent value: " + currLineSpacing + "%");
        
        //Behaviour 1 (Updated Preference)
        user_cssPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
        	{

				public boolean onPreferenceChange(Preference preference, Object newValue) {
						Boolean set = (Boolean) newValue;
						
						if(set){
				        	lineSpacePref.setEnabled(false);
				        	justifyPref.setEnabled(false);
				        	customCssPathPref.setEnabled(true);

						} else {
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
						preference.setSummary("Increases the space between lines. The greater the number, the more padding it has. Current value " + set + "%");
					return true;
				}
        	}
        );
    }

	
	private void clearImages() {
		UIHelper.createYesNoDialog(this, "Do you want to clear the Image Cache?", "Clear Image Cache", new OnClickListener() {
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
		UIHelper.createYesNoDialog(this, "Do you want to clear the DB?", "Clear DB", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE) {
					Toast.makeText(getApplicationContext(), "Clearing Database...", Toast.LENGTH_SHORT).show();
					NovelsDao.getInstance(getApplicationContext()).deleteDB();
					Toast.makeText(getApplicationContext(), "Database cleared!", Toast.LENGTH_SHORT).show();
				}
			}
		}).show();
	}
	
	private void copyDB(boolean makeBackup) throws IOException {
		dialog = ProgressDialog.show(this, "Database Manager", "Creating Database backup...", true, true);
		String filePath = NovelsDao.getInstance(getApplicationContext()).copyDB(getApplicationContext(),makeBackup);
		if (filePath == "null") {
			Toast.makeText(getApplicationContext(), "Could not find a backup database.", Toast.LENGTH_SHORT).show();
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
		Preference runUpdates = (Preference) findPreference(Constants.PREF_RUN_UPDATES);
		runUpdates.setSummary("Running...");
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
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);
	    
	    fileOrDirectory.delete();
	}

	@SuppressWarnings("deprecation")
	public void onCallback(ICallbackEventData message) {
		Preference runUpdates = (Preference) findPreference(Constants.PREF_RUN_UPDATES);
		runUpdates.setSummary("Status: " + message.getMessage());
	}
	
	private void recreateUI() {
		UIHelper.Recreate(this);
	}
	
	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
}
