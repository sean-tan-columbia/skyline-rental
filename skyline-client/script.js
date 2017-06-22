angular.module("skyline-rental", ['ngRoute', 'skyline-discover', 'skyline-detail', 'skyline-post'])

.config(['$locationProvider', '$routeProvider',
    function($location, $routeProvider) {
        $routeProvider
        .when('/', {
            templateUrl: 'discover-view/discover.html',
        })
        .when('/detail/:rentalId', {
            templateUrl: 'detail-view/detail.html',
        })
        .when('/post', {
            templateUrl: 'post-view/post.html',
        })
}])
