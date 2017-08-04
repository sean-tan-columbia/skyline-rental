angular.module("skyline-rental", ['ngRoute',
                                  'skyline-discover',
                                  'skyline-detail',
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
        })
        .when('/edit/:rentalId', {
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
