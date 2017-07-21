angular.module('skyline-discover', ['ngRoute', 'ngMap', 'ngMaterial', 'ngMessages', 'ngCookies'])

.controller('rentalDiscoverController', function ($scope, $http, $routeParams, $cookies, NgMap, config, $window) {
    $scope.googleCloudStorageBaseUrl = config.googleCloudStorageBaseUrl;
    $scope.googleCloudStorageBucket = config.googleCloudStorageBucket;
    $scope.markerPink='http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|FFC0CB';
    $scope.markerRed='http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|FF0000';
    $scope.rentals = [];
    $scope.currentSlideIndices = [];
    $scope.customMarkerShown = [];
    $scope.markerIcons = [];
    $http.get(config.serverUrl + "/api/public/discover").then(function(r1) {
        rentalIds = r1.data;
        $scope.rentalIds = r1.data;
        $scope.pageSize = 5;
        $scope.pages = Math.ceil($scope.rentalIds.length / $scope.pageSize);
        $scope.newPages = $scope.pages > 5 ? 5 : $scope.pages;
        $scope.selectedPage = 1;
        $scope.selectedRentalIds = $scope.rentalIds.slice(0, $scope.pageSize);
        $scope.pageList = [];
        for (var i = 0; i < $scope.newPages; i++) {
            $scope.pageList.push(i + 1);
        }
        $scope.httpGetRentals();
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
    $scope.setData = function () {
        $scope.selectedRentalIds = $scope.rentalIds.slice(($scope.pageSize * ($scope.selectedPage - 1)), ($scope.selectedPage * $scope.pageSize));
        $scope.httpGetRentals();
    }
    $scope.isActivePage = function (page) {
        return $scope.selectedPage == page;
    };
    $scope.selectPage = function (page) {
        if (page < 1 || page > $scope.pages) return;
        if (page > 2) {
            var newpageList = [];
            for (var i = (page - 3) ; i < ((page + 2) > $scope.pages ? $scope.pages : (page + 2)) ; i++) {
                newpageList.push(i + 1);
            }
            $scope.pageList = newpageList;
        }
        $scope.selectedPage = page;
        $scope.setData();
        $scope.isActivePage(page);
        console.log("Selected Page：" + page);
    };
    $scope.prevPages = function () {
        $scope.selectPage($scope.selectedPage - 1);
    }
    $scope.nextPages = function () {
        $scope.selectPage($scope.selectedPage + 1);
    };
    $scope.httpGetRentals = function() {
        $scope.rentals = [];
        $scope.currentSlideIndices = [];
        likedRentalSet = $scope.getLikedRentalSet();
        console.log(likedRentalSet);
        for (i = 0; i < $scope.selectedRentalIds.length; i++) {
            $http.get(config.serverUrl + "/api/public/rental/" + $scope.selectedRentalIds[i])
            .then(function(r2) {
                rentalObj = r2.data;
                rentalObj.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(", ");
                rentalObj.price = Math.floor(rentalObj.price).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
                rentalObj.moveInDate = $scope.parseDate(rentalObj.startDate);
                rentalObj.isLiked = likedRentalSet.has(rentalObj.id);
                if (rentalObj.isLiked) {
                    rentalObj.likedImg = "../asset/image/filled_heart_32.png";
                } else {
                    rentalObj.likedImg = "../asset/image/empty_heart_32.png";
                }
                $scope.rentals.push(rentalObj);
                $scope.currentSlideIndices.push(0);
                $scope.customMarkerShown.push(false);
                $scope.markerIcons.push($scope.markerPink);
                $scope.parseDate();
            })
        }
        console.log($scope.selectedRentalIds);
    };
    $scope.parseDate = function (unix_time) {
        var _date = new Date(parseInt(unix_time));
        return _date.getMonth() + "/" + _date.getDate() + "/" + _date.getFullYear();
    };
    $scope.getLikedRentalSet = function() {
        var likedRentalSet = null;
        var likedRentals = $cookies.get('liked_rentals');
        if (likedRentals != null && likedRentals.length > 0) {
            likedRentalSet = new Set(JSON.parse(likedRentals));
        } else {
            likedRentalSet = new Set();
        }
        return likedRentalSet;
    };
    $scope.likeRental = function (rental_index) {
        $scope.rentals[rental_index].isLiked = true;
        $scope.rentals[rental_index].likedImg = "../asset/image/filled_heart_32.png";
        var likedRentalSet = $scope.getLikedRentalSet();
        if (rental_index < $scope.rentals.length && $scope.rentals[rental_index] != null) {
            rental_id = $scope.rentals[rental_index].id;
            likedRentalSet.add(rental_id);
        }
        now = new Date();
        $cookies.put('liked_rentals',
                     JSON.stringify(Array.from(likedRentalSet)),
                     { expires: new Date(now.getFullYear() + 1, now.getMonth(), now.getDate())}
        );
    };
    $scope.unlikeRental = function(rental_index) {
        $scope.rentals[rental_index].isLiked = false;
        $scope.rentals[rental_index].likedImg = "../asset/image/empty_heart_32.png";
        var likedRentalSet = $scope.getLikedRentalSet();
        if (rental_index < $scope.rentals.length && $scope.rentals[rental_index] != null) {
            rental_id = $scope.rentals[rental_index].id;
            if (likedRentalSet.has(rental_id)) {
                likedRentalSet.delete(rental_id);
            }
        }
        now = new Date();
        $cookies.put('liked_rentals',
                     JSON.stringify(Array.from(likedRentalSet)),
                     { expires: new Date(now.getFullYear() + 1, now.getMonth(), now.getDate())}
        );
    }
    $scope.toggleLikeUnlike = function(rental_index) {
        if ($scope.rentals[rental_index].isLiked) {
            $scope.unlikeRental(rental_index);
        } else {
            $scope.likeRental(rental_index);
        }
    }
    NgMap.getMap('ng-map').then(function(map) {
        // google.maps.event.trigger(map, 'resize');
        $scope.showCustomMarker = function(evt, markerId) {
            $scope.$apply(function(){
                $scope.closeAllCustomMarker();
                $scope.customMarkerShown[markerId] = true;
            });
            // map.customMarkers[markerId].setVisible(true);
            map.customMarkers[markerId].setPosition(this.getPosition());
        };
        $scope.closeCustomMarker = function(evt, markerId) {
            $scope.customMarkerShown[markerId] = false;
            // this.style.display = 'none';
        };
        $scope.closeAllCustomMarker = function() {
            for (markerId = 0; markerId < $scope.customMarkerShown.length; markerId++) {
                $scope.customMarkerShown[markerId] = false;
            }
        };
        $scope.toggleMarkerColor = function(markerId) {
            if ($scope.markerIcons[markerId] == $scope.markerPink) {
                $scope.markerIcons[markerId] = $scope.markerRed;
            } else {
                $scope.markerIcons[markerId] = $scope.markerPink;
            }
        }
    });
});
