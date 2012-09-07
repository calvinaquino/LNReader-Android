//DEVELOPMENT REQUIREMENT:
  Android SDK
  eclipse Juno + ADT
  JSoup
  Android 3+ (Honeycomb)

//APPLICATION REQUIREMENT:
  Recommended Android 3.0+ (API Level 11)
  Minimum     Android 2.2  (API Level 8)
  Some features might not working if running below the recommended version.

//LINE-ENDINGS & SOURCE ENCODING CONFIG
  Git Config: https://help.github.com/articles/dealing-with-line-endings
              https://stackoverflow.com/questions/1889559/git-diff-to-ignore-m
  Eclipse Config: http://stackoverflow.com/questions/1886185/eclipse-and-windows-newlines

//TODO:
  Remodel UI - Working
  Create icons,change colors. - Colors working/Icons later
  MAYBE Rename work. - Baka-Tsuki Reader / TsukiReader or another
  No Auto checking for novel translation updates
  Clear volume/chapter cache is not yet implemented

//UNHANDLED EXCEPTION
  - Performing pause of activity that is not resumed: {com.erakk.lnreader/com.erakk.lnreader.activity.DisplayLightNovelListActivity}
    This can happen in any activity...


//CHECK
  Unhandled exception when creating menus...

//ANDROID PERMISSION LIST
  android.permission.ACCESS_NETWORK_STATE
  - For checking internet state.
  android.permission.INTERNET
  - For downloading data from Baka Tsuki.
  android.permission.WRITE_EXTERNAL_STORAGE
  - For saving image cache.
  android.permission.READ_EXTERNAL_STORAGE
  - For reading image cache.
