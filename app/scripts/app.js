'use strict';

angular.module('asgardApp', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'ngAnimate',
  'ui.bootstrap'
])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: '/views/main.html',
        controller: 'MainCtrl'
      })
      .when('/deployment/detail/:deploymentId', {
        templateUrl: '/views/deployment/detail.html',
        controller: 'DeploymentDetailCtrl'
      })
      .when('/deployment/new/:clusterName', {
        templateUrl: '/views/deployment/new.html',
        controller: 'DeploymentNewCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
