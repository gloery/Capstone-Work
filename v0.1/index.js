
//TODO: this only checks for in-line insertions, not block insertions
//possible solution to BIG problem of determining when a string is affected:
//if a block insertion/deletion affects the string, and any PART (define later) of the string
//is affected by an inline insertion/deletion, then that string is affected by that revision
/*
 * Uses regular expressions to check a given input string for inline insertions (prefaced by "diffchange-inline")
 */
function parseHTMLForInsertion(pageToParse){
	return "in December";
}


/*
 * Uses regular expressions to check a given input string for inline deletion
 */
function parseHTMLForDeletion(pageToParse){
	return "deleted text";
}


function affectsString(insertionBlob, deletionBlob, affectedString, midpointId){
	return "YES";
}


function checkShortList(listToCheck, stringToCheck,
			url, mostCurrentId){
	return listToCheck[0];
}


/*
 * Takes a given mediaWiki URL, returns the JSON gotten as a result of that
 * call
 */
function getHTML(passedUrl){
	//TODO: implement this asynchrously
//	$.ajax({
//		  type:     "GET",
//		  url:      passedUrl,
//		  dataType: "jsonp",
//		  success: function(data){
//		    console.log(data);
//		    //TODO: the fact that one of these is a fairly random number makes me nervous...
//		    var content = data.query.pages["57572"].revisions[0].diff["*"];
////		    console.log("CONTENT: "+content);
//		    return content
//		  }
//		});
	
	return "Hello world! January Februrary March December.";
}


/*
 * Get the 500 most recent revisions for a given Wikipedia API URL
 */
function getAllRevIds(theUrl, startId)
{
	var revisionIds = [];
	
	//if we're getting the first 500 revisions:
	if(startId === ""){
		theUrl += "&prop=revisions&rvprop=ids&rvlimit=500";
	}
	//if we're getting revisions after the first 500, we need to manually set the start position:
	else{
		theUrl+= "&prop=revisions&rvstartid="+ startId +"&rvprop=ids&rvlimit=500";
	}
//	console.log("URL: "+theUrl);
	
	
	//TODO: restructure this for asynchrous calls
//	$.ajax({
//		  type:     "GET",
//		  url:      theUrl,
//		  dataType: "jsonp",
//		  success: function(data){
//		    console.log(data);
//		    //TODO: the fact that one of these is a fairly random number makes me nervous...
//		    var revisions = data.query.pages["57572"].revisions;
//		    for(var i = 0; i < revisions.length; i++){
//		    	revisionIds.push(revisions[i].revid);
//		    }
////		    console.log("REV IDS: "+revisionIds);
//		    return revisionIds; 
//		  }
//		});
	
	
	return ["123456", "54321"];
}
/*
 * Use the binary search method to find the most recent revision that affects a given string.
 * Returns "None" if no revision is found that affects that string
 */
function iterativeBinarySearch(url, midpointId, stringToCheck, mostCurrentId, curRevs){
	var resultPage = "";
	var insertionBlob = "";
	var deletionBlob = "";
	while(true){
		//get the diff of the current revision (midpoint) to the most recent revision
		resultPage = getHTML(url+"&prop=revisions&rvstartid=" + midpointId + "&rvendid=" + midpointId + "&rvdiffto="+mostCurrentId);
//		console.log("RESULT PAGE: "+resultPage);
		//put all inline insertion statements together in one blob
		insertionBlob = parseHTMLForInsertion(resultPage);
		
		//put all inline deletion statements together
		deletionBlob = parseHTMLForDeletion(resultPage);
		
		if(affectsString(insertionBlob, deletionBlob, stringToCheck, midpointId) != null){
			//this ID is the first one that affects that string ONLY if it is the only item in the list
			//of remaining revisions
			if(curRevs.length == 1){
//				console.log("RETURNING ID: "+midpointId);
				return midpointId;
			}
			
			//if the size of the list is less than 3, it makes more sense to use a linear search on the remaining
			//very short list
			else{
//				console.log("CHECKING NEWER LIST: "+midpointId);
				console.log("OLD LIST SIZE: "+ curRevs.length);
				if(curRevs.length > 3){
					curRevs = curRevs.slice(0, curRevs.indexOf(midpointId) + 1);
				}
				else{
					//check list of size 3
					return checkShortList(curRevs, stringToCheck, url, mostCurrentId);
				}
			}
				
			console.log("NEW LIST SIZE: "+curRevs.length);
			midpointId = curRevs[((curRevs.length)/2)];
		}
			//otherwise, if the current revision does NOT affect the string, search the upper (older) half of the
			//current list to find a revision that does affect the given string
			else{
				//if this is the only item left in the list and it doesn't affect the string,
				//then that means that none exist
				if(curRevs.length == 1){
					console.log("WE'RE IN");
					console.log("MIDPOINT NO: "+midpointId);
					return "NONE FOUND";
				}
				
				console.log("CHECKING OLDER LIST: "+midpointId);
				if(curRevs.length > 3){
					//change this to size - 1?
					curRevs = curRevs.slice(curRevs.indexOf(midpointId), curRevs.length);
				}
				
				else{
					console.log("LIST OF SIZE <=3: "+ curRevs);
					//check list of size 3
					return checkShortList(curRevs, stringToCheck, url, mostCurrentId);
				}
				
				midpointId = curRevs[((curRevs.length)/2)];
				
		}
		
	}
}



/*
 * function to find the first revision that affects a given string in comparison
 * to the most recent revision (affects = insertion or deletion)
 */
function getFirstChange(url, stringToTest){
	var firstChangeId = "";
	//get the first 500 revisions of this article
	var curRevs = getAllRevIds(url, "");
	
	//the revision we will be diffing all others against
	var mostCurrentId = curRevs[0];
	
	//first search the revision halfway between first and last
	var midpoint = curRevs[(curRevs.length / 2)];
	
	firstChangeId = iterativeBinarySearch(url, midpoint, stringToTest, mostCurrentId, curRevs);
	
	//keep getting batches of 500 revisions until all are checked
	while(firstChangeId === "NONE"){
		var lastId = curRevs[((curRevs.length)-1)];
		curRevs = getAllRevIds(url, lastId);
		
		//first search the revision halfway between first and last
		midpoint = curRevs[((curRevs.length)/2)];
		firstChangeId = iterativeBinarySearch(url, midpoint, stringToCheck, mostCurrentId, curRevs);
	}
	return ["2014-09-08T20:41:34Z","CorinneSD","/* The advent of 'cake in a box' */ Edited for clarity.","CorinneSD","2014-09-08T20:41:34Z","624717746","624717323","Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu."];
}


//TODO: return author as well as id of first revision
function main(text){
	var stringToTest = "December";
	var url = "http://en.wikipedia.org/w/api.php?format=json&action=query&titles=Cake";
	var result = getFirstChange(url, stringToTest);
	//alert(result);
	return result;
}