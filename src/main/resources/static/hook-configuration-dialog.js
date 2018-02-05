var externalHooksConfiguration = [];
define('trimble/teamcity/test', [
    'jquery',
    'aui',
    'bitbucket/internal/util/ajax',
    'bitbucket/util/navbuilder',
    'bitbucket/internal/model/page-state',
    'bitbucket/internal/util/error',
    'exports'
], function ($, AJS, ajax, navBuilder, pageState, errorUtil, exports) {

    function resourceUrl(resourceName) {
        return AJS.contextPath() + '/rest/teamcity/latest/projects/' + pageState.getProject().getKey() +
            '/repos/' + pageState.getRepository().getSlug() + '/' + resourceName;
    }
      
    function ClickOnRowEvent() {
      $('.rowselected').removeClass('rowselected');

      var id = $(this).attr('id');
      if(id === "headerTableId") {
        return;
      }

      $(this).addClass('rowselected');        
      var type = $('.rowselected').children()[0].innerHTML;
      var source = $('.rowselected').children()[1].innerHTML;
      var desc = $('.rowselected').children()[2].innerHTML;
      var dependencies = $('.rowselected').children()[3].innerHTML;
      var url = $('.rowselected').children()[4].innerHTML;

      $("#DescriptionOfExtHook").val(desc);
      $('#externalTriggerType').val(type).change();
      $("#sourceTriggerType").val(source);
      $("#ExternalBuildsTwoDepId").val(dependencies);
      $("#HookUrl").val(url);
    }

    function InitTableRows() {
      if (externalHooksConfiguration === null) {
        externalHooksConfiguration = [];
      }

      var table = document.getElementById('hookTableId');        
      externalHooksConfiguration.forEach(function(elem) {
        var row = table.insertRow(1);
        row.addEventListener('click', ClickOnRowEvent);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(1);
        var cell3 = row.insertCell(2);
        var cell4 = row.insertCell(3);
        var cell5 = row.insertCell(4);
        cell1.innerHTML = elem.type;
        cell2.innerHTML = elem.source;
        cell3.innerHTML = elem.desc;                     
        cell4.innerHTML = elem.dependencies;
        cell5.innerHTML = elem.url;
      });        
    }

    function SyncBitbucketUrl() {
        // sync data from config
        if (document.getElementById("bitbuckerUrlToData").value !== document.getElementById("bitbuckerUrl").value) {
          document.getElementById("bitbuckerUrl").value = document.getElementById("bitbuckerUrlToData").value;        
        }

        // enable events for sync
        $("#bitbuckerUrl").change(function() {
          if (document.getElementById("bitbuckerUrlToData").value !== document.getElementById("bitbuckerUrl").value) {
            document.getElementById("bitbuckerUrlToData").value = document.getElementById("bitbuckerUrl").value;        
          }
        });

        $("#bitbuckerUrlToData").change(function() {
          if (document.getElementById("bitbuckerUrlToData").value !== document.getElementById("bitbuckerUrl").value) {
            document.getElementById("bitbuckerUrl").value = document.getElementById("bitbuckerUrlToData").value;        
          }
        });
    }

    function SyncTeamCityUrl() {
        // sync data from config
        if (document.getElementById("TeamCityUrlToData").value !== document.getElementById("TeamCityUrl").value) {
          document.getElementById("TeamCityUrl").value = document.getElementById("TeamCityUrlToData").value;        
        }

        // enable events for sync
        $("#TeamCityUrl").change(function() {
          if (document.getElementById("TeamCityUrlToData").value !== document.getElementById("TeamCityUrl").value) {
            document.getElementById("TeamCityUrlToData").value = document.getElementById("TeamCityUrl").value;        
          }
        });

        $("#TeamCityUrlToData").change(function() {
          if (document.getElementById("TeamCityUrlToData").value !== document.getElementById("TeamCityUrl").value) {
            document.getElementById("TeamCityUrl").value = document.getElementById("TeamCityUrlToData").value;        
          }
        });
    }

    function SyncTriggerType() {

        if ($("#triggerType").val() === "" || $("#triggerType").val() === null) {
            var asdas = document.getElementById("triggerTypeToData").value;
            if (document.getElementById("triggerTypeToData").value === null) {
              $("#triggerType").val("buildConfig");        
            } else {
              $("#triggerType").val(document.getElementById("triggerTypeToData").value);                  
            }
        }

        // sync data from config
        if (document.getElementById("triggerTypeToData").value !== $("#triggerType").val()) {
          var dataInTriggetype = document.getElementById("triggerTypeToData").value;
          if( typeof dataInTriggetype === 'undefined' || dataInTriggetype === null || dataInTriggetype == "") {
            document.getElementById("triggerTypeToData").value = $("#triggerType").val();
          } else {
            $("#triggerType").val(document.getElementById("triggerTypeToData").value);
          }
                 
        }

        // enable events for sync
        $("#triggerType").change(function() {
          if (document.getElementById("triggerTypeToData").value !== $("#triggerType").val()) {
            document.getElementById("triggerTypeToData").value = $("#triggerType").val();        
          }
        });

        $("#triggerTypeToData").change(function() {
            if (document.getElementById("triggerTypeToData").value !== $("#triggerType").val()) {
              $("#triggerType").val(document.getElementById("triggerTypeToData").value);        
            }
        });
    }

    function SyncUserName() {
        // sync data from config
        if (document.getElementById("TeamCityUserNameToData").value !== document.getElementById("TeamCityUserName").value) {
          document.getElementById("TeamCityUserName").value = document.getElementById("TeamCityUserNameToData").value;        
        }

        // enable events for sync
        $("#TeamCityUserName").change(function() {
          if (document.getElementById("TeamCityUserNameToData").value !== document.getElementById("TeamCityUserName").value) {
            document.getElementById("TeamCityUserNameToData").value = document.getElementById("TeamCityUserName").value;        
          }
        });

        $("#TeamCityUserNameToData").change(function() {
          if (document.getElementById("TeamCityUserNameToData").value !== document.getElementById("TeamCityUserName").value) {
            document.getElementById("TeamCityUserName").value = document.getElementById("TeamCityUserNameToData").value;        
          }
        });
    }

    function SyncDebugEnabled() {
        // sync data from config
        if (document.getElementById("isDebugEnabledToData").value !== $('#isDebugEnabled').attr('checked')) {
          $('#isDebugEnabled').attr('checked', document.getElementById("isDebugEnabledToData").value);         
        }

        // enable events for sync
        $("#isDebugEnabled").change(function() {
          if (document.getElementById("isDebugEnabledToData").value !== $('#isDebugEnabled').attr('checked')) {
            document.getElementById("isDebugEnabledToData").value = $('#isDebugEnabled').attr('checked');        
            }
        });

        $("#isDebugEnabledToData").change(function() {
          if (document.getElementById("isDebugEnabledToData").value !== $('#isDebugEnabled').attr('checked')) {
            $('#isDebugEnabled').attr('checked', document.getElementById("isDebugEnabledToData").value);        
          }
        });
    }

    function SyncmasterRuleToData() {
        // sync data from config
        if (document.getElementById("masterRuleToData").value !== document.getElementById("masterRule").value) {
          document.getElementById("masterRule").value = document.getElementById("masterRuleToData").value;        
        }

        // enable events for sync
        $("#masterRule").change(function() {
          if (document.getElementById("masterRuleToData").value !== document.getElementById("masterRule").value) {
            document.getElementById("masterRuleToData").value = document.getElementById("masterRule").value;        
          }
        });

        $("#masterRuleToData").change(function() {
          if (document.getElementById("masterRuleToData").value !== document.getElementById("masterRule").value) {
            document.getElementById("masterRule").value = document.getElementById("masterRuleToData").value;        
          }
        });
    }
    function SyncbugFixRuleToData() {
        // sync data from config
        if (document.getElementById("bugFixRuleToData").value !== document.getElementById("bugFixRule").value) {
          document.getElementById("bugFixRule").value = document.getElementById("bugFixRuleToData").value;        
        }

        // enable events for sync
        $("#bugFixRule").change(function() {
          if (document.getElementById("bugFixRuleToData").value !== document.getElementById("bugFixRule").value) {
            document.getElementById("bugFixRuleToData").value = document.getElementById("bugFixRule").value;        
          }
        });

        $("#bugFixRuleToData").change(function() {
          if (document.getElementById("bugFixRuleToData").value !== document.getElementById("bugFixRule").value) {
            document.getElementById("bugFixRule").value = document.getElementById("bugFixRuleToData").value;        
          }
        });
    }
    function SyncfeatureRuleToData() {
        // sync data from config
        if (document.getElementById("featureRuleToData").value !== document.getElementById("featureRule").value) {
          document.getElementById("featureRule").value = document.getElementById("featureRuleToData").value;        
        }

        // enable events for sync
        $("#featureRule").change(function() {
          if (document.getElementById("featureRuleToData").value !== document.getElementById("featureRule").value) {
            document.getElementById("featureRuleToData").value = document.getElementById("featureRule").value;        
          }
        });

        $("#featureRuleToData").change(function() {
          if (document.getElementById("featureRuleToData").value !== document.getElementById("featureRule").value) {
            document.getElementById("featureRule").value = document.getElementById("featureRuleToData").value;        
          }
        });
    }
    function SynchotfixRuleToData() {
        // sync data from config
        if (document.getElementById("hotfixRuleToData").value !== document.getElementById("hotfixRule").value) {
          document.getElementById("hotfixRule").value = document.getElementById("hotfixRuleToData").value;        
        }

        // enable events for sync
        $("#hotfixRule").change(function() {
          if (document.getElementById("hotfixRuleToData").value !== document.getElementById("hotfixRule").value) {
            document.getElementById("hotfixRuleToData").value = document.getElementById("hotfixRule").value;        
          }
        });

        $("#hotfixRuleToData").change(function() {
          if (document.getElementById("hotfixRuleToData").value !== document.getElementById("hotfixRule").value) {
            document.getElementById("hotfixRule").value = document.getElementById("hotfixRuleToData").value;        
          }
        });
    }
    function SyncreleaseRuleToData() {
        // sync data from config
        if (document.getElementById("releaseRuleToData").value !== document.getElementById("releaseRule").value) {
          document.getElementById("releaseRule").value = document.getElementById("releaseRuleToData").value;        
        }

        // enable events for sync
        $("#releaseRule").change(function() {
          if (document.getElementById("releaseRuleToData").value !== document.getElementById("releaseRule").value) {
            document.getElementById("releaseRuleToData").value = document.getElementById("releaseRule").value;        
          }
        });

        $("#releaseRuleToData").change(function() {
          if (document.getElementById("releaseRuleToData").value !== document.getElementById("releaseRule").value) {
            document.getElementById("releaseRule").value = document.getElementById("releaseRuleToData").value;        
          }
        });
    }
    function SyncBranchDefinitionToData() {
        // sync data from config
        if (document.getElementById("BranchDefinitionToData").value !== document.getElementById("BranchDefinition").value) {
          document.getElementById("BranchDefinition").value = document.getElementById("BranchDefinitionToData").value;        
        }

        // enable events for sync
        $("#BranchDefinition").change(function() {
          if (document.getElementById("BranchDefinitionToData").value !== document.getElementById("BranchDefinition").value) {
            document.getElementById("BranchDefinitionToData").value = document.getElementById("BranchDefinition").value;        
          }
        });

        $("#BranchDefinitionToData").change(function() {
          if (document.getElementById("BranchDefinitionToData").value !== document.getElementById("BranchDefinition").value) {
            document.getElementById("BranchDefinition").value = document.getElementById("BranchDefinitionToData").value;        
          }
        });
    }
    function SyncBranchCustomTypesToData() {
        // sync data from config
        if (document.getElementById("BranchCustomTypesToData").value !== document.getElementById("BranchCustomTypes").value) {
          document.getElementById("BranchCustomTypes").value = document.getElementById("BranchCustomTypesToData").value;        
        }

        // enable events for sync
        $("#BranchCustomTypes").change(function() {
          if (document.getElementById("BranchCustomTypesToData").value !== document.getElementById("BranchCustomTypes").value) {
            document.getElementById("BranchCustomTypesToData").value = document.getElementById("BranchCustomTypes").value;        
          }
        });

        $("#BranchCustomTypesToData").change(function() {
          if (document.getElementById("BranchCustomTypesToData").value !== document.getElementById("BranchCustomTypes").value) {
            document.getElementById("BranchCustomTypes").value = document.getElementById("BranchCustomTypesToData").value;        
          }
        });
    }


    function SyncExternalBuildsOneNameIdFeatureToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneNameIdFeatureToData").value !== document.getElementById("ExternalBuildsOneNameIdFeature").value) {
          document.getElementById("ExternalBuildsOneNameIdFeature").value = document.getElementById("ExternalBuildsOneNameIdFeatureToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneNameIdFeature").change(function() {
          if (document.getElementById("ExternalBuildsOneNameIdFeatureToData").value !== document.getElementById("ExternalBuildsOneNameIdFeature").value) {
            document.getElementById("ExternalBuildsOneNameIdFeatureToData").value = document.getElementById("ExternalBuildsOneNameIdFeature").value;        
          }
        });

        $("#ExternalBuildsOneNameIdFeatureToData").change(function() {
          if (document.getElementById("ExternalBuildsOneNameIdFeatureToData").value !== document.getElementById("ExternalBuildsOneNameIdFeature").value) {
            document.getElementById("ExternalBuildsOneNameIdFeature").value = document.getElementById("ExternalBuildsOneNameIdFeatureToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneConfigurationsIdFeatureToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneConfigurationsIdFeatureToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdFeature").value) {
          document.getElementById("ExternalBuildsOneConfigurationsIdFeature").value = document.getElementById("ExternalBuildsOneConfigurationsIdFeatureToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneConfigurationsIdFeature").change(function() {
          if (document.getElementById("ExternalBuildsOneConfigurationsIdFeatureToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdFeature").value) {
            document.getElementById("ExternalBuildsOneConfigurationsIdFeatureToData").value = document.getElementById("ExternalBuildsOneConfigurationsIdFeature").value;        
          }
        });

        $("#ExternalBuildsOneConfigurationsIdFeatureToData").change(function() {
          if (document.getElementById("ExternalBuildsOneConfigurationsIdFeatureToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdFeature").value) {
            document.getElementById("ExternalBuildsOneConfigurationsIdFeature").value = document.getElementById("ExternalBuildsOneConfigurationsIdFeatureToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneDepIdFeatureToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneDepIdFeatureToData").value !== document.getElementById("ExternalBuildsOneDepIdFeature").value) {
          document.getElementById("ExternalBuildsOneDepIdFeature").value = document.getElementById("ExternalBuildsOneDepIdFeatureToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneDepIdFeature").change(function() {
          if (document.getElementById("ExternalBuildsOneDepIdFeatureToData").value !== document.getElementById("ExternalBuildsOneDepIdFeature").value) {
            document.getElementById("ExternalBuildsOneDepIdFeatureToData").value = document.getElementById("ExternalBuildsOneDepIdFeature").value;        
          }
        });

        $("#ExternalBuildsOneDepIdFeatureToData").change(function() {
          if (document.getElementById("ExternalBuildsOneDepIdFeatureToData").value !== document.getElementById("ExternalBuildsOneDepIdFeature").value) {
            document.getElementById("ExternalBuildsOneDepIdFeature").value = document.getElementById("ExternalBuildsOneDepIdFeatureToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneNameIdBugFixToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneNameIdBugFixToData").value !== document.getElementById("ExternalBuildsOneNameIdBugFix").value) {
          document.getElementById("ExternalBuildsOneNameIdBugFix").value = document.getElementById("ExternalBuildsOneNameIdBugFixToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneNameIdBugFix").change(function() {
          if (document.getElementById("ExternalBuildsOneNameIdBugFixToData").value !== document.getElementById("ExternalBuildsOneNameIdBugFix").value) {
            document.getElementById("ExternalBuildsOneNameIdBugFixToData").value = document.getElementById("ExternalBuildsOneNameIdBugFix").value;        
          }
        });

        $("#ExternalBuildsOneNameIdBugFixToData").change(function() {
          if (document.getElementById("ExternalBuildsOneNameIdBugFixToData").value !== document.getElementById("ExternalBuildsOneNameIdBugFix").value) {
            document.getElementById("ExternalBuildsOneNameIdBugFix").value = document.getElementById("ExternalBuildsOneNameIdBugFixToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneDepIdBugFixToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneDepIdBugFixToData").value !== document.getElementById("ExternalBuildsOneDepIdBugFix").value) {
          document.getElementById("ExternalBuildsOneDepIdBugFix").value = document.getElementById("ExternalBuildsOneDepIdBugFixToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneDepIdBugFix").change(function() {
          if (document.getElementById("ExternalBuildsOneDepIdBugFixToData").value !== document.getElementById("ExternalBuildsOneDepIdBugFix").value) {
            document.getElementById("ExternalBuildsOneDepIdBugFixToData").value = document.getElementById("ExternalBuildsOneDepIdBugFix").value;        
          }
        });

        $("#ExternalBuildsOneDepIdBugFixToData").change(function() {
          if (document.getElementById("ExternalBuildsOneDepIdBugFixToData").value !== document.getElementById("ExternalBuildsOneDepIdBugFix").value) {
            document.getElementById("ExternalBuildsOneDepIdBugFix").value = document.getElementById("ExternalBuildsOneDepIdBugFixToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneConfigurationsIdBugFixToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneConfigurationsIdBugFixToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdBugFix").value) {
          document.getElementById("ExternalBuildsOneConfigurationsIdBugFix").value = document.getElementById("ExternalBuildsOneConfigurationsIdBugFixToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneConfigurationsIdBugFix").change(function() {
          if (document.getElementById("ExternalBuildsOneConfigurationsIdBugFixToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdBugFix").value) {
            document.getElementById("ExternalBuildsOneConfigurationsIdBugFixToData").value = document.getElementById("ExternalBuildsOneConfigurationsIdBugFix").value;        
          }
        });

        $("#ExternalBuildsOneConfigurationsIdBugFixToData").change(function() {
          if (document.getElementById("ExternalBuildsOneConfigurationsIdBugFixToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdBugFix").value) {
            document.getElementById("ExternalBuildsOneConfigurationsIdBugFix").value = document.getElementById("ExternalBuildsOneConfigurationsIdBugFixToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneNameIdHotFixToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneNameIdHotFixToData").value !== document.getElementById("ExternalBuildsOneNameIdHotFix").value) {
          document.getElementById("ExternalBuildsOneNameIdHotFix").value = document.getElementById("ExternalBuildsOneNameIdHotFixToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneNameIdHotFix").change(function() {
          if (document.getElementById("ExternalBuildsOneNameIdHotFixToData").value !== document.getElementById("ExternalBuildsOneNameIdHotFix").value) {
            document.getElementById("ExternalBuildsOneNameIdHotFixToData").value = document.getElementById("ExternalBuildsOneNameIdHotFix").value;        
          }
        });

        $("#ExternalBuildsOneNameIdHotFixToData").change(function() {
          if (document.getElementById("ExternalBuildsOneNameIdHotFixToData").value !== document.getElementById("ExternalBuildsOneNameIdHotFix").value) {
            document.getElementById("ExternalBuildsOneNameIdHotFix").value = document.getElementById("ExternalBuildsOneNameIdHotFixToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneDepIdHotFixToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneDepIdHotFixToData").value !== document.getElementById("ExternalBuildsOneDepIdHotFix").value) {
          document.getElementById("ExternalBuildsOneDepIdHotFix").value = document.getElementById("ExternalBuildsOneDepIdHotFixToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneDepIdHotFix").change(function() {
          if (document.getElementById("ExternalBuildsOneDepIdHotFixToData").value !== document.getElementById("ExternalBuildsOneDepIdHotFix").value) {
            document.getElementById("ExternalBuildsOneDepIdHotFixToData").value = document.getElementById("ExternalBuildsOneDepIdHotFix").value;        
          }
        });

        $("#ExternalBuildsOneDepIdHotFixToData").change(function() {
          if (document.getElementById("ExternalBuildsOneDepIdHotFixToData").value !== document.getElementById("ExternalBuildsOneDepIdHotFix").value) {
            document.getElementById("ExternalBuildsOneDepIdHotFix").value = document.getElementById("ExternalBuildsOneDepIdHotFixToData").value;        
          }
        });
    }
    function SyncExternalBuildsOneConfigurationsIdHotFixToData() {
        // sync data from config
        if (document.getElementById("ExternalBuildsOneConfigurationsIdHotFixToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdHotFix").value) {
          document.getElementById("ExternalBuildsOneConfigurationsIdHotFix").value = document.getElementById("ExternalBuildsOneConfigurationsIdHotFixToData").value;        
        }

        // enable events for sync
        $("#ExternalBuildsOneConfigurationsIdHotFix").change(function() {
          if (document.getElementById("ExternalBuildsOneConfigurationsIdHotFixToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdHotFix").value) {
            document.getElementById("ExternalBuildsOneConfigurationsIdHotFixToData").value = document.getElementById("ExternalBuildsOneConfigurationsIdHotFix").value;        
          }
        });

        $("#ExternalBuildsOneConfigurationsIdHotFixToData").change(function() {
          if (document.getElementById("ExternalBuildsOneConfigurationsIdHotFixToData").value !== document.getElementById("ExternalBuildsOneConfigurationsIdHotFix").value) {
            document.getElementById("ExternalBuildsOneConfigurationsIdHotFix").value = document.getElementById("ExternalBuildsOneConfigurationsIdHotFixToData").value;        
          }
        });
    }

    function SyncDataBetweenUiAndConfiguration() {
        SyncBitbucketUrl();
        SyncTeamCityUrl();
        SyncTriggerType();
        SyncUserName();
        SyncDebugEnabled();
        SyncBranchCustomTypesToData();
        SyncBranchDefinitionToData();
        SyncreleaseRuleToData();
        SynchotfixRuleToData();
        SyncfeatureRuleToData();
        SyncbugFixRuleToData();
        SyncmasterRuleToData();

        SyncExternalBuildsOneNameIdHotFixToData();
        SyncExternalBuildsOneDepIdHotFixToData();
        SyncExternalBuildsOneConfigurationsIdHotFixToData();
        SyncExternalBuildsOneNameIdBugFixToData();
        SyncExternalBuildsOneDepIdBugFixToData();
        SyncExternalBuildsOneConfigurationsIdBugFixToData();
        SyncExternalBuildsOneNameIdFeatureToData();
        SyncExternalBuildsOneConfigurationsIdFeatureToData();
        SyncExternalBuildsOneDepIdFeatureToData();
    }
    
    function init() {
      console.log("init started");
      enabled = true;
      var $button = $("#testTeamcityConnectionButtonId");        
      var $buttonAddHook = $("#AddExternalHookButtonId");
      var $buttonRemoveHook = $("#RemoveExternalHookButtonId");
      
      // parse json string
      var data = $("#ExternalHooksConfigurationV2").val();
      if (data !== "{}") {        
        externalHooksConfiguration = $.parseJSON(data);        
      } else {
        externalHooksConfiguration = [];
      }
      
      function setStatus(status, color) {
          $("#connectionStatusTeamcityId").text(status);
          $("#connectionStatusTeamcityId").css('color', color);
      }
            
      $("#hookTableId tr").click(ClickOnRowEvent);
      
      $buttonAddHook.click(function() {
        var desc = $("#DescriptionOfExtHook").val();       
        var url = $("#HookUrl").val();
        
        if (desc === "" || url === "") {
          $("#AddRemoveExternalHookButtonStatusId").text("url and description should be defined.");
          $("#AddRemoveExternalHookButtonStatusId").css('color', "red");
          return;
        }
                
        var sourceSel = document.getElementById('sourceTriggerType');
        var source = sourceSel.value;

        var sel = document.getElementById('externalTriggerType');
        var type = sel.value;

        var dependenciesSel = document.getElementById('ExternalBuildsTwoDepId');
        var dependencies = dependenciesSel.value;

        var found = false;
        try
        {
            var found = externalHooksConfiguration.find(function(elem) {
               if (elem.type === type && desc === elem.desc && elem.url === url && elem.source === source && elem.dependencies === dependencies) {
                $("#AddRemoveExternalHookButtonStatusId").text("hook already defined.");
                $("#AddRemoveExternalHookButtonStatusId").css('color', "red");
                return true;
               }          
            });
        } catch(err) {

        }

        if (found) {
          return;
        }
               
        var table = document.getElementById('hookTableId');
        var row = table.insertRow(1);
        row.addEventListener('click', ClickOnRowEvent);
        var cell1 = row.insertCell(0);
        var cell2 = row.insertCell(1);
        var cell3 = row.insertCell(2);
        var cell4 = row.insertCell(3);
        var cell5 = row.insertCell(4);

        cell1.innerHTML = type;
        cell2.innerHTML = source;        
        cell3.innerHTML = desc;    
        cell4.innerHTML = dependencies;
        cell5.innerHTML = url;
        
        // append element to table and to configuration
        externalHooksConfiguration.push({type:type, source:source, desc:desc, url:url, dependencies:dependencies});
        
        var current = document.getElementById("ExternalHooksConfigurationV2");
        current.value = JSON.stringify(externalHooksConfiguration);                  
        
        if (externalHooksConfiguration.length === 0) {
          current.value = "";
        }
      });
            
      $buttonRemoveHook.click(function() {
        var selectedRow = $('.rowselected');
        var type = selectedRow.children()[0].innerHTML;
        var source = selectedRow.children()[1].innerHTML;
        var desc = selectedRow.children()[2].innerHTML;
        var dependencies = selectedRow.children()[3].innerHTML;
        var url = selectedRow.children()[4].innerHTML;
        
        if (selectedRow) {
          document.getElementById("hookTableId").deleteRow(selectedRow.index());
        } else {
            $("#AddRemoveExternalHookButtonStatusId").text("select row from table to remove.");
            $("#AddRemoveExternalHookButtonStatusId").css('color', "red");          
        }
                
        for(var i = externalHooksConfiguration.length - 1; i >= 0; i--) {
          
          if(externalHooksConfiguration[i].type === type && 
              externalHooksConfiguration[i].desc === desc && 
              externalHooksConfiguration[i].url === url && 
              externalHooksConfiguration[i].dependencies === dependencies && 
              externalHooksConfiguration[i].source === source) {
            externalHooksConfiguration.splice(i, 1);
            
            var current = document.getElementById("ExternalHooksConfigurationV2");
            current.value = JSON.stringify(ExternalHooksConfigurationV2);
            
            return;
          }
        }      
      });
      
      $button.click(function() {
        console.log("username: " + username);
        //alert( "Handler for .click() called." );
        
        var url = $( "#TeamCityUrl" ).val();
        var username = $( "#TeamCityUserName" ).val();
        var password = $( "#FormIdForPassword" ).val();

        var restpoint = resourceUrl('testconnection') + "?url=" + url + "&username=" + username + "&password=" + encodeURIComponent(password);          
        
        setStatus("Trying...", "green");
        $.get(restpoint)
        .success(function(data) {
          if (data.status == "ok") {
            setStatus("Ok...", "green");  
            var current = document.getElementById("TeamCityPasswordOk");
            current.value = "OkPassword";
          } else {
            setStatus("Fail... " + data.message, "red");  
          }                   
        })
        .error(function(data) {
          var data = JSON.parse(data.responseText);
          setStatus("Fail... " + data.message, "red");  
        })        
        .done(function( data ) {
          console.log("rest: " + data)
        });
      });      
    }
    
    exports.onReady = function () {      
        var fileref=document.createElement("link");
        fileref.setAttribute("rel", "stylesheet");
        fileref.setAttribute("type", "text/css");
        fileref.setAttribute("href", resourceUrl("loadcss") + "?page=config.css"); 
        document.getElementsByTagName("head")[0].appendChild(fileref)
        
        // Get the element with id="defaultOpen" and click on it
        document.getElementById("defaultOpen").click();
        
        // init values for check boxes
        var isDefaultBranchMaster = document.getElementById("isDefaultBranch");
        
        if (isDefaultBranchMaster !== null && isDefaultBranchMaster.value === "true") {
          document.getElementById("isDefaultBranchBox").checked = true;          
        } else {
          document.getElementById("isDefaultBranchBox").checked = false;
        }
        
        // register jquery on change event
        $('#isDefaultBranchBox').change(function() {
            if($(this).is(":checked")) {
              document.getElementById("isDefaultBranch").value = "true";
            } else {
              document.getElementById("isDefaultBranch").value = "false";
            }            
        });

        SyncDataBetweenUiAndConfiguration();



        var usePrForFeature = document.getElementById("usePrFromFeature");
      
        if (usePrForFeature !== null && usePrForFeature.value === "true") {
          document.getElementById("prInsteadFeature").checked = true;          
        } else {
          document.getElementById("prInsteadFeature").checked = false;
        }
        
        // register jquery on change event
        $('#prInsteadFeature').change(function() {
            if($(this).is(":checked")) {
              document.getElementById("usePrFromFeature").value = "true";
            } else {
              document.getElementById("usePrFromFeature").value = "false";
            }            
        });
        

        var usePrForHotFix = document.getElementById("usePrFromhotFix");
        
        if (usePrForHotFix !== null && usePrForHotFix.value === "true") {
          document.getElementById("prInsteadHotFix").checked = true;          
        } else {
          document.getElementById("prInsteadHotFix").checked = false;
        }
        
        // register jquery on change event
        $('#prInsteadHotFix').change(function() {
            if($(this).is(":checked")) {
              document.getElementById("usePrFromhotFix").value = "true";
            } else {
              document.getElementById("usePrFromhotFix").value = "false";
            }            
        });
                
        
        var usePrForBugFix = document.getElementById("usePrFrombugFix");
        if (usePrForBugFix !== null && usePrForBugFix.value === "true") {
          document.getElementById("prInsteadBugFix").checked = true;          
        } else {
          document.getElementById("prInsteadBugFix").checked = false;
        }
        
        // register jquery on change event
        $('#prInsteadBugFix').change(function() {
            if($(this).is(":checked")) {
              document.getElementById("usePrFrombugFix").value = "true";
            } else {
              document.getElementById("usePrFrombugFix").value = "false";
            }            
        });
                
        var useDebugEnabled = document.getElementById("useDebugEnabled");
        if (useDebugEnabled !== null && useDebugEnabled.value === "true") {
          document.getElementById("isDebugEnabled").checked = true;          
        } else {
          document.getElementById("isDebugEnabled").checked = false;
        }
        
        // register jquery on change event
        $('#isDebugEnabled').change(function() {
            if($(this).is(":checked")) {
              document.getElementById("useDebugEnabled").value = "true";
            } else {
              document.getElementById("useDebugEnabled").value = "false";
            }            
        });
      
      
        init();
        InitTableRows();
    }
});

function openCity(evt, cityName) {
    // Declare all variables
    var i, tabcontent, tablinks;

    // Get all elements with class="tabcontent" and hide them
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    // Get all elements with class="tablinks" and remove the class "active"
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }

    // Show the current tab, and add an "active" class to the link that opened the tab
    var d = document.getElementById(cityName);
    d.style.display = "block";
    evt.currentTarget.className += " active";
}

console.log("Load teamcity.js");
console.log("Exports Ready");
