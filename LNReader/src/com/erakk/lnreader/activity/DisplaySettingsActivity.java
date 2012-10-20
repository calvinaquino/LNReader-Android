package com.erakk.lnreader.activity;

import java.io.File;
import java.util.Collection;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
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
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.service.MyScheduleReceiver;

public class DisplaySettingsActivity extends PreferenceActivity implements ICallbackNotifier{
	private static final String TAG = DisplaySettingsActivity.class.toString();
	private boolean isInverted;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
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
        		/*
        		 * CODE TO CLEAR DATABASE HERE
        		 */
        		NovelsDao dao = NovelsDao.getInstance(getApplicationContext());
        		dao.deleteDB();
        		Toast.makeText(getApplicationContext(), "Database cleared!", Toast.LENGTH_LONG).show();	
        		return true;
            }
        });
        
        Preference clearImages = (Preference)  findPreference("clear_image_cache");
        clearImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
        		/*
        		 * CODE TO CLEAR IMAGE CACHE HERE
        		 */        		
        		DeleteRecursive(new File(Constants.IMAGE_ROOT));
        		Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_LONG).show();	
        		
                return true;
            }
        });
        
        final Preference updatesInterval = (Preference)  findPreference(Constants.PREF_UPDATE_INTERVAL);
        updatesInterval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int updatesIntervalInt = Integer.parseInt(newValue.toString());	
				MyScheduleReceiver.reschedule(updatesIntervalInt);
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
        
        Preference asyncTaskCount = (Preference) findPreference("task_count");
        int running = 0;
        int finished = 0;
        Collection<AsyncTask<?, ?, ?>> list = LNReaderApplication.getTaskList().values(); 
        for (AsyncTask<?, ?, ?> asyncTask : list) {
			if(asyncTask.getStatus() == Status.RUNNING){
				++running;
			}
			else if(asyncTask.getStatus() == Status.FINISHED) {
				++finished;
			}
        }        
        asyncTaskCount.setSummary("AsyncTask Count: " + running + " running, "+ finished + " stopped.");
        
        Preference tos = (Preference) findPreference("tos");
        tos.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
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
        
        
        LNReaderApplication.getInstance().setUpdateServiceListener(this);
        isInverted = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false);
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
        if(isInverted != PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false)) {
        	UIHelper.Recreate(this);
        }
    }
	
	@Override
	protected void onStop(){
		super.onStop();
		LNReaderApplication.getInstance().setUpdateServiceListener(null);
	}
	
	private void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
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
}
