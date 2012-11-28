var pCollections = document.getElementsByTagName("p");

window.onscroll = function scroll () {
	var element = document.elementFromPoint(window.pageXOffset, window.pageYOffset);
	var i = 0;
    for (i = 0; i < pCollections.length; ++i) {
		if(findPos(pCollections[i]) >= window.pageYOffset) {
			console.log("SCROLL_EVENT:" + pCollections[i].id);
			break;
		}
	}	
};

function setup() {
	/* Assign id to p tag */
	var i = 0;
    for (i = 0; i < pCollections.length; ++i) {
		pCollections[i].id = "" + i;
	}
    highlightBookmark();
}

/* Handle touch event for bookmark highlighting */
function toogleHighlight(element, ev) {
    var mode = "";
    var target = event.srcElement || event.target;

    if ("p" === target.nodeName.toLowerCase()) {
        if (target.className.indexOf("highlighted") === -1) {
            target.className = target.className + " highlighted";
			mode = "highlighted";
        }
        else {
			target.className = target.className.replace(" highlighted", "");
			mode = "clear";
		}
    }
    
    if(target.id != undefined && target.id != "") {
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
	if(index != undefined && index > -1) {
		window.scroll(0, findPos(pCollections[index]));
	}
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