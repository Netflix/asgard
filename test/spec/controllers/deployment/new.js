'use strict';

describe('Controller: DeploymentNewCtrl', function () {

  // load the controller's module
  beforeEach(module('asgardApp'));

  var DeploymentNewCtrl, scope, $httpBackend, $location, routeParams;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope, _$httpBackend_, _$location_) {
    routeParams = {};
    routeParams.clusterName = 'helloworld';
    $httpBackend = _$httpBackend_;
    $location = _$location_;
    scope = $rootScope.$new();
    DeploymentNewCtrl = $controller('DeploymentNewCtrl', {
      $scope: scope, $routeParams : routeParams, $location: $location
    });
  }));

  it('should set initial scope', function () {
    $httpBackend.expectGET('deployment/prepareDeployment/helloworld').respond({
      deploymentOptions: 'deploymentOptions1',
      environment: 'environment1',
      lcOptions: 'lcOptions1',
      asgOptions: {
        name: "asgOptions1",
        suspendedProcesses: ["AddToLoadBalancer"]
      }
    });
    $httpBackend.flush();
    expect(scope.clusterName).toEqual('helloworld');
    expect(scope.hideAdvancedItems).toEqual(true);
    expect(scope.deploymentOptions).toEqual('deploymentOptions1');
    expect(scope.environment).toEqual('environment1');
    expect(scope.asgOptions.name).toEqual('asgOptions1');
    expect(scope.lcOptions).toEqual('lcOptions1');
    expect(scope.vpcId).toEqual(undefined);
    expect(scope.suspendAZRebalance).toEqual(false);
    expect(scope.suspendAddToLoadBalancer).toEqual(true);
  });

  it('should set VPC id based on subnet purpose', function () {
    $httpBackend.expectGET('deployment/prepareDeployment/helloworld').respond({
      environment: {
        purposeToVpcId: {
          'internal': 'vpc1',
          'external': 'vpc2'
        }
      }
    });
    $httpBackend.flush();
    scope.asgOptions = {};
    scope.asgOptions.subnetPurpose = 'internal';
    scope.$apply();
    expect(scope.vpcId).toEqual('vpc1');
    scope.asgOptions.subnetPurpose = 'external';
    scope.$apply();
    expect(scope.vpcId).toEqual('vpc2');
    scope.asgOptions.subnetPurpose = 'neither';
    scope.$apply();
    expect(scope.vpcId).toEqual('');
  });

  it('should toggle suspended processes', function () {
    $httpBackend.expectGET('deployment/prepareDeployment/helloworld').respond({
      asgOptions: {
        suspendedProcesses: []
      }
    });
    $httpBackend.flush();
    expect(scope.suspendAZRebalance).toEqual(false);
    expect(scope.suspendAddToLoadBalancer).toEqual(false);
    expect(scope.asgOptions.suspendedProcesses).toEqual([]);

    scope.$apply();
    expect(scope.asgOptions.suspendedProcesses).toEqual([]);

    scope.suspendAZRebalance = true;
    scope.$apply();
    expect(scope.asgOptions.suspendedProcesses).toEqual(["AZRebalance"]);

    scope.suspendAddToLoadBalancer = true;
    scope.$apply();
    expect(scope.asgOptions.suspendedProcesses).toEqual(["AZRebalance", "AddToLoadBalancer"]);

    scope.suspendAZRebalance = false;
    scope.$apply();
    expect(scope.asgOptions.suspendedProcesses).toEqual(["AddToLoadBalancer"]);

    scope.suspendAddToLoadBalancer = false;
    scope.$apply();
    expect(scope.asgOptions.suspendedProcesses).toEqual([]);
  });

  it('should toggle advanced items', function () {
    expect(scope.hideAdvancedItems).toEqual(true);
    scope.toggleAdvanced();
    expect(scope.hideAdvancedItems).toEqual(false);
    scope.toggleAdvanced();
    expect(scope.hideAdvancedItems).toEqual(true);
  });

  it('should start deployment', function () {
    $httpBackend.expectGET('deployment/prepareDeployment/helloworld').respond({
      deploymentOptions: 'deploymentOptions1'
    });
    $httpBackend.flush();
    expect(scope.startingDeployment).toEqual(undefined);
    scope.startDeployment();
    expect(scope.startingDeployment).toEqual(true);
    $httpBackend.expectPOST('deployment/startDeployment', {"deploymentOptions":"deploymentOptions1"}).respond(200, {
      deploymentId: "123"
    });
    $httpBackend.flush();
    expect($location.path()).toEqual('/deployment/detail/123');
  });

  it('should show errors on failure to start deployment', function () {
    $httpBackend.expectGET('deployment/prepareDeployment/helloworld').respond({
      deploymentOptions: 'deploymentOptions1'
    });
    $httpBackend.flush();
    expect(scope.startingDeployment).toEqual(undefined);
    scope.startDeployment();
    expect(scope.startingDeployment).toEqual(true);
    $httpBackend.expectPOST('deployment/startDeployment', {"deploymentOptions":"deploymentOptions1"}).respond(422, {
      validationErrors: 'errors'
    });
    $httpBackend.flush();
    expect(scope.startingDeployment).toEqual(false);
    expect(scope.validationErrors).toEqual('errors');
    expect($location.path()).toEqual('');
  });

});
