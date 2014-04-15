'use strict';

angular.module("asgardApp")
  .controller("DeploymentNewCtrl", function ($scope, $routeParams, $http, $location) {
    $scope.clusterName = $routeParams.clusterName;
    $scope.hideAdvancedItems = true;

    $http.get("deployment/prepareDeployment/" + $scope.clusterName).success(function(data, status, headers, config) {
      $scope.deploymentOptions = data.deploymentOptions;
      $scope.environment = data.environment;
      $scope.asgOptions = data.asgOptions;
      $scope.lcOptions = data.lcOptions;
    });

    $scope.$watch("asgOptions.subnetPurpose", function() {
      if ($scope.environment) {
        $scope.vpcId = $scope.environment.purposeToVpcId[$scope.asgOptions.subnetPurpose] || "";
      }
    }, true);

    $scope.toggleAdvanced = function() {
      $scope.hideAdvancedItems = !$scope.hideAdvancedItems
    };

    $scope.startDeployment = function() {
      $scope.startingDeployment = true;
      var deployment = {
        deploymentOptions: $scope.deploymentOptions,
        asgOverrides: $scope.asgOptions,
        lcOverrides: $scope.lcOptions
      };
      var json = JSON.stringify(deployment);
      $http.post("deployment/startDeployment", json).success(function (data, status, headers) {
        var deploymentId = data.deploymentId;
        $location.path("deployment/detail/" + deploymentId);
      }).error(function (data, status, headers) {
        $scope.validationErrors = data.validationErrors;
        $scope.startingDeployment = false
      });
    };

  });
