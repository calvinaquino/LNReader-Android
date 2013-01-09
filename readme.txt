DEVELOPMENT REQUIREMENT:
====================================================
* Android SDK
* eclipse Juno + ADT
* JSoup
* Android 3+ (Honeycomb)

APPLICATION REQUIREMENT:
====================================================
* Recommended Android 3.0+ (API Level 11)
* Minimum     Android 2.2  (API Level 8)
* Some features might not working if running below the recommended version.

LINE-ENDINGS & SOURCE ENCODING CONFIG
====================================================
* Git Config:
  - https://help.github.com/articles/dealing-with-line-endings
  - https://stackoverflow.com/questions/1889559/git-diff-to-ignore-m
* Eclipse Config:
  - http://stackoverflow.com/questions/1886185/eclipse-and-windows-newlines



TASK LIST - Version 20130106 beta
====================================================
//DONE
- inverted default VolumeRocker scroll direction.
- Removed annoying sound when using VolumeRocker.
- Database backup/restore implemented
- Downloads intent+++ Make it cooler
- anti sleep fuinction 
- Fix volume buttons when not in scroll mode
- Unit steps for scrolling Volume Rocker 1 = 100 normalized
- Verify clicking mascot in chapter and going to jump to screen.
- make bookmarks accessible in every part of the app
- Download all info make a background task
- Reformatted last update/check text
- Jump to inside content layout fixed

//TODO
- Bookmarks screen fix layout TODO - Doing it now
- fix novel content headers/titles TODO
- Manual add series tutorial for new users TODO lowPriority
- When loading main page for first time, add message "found x novel series" TODO lowPriority
- dialog inside a chapter/content reading "Downloading, please wait" TODO
- Make lightNovel list download a download list task TODO

//TODO NEXT VERSION ONLY
- let finished downloads stay unless deleted* TODO

//NEEDS VERIFICATION
- remove stars from novel detail name TODO??

//FUTURE THINKING
-Advanced Customization options
-Integration with external cloud apps for image/database saving:
	DropBox
	Box
	Skydrive
	Google Drive

UNHANDLED EXCEPTION:
====================================================
* Performing pause of activity that is not resumed: {com.erakk.lnreader/com.erakk.lnreader.activity.DisplayLightNovelListActivity}
  This can happen in any activity...
* Unhandled exception when creating menus...

ANDROID PERMISSION LIST
====================================================
* android.permission.ACCESS_NETWORK_STATE
  - For checking internet state.
* android.permission.INTERNET
  - For downloading data from Baka Tsuki.
* android.permission.WRITE_EXTERNAL_STORAGE
  - For saving image cache.
* android.permission.READ_EXTERNAL_STORAGE
  - For reading image cache.
* android.permission.VIBRATE
  - For notification.