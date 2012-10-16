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

TODO:
====================================================
* Remodel UI - Working
* Create icons,change colors. - Colors working/Icons later
* MAYBE Rename work. - Baka-Tsuki Reader / TsukiReader or another
* Basic Auto checking for novel translation updates

TODO Parser:
- http://www.baka-tsuki.org/project/index.php?title=Tasogare-iro_no_Uta_Tsukai = no chapter because the list is inside a table. 
  => OK, find all h3 inside element if current element != h3
- http://www.baka-tsuki.org/project/index.php?title=White_Album_2_Omake = no structure..
- http://www.baka-tsuki.org/project/index.php?title=To_Aru_Majutsu_no_Index = New Testament Vol.4 , why table...
  => OK, removed dl/ul/div checking, just get all li elements.
- http://www.baka-tsuki.org/project/index.php?title=Sayonara_Piano_Sonata = External link? Somehow teaser not parsed.
  => OK, now parsed as side effect from Index parser update. No external content yet.
- http://www.baka-tsuki.org/project/index.php?title=Maru-MA = different identifier with the title. 
  => OK, add _Series/_series as keyword
- http://www.baka-tsuki.org/project/index.php?title=Ginban_Kaleidoscope = Using h1, instead of h2 
  => OK, add h1 in selector

- External link, e.g: Drrrr. I thinks there are more.
- Add extra info for Side story/spin off, e.g.: Index NT, Seitokai Mokushiroku?
- Template:Abandoned, e.g.: NHK e youkouzo.
  => Need db change to save the flag

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
