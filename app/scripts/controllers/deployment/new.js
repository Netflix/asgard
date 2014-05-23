'use strict';

angular.module("asgardApp")
  .controller("DeploymentNewCtrl", function ($scope, $routeParams, $http, $location) {
    $scope.clusterName = $routeParams.clusterName;
    $scope.hideAdvancedItems = true;
    $scope.hideJsonSteps = true;
    $scope.hideHtmlSteps = false;
    $scope.hideShowMoreAmisLink = false;
    $scope.targetAsgTypes = ["Previous", "Next"];
    $scope.count= 0;
    $scope.selectionsForSubnet = {}

    var isSameStepBeforeOrAfter = function(stepTypeName, index) {
      if (index > 0 && index < $scope.generated.stepsDisplay.length - 1) {
        return $scope.generated.stepsDisplay[index - 1].type === stepTypeName || $scope.generated.stepsDisplay[index + 1].type === stepTypeName;
      }
      if (index > 1) {
        return $scope.generated.stepsDisplay[index - 1].type === stepTypeName;
      }
      if (index < $scope.generated.stepsDisplay.length) {
        return $scope.generated.stepsDisplay[index + 1].type === stepTypeName;
      }
      return true;
    };

    var isLastStep = function(index) {
      return index === $scope.generated.stepsDisplay.length - 1;
    };

    var isFirstStep = function(index) {
      return index === 0;
    };

    var firstIndexOfStepType = function(stepTypeName) {
      var i, n = $scope.generated.stepsDisplay.length;
      for (i = 0; i < n; ++i) {
        var nextStep = $scope.generated.stepsDisplay[i];
        if ('type' in nextStep && nextStep.type === stepTypeName) {
          return i;
        }
      }
      return undefined;
    };

    var isBeforeCreateStep = function(index) {
        return index < firstIndexOfStepType("CreateAsg");
    };

    var isAfterDeleteStep = function(index) {
      return index > firstIndexOfStepType("DeleteAsg");
    };

    var stepTypes = {
      "Wait": {
        display: "Wait",
        isAllowed: function(index) {
          return !isSameStepBeforeOrAfter("Wait", index) && !isLastStep(index);
        },
        add: function(index) {
          $scope.generated.stepsDisplay.splice(index, 0, {"type":"Wait", "durationMinutes":60});
        }
      },
      "Judgment": {
        display: "Judgment",
        isAllowed: function(index) {
          return !isSameStepBeforeOrAfter("Judgment", index) && !isLastStep(index);
        },
        add: function(index) {
          $scope.generated.stepsDisplay.splice(index, 0, {"type":"Judgment", "durationMinutes":120});
        }
      },
      "ResizeAsg": {
        display: "Resize",
        isAllowed: function(index) {
          return !isBeforeCreateStep(index) && !isAfterDeleteStep(index);
        },
        add: function(index) {
          $scope.generated.stepsDisplay.splice(index, 0,
            {"type":"Resize", "targetAsg":"Next", "capacity":0, "startUpTimeoutMinutes":40});
        }
      },
      "DisableAsg": {
        display: "Disable",
        isAllowed: function(index) {
          return !isBeforeCreateStep(index) && !isAfterDeleteStep(index);
        },
        add: function(index) {
          $scope.generated.stepsDisplay.splice(index, 0, {"type":"DisableAsg", "targetAsg":"Previous"});
        }
      },
      "EnableAsg": {
        display: "Enable",
        isAllowed: function(index) {
          return !isBeforeCreateStep(index) && !isAfterDeleteStep(index);
        },
        add: function(index) {
          $scope.generated.stepsDisplay.splice(index, 0, {"type":"EnableAsg", "targetAsg":"Next"});
        }
      },
      "DeleteAsg": {
        display: "Delete",
        isAllowed: function(index) {
          return !isBeforeCreateStep(index) && !isAfterDeleteStep(index) && !firstIndexOfStepType("DeleteAsg")
            && isLastStep(index);
        },
        add: function(index) {
          $scope.generated.stepsDisplay.splice(index, 0, {"type":"DeleteAsg", "targetAsg":"Previous"});
        }
      }
    };

    $scope.stepTypeNames = Object.keys(stepTypes);

    $scope.isStepAllowed = function(stepTypeName, index) {
      return stepTypes[stepTypeName].isAllowed(index);
    };

    $scope.stepTypeDisplay = function(stepTypeName) {
      return stepTypes[stepTypeName].display;
    };

    $scope.addStep = function(stepTypeName, index) {
      resetStepsDisplay();
      stepTypes[stepTypeName].add(index);
      $scope.generated.stepsDisplay.splice(index, 0, {showSteps: false});
    };

    $scope.removeStep = function(index) {
      resetStepsDisplay();
      $scope.generated.stepsDisplay.splice(index, 2);
    };

    var resetStepsDisplay = function() {
      var i, n = $scope.generated.stepsDisplay.length;
      for (i = 0; i < n; ++i) {
        var nextStep = $scope.generated.stepsDisplay[i];
        if ('showSteps' in nextStep) {
          nextStep.showSteps = false
        }
      }
    };

    var initStepsDisplay = function() {
      $scope.generated = {};
      $scope.generated.stepsDisplay = [{showSteps: false}];
      var i, n = $scope.deploymentOptions.steps.length;
      for (i = 0; i < n; ++i) {
        var nextStep = $scope.deploymentOptions.steps[i];
        $scope.generated.stepsDisplay.push(nextStep);
        $scope.generated.stepsDisplay.push({showSteps: false});
      }
    };

    $scope.editJsonSteps = function() {
      $scope.hideHtmlSteps = true;
    };

    $scope.saveJsonSteps = function() {
      $scope.jsonStepsParseError = null;
      var jsonSteps;
      try {
        jsonSteps = angular.fromJson($scope.generated.jsonSteps);
      } catch(e) {
        $scope.jsonStepsParseError = e.stack;
        return;
      }
      var steps = [];
      var i, n = jsonSteps.length;
      for (i = 0; i < n; ++i) {
        var nextStep = jsonSteps[i];
        steps.push(nextStep);
      }
      $scope.deploymentOptions.steps = steps;
      initStepsDisplay();
      $scope.hideHtmlSteps = false;
    };

    var constructStepsFromDisplay = function() {
      var steps = [];
      var i, n = $scope.generated.stepsDisplay.length;
      for (i = 0; i < n; ++i) {
        var nextStep = $scope.generated.stepsDisplay[i];
        if ('type' in nextStep) {
          steps.push(nextStep);
        }
      }
      $scope.deploymentOptions.steps = angular.fromJson(angular.toJson(steps));
    };

    $scope.$watch("generated.stepsDisplay", function() {
      if ($scope.deploymentOptions) {
        constructStepsFromDisplay();
      }
    }, true);

    $scope.$watch("deploymentOptions.steps", function() {
      if ($scope.deploymentOptions) {
        var text ='[\n';
        var i, n = $scope.deploymentOptions.steps.length;
        for (i = 0; i < n; ++i) {
          var nextStep = $scope.deploymentOptions.steps[i];
          text = text + '  ' + angular.toJson(nextStep);
          if (i < n - 1) {
            text = text + ',\n';
          }
        }
        text = text + '\n]';
        $scope.generated.jsonSteps = text;
      }
    });

    $scope.toggleShowStepTypes = function(index) {
      var value = $scope.generated.stepsDisplay[index].showSteps;
      $scope.generated.stepsDisplay[index].showSteps = !value;
    };

    var prepareParams = {
      params: {
        includeEnvironment: true,
        deploymentTemplateName: "CreateAndCleanUpPreviousAsg"
      }
    };

    $http.get("deployment/prepare/" + $scope.clusterName, prepareParams).success(function(data) {
      $scope.deploymentOptions = data.deploymentOptions;
      $scope.environment = data.environment;
      $scope.asgOptions = data.asgOptions;
      $scope.lcOptions = data.lcOptions;
      $scope.suspendAZRebalance = $scope.asgOptions.suspendedProcesses.indexOf("AZRebalance") > -1;
      $scope.suspendAddToLoadBalancer = $scope.asgOptions.suspendedProcesses.indexOf("AddToLoadBalancer") > -1;
      initStepsDisplay();
      angular.forEach($scope.environment.subnetPurposes.concat(""), function(value) {
        $scope.selectionsForSubnet[value] = {
          securityGroups: [],
          availabilityZones: [],
          loadBalancerNames: []
        }
      });
      $scope.selectionsForSubnet[$scope.asgOptions.subnetPurpose] = {
        securityGroups: $scope.lcOptions.securityGroups,
        availabilityZones: $scope.asgOptions.availabilityZones,
        loadBalancerNames: $scope.asgOptions.loadBalancerNames
      };
    });

    $scope.$watch("asgOptions.subnetPurpose", function() {
      if ($scope.environment) {
        $scope.vpcId = $scope.environment.purposeToVpcId[$scope.asgOptions.subnetPurpose] || "";
      }
    });

    $scope.$watch("selectionsForSubnet[asgOptions.subnetPurpose].securityGroups", function() {
      if ($scope.asgOptions) {
        $scope.selectedSecurityGroupNames = $scope.environment.securityGroups.filter(function(value) {
            return $scope.selectionsForSubnet[$scope.asgOptions.subnetPurpose].securityGroups.indexOf(value.id) !== -1;
          }).map(function(value) { return value.name })
      }
    });

    $scope.$watch("suspendAZRebalance", function() {
      if ($scope.asgOptions) {
        toggleSuspendedProcess("AZRebalance", $scope.suspendAZRebalance);
      }
    });

    $scope.$watch("suspendAddToLoadBalancer", function() {
      if ($scope.asgOptions) {
        toggleSuspendedProcess("AddToLoadBalancer", $scope.suspendAddToLoadBalancer);
      }
    });

    var toggleSuspendedProcess = function(name, suspend) {
      var suspendedProcesses = $scope.asgOptions.suspendedProcesses;
      if (suspend) {
        if (suspendedProcesses.indexOf(name) == -1) {
          suspendedProcesses.push(name);
        }
      } else {
        for(var i = suspendedProcesses.length - 1; i >= 0; i--) {
          if(suspendedProcesses[i] === name) {
            suspendedProcesses.splice(i, 1);
          }
        }
      }
    };

    $scope.toggleAdvanced = function() {
      $scope.hideAdvancedItems = !$scope.hideAdvancedItems
    };

    $scope.toggleJsonSteps = function() {
      $scope.hideJsonSteps = !$scope.hideJsonSteps
    };

    $scope.stepUrl = function(type) {
      return '/views/deployment/' + type + 'Step.html';
    };

    $scope.startDeployment = function() {
      $scope.startingDeployment = true;
      constructStepsFromDisplay();
      var subnetSpecificSelections = $scope.selectionsForSubnet[$scope.asgOptions.subnetPurpose];
      $scope.lcOptions.securityGroups = subnetSpecificSelections.securityGroups;
      $scope.asgOptions.availabilityZones = subnetSpecificSelections.availabilityZones;
      $scope.asgOptions.loadBalancerNames = subnetSpecificSelections.loadBalancerNames;
      var deployment = {
        deploymentOptions: $scope.deploymentOptions,
        asgOptions: $scope.asgOptions,
        lcOptions: $scope.lcOptions
      };
      var json = JSON.stringify(deployment);
      $http.post("deployment/start", json).success(function (data) {
        var deploymentId = data.deploymentId;
        $location.path("deployment/detail/" + deploymentId);
      }).error(function (data) {
        $scope.validationErrors = data.validationErrors;
        $scope.startingDeployment = false
      });
    };

    $scope.retrieveAllAmis = function() {
      $http.get("deployment/allAmis/").success(function(data) {
        $scope.environment.images = data;
        $scope.hideShowMoreAmisLink = true
      });
    };

  });
