var AJSLOCAL = {}
var pageStateLocal = {}
var BuildWindowLaunched = false;

function resourceUrl(resourceName) {
    return AJSLOCAL.contextPath() + '/rest/teamcity/latest/projects/' + pageStateLocal.getProject().getKey() +
        '/repos/' + pageStateLocal.getRepository().getSlug() + '/' + resourceName;
}

function translateResource(id) {
  return $i18n.getText(id);
}

define('plugin/teamcity/pr-triggerbutton', [
  'jquery',
  'aui',
  'bitbucket/internal/model/page-state'
  
], function($, AJS, pageState) {

  pageStateLocal = pageState;
  AJSLOCAL = AJS;
  
  // start frame
  var modalDialogParentDiv = document.createElement("div");
  modalDialogParentDiv.setAttribute("class", "modal");
  modalDialogParentDiv.setAttribute("id", "myModal");
     
  var iframeElement = document.createElement("iframe");
  iframeElement.setAttribute("id", "mainFrameId");
  iframeElement.setAttribute("style", "width: 100%; height: 100%;");
  iframeElement.setAttribute("frameborder", "0");
  iframeElement.setAttribute("src", resourceUrl('loadhtml') + "?page=index.html");
  modalDialogParentDiv.appendChild(iframeElement);
  document.body.appendChild(modalDialogParentDiv);
           
  window.onclick = function(event) {
    var modal = document.getElementById('myModal');
    var triggerButton = $(".triggerTeamcityBuild");
    
    if (event.target !== modal && event.target !== triggerButton[0]) {
        modal.style.display = "none";
        BuildWindowLaunched = false;        
    }
  }    

  $(".triggerTeamcityBuild").click(function() {
    //wrapperDiv = document.createElement("div");
    //wrapperDiv.setAttribute("style", "position: absolute; left: 0px; top: 0px; background-color: rgb(255, 255, 255); opacity: 0.5; z-index: 2000; height: 1083px; width: 100%;");
    //wrapperDiv.setAttribute("id", "modalWindow");

    //var iframeElementDisableBackground = document.createElement("iframe"); 
    //iframeElementDisableBackground.setAttribute("id", "iframeElementId");
    //iframeElementDisableBackground.setAttribute("style", "width: 100%; height: 100%;");
    //wrapperDiv.appendChild(iframeElementDisableBackground);  

    //document.body.appendChild(wrapperDiv);         

    modalDialogParentDiv.setAttribute("style", "border-radius: 2px; position: absolute; width: 800px; height: 500px; border: 1px solid #f5f5f5; padding: 3px; background-color: rgb(255, 255, 255); z-index: 2001; top: 20%; left: 25%; margin: -1px 0 0 -1px;");
    BuildWindowLaunched = true;
  });  
});

AJS.$(document).ready(function() {
    require('plugin/teamcity/pr-triggerbutton');
});

function CloseModal() {
  var modal = document.getElementById('myModal');
  modal.style.display = "none";
  BuildWindowLaunched = false;
}

function GetBuildInformation(buildid, htmlid, callback) {
  require(['jquery'], function($) {
    
    var restpoint = parent.resourceUrl('build') +
            "?id=" + buildid;
    $.get(restpoint)
    .success(function(data) {        
      callback(data, htmlid);
    })
    .error(function(data) {
      var data = JSON.parse(data.responseText);
      callback(data, id);
    })        
    .done(function( data ) {
    });      
  });    
}


function TriggerExternalUrl(url, id, callback) {  
  require(['jquery'], function($) {
    
    var restpoint = parent.resourceUrl('triggerexternalurl') +
            "?url=" + encodeURIComponent(url) + "&method=GET";
    $.get(restpoint)
    .success(function(data) {        
      callback(data, id);
    })
    .error(function(data) {
      var data = JSON.parse(data.responseText);
      callback(data, id);
    })        
    .done(function( data ) {
    });      
  });  
}
function TriggerBuildConfiguration(buildid, branch, callback) {
  require(['jquery'], function($) {
    var pr = pageStateLocal.getPullRequest();
    var restpoint = parent.resourceUrl('triggerbuild') +
            "?buildconfig=" + buildid +
            "&branch=" + branch;
    
      $.get(restpoint)
      .success(function(data) {        
        callback(data);
      })
      .error(function(data) {
        var data = JSON.parse(data.responseText);
        callback(data);
      })        
      .done(function( data ) {
      });  
      // do things with the file-handlers module and some other module.
  });  
}

function GetBuildsConfigurations(callback) {
  require(['jquery'], function($) {
    var pr = pageStateLocal.getPullRequest();
    var fromRef = pr.getFromRef();
    var restpoint = parent.resourceUrl('builds') +
            "?prid=" + pr.id +
            "&branch=" + fromRef.getDisplayId() +
            "&hash=" + fromRef.getLatestCommit();
    
      $.get(restpoint)
      .success(function(data) {        
        callback(data, fromRef.getDisplayId());
      })
      .error(function(data) {
        var data = JSON.parse(data.responseText);
        callback(data);
      })        
      .done(function( data ) {
      });  
      // do things with the file-handlers module and some other module.
  });
}

function GetExternalBuildsConfigurations(id, callback) {
  require(['jquery'], function($) {
    var pr = pageStateLocal.getPullRequest();
    var fromRef = pr.getFromRef();
    var restpoint = parent.resourceUrl('externalbuilds') +
            "?id=" + id +
            "&prid=" + pr.id +
            "&branch=" + fromRef.getDisplayId() +
            "&hash=" + fromRef.getLatestCommit();
    
      $.get(restpoint)
      .success(function(data) {
        callback(data, fromRef.getDisplayId(), pr.id);
      })
      .error(function(data) {
        var data = JSON.parse(data.responseText);
        callback(data, fromRef.getDisplayId());
      })        
      .done(function( data ) {
      });  
      // do things with the file-handlers module and some other module.
  });
}

function CallTrigger() {
  require(['jquery'], function($) {
    var restpoint = parent.resourceUrl('testconnection') + "?url=http://teamcity&username=buildmaster&password=sads";          
      $.get(restpoint)
      .success(function(data) {
        if (data.status == "ok") {
          console.log(data.status);  
        } else {
            console.log(data.message);  
        }                   
      })
      .error(function(data) {
        var data = JSON.parse(data.responseText);
          console.log(data.message);  
      })        
      .done(function( data ) {
      });  
      // do things with the file-handlers module and some other module.
  });
}