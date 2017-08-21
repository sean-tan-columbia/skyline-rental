angular.module("skyline-rental", ['ngRoute',
                                  'skyline-discover',
                                  'skyline-detail',
                                  'skyline-post',
                                  'skyline-login',
                                  'skyline-register',
                                  'skyline-dashboard'])

.config(['$locationProvider', '$routeProvider',
    function($location, $routeProvider) {
        $routeProvider
        .when('/', {
            templateUrl: 'discover-view/discover.html',
        })
        .when('/discover', {
            templateUrl: 'discover-view/discover.html',
        })
        .when('/detail/:rentalId', {
            templateUrl: 'detail-view/detail.html',
        })
        .when('/post', {
            templateUrl: 'dashboard-view/dashboard.html',
            controller: 'userDashboardController',
            resolve: {
                "userInfo": function($http, $location){
                    return $http({ method: 'GET', url: 'http://localhost:8080/api/private/user'})
                    .then(
                    function successCallback(response) {
                        return response;
                    },
                    function errorCallback(response) {
                        $location.path('/login');
                    });
                }
            }
        })
        .when('/edit/:rentalId', {
            templateUrl: 'dashboard-view/dashboard.html',
            controller: 'userDashboardController',
            resolve: {
                "userInfo": function($http, $location){
                    return $http({ method: 'GET', url: 'http://localhost:8080/api/private/user'})
                    .then(
                    function successCallback(response) {
                        return response;
                    },
                    function errorCallback(response) {
                        $location.path('/login');
                    });
                }
            }
        })
        .when('/delete/:rentalId', {
            templateUrl: 'dashboard-view/dashboard.html',
        })
        .when('/login', {
            templateUrl: 'login-view/login.html',
        })
        .when('/register', {
            templateUrl: 'register-view/register.html',
        })
}])
.constant('config', {
    serverUrl: 'http://localhost:8080',
    googleCloudStorageBaseUrl: 'https://storage.googleapis.com',
    googleCloudStorageBucket: 'skylinerental-static-dev'
})
