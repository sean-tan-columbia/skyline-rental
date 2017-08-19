angular.module('skyline-discover', ['ngRoute', 'ngMap', 'ngMaterial', 'ngMessages', 'ngCookies'])

.controller('rentalDiscoverController', function ($scope, $http, $routeParams, $cookies, NgMap, config, $window, $element) {
    $scope.googleCloudStorageBaseUrl = config.googleCloudStorageBaseUrl;
    $scope.googleCloudStorageBucket = config.googleCloudStorageBucket;
    $scope.markerPink='http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|FFC0CB';
    $scope.markerRed='http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|FF0000';
    $scope.rentals = [];
    $scope.currentSlideIndices = [];
    $scope.customMarkerShown = [];
    $scope.markerIcons = [];
    $scope.pageSize = 20;
    $scope.searchParams = {};
    $scope.mapParamStack = [{'map_center': {'lat':40.785, 'lng':-73.968}, 'map_zoom':12}];
    $scope.isSingleLoc = false;
    $http.get(config.serverUrl + "/api/public/discover/last_updated_timestamp/desc").then(function(r1) {
        rentalIds = r1.data;
        $scope.getRentalsWithIds(rentalIds);
    });
    $scope.getRentalsWithIds = function(rentalIds) {
        $scope.rentalIds = rentalIds;
        $scope.totalIds = rentalIds.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        $scope.pages = Math.ceil($scope.rentalIds.length / $scope.pageSize);
        $scope.newPages = $scope.pages > 5 ? 5 : $scope.pages;
        $scope.selectedPage = 1;
        $scope.selectedRentalIds = $scope.rentalIds.slice(0, $scope.pageSize);
        $scope.pageList = [];
        for (var i = 0; i < $scope.newPages; i++) {
            $scope.pageList.push(i + 1);
        }
        $scope.httpGetRentals($scope.selectedRentalIds);
    }
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
        var selectedRentalIds = $scope.rentalIds.slice(($scope.pageSize * ($scope.selectedPage - 1)), ($scope.selectedPage * $scope.pageSize));
        $scope.httpGetRentals(selectedRentalIds);
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
    };
    $scope.prevPages = function () {
        $scope.selectPage($scope.selectedPage - 1);
    }
    $scope.nextPages = function () {
        $scope.selectPage($scope.selectedPage + 1);
    };
    $scope.httpGetRentals = function(selectedRentalIds) {
        $scope.rentals = [];
        $scope.currentSlideIndices = [];
        likedRentalSet = $scope.getLikedRentalSet();
        for (i = 0; i < selectedRentalIds.length; i++) {
            if ($scope.search.showLiked && !likedRentalSet.has(selectedRentalIds[i])) {
                continue;
            }
            $http.get(config.serverUrl + "/api/public/rental/" + selectedRentalIds[i])
            .then(function(r2) {
                rentalObj = r2.data;
                console.log(rentalObj);
                if (rentalObj.id == undefined) {
                    return;
                }
                // rentalObj.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(", ");
                rentalObj.price = Math.floor(rentalObj.price).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
                rentalObj.moveInDate = $scope.parseDate(rentalObj.startDate);
                rentalObj.isLiked = likedRentalSet.has(rentalObj.id);
                rentalObj.age = getRentalAge(rentalObj.lastUpdatedTimestamp);
                if (rentalObj.isLiked) {
                    rentalObj.likedImg = "../asset/image/filled_heart_32.png";
                } else {
                    rentalObj.likedImg = "../asset/image/empty_heart_32.png";
                }
                switch (rentalObj.bedroom) {
                    case "STUDIO":
                        rentalObj.bedroom = "Studio";
                        break;
                    case "ONE":
                        rentalObj.bedroom = "1 Bedroom";
                        break;
                    case "TWO":
                        rentalObj.bedroom = "2 Bedroom";
                        break;
                    case "THREE":
                        rentalObj.bedroom = "3 Bedroom";
                        break;
                }
                switch (rentalObj.bathroom) {
                    case "SHARED":
                        rentalObj.bathroom = "Shared Bathroom";
                        break;
                    case "ONE":
                        rentalObj.bathroom = "1 Bathroom";
                        break;
                    case "TWO":
                        rentalObj.bathroom = "2 Bathroom";
                        break;
                    case "THREE":
                        rentalObj.bathroom = "3 Bathroom";
                        break;
                }
                // console.log(rentalObj);
                $scope.rentals.push(rentalObj);
                $scope.currentSlideIndices.push(0);
                $scope.customMarkerShown.push(false);
                $scope.markerIcons.push($scope.markerPink);
                $scope.parseDate();
            })
        }
        // console.log(selectedRentalIds);
    };
    $scope.parseDate = function (unix_time) {
        var _date = new Date(parseInt(unix_time));
        return (_date.getMonth() + 1) + "/" + _date.getDate() + "/" + _date.getFullYear();
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
    };
    $scope.placeChanged = function() {
        $scope.search.showLiked = false; // Clear the "only favorites" option
        var place = this.getPlace();
        var lat = place.geometry.location.lat();
        var lng = place.geometry.location.lng();
        $scope.map.setCenter(place.geometry.location);
        $scope.map.setZoom(15); // Call map.on-zoom_changed automatically.
    }
    $scope.sort = function() {
        switch ($scope.search.selectedSorter) {
            case "latest":
                $scope.searchParams['primary'] = "last_updated_timestamp";
                $scope.searchParams['order'] = "desc";
                break;
            case "expensive":
                $scope.searchParams['primary'] = "price";
                $scope.searchParams['order'] = "desc";
                break;
            case "cheap":
                $scope.searchParams['primary'] = "price";
                $scope.searchParams['order'] = "asc";
                break;
            default:
                $scope.searchParams['primary'] = "last_updated_timestamp";
                $scope.searchParams['order'] = "desc";
                break;
        };
        $scope.search();
    }
    /* Search -> */
    $scope.bedrooms = ['Studio' ,'1 Bed' ,'2 Bed' ,'3 Bed'];
    $scope.bathrooms = ['Shared' ,'1 Bath' ,'2 Bath' ,'3 Bath'];
    $element.find('input').on('keydown', function(ev) {
        ev.stopPropagation();
    });
    $scope.submitSearch = function() {
        $scope.search.showLiked = false;
        $scope.isSingleLoc = false;
        $scope.resetSearchParams();
        if ($scope.inputMoveInDate != undefined) {
            $scope.searchParams['move_in_date'] = ($scope.inputMoveInDate/1000).toString();
        }
        if ($scope.inputPriceMin != undefined) {
            $scope.searchParams['price_min'] = $scope.inputPriceMin.toString();
        }
        if ($scope.inputPriceMax != undefined) {
            $scope.searchParams['price_max'] = $scope.inputPriceMax.toString();
        }
        if ($scope.selectedQuantifier != undefined) {
            $scope.searchParams['quantifiers'] = [$scope.selectedQuantifier];
        }
        if ($scope.selectedBedrooms != undefined && $scope.selectedBedrooms.length > 0) {
            $scope.searchParams['bedrooms'] = $scope.selectedBedrooms;
        }
        if ($scope.selectedBathrooms != undefined && $scope.selectedBathrooms.length > 0) {
            $scope.searchParams['bathrooms'] = $scope.selectedBathrooms;
        }
        $scope.search();
    };
    $scope.search = function() {
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/search',
                data: $scope.searchParams
        }).then(function(r) {
            // console.log(r.data);
            rentalIds = r.data;
            $scope.getRentalsWithIds(rentalIds);
        });
    };
    $scope.searchLoc = function(evt, rentalObj) {
        $scope.isSingleLoc = true;
        var mapSearchParams = {};
        pos_delta = 0.0000001;
        mapSearchParams['lng_min'] = (parseFloat(rentalObj.longitude) - pos_delta).toString();
        mapSearchParams['lng_max'] = (parseFloat(rentalObj.longitude) + pos_delta).toString();
        mapSearchParams['lat_min'] = (parseFloat(rentalObj.latitude) - pos_delta).toString();
        mapSearchParams['lat_max'] = (parseFloat(rentalObj.latitude) + pos_delta).toString();
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/mapsearch',
                data: mapSearchParams
        }).then(function(r) {
            console.log(r.data);
            rentalIds = r.data;
            $scope.getRentalsWithIds(rentalIds);
        });
    };
    $scope.searchMap = function() {
        if ($scope.map == undefined) {
            return;
        }
        $scope.isSingleLoc = false;
        $scope.saveSearchParams();
        $scope.search.showLiked = false;
        var center = $scope.map.getCenter();
        var bounds = $scope.map.getBounds();
        $scope.searchParams['lng_min'] = bounds.getSouthWest().lng().toString();
        $scope.searchParams['lng_max'] = bounds.getNorthEast().lng().toString();
        $scope.searchParams['lat_min'] = bounds.getSouthWest().lat().toString();
        $scope.searchParams['lat_max'] = bounds.getNorthEast().lat().toString();
        console.log($scope.searchParams);
        $scope.search();
    };
    $scope.resetSearchParams = function() {
        delete $scope.searchParams['move_in_date'];
        delete $scope.searchParams['price_min'];
        delete $scope.searchParams['price_max'];
        delete $scope.searchParams['bedrooms'];
        delete $scope.searchParams['bathrooms'];
        $scope.searchParams['quantifiers'] = [$scope.selectedQuantifier];
    };
    $scope.clearSearch = function() {
        $scope.inputMoveInDate = undefined;
        $scope.inputPriceMin = undefined;
        $scope.inputPriceMax = undefined;
        $scope.selectedBedrooms = undefined;
        $scope.selectedBathrooms = undefined;
        $scope.selectedQuantifier = "0";
        $scope.resetSearchParams();
        $scope.search();
    };
    $scope.saveSearchParams = function() {
        var center = $scope.map.getCenter();
        var zoom = $scope.map.getZoom();
        var mapParams = {};
        mapParams['map_center'] = {'lat': center.lat(), 'lng': center.lng()};
        mapParams['map_zoom'] = zoom;
        if ($scope.mapParamStack.length >= 10) {
            $scope.mapParamStack.shift();
        }
        $scope.mapParamStack.push(mapParams);
    };
    $scope.returnToPrevMap = function() {
        if ($scope.isSingleLoc) {
            $scope.search();
            $scope.isSingleLoc = false;
            return;
        }
        $scope.targetAddress = undefined;
        if ($scope.mapParamStack.length == 0) {
            return;
        }
        currMapParams = $scope.mapParamStack.pop();
        if ($scope.mapParamStack.length == 0) {
            return;
        }
        prevMapParams = $scope.mapParamStack.pop();
        $scope.map.setCenter(prevMapParams['map_center']);
        $scope.map.setZoom(prevMapParams['map_zoom']);
    };
    /* <- Search */
    NgMap.getMap('ng-map').then(function(map) {
        // google.maps.event.trigger(map, 'resize');
        $scope.map = map;
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
        $scope.toggleMarkerColor = function(markerId, targetColor) {
            if (targetColor == "red") {
                $scope.markerIcons[markerId] = $scope.markerRed;
            } else {
                $scope.markerIcons[markerId] = $scope.markerPink;
            }
        }
    });
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