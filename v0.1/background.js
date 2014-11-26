var panelID = 0;
var data = "";
var heatMap = 0;

// Query chrome to get an array of all tabs in current window that are focused and active
function queryForData() {
  if (isOnWiki) {
    chrome.tabs.query({active: true, currentWindow: true}, getInfo);
  }
  //chrome.windows.create({url:'/panel.html', type: 'panel', width:500, height:500}, createPanel);	
}

// The first element of tabs will be the page the user is currently looking at. Execute code to get highlighted text.
function getInfo(tabs) {
  if (heatMap === 1) {
    chrome.tabs.executeScript(tabs[0].id, {file: '/simpleHeatMap.js'});
    heatMap = 0;
  } else {
    chrome.tabs.executeScript(tabs[0].id, {code: 'var text = window.getSelection().toString(); text'}, receivingResponse);
  }
}


// Response of our executed script will have the highlighted text. Set our text var to equal that string and then trigger the next event
function receivingResponse(response) {
  text = response[0];
  data = main(text);
  document.dispatchEvent(evt);
}

// Creates a new window each time the user triggers an event. 
// TODO: Check to see if a window has been created, focus that window if one has been created instead of creating a new window.
function checkPanel(response) {
  chrome.windows.create({url:'/panel.html', type: 'panel', width:500, height:500}, queryUI);
}

// Gets all active tabs of our UI window, since no tabs are allowed the only tab in the array will be our UI
function queryUI(window) {
  panelID = window.id;
  chrome.tabs.query({windowId: panelID}, addInfo);
}

// Add info to our UI by sending it the text to append to the body node
function addInfo(tabs) {
  var htmlToAdd = buildHTMLToAdd(data);
  chrome.tabs.sendMessage(tabs[0].id, {'html':htmlToAdd}, respond);
}

// Log the response we get returned from the message we sent to the UI. For debugging purposes.
function respond(response) {
  console.log(response);
}

// When the UI has been closed, set the panelID to a null value so that we know it has been closed.
// TODO: Get this working with checkPanel.
function handleClose(windowID) {
  if (panelID === windowID) {
  	panelID = 0;
  }
}

function handleCommand(command) {
  if(command === 'toggle-feature-foo') {
    heatMap = 1;
    queryForData();
  }
}

var evt = new CustomEvent("getInformation");

document.addEventListener("getInformation", checkPanel);
chrome.browserAction.onClicked.addListener(queryForData);
chrome.commands.onCommand.addListener(handleCommand);
//chrome.windows.onRemoved.addListener(handleClose)