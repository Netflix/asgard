'use strict';

angular.module("asgardApp")
  .controller("DeploymentNewCtrl", function ($scope, $routeParams, $http, $location) {
    $scope.clusterName = $routeParams.clusterName;
    $scope.hideAdvancedItems = true;
    var prepareParams = {
      params: {
        includeEnvironment: true,
        deploymentTemplateName: "CreateJudgeAndCleanUp"
      }
    };

    $http.get("deployment/prepare/" + $scope.clusterName, prepareParams).success(function(data) {
      $scope.deploymentOptions = data.deploymentOptions;
      $scope.environment = data.environment;
      $scope.asgOptions = data.asgOptions;
      $scope.lcOptions = data.lcOptions;
      if ($scope.asgOptions) {
        $scope.suspendAZRebalance = $scope.asgOptions.suspendedProcesses.indexOf("AZRebalance") > -1;
        $scope.suspendAddToLoadBalancer = $scope.asgOptions.suspendedProcesses.indexOf("AddToLoadBalancer") > -1;
      }
    });

    $scope.$watch("asgOptions.subnetPurpose", function() {
      if ($scope.environment) {
        $scope.vpcId = $scope.environment.purposeToVpcId[$scope.asgOptions.subnetPurpose] || "";
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

    $scope.startDeployment = function() {
      $scope.startingDeployment = true;
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

  });
