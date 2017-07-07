angular.module('skyline-discover', ['ngRoute', 'ngMap'])

.controller('rentalDiscoverController', function ($scope, $http, $routeParams, NgMap) {
    $scope.googleMapsUrl="https://maps.googleapis.com/maps/api/js?key=AIzaSyAXyVVSnP8Rv-yVGEVKLdoLCJAOKeZEPpk";
    $scope.mapHeight = window.innerHeight - 110 + 'px';
    $scope.hostname = "http://localhost:8080";
    $scope.baseURL = "https://storage.googleapis.com";
    $scope.bucketName = "skylinerental-static-dev";
    $scope.rentals = [];
    $scope.currentSlideIndices = [];
    $http.get("http://localhost:8080/discover").then(function(r1) {
        rentalIds = r1.data;
        for (i = 0; i < rentalIds.length; i++) {
            $http.get($scope.hostname + "/rental/" + rentalIds[i])
            .then(function(r2) {
                rentalObj = r2.data;
                rentalObj.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(", ");
                rentalObj.price = Math.floor(rentalObj.price);
                rentalObj.quantifier = rentalObj.quantifier.toLowerCase();
                $scope.rentals.push(rentalObj);
                $scope.currentSlideIndices.push(0);
                console.log(rentalObj);
            })
        }
    });
    $scope.isCurrentSlideIndex = function (rentalIndex, slideIndex) {
        return $scope.currentSlideIndices[rentalIndex] === slideIndex;
    };
    $scope.prevSlide = function (rentalIndex) {
        $scope.currentSlideIndices[rentalIndex] = ($scope.currentSlideIndices[rentalIndex] < $scope.rentals[rentalIndex].imageIds.length - 1) ? ++$scope.currentSlideIndices[rentalIndex] : 0;
    };
    $scope.nextSlide = function (rentalIndex) {
        $scope.currentSlideIndices[rentalIndex] = ($scope.currentSlideIndices[rentalIndex] > 0) ? --$scope.currentSlideIndices[rentalIndex] : $scope.rentals[rentalIndex].imageIds.length - 1;
    };
})
