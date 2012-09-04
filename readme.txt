//REQUIRED:
  Android SDK
  eclipse Juno + ADT
  JSoup
  Android 3+ (Honeycomb)

//LINE-ENDINGS & SOURCE ENCODING CONFIG
  Git Config: https://help.github.com/articles/dealing-with-line-endings
              https://stackoverflow.com/questions/1889559/git-diff-to-ignore-m
  Eclipse Config: http://stackoverflow.com/questions/1886185/eclipse-and-windows-newlines

//LIMITATION
  Minimal OS: Honeycomb+ (Android 3++)

//TODO:
  Remodel UI - Working
  Create icons,change colors. - Colors working/Icons later
  MAYBE Rename work. - Baka-Tsuki Reader / TsukiReader or another
  No Auto checking for novel translation updates
  Clear volume/chapter cache is not yet implemented

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
