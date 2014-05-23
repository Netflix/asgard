'use strict';

angular.module('asgardApp')
  .controller('DeploymentDetailCtrl', function ($scope, $routeParams, $http, $timeout) {
    var deploymentId = $routeParams.deploymentId;
    var shouldPoll = true;
    $scope.readOnlyDeploymentSteps = true;
    $scope.targetAsgTypes = ["Previous", "Next"];

    var retrieveDeployment = function() {
      $http.get('deployment/show/' + deploymentId + '.json').success(function(data, status, headers, config) {
        $scope.deployment = data;
        shouldPoll = !$scope.deployment.done;
        var text = '';
        angular.forEach($scope.deployment.log, function(value) {
          text = text + value + '\n';
        });
        $scope.logText = text;
      });
    };

    var poll = function() {
        retrieveDeployment();
      if (shouldPoll) {
        $timeout(poll, 1000);
      }
    };
    poll();

    $scope.getLogForStep = function(stepIndex) {
      return $scope.deployment.logForSteps[stepIndex];
    };

    $scope.stepUrl = function(type) {
      return '/views/deployment/' + type + 'Step.html';
    };

    $scope.encodedWorkflowExecutionIds = function() {
      var runId = $scope.deployment.workflowExecution.runId;
      var workflowId = $scope.deployment.workflowExecution.workflowId;
      return "runId=" + encodeURIComponent(runId) + "&workflowId=" + encodeURIComponent(workflowId);
    };

    $scope.stopDeployment = function() {
      $http.get('deployment/cancel/' + deploymentId + '.json');
    };

    $scope.rollbackDeployment = function() {
      judgeDeployment('rollback');
    };

    $scope.proceedWithDeployment = function() {
      judgeDeployment('proceed');
    };

    $scope.getCurrentStep = function() {
      return $scope.deployment.logForSteps.length - 1;
    };

    $scope.getStepStatus = function(stepIndex) {
      var currentStep = $scope.getCurrentStep();
      if (stepIndex < currentStep) {
        return "success";
      }
      if (stepIndex === currentStep) {
        if ($scope.deployment.status === "completed" && currentStep === $scope.deployment.steps.length - 1) {
          return "success";
        }
        if ($scope.deployment.status !== "running") {
          return "failure";
        }
        return "running";
      }
      return "queued";
    };

    var judgeDeployment = function(judgment) {
      $http.post('deployment/' + judgment, {
        id: deploymentId,
        token: $scope.deployment.token
      });
    };
  });
