angular.module("skyline-rental", ['ngRoute', 'skyline-discover', 'skyline-detail'])

.config(['$locationProvider', '$routeProvider',
    function($location, $routeProvider) {
        $routeProvider
        .when('/', {
            templateUrl: 'discover-view/discover.html',
            controller: 'rentalListController'
        })
        .when('/detail/:rentalId', {
            templateUrl: 'detail-view/detail.html',
            controller: 'imageSlideController'
        })
}])