angular.module("skyline-rental", ['ngRoute',
                                  'skyline-discover',
                                  'skyline-detail',
                                  'skyline-dashboard',
                                  'skyline-auth'])

.constant('config', {
    serverUrl: 'http://localhost:8080',
    googleCloudStorageBaseUrl: 'https://storage.googleapis.com',
    googleCloudStorageBucket: 'skylinerental-static-dev'
})

.config(['$locationProvider', '$routeProvider', 'config',
    function($location, $routeProvider, config) {
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
        .when('/dashboard', {
            templateUrl: 'dashboard-view/dashboard.html',
            controller: 'userDashboardController',
            resolve: {
                "userInfo": function($http, $location, $window){
                    return $http({ method: 'GET', url: config.serverUrl + '/api/private/user'})
                    .then(
                    function successCallback(response) {
                        return response;
                    },
                    function errorCallback(response) {
                        // $location.path('/login');
                        $window.location.href = '#/login';
                    });
                }
            }
        })
        .when('/delete/:rentalId', {
            templateUrl: 'dashboard-view/dashboard.html',
        })
        .when('/login', {
            templateUrl: 'auth-view/login.html',
        })
        .when('/register', {
            templateUrl: 'auth-view/register.html',
        })
        .when('/reset', {
            templateUrl: 'auth-view/reset.html',
        })
        .when('/set_password/:type/:salt', {
            templateUrl: 'auth-view/set_password.html',
        })
}])