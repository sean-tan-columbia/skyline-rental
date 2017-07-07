angular.module("skyline-rental", ['ngRoute',
                                  'skyline-discover',
                                  'skyline-detail',
                                  'skyline-post',
                                  'skyline-login' ])

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
            templateUrl: 'post-view/post.html',
        })
        .when('/login', {
            templateUrl: 'login-view/login.html',
        })
}])
