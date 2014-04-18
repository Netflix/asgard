'use strict';

describe('Controller: DeploymentDetailCtrl', function () {

  // load the controller's module
  beforeEach(module('asgardApp'));

  var DeploymentDetailCtrl, scope, $httpBackend, routeParams, $timeout;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope, _$httpBackend_, _$timeout_) {
    routeParams = {};
    routeParams.deploymentId = '123';
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    scope = $rootScope.$new();
    DeploymentDetailCtrl = $controller('DeploymentDetailCtrl', {
      $scope: scope, $routeParams : routeParams, $timeout: _$timeout_
    });
  }));

  it('should retrieve deployment info until done', function () {
    $httpBackend.expectGET('deployment/show/123.json').respond({
      done: false,
      log: [
        "log message 1",
        "log message 2"
      ]
    });
    $httpBackend.flush();
    expect(scope.logText).toEqual('log message 1\nlog message 2\n');

    $httpBackend.expectGET('deployment/show/123.json').respond({
      done: true,
      log: [
        "log message 1",
        "log message 2",
        "log message 3"
      ]
    });
    $timeout.flush();
    $httpBackend.flush();
    expect(scope.logText).toEqual('log message 1\nlog message 2\nlog message 3\n');
  });

  it('should encoded workflow execution ids', function () {
    $httpBackend.expectGET('deployment/show/123.json').respond({
      done: true,
      workflowExecution: {
        runId: "1\\2/3",
        workflowId: "a&b c"
      }
    });
    $httpBackend.flush();
    expect(scope.encodedWorkflowExecutionIds()).toEqual("runId=1%5C2%2F3&workflowId=a%26b%20c");
  });

  it('should stop deployment', function () {
    $httpBackend.expectGET('deployment/show/123.json').respond({});
    scope.stopDeployment();
    $httpBackend.expectGET('deployment/cancel/123.json').respond({});
    $httpBackend.flush();
  });

  it('should rollback deployment', function () {
    $httpBackend.expectGET('deployment/show/123.json').respond({
      done: true,
      token: "123abc"
    });
    $httpBackend.flush();
    scope.rollbackDeployment();
    $httpBackend.expectPOST('deployment/rollback', '{"id":"123","token":"123abc"}').respond({});
    $httpBackend.flush();
  });

  it('should proceed with deployment', function () {
    $httpBackend.expectGET('deployment/show/123.json').respond({
      done: true,
      token: "123abc"
    });
    $httpBackend.flush();
    scope.proceedWithDeployment();
    $httpBackend.expectPOST('deployment/proceed', '{"id":"123","token":"123abc"}').respond({});
    $httpBackend.flush();
  });

  it('should return step status', function () {
    scope.deployment = {
      status: "running",
      steps: [0, 1, 2],
      logForSteps: [0, 1]
    };
    expect(scope.getStepStatus(0)).toEqual("success");
    expect(scope.getStepStatus(1)).toEqual("running");
    expect(scope.getStepStatus(2)).toEqual("queued");

    scope.deployment.status = "failure";
    expect(scope.getStepStatus(0)).toEqual("success");
    expect(scope.getStepStatus(1)).toEqual("failure");
    expect(scope.getStepStatus(2)).toEqual("queued");

    scope.deployment.status = "completed";
    scope.deployment.logForSteps = [0, 1, 2];
    expect(scope.getStepStatus(0)).toEqual("success");
    expect(scope.getStepStatus(1)).toEqual("success");
    expect(scope.getStepStatus(2)).toEqual("success");
  });

});
