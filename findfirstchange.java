import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;

/*
 TODO: 

 This solution is based on Wikipedia's diff algorithm, so it would say a string is affected by a diff even if text was just moved, not modified.

 UNRESOLVED: this only takes care of inline insertions, can have another pattern for block insertions/deletions

 FIXED: diffchange tag also includes COMMENTS as well, we don't want that to be the case, RESOLVED using different regexes


 RESOLVED: Specifically, it's not getting diffs further back in the page's history that's taking a long time, it's using regexes to search for a really long term that takes
 a long time. RESOLVED by using binary search method instead of linear search. Can now search entire history much faster.

 */

public class findfirstchange {

	/*
	 * CURRENTLY THIS BREAKS IF we do a phrase like: "photographs in" because
	 * the diff is broken up into "photographs" and "in magazine" CRUDE FIX:
	 * first get all insertions together into one big blob-string, then search
	 * the blob string for that phrase.
	 */
	
	//TODO: this only checks for in-line insertions, not block insertions
	//possible solution to BIG problem of determining when a string is affected:
	//if a block insertion/deletion affects the string, and any PART (define later) of the string
	//is affected by an inline insertion/deletion, then that string is affected by that revision
	/*
	 * Uses regular expressions to check a given input string for inline insertions (prefaced by "diffchange-inline")
	 */
	public String parseHTMLForInsertion(String input) {
		Pattern p = Pattern.compile("(diffchange-inline.\\\">)(?!\\{\\{)([^<]*?)(</ins)");
		Matcher m = p.matcher(input);
		String blob = "";
		
		while(m.find()){
			blob +=m.group(2) + " ";
		}
		
		// System.out.println("BLOB: "+blob);
		return blob;

	}

	/*
	 * Uses regular expressions to check a given input string for inline deletion
	 */
	public String parseHTMLForDeletion(String input) {
		Pattern p = Pattern.compile("(diffchange-inline.\\\">)(?!\\{\\{)([^<]*?)(</del)");
		Matcher m = p.matcher(input);
		String blob = "";
		
		while(m.find()){
			blob +=m.group(2) + " ";
		}

		return blob;

	}

	/*
	 * utility function to search for a term in a given blob composed of all insertions or deletions made in a revision
	 */
	public boolean searchForTerm(String blob, String searchTerm) {
		if (blob.contains(searchTerm)) {
			return true;
		}
		return false;
	}
	
	/*
	 * utility function to see if a term is moved: both inserted and deleted in a particular revision
	 */
	public boolean checkForRearrangement(String insertionBlob, String deletionBlob, String searchTerm){
		if(searchForTerm(insertionBlob, searchTerm) && searchForTerm(deletionBlob, searchTerm)){
			return true;
		}
		return false;
	}

	/*
	 * Takes a given mediaWiki URL, returns the JSON gotten as a result of that
	 * call
	 */
	public String getHTML(String passedUrl) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";

		try {
			url = new URL(passedUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			//read lines of input from the Stream Reader
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/*
	 * Given a Wikipedia API URL address, gets the most recent revision ids in a list for
	 * an article title specified in the url (number of ids is specified by API URL address)
	 */
	public List<String> getRevId(String url) {
		String result = getHTML(url);
		Pattern p = Pattern.compile("(.*?revid.*?)([0-9]+)(.*?)");
		Matcher m = p.matcher(result);

		List<String> revIds = new ArrayList<String>();
		while (m.find()) {
//			System.out.println("FOUND AN ID: " + m.group(2));
			revIds.add(m.group(2));
		}
		return revIds;
	}
	
	/*
	 * Get the 500 most recent revisions for a given Wikipedia API URL
	 */
	public List<String> getAllRevIds(String url, String startId){
		//set number of revisions to the maximum allowed by the api
		//what about articles with more than 500 revisions?
		
		//SOLVED, just get the next 500 and keep going, same process
		
		//if we're getting the first 500 revisions:
		if(startId.equals("")){
			url += "&prop=revisions&rvprop=ids&rvlimit=500";
		}
		//if we're getting revisions after the first 500, we need to manually set the start position:
		else{
			url+= "&prop=revisions&rvstartid="+ startId +"rvprop=ids&rvlimit=500";
		}
		List<String> listToReturn = getRevId(url);
		return listToReturn;
	}
	
	/*
	 * Check to see whether a given string is affected by a given revision (whether the string is inserted, deleted,
	 * or moved)
	 */
	public String affectsString(String insertionBlob, String deletionBlob, String stringToCheck, 
			String revisionId){
			String returnThis = null;
		//first check to see whether string was moved
		if (checkForRearrangement(insertionBlob, deletionBlob, stringToCheck)){
			System.out.println("FOUND REARRANGEMENT");
			return null;
		}
		//if not, check to see whether it was inserted
		else if (searchForTerm(insertionBlob, stringToCheck)){
			System.out.println("DETECTED INSERTION: " + revisionId);
			returnThis = revisionId;
		}
		//otherwise, check to see if it was deleted
		else if (searchForTerm(deletionBlob, stringToCheck)){
			System.out.println("DETECTED DELETION");
			returnThis = revisionId;
		}
		return returnThis;
	}
	
	/*
	 * If a list contains 3 or fewer items, it makes sense to use a linear search for the remaining
	 * items in the list; binary search is no longer necessary
	 */
	public String checkShortList(List<String> listToCheck, String stringToCheck,
			String url, String mostCurrentId){
		System.out.println("CHECKING SHORT LIST");
		String validId = "NONE";
		
		for (String id : listToCheck){
			System.out.println("ID TO CHECk: "+id);
			String resultPage = getHTML(url+"&prop=revisions&rvstartid=" + id + "&rvendid=" + id + "&rvdiffto="+mostCurrentId);
			String insertionBlob = parseHTMLForInsertion(resultPage);
			String deletionBlob = parseHTMLForDeletion(resultPage);
			//if an ID is found that affects the string, break and immediately return that ID
			if(affectsString(insertionBlob, deletionBlob, stringToCheck, id) != null){
				validId = id;
				break;
			}
		}
		return validId;
	}
	
	/*
	 * Use the binary search method to find the most recent revision that affects a given string.
	 * Returns "None" if no revision is found that affects that string
	 */
	public String iterativeBinarySearch(String url, String midpointId, String stringToCheck,
			String mostCurrentId, List<String> curRevs){
		String resultPage = "";
		String insertionBlob = "";
		String deletionBlob = "";
		
		//base case: midpoint affects the string, midpoint - 1 doesn't
		System.out.println("ID NOW: "+ midpointId);
		while(true){
			//get the diff of the current revision (midpoint) to the most recent revision
			resultPage = getHTML(url+"&prop=revisions&rvstartid=" + midpointId + "&rvendid=" + midpointId + "&rvdiffto="+mostCurrentId);
			
			//put all inline insertion statements together in one blob
			insertionBlob = parseHTMLForInsertion(resultPage);
			
			//put all inline deletion statements together
			deletionBlob = parseHTMLForDeletion(resultPage);
			
			if(affectsString(insertionBlob, deletionBlob, stringToCheck, midpointId) != null){
				//this ID is the first one that affects that string ONLY if it is the only item in the list
				//of remaining revisions
				if(curRevs.size() == 1){
					System.out.println("MIDPOINT GOOD: "+midpointId);
					return midpointId;
				}
					//if the current revision (midpoint) affects the string but is NOT the only item in the list:
					//iteratively search the midpoint of a new list of half the size of the original:
					//LOWER (more recent) half of list, in order to find the one that is most recent
				else{
					System.out.println("CHECKING NEWER LIST: "+midpointId);
					System.out.println("OLD LIST SIZE: "+ curRevs.size());
					
					//if the size of the list is less than 3, it makes more sense to use a linear search on the remaining
					//very short list
					if(curRevs.size() > 3){
						curRevs = curRevs.subList(0, curRevs.indexOf(midpointId));
					}
					else{
						System.out.println("LIST OF SIZE <=3: "+ curRevs);
						//check list of size 3
						return checkShortList(curRevs, stringToCheck, url, mostCurrentId);
					}
					System.out.println("NEW LIST SIZE: "+curRevs.size());
					midpointId = curRevs.get(curRevs.size()/2);
				}
			}
			//otherwise, if the current revision does NOT affect the string, search the upper (older) half of the
			//current list to find a revision that does affect the given string
			else{
				//if this is the only item left in the list and it doesn't affect the string,
				//then that means that none exist
				if(curRevs.size() == 1){
					System.out.println("WE'RE IN");
					System.out.println("MIDPOINT NO: "+midpointId);
					return "NONE FOUND";
				}
				System.out.println("CHECKING OLDER LIST: "+midpointId);
				if(curRevs.size() > 3){
					curRevs = curRevs.subList(curRevs.indexOf(midpointId), curRevs.size() - 1);
				}
				
				else{
					System.out.println("LIST OF SIZE <=3: "+ curRevs);
					//check list of size 3
					return checkShortList(curRevs, stringToCheck, url, mostCurrentId);
				}
				
				midpointId = curRevs.get(curRevs.size()/2);
			}
		}
		//recursive version of the code below. Ignore for now.
		
//		if(affectsString(insertionBlob, deletionBlob, stringToCheck, midpointId) != null){
//			if(curRevs.size() == 1){
//				System.out.println("MIDPOINT GOOD: "+midpointId);
//				return midpointId;
//			}
//				//recursively search the midpoint of a new list of half the size of the original:
//				//LOWER half of list
//			else{
//				System.out.println("CHECKING NEWER LIST: "+midpointId);
//				List<String> newList = null;
//				if(curRevs.size() > 3){
//					newList = curRevs.subList(0, curRevs.indexOf(midpointId));
//				}
//				else{
//					System.out.println("LIST OF SIZE 3: "+ curRevs);
//					//check list of size 3
//					return checkShortList(curRevs, stringToCheck, url, mostCurrentId);
//				}
//				System.out.println("OLD LIST SIZE: "+ curRevs.size());
//				System.out.println("NEW LIST SIZE: "+newList.size());
//				String newMidpoint = newList.get(newList.size()/2);
//				return iterativeBinarySearch(url, newMidpoint, stringToCheck, mostCurrentId, newList);
//			}
//		}
//		
//		
//		
//		
//		
//		else{
//			if(curRevs.size() == 1){
//				System.out.println("WE'RE IN");
//				System.out.println("MIDPOINT NO: "+midpointId);
//				return "NONE FOUND";
//			}
////			System.out.println("STILL WORK?? "+ affectsString(insertionBlob, deletionBlob, stringToCheck, midpointId) != null);
//			System.out.println("CHECKING OLDER LIST: "+midpointId);
//			List<String> newList = null;
//			if(curRevs.size() > 3){
//				newList = curRevs.subList(curRevs.indexOf(midpointId), curRevs.size());
//			}
//			
//			else{
//				System.out.println("LIST OF SIZE 3: "+ curRevs);
//				//check list of size 3
//				return checkShortList(curRevs, stringToCheck, url, mostCurrentId);
//			}
//			
//			String newMidpoint = newList.get(newList.size()/2);
//			return iterativeBinarySearch(url, newMidpoint, stringToCheck, mostCurrentId, newList);
//		}
//		
		//TODO: maybe put in a check to ensure the string is in the article?
		//TODO: eventually do something with the output of affectsString

		
	}
	
	/*
	 * function to find the first revision that affects a given string in comparison
	 * to the most recent revision (affects = insertion or deletion)
	 */
	public String getFirstChange(String url, String stringToCheck){
		String firstChangeId = "";
		//get the first 500 revisions of this article
		List<String> curRevs = getAllRevIds(url, "");
		
		//the revision we will be diffing all others against
		String mostCurrentId = curRevs.get(0);
			
		//first search the revision halfway between first and last
		String midpoint = curRevs.get(curRevs.size()/2);
		System.out.println("MIDPOINT: "+midpoint);
		
		firstChangeId = iterativeBinarySearch(url, midpoint, stringToCheck, mostCurrentId, curRevs);
		//keep getting batches of 500 revisions until all are checked
		while(firstChangeId.equals("NONE")){
			String lastId = curRevs.get(curRevs.size()-1);
			System.out.println("LAST ID: "+ lastId);
			curRevs = getAllRevIds(url, lastId);
			
			//first search the revision halfway between first and last
			midpoint = curRevs.get(curRevs.size()/2);
			System.out.println("MIDPOINT: "+midpoint);
			firstChangeId = iterativeBinarySearch(url, midpoint, stringToCheck, mostCurrentId, curRevs);
		}
		return firstChangeId;
	}

	@SuppressWarnings("resource")
	public static void main(String args[]) {
		//in this version, you want to search for the string "December" for a true positive
		//type a random string of gibberish for a true negative.

		//TO RUN (after compiling): type "java findfirstchange [search term]"
		//for example: java findfirstchange december
		String stringToCheck = args[0];
		
		findfirstchange c = new findfirstchange();
		System.out.println("\n\nID OF FIRST CHANGE: "+c.getFirstChange("http://en.wikipedia.org/w/api.php?format=json&action=query&titles=Cake", stringToCheck));
		

	}
}