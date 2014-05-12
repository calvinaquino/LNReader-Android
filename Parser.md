# Alternate Language Parser #

[Reference XML](https://github.com/calvinaquino/LNReader-Android/blob/master/LNReader/res/xml/parse_lang_info.xml)

## Alternate Language XML Structure ##

- `<LanguageInfos />`
	- `<LanguageInfo />`
		- `<Language>Bahasa Indonesia</Language>` = Title to be used for Alt. Lang Tab Header.  
		- `<Category>Indonesian</Category>` = Baka-Tsuki wiki category link to be used. Will be prefixed with `Category:`
		- `<MarkerSynopsis>#Sinopsis_Cerita</MarkerSynopsis>` = id to be used for synopsis.
		- `<ParserRules>` = list of ids to be used for detecting start of book/chapter listing.
			- `<Rule>_oleh</Rule>` = the actual id

## How to add new Alternate Language ##
1. Determine the title to be shown in the tab, e.g.: for German language = `<Language>Deutsch</Language>`.
2. Determine the novel listing page, e.g.: for German translated light novels in http://www.baka-tsuki.org/project/index.php?title=Category:Light_novel_(German) = `<Category>Light_novel_(German)</Category>`.
3. Determine the synopsis marker, e.g. `<MarkerSynopsis>#Inhalt</MarkerSynopsis>`.
4. Determine the id used for beginning of the book/volume listing, e.g. `<Rule>_von</Rule>`
5. You need to submit the information required for the xml info to [the forum thread](http://www.baka-tsuki.org/forums/viewtopic.php?f=16&t=5389&p=234941).

## How the parser works ##
### Novel Lists ###
1. The novel list is parsed from the category page, for example:  `<Category>Indonesian</Category>` will open this page (http://baka-tsuki.org/project/index.php?title=Category:**Indonesian**).
2. The application then will parse each sub-categories.
3. From each sub-categories, the application then parse the novel name based on each bullet item `<li>` element.

### Novel Details ###
1. When the user open one of the novel in the alternate language list, the application will parse the page in Baka-Tsuki page, e.g. for Baka_to_Test_to_Shoukanjuu_(Indonesia) it will open http://baka-tsuki.org/project/index.php?title=Baka_to_Test_to_Shoukanjuu_(Indonesia).
2. It will parse the synopsis based on the `<MarkerSynopsis>#Sinopsis_Cerita</MarkerSynopsis>` id. If it is empty, then it will start parse from the beginning of the page. From there, it will find the nearest `<p>` element and save it as the synopsis up to 10 `<p>` element.
3. After that, it will try to get the novel cover based on the first image with `id=thumbimage`.
4. From there, it will start to parse the book/volume list. First, it will get all the `<h1>` and `<h2>` elements.
5. For each element, it will check if the descendant element's id contains one of the `<ParserRules>` rule. If yes, it will be used for the marks of beginning of book/volume listing.
6. From here, the application will detect the book/chapter pairing based on the `<h3>` and `<li>` elements. This depend on the convention from Baka-Tsuki standard.

### Novel Contents ###
1. From the given chapter, the application will parse the page in Baka-Tsuki, for example: http://baka-tsuki.org/project/index.php?title=Baka_to_Tesuto_to_Syokanju:Volume1_Soal_Pertama
2. From here, the application save the `id=mw-content-text` portion to to the DB.
