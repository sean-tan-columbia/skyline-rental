angular.module('skyline-detail', ['ngAnimate', 'ngRoute'])

.controller('imageSlideController', function ($scope, $http, $routeParams, config) {
    $http.get(config.serverUrl + "/api/public/rental/" + $routeParams.rentalId)
    .then(function(response) {
        console.log($routeParams.rentalId)
        rentalObj = response.data
        rentalObj.price = Math.floor(rentalObj.price).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        rentalObj.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(", ")
        $scope.rental = rentalObj
        $scope.slides = rentalObj.imageIds
        $scope.parseAddress();
    });
    $scope.googleCloudStorageBaseUrl = config.googleCloudStorageBaseUrl;
    $scope.googleCloudStorageBucket = config.googleCloudStorageBucket;
    $scope.slides = [];
    $scope.direction = 'left';
    $scope.currentIndex = 0;
    $scope.setCurrentSlideIndex = function (index) {
        $scope.direction = (index > $scope.currentIndex) ? 'left' : 'right';
        $scope.currentIndex = index;
    };
    $scope.isCurrentSlideIndex = function (index) {
        return $scope.currentIndex === index;
    };
    $scope.prevSlide = function () {
        $scope.direction = 'left';
        $scope.currentIndex = ($scope.currentIndex < $scope.slides.length - 1) ? ++$scope.currentIndex : 0;
    };
    $scope.nextSlide = function () {
        $scope.direction = 'right';
        $scope.currentIndex = ($scope.currentIndex > 0) ? --$scope.currentIndex : $scope.slides.length - 1;
    };
    $scope.parseAddress = function () {
        var _address = $scope.rental.address.split(", ");
        $scope.r_street = _address[0]
        $scope.r_city = _address[1] + ", " + _address[2]
    };
})
.animation('.slide-animation', function () {
    return {
        beforeAddClass: function (element, className, done) {
            var scope = element.scope();
            if (className == 'ng-hide') {
                var finishPoint = element.parent().width();
                if(scope.direction !== 'right') {
                    finishPoint = -finishPoint;
                }
                TweenMax.to(element, 0.5, {left: finishPoint, onComplete: done });
            }
            else {
                done();
            }
        },
        removeClass: function (element, className, done) {
            var scope = element.scope();
            if (className == 'ng-hide') {
                element.removeClass('ng-hide');

                var startPoint = element.parent().width();
                if(scope.direction === 'right') {
                    startPoint = -startPoint;
                }
                TweenMax.fromTo(element, 0.5, { left: startPoint }, {left: 0, onComplete: done });
            }
            else {
                done();
            }
        }
    };
});
function numberWithCommas(x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}