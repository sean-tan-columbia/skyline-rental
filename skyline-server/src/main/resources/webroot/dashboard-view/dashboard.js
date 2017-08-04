angular.module('skyline-dashboard', ['ngRoute', 'ngMap'])

.controller('userDashboardController', function ($scope, $http, config, $rootScope) {
    console.log($rootScope.username);
    $scope.rentals = [];
    $http.get(config.serverUrl + "/api/public/discover/last_updated_timestamp/desc").then(function(r1) {
        rentalIds = r1.data;
        for (i = 0; i < rentalIds.length; i++) {
            $http.get(config.serverUrl + "/api/public/rental/" + rentalIds[i])
            .then(function(r2) {
                rentalObj = r2.data;
                address_parts = rentalObj.address.split(",")
                rentalObj.displayAddress = address_parts[0] + "," + address_parts[1];
                rentalObj.displayCreatedTimestamp = parseDate(rentalObj.createdTimestamp);
                $scope.rentals.push(rentalObj);
            })
        }
    });
})

.directive("imagereader", ['$http', '$q', 'NgMap', '$window', 'config', '$routeParams',
                            function ($http, $q, NgMap, $window, config, $routeParams) {
    return {
        restrict: 'E',
        templateUrl: 'dashboard-view/rental-info.html',
        scope: { images: '=', files: '='},
        link: function (scope, element, attributes) {
            element.bind('change', function (changeEvent) {
                var hashids = new Hashids("SKYLINE_IMAGE")
                var reader = new FileReader();
                reader.onload = function(loadEvent) {
                    base64 = loadEvent.target.result;
                    imageType = getImageType(base64);
                    imageId = scope.rentalId + '-' + hashids.encode(Date.now()) + '.' + imageType;

                    // Compress image / lower the image quality to 30%
                    var sourceImgObj = new Image();
                    sourceImgObj.onload = function() {
                        var cvs = document.createElement('canvas');
                        cvs.width  = sourceImgObj.width;
                        cvs.height = sourceImgObj.height;
                        cvs.getContext('2d').drawImage(sourceImgObj, 0, 0, cvs.width, cvs.height);
                        var compressedBase64 = cvs.toDataURL('image/' + imageType, 0.3);

                        scope.$apply(function() {
                            scope.images.push({'id': imageId, 'base64': compressedBase64, 'type': imageType});
                            scope.files.push(base64ToArrayBuffer(compressedBase64));
                        });
                    };
                    sourceImgObj.src = base64;
                }
                if (changeEvent.target.files != null) {
                    // document.getElementById('...').files[0] == changeEvent.target.files[0]
                    reader.readAsDataURL(changeEvent.target.files[0]);
                }
            });
            scope.httpGetRental = function(rentalId) {
                $http.get(config.serverUrl + "/api/public/rental/" + rentalId)
                .then(function(response) {
                    rentalObj = response.data
                    scope.rental = rentalObj;
                    scope.inputPrice = parseFloat(rentalObj.price);
                    scope.address = rentalObj.address;
                    scope.inputMoveInDate = new Date(parseInt(rentalObj.startDate));
                    scope.description = rentalObj.description;
                    if (rentalObj.endDate != undefined) {
                        scope.inputMoveOutDate = new Date(parseInt(rentalObj.endDate));
                    }
                    switch (rentalObj.bedroom) {
                        case "STUDIO":
                            scope.selectedBedroom = "0";
                            break;
                        case "ONE":
                            scope.selectedBedroom = "1";
                            break;
                        case "TWO":
                            scope.selectedBedroom = "2";
                            break;
                        case "THREE":
                            scope.selectedBedroom = "3";
                            break;
                    }
                    switch (rentalObj.bathroom) {
                        case "SHARED":
                            scope.selectedBathroom = "0";
                            break;
                        case "ONE":
                            scope.selectedBathroom = "1";
                            break;
                        case "TWO":
                            scope.selectedBathroom = "2";
                            break;
                        case "THREE":
                            scope.selectedBathroom = "3";
                            break;
                    };
                    switch (rentalObj.quantifier) {
                        case "MONTH":
                            scope.selectedQuantifier = "0";
                            break;
                        case "DAY":
                            scope.selectedQuantifier = "1";
                            break;
                    }
                    scope.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(",")
                    for (i = 0; i < scope.imageIds.length; i++) {
                        imageUrl = config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + scope.imageIds[i].trim();
                        toBase64(imageUrl, function(base64){
                            scope.$apply(function() {
                                scope.images.push({'id': scope.imageIds[i], 'base64': base64, 'type': 'jpeg'});
                            });
                        });
                    }
                    console.log(scope.rental);
                });
            }
            scope.posterId = 'jtan';
            var posterHashids = new Hashids("SKYLINE_POSTER");
            if ($routeParams.rentalId != undefined) {
                scope.rentalId = $routeParams.rentalId;
                scope.httpGetRental(scope.rentalId);
            } else {
                scope.rentalId = posterHashids.encode(hashCode(scope.posterId), Date.now());
            }
            console.log(scope.rentalId);
            scope.remove = function(index) {
                scope.images.splice(index, 1);
            }
            scope.getAccessToken = function(callback) {
                $http({ method: 'GET',
                        url: config.serverUrl + '/api/private/gcstoken',
                }).then(function(response) {
                    console.log(response.data)
                    callback(response.data);
                });
            }
            scope.upload = function(oauth2) {
                var promises = [];
                for (i = 0; i < scope.files.length; i++) {
                    var p = $http({
                        url: config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + scope.images[i].id,
                        method: 'PUT',
                        headers: {'Authorization': 'Bearer ' + oauth2,
                                  'Content-Type' : 'image/' + scope.images[i].type},
                        data: scope.files[i],
                        transformRequest: []
                    });
                    promises.push(p);
                }
                $q.all(promises).then(function(results){
                    scope.postRental();
                })
            }
            scope.send = function() {
                scope.getAccessToken(scope.upload);
            }
            scope.postRental = function() {
                var imageIds = [];
                scope.images.forEach(function(image){
                    imageIds.push({'id': image.id});
                });
                data = {
                    id           : scope.rentalId,
                    posterId     : scope.posterId,
                    price        : scope.inputPrice,
                    quantifier   : parseInt(scope.selectedQuantifier),
                    rentalType   : '',
                    address      : scope.address,
                    lat          : scope.lat,
                    lng          : scope.lng,
                    neighborhood : '',
                    move_in_date : scope.inputMoveInDate.getTime(),
                    move_out_date: scope.inputMoveOutDate.getTime(),
                    bedroom      : parseInt(scope.selectedBedroom),
                    bathroom     : parseInt(scope.selectedBathroom),
                    image_ids    : imageIds,
                    description  : scope.description,
                };
                $http({ method: 'POST',
                        url: config.serverUrl + '/api/private/rental',
                        data: data
                })
                .then(function(response) {
                    console.log('Post Success!');
                    $window.location.href = '/';
                });
            }
            NgMap.getMap().then(function(map) {
                scope.map = map;
            });
            scope.types = "['address']";
            scope.placeChanged = function() {
                scope.place = this.getPlace();
                scope.lat = scope.place.geometry.location.lat();
                scope.lng = scope.place.geometry.location.lng();
                console.log(scope.place.formatted_address);
                scope.map.setCenter(scope.place.geometry.location);
            }
        }
    }
}]);

var getImageType = function(base64) {
    return base64.split(';')[0].split('/')[1];
};

var hashCode = function(string) {
    var hash = 0, i, chr;
    if (string.length === 0) return hash;
    for (i = 0; i < string.length; i++) {
        chr   = string.charCodeAt(i);
        hash  = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
    }
    return hash;
};

var base64ToArrayBuffer = function(base64) {
    var byteString = atob(base64.split(',')[1]);
    // separate out the mime component
    var mimeString = base64.split(',')[0].split(':')[1].split(';')[0]
    // write the bytes of the string to an ArrayBuffer
    var ab = new ArrayBuffer(byteString.length);
    var ia = new Uint8Array(ab);
    for (var i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
    }
    return ia;
};

var toBase64 = function (url, callback, outputFormat) {
    var img = new Image();
    img.crossOrigin = 'Anonymous';
    img.onload = function() {
        var canvas = document.createElement('canvas');
        var ctx = canvas.getContext('2d');
        var dataURL;
        canvas.height = this.height;
        canvas.width = this.width;
        ctx.drawImage(this, 0, 0);
        dataURL = canvas.toDataURL("image/jpeg");
        callback(dataURL);
        canvas = null;
    };
    img.src = url;
}
var parseDate = function (unix_time) {
    var _date = new Date(parseInt(unix_time));
    return (_date.getMonth() + 1) + "/" + _date.getDate() + "/" + _date.getFullYear();
};