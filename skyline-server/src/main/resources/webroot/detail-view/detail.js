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
        $scope.rentalAge = getRentalAge(parseInt(rentalObj.lastUpdatedTimestamp));
        switch (rentalObj.bedroom) {
            case "STUDIO":
                $scope.bedroom = "Studio";
                break;
            case "ONE":
                $scope.bedroom = "1";
                break;
            case "TWO":
                $scope.bedroom = "2";
                break;
            case "THREE":
                $scope.bedroom = "3";
                break;
        }
        switch (rentalObj.bathroom) {
            case "SHARED":
                $scope.bathroom = "Shared";
                break;
            case "ONE":
                $scope.bathroom = "1";
                break;
            case "TWO":
                $scope.bathroom = "2";
                break;
            case "THREE":
                $scope.bathroom = "3";
                break;
        }
        $scope.parseAddress();
        $scope.parseDate();
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
        console.log(_address);
        $scope.short_address = "";
        if (_address.length > 0) {
            $scope.short_address = $scope.short_address + _address[0];
        }
        if (_address.length > 1) {
            $scope.short_address = $scope.short_address + ", " + _address[1];
        }
    };
    $scope.parseDate = function () {
        var _date = new Date(parseInt($scope.rental.startDate));
        $scope.moveInDate = _date.getMonth() + "/" + _date.getDate() + "/" + _date.getFullYear();
        console.log($scope.moveInDate);
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

var getRentalAge = function(lastUpdatedTimestamp) {
    diff = new Date().getTime() - lastUpdatedTimestamp;
    if (diff < 3600 * 1000) {
        return Math.floor(diff / 60000) + "min";
    } else if (diff < 3600 * 24 * 1000) {
        return Math.floor(diff / 3600000) + "hr";
    } else {
        return Math.floor(diff / (3600 * 24 * 1000)) + "d";
    }
}