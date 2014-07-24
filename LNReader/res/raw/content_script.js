var pCollections = document.getElementsByTagName("p");

/* Handle touch event for bookmark highlighting */
function toogleHighlight(element, ev) {
    if (!isBookmarkEnabled) {
        return;
    }
    var mode = "", target = event.srcElement || event.target;

    if ("p" === target.nodeName.toLowerCase()) {
        if (target.className.indexOf("highlighted") === -1) {
            target.className = target.className + " highlighted";
            mode = "highlighted";
        } else {
            target.className = target.className.replace(" highlighted", "");
            mode = "clear";
        }
    }

    if (target.id !== undefined && target.id !== "") {
        var excerpt = target.innerText || target.textContent || target.innerHTML;
        console.log("HIGHLIGHT_EVENT:" + target.id + ":" + mode + ":" + excerpt);
    }
}

/* Highlight given bookmarks */
function highlightBookmark() {
    for (var index = 0; index < bookmarkCol.length; ++index) {
        pCollections[bookmarkCol[index]].className += " highlighted";        
    }
}

/* Scroll to given paragraph index */
function goToParagraph(index) {
	goToParagraph(index, false);
}

function goToParagraph(index, useSmoothScroll) {
	var targetPost = findPos(pCollections[index]);
	var currPos = window.pageYOffset || document.documentElement.scrollTop;
	
	//if (currPos >= targetPost - 10 && ) {
	//	return;
	//}
    if (index != undefined && index > 0) {
        if (useSmoothScroll) {
            animate(document.body, "scrollTop", "", currPos, targetPost, 500, true);
        } else {
            window.scroll(currPos, targetPost);
        }
    }
    if(index == 0) {
    	targetPost = 0;
    }
    window.scrollTo(0, targetPost);
}

/* Helper method to get paragraph position */
function findPos(obj) {
    var curtop = 0;
    if (obj.offsetParent) {
        do {
            curtop += obj.offsetTop;
        } while (obj = obj.offsetParent);
        return [curtop];
    }
}

function toogleEnableBookmark(enable) {
    isBookmarkEnabled = enable;
}

function doSpeak() {
    var text = document.getElementsByTagName('body')[0].innerHTML;
    console.log("SPEAK_EVENT:" + text);
}

/* helper function for smooth scrolling
 * http://stackoverflow.com/a/17733311 */
function animate(elem, style, unit, from, to, time, prop) {
    if (!elem) return;
    var start = new Date().getTime();
    var timer = setInterval(function() {
            var step = Math.min(1, (new Date().getTime() - start) / time);
            if (prop) {
                elem[style] = (from + step * (to - from)) + unit;
            } else {
                elem.style[style] = (from + step * (to - from)) + unit;
            }
            if (step == 1) clearInterval(timer);
        }, 25);
    elem.style[style] = from + unit;
}

window.onscroll = function scroll() {
    var element = document.elementFromPoint(window.pageXOffset, window.pageYOffset);
    var i = 0;
    for (i = 0; i < pCollections.length; i++) {
        if (findPos(pCollections[i]) >= window.pageYOffset) {
            console.log("SCROLL_EVENT:" + pCollections[i].id);
            break;
        }
    }
};

function setup() {
    /* Assign id to p tag */
    var i = 0;
    for (i = 0; i < pCollections.length; i++) {
        pCollections[i].id = "" + i;
    }
    highlightBookmark();
    goToParagraph(lastPos);
    setTimeout(function() { goToParagraph(lastPos); }, 1000);
    console.log("LOAD_COMPLETE_EVENT:" + pCollections.length + ":" + lastPos);
}

function recalcWidth(){
	var style = document.body.currentStyle || window.getComputedStyle(document.body);
	var marginLeft = style.marginLeft.replace("px","") / screen.width;
	var marginRight = style.marginRight.replace("px","") / screen.width;
	document.body.style.width = ~~(window.innerWidth * (1 - (marginLeft + marginRight)));;
	window.scrollTo(0, window.pageYOffset || document.documentElement.scrollTop);
	console.log("RECALC_EVENT:" + document.body.style.width + ":" + marginLeft + ":" + marginRight + ":" + screen.width);
}