//REQUIRED:
Android SDK
eclipse + ADT
JSoup (Nandaka)

//LINE-ENDINGS & SOURCE ENCODING CONFIG
Git Config: https://help.github.com/articles/dealing-with-line-endings
Eclipse Config: http://stackoverflow.com/questions/1886185/eclipse-and-windows-newlines

//WARNING - updated
Sometimes loading a novel gives CalledFromWrongThreadException (this may be my fault but i couldnt detectd)
- It because we try to update the data not from the ui thread, this should be fixed already.
I can't register the ExpandList for contextMenu. it allways crashes... do you have any idea?
- done.


//TODO:
get LN chapters and update info - Working
Download and save - DONE
Remodel UI - Working
Create icons,change colors. - Colors working/Icons later
MAYBE Rename work. - Baka-Tsuki Reader / TsukiReader or another

//CHECK
File format problem - I just redid the changes to UTF-8 and UNIX line breaks. lets hope it wont bother again