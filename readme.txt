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



TASK LIST - Version 1.1.0 beta build 25 (Old Version 20130106)
====================================================
//DONE
- inverted default VolumeRocker scroll direction.
- Removed annoying sound when using VolumeRocker.
- Database backup/restore implemented
- Downloads intent+++ Make it cooler
- anti sleep function 
- Fix volume buttons when not in scroll mode
- Unit steps for scrolling Volume Rocker 1 = 100 normalized
- make bookmarks accessible in every part of the app
- Download all info make a background task
- Reformatted last update/check text
- Jump to inside content layout fixed
- Bookmarks screen fix layout
- fix novel content headers/titles
//CRASH FIXES
- Fixed a crash that would occur if trying to unbind an unregistered service on low memory warning.
- Fixed a crash that related to scrolling while reading a novel.

//TODO
- Add download List too all intents
- Add Orignal Light Novels List
- Remove Downloads ans Settings buttons from main menu, add them as popup menu.
- Teaser list to use Download List Model
- Rename ImageActivity to something better
- Remove asyncTaskCount from settings
- Fix manual add crash
- make external chapters open in an internal customized webview
- Light Novel List Layout <DOING
- Empty WatchList hangup bug 
- dialog inside a chapter/content reading "Downloading, please wait"
- Manual add series tutorial for new users lowPriority
- Add first time Tutorial for new users to show off functionalities, skipable. // May go to next version only, or will be added later.
- TOC/BT Copyrights on first time run // Need to prepare a TOC. or only show BT's copyrights.

//TODO NEXT VERSION ONLY
- let finished downloads stay unless deleted*

maybe ill add an option to "remove complete downloads automatically" for those who dont like removing manually...

//NEEDS RETHINKING/CHECKING
- Verify clicking mascot in chapter and going to jump to screen.
- Make lightNovel list download a download list task
- When loading main page for first time, add message "found x novel series" lowPriority

this mascot (is the icon on the upper left, only shows on newer android versions, like a back button) bug is not checked yet.

//FUTURE IDEAS
- Advanced Customization options
- Integration with external cloud apps for image/database saving:
> DropBox
> Box
> Skydrive
> Google Drive
> This is just an IDEA, and as such, not to worry for now.

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