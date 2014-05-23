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

  it('should set VPC id based on subnet purpose', function () {
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
      deploymentOptions: { steps: [] },
      asgOptions: {
        subnetPurpose: "",
        suspendedProcesses: [],
        availabilityZones: [],
        loadBalancerNames: []
      },
      lcOptions: {
        securityGroups: []
      },
      environment: {
        subnetPurposes: [],
        purposeToVpcId: {
          'internal': 'vpc1',
          'external': 'vpc2'
        },
        securityGroups: []
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
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: { steps: [] },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
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
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: { steps: [] },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
        }
    });
    $httpBackend.flush();
    expect(scope.startingDeployment).toEqual(undefined);
    scope.startDeployment();
    expect(scope.startingDeployment).toEqual(true);
    $httpBackend.expectPOST('deployment/start', {
      deploymentOptions: { steps: [] },
      asgOptions: {
        subnetPurpose: "",
        suspendedProcesses: [],
        availabilityZones: [],
        loadBalancerNames: []
      },
      lcOptions: {
        securityGroups: []
      }
    }).respond(200, {
      deploymentId: "123"
    });
    $httpBackend.flush();
    expect($location.path()).toEqual('/deployment/detail/123');
  });

  it('should show errors on failure to start deployment', function () {
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: { steps: [] },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
        }
    });
    $httpBackend.flush();
    expect(scope.startingDeployment).toEqual(undefined);
    scope.startDeployment();
    expect(scope.startingDeployment).toEqual(true);
    $httpBackend.expectPOST('deployment/start', {
      deploymentOptions: { steps: [] },
      asgOptions: {
        subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
      },
      lcOptions: {
        securityGroups: []
      }
    }).respond(422, {
      validationErrors: 'errors'
    });
    $httpBackend.flush();
    expect(scope.startingDeployment).toEqual(false);
    expect(scope.validationErrors).toEqual('errors');
    expect($location.path()).toEqual('');
  });

  it('should conditionally allow steps', function() {
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: {
          steps: [
            {"type":"Wait","durationMinutes":60},
            {"type":"CreateAsg"},
            {"type":"Resize","targetAsg":"Next","capacity":0,"startUpTimeoutMinutes":40},
            {"type":"Judgment","durationMinutes":120},
            {"type":"DisableAsg","targetAsg":"Previous"}
          ]
        },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
        }
    });
    $httpBackend.flush();
    expect(scope.isStepAllowed("Wait", 4)).toEqual(true);
    expect(scope.isStepAllowed("Wait", 0)).toEqual(false); // before another Wait
    expect(scope.isStepAllowed("Wait", 2)).toEqual(false); // after another Wait
    expect(scope.isStepAllowed("Wait", 10)).toEqual(false); // at the end
    expect(scope.isStepAllowed("Judgment", 4)).toEqual(true);
    expect(scope.isStepAllowed("Judgment", 6)).toEqual(false); // before another Judgment
    expect(scope.isStepAllowed("Judgment", 8)).toEqual(false); // after another Judgment
    expect(scope.isStepAllowed("Judgment", 10)).toEqual(false); // at the end
    expect(scope.isStepAllowed("ResizeAsg", 4)).toEqual(true);
    expect(scope.isStepAllowed("ResizeAsg", 2)).toEqual(false); // before Create
    expect(scope.isStepAllowed("DisableAsg", 8)).toEqual(true);
    expect(scope.isStepAllowed("DisableAsg", 2)).toEqual(false); // before Create
    expect(scope.isStepAllowed("EnableAsg", 4)).toEqual(true);
    expect(scope.isStepAllowed("EnableAsg", 2)).toEqual(false); // before Create
    expect(scope.isStepAllowed("DeleteAsg", 2)).toEqual(false); // before Create
    expect(scope.isStepAllowed("DeleteAsg", 10)).toEqual(true); // at the end
    expect(scope.isStepAllowed("DeleteAsg", 8)).toEqual(false); // not at the end
  });

  it('should conditionally allow steps with DeleteAsg step', function() {
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: {
          steps: [
            {"type":"CreateAsg"},
            {"type":"DisableAsg","targetAsg":"Previous"},
            {"type":"DeleteAsg","targetAsg":"Previous"}
          ]
        },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
        }
      });
    $httpBackend.flush();
    expect(scope.isStepAllowed("DeleteAsg", 4)).toEqual(false); // there can be only one
    expect(scope.isStepAllowed("DeleteAsg", 6)).toEqual(false); // there can be only one
    expect(scope.isStepAllowed("DisableAsg", 4)).toEqual(true);
    expect(scope.isStepAllowed("DisableAsg", 6)).toEqual(false); // after Delete
    expect(scope.isStepAllowed("EnableAsg", 4)).toEqual(true);
    expect(scope.isStepAllowed("EnableAsg", 6)).toEqual(false); // after Delete
    expect(scope.isStepAllowed("ResizeAsg", 4)).toEqual(true);
    expect(scope.isStepAllowed("ResizeAsg", 6)).toEqual(false); // after Delete
  });

  it('should add step', function() {
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: {
          steps: [
            {"type":"CreateAsg"},
            {"type":"DeleteAsg","targetAsg":"Previous"}
          ]
        },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
        }
      });
    $httpBackend.flush();
    scope.toggleShowStepTypes(2);

    expect(scope.deploymentOptions.steps).toEqual([
      { type : 'CreateAsg' },
      { type : 'DeleteAsg', targetAsg : 'Previous' }
    ]);
    expect(scope.generated.stepsDisplay).toEqual([
      { showSteps : false },
      { type : 'CreateAsg' },
      { showSteps : true },
      { type : 'DeleteAsg', targetAsg : 'Previous' },
      { showSteps : false }
    ]);
    expect(scope.generated.jsonSteps).
      toEqual('[\n  {"type":"CreateAsg"},\n  {"type":"DeleteAsg","targetAsg":"Previous"}\n]');

    scope.addStep("DisableAsg", 2);
    scope.$apply();

    expect(scope.deploymentOptions.steps).toEqual([
      { type : 'CreateAsg' },
      { type : 'DisableAsg', targetAsg : 'Previous' },
      { type : 'DeleteAsg', targetAsg : 'Previous' }
    ]);
    expect(scope.generated.stepsDisplay).toEqual([
      { showSteps : false },
      { type : 'CreateAsg' },
      { showSteps : false },
      { type : 'DisableAsg', targetAsg : 'Previous' },
      { showSteps : false },
      { type : 'DeleteAsg', targetAsg : 'Previous' },
      { showSteps : false }
    ]);
    expect(scope.generated.jsonSteps).
      toEqual('[\n  {"type":"CreateAsg"},\n  {"type":"DisableAsg","targetAsg":"Previous"},\n  {"type":"DeleteAsg","targetAsg":"Previous"}\n]');
  });

  it('should remove step', function() {
    $httpBackend.expectGET(
        'deployment/prepare/helloworld?deploymentTemplateName=CreateAndCleanUpPreviousAsg&includeEnvironment=true').respond({
        deploymentOptions: {
          steps: [
            {"type":"CreateAsg"},
            { type : 'DisableAsg', targetAsg : 'Previous' },
            {"type":"DeleteAsg","targetAsg":"Previous"}
          ]
        },
        asgOptions: {
          subnetPurpose: "",
          suspendedProcesses: [],
          availabilityZones: [],
          loadBalancerNames: []
        },
        lcOptions: {
          securityGroups: []
        },
        environment: {
          purposeToVpcId: {},
          subnetPurposes: [],
          securityGroups: []
        }
      });
    $httpBackend.flush();
    scope.toggleShowStepTypes(2);

    expect(scope.deploymentOptions.steps).toEqual([
      { type : 'CreateAsg' },
      { type : 'DisableAsg', targetAsg : 'Previous' },
      { type : 'DeleteAsg', targetAsg : 'Previous' }
    ]);
    expect(scope.generated.stepsDisplay).toEqual([
      { showSteps : false },
      { type : 'CreateAsg' },
      { showSteps : true },
      { type : 'DisableAsg', targetAsg : 'Previous' },
      { showSteps : false },
      { type : 'DeleteAsg', targetAsg : 'Previous' },
      { showSteps : false }
    ]);
    expect(scope.generated.jsonSteps).
      toEqual('[\n  {"type":"CreateAsg"},\n  {"type":"DisableAsg","targetAsg":"Previous"},\n  {"type":"DeleteAsg","targetAsg":"Previous"}\n]');

    scope.removeStep(2);
    scope.$apply();

    expect(scope.deploymentOptions.steps).toEqual([
      { type : 'CreateAsg' },
      { type : 'DeleteAsg', targetAsg : 'Previous' }
    ]);
    expect(scope.generated.stepsDisplay).toEqual([
      { showSteps : false },
      { type : 'CreateAsg' },
      { showSteps : false },
      { type : 'DeleteAsg', targetAsg : 'Previous' },
      { showSteps : false }
    ]);
    expect(scope.generated.jsonSteps).
      toEqual('[\n  {"type":"CreateAsg"},\n  {"type":"DeleteAsg","targetAsg":"Previous"}\n]');
  });

});
