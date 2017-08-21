angular.module('skyline-dashboard', ['ngRoute', 'ngMap'])

.controller('userDashboardController', function ($scope, $route, $http, config, $q, $window, userInfo) {
    $scope.rentals = [];
    var user = userInfo.data;
    $scope.id = user.id;
    $scope.email = user.email;
    for (i = 0; i < user.rentals.length; i++) {
        rentalObj = user.rentals[i];
        address_parts = rentalObj.address.split(",")
        rentalObj.displayAddress = address_parts[0] + "," + address_parts[1];
        rentalObj.displayCreatedTimestamp = parseDate(rentalObj.createdTimestamp);
        $scope.rentals.push(rentalObj);
    }
    $scope.deleteRental = function(rentalIndex) {
        rentalId = $scope.rentals[rentalIndex].id;
        $http.delete(config.serverUrl + "/api/private/rental/" + rentalId)
        .then(function(r) {
            if (r.status == 200) {
                // gcstoken = r.data;
                // // imageIds = $scope.rentals[rentalIndex].imageIds.substring(1, rentalObj.imageIds.length-1).split(", ");
                // imageIds = $scope.rentals[rentalIndex].imageIds
                // $scope.deleteImages(gcstoken, imageIds, function() {
                    if ($scope.rentals.length > 1) {
                        $route.reload();
                    } else {
                        $window.location.href = '/';
                    }
                // });
            } else if (r.status == 202) {
                console.log('Rental stays unchanged');
            }
        });
    };
    $scope.deleteImages = function(gcstoken, imageIds, callback) {
        var promises = [];
        for (i = 0; i < imageIds.length; i++) {
            var p = $http({
                url: config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + imageIds[i].trim(),
                method: 'DELETE',
                headers: { 'Authorization': 'Bearer ' + gcstoken,
                           'Content-Type' : 'image/' + 'jpeg' },
                transformRequest: []
            });
            promises.push(p);
        }
        $q.all(promises).then(function(results){
            callback();
        })
    };
})

.directive("imagereader", ['$http', '$q', 'NgMap', '$window', 'config', '$routeParams', '$route',
                            function ($http, $q, NgMap, $window, config, $routeParams, $route) {
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
                            // scope.files.push(base64ToArrayBuffer(compressedBase64));
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
                    if (rentalObj.id == undefined) {
                        return;
                    }
                    scope.rental = rentalObj;
                    scope.inputPrice = parseFloat(rentalObj.price);
                    scope.address = rentalObj.address;
                    scope.lat = parseFloat(rentalObj.latitude);
                    scope.lng = parseFloat(rentalObj.longitude);
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
                    // scope.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(",")
                    scope.imageIds = rentalObj.imageIds;
                    scope.images = [];
                    for (i = 0; i < scope.imageIds.length; i++) {
                        scope.images.push({});
                    }
                    for (i = 0; i < scope.imageIds.length; i++) {
                        scope.existedImages.add(scope.imageIds[i]);
                        imageUrl = config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + scope.imageIds[i].trim();
                        toBase64(i, imageUrl, function(imageIndex, base64){
                            scope.$apply(function() {
                                scope.images[imageIndex] = {'id': scope.imageIds[imageIndex].trim(), 'base64': base64, 'type': 'jpeg'};
                            });
                        });
                    }
                });
            };
            if ($routeParams.rentalId != undefined) {
                scope.existedImages = new Set();
                scope.deletedImages = [];
                scope.rentalId = $routeParams.rentalId;
                scope.httpGetRental(scope.rentalId);
                console.log(scope.rentalId);
            } else {
                // TODO: Make sure hashids doesn't use '/'
                var posterHashids = new Hashids("SKYLINE_POSTER");
                $http.get("https://ipinfo.io")
                .then(function(response) {
                    ipinfo = response.data;
                    scope.rentalId = posterHashids.encode(ip2int(ipinfo.ip), Date.now());
                    console.log(scope.rentalId);
                });
            };
            scope.remove = function(index) {
                if (scope.deletedImages != undefined) {
                    scope.deletedImages.push(scope.images[index].id);
                }
                scope.images.splice(index, 1);
            };
            scope.deleteImages = function(gcstoken, imageIds, callback) {
                var promises = [];
                for (i = 0; i < imageIds.length; i++) {
                    var p = $http({
                        url: config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + imageIds[i].trim(),
                        method: 'DELETE',
                        headers: { 'Authorization': 'Bearer ' + gcstoken,
                                   'Content-Type' : 'image/' + 'jpeg' },
                        transformRequest: []
                    });
                    promises.push(p);
                };
                $q.all(promises).then(function(results){
                    callback();
                });
            };
            scope.uploadImages = function(oauth2, images, callback) {
                var promises = [];
                for (i = 0; i < images.length; i++) {
                    if (scope.existedImages != undefined && scope.existedImages.has(images[i].id)) {
                        continue;
                    }
                    var p = $http({
                        url: config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + images[i].id,
                        method: 'PUT',
                        headers: {'Authorization': 'Bearer ' + oauth2,
                                  'Content-Type' : 'image/' + images[i].type},
                        data: base64ToArrayBuffer(images[i].base64),
                        transformRequest: []
                    });
                    promises.push(p);
                }
                $q.all(promises).then(function(results){
                    // scope.postRental();
                    callback();
                })
            }
            scope.send = function() {
                scope.submitRental();
            }
            scope.submitRental = function() {
                var imageIds = [];
                scope.images.forEach(function(image){
                    imageIds.push({'id': image.id});
                });
                var move_out_date = undefined;
                if (scope.inputMoveOutDate != undefined) {
                    move_out_date = scope.inputMoveOutDate.getTime();
                }
                data = {
                    id           : scope.rentalId,
                    price        : scope.inputPrice,
                    quantifier   : parseInt(scope.selectedQuantifier),
                    rentalType   : '',
                    address      : scope.address,
                    lat          : scope.lat,
                    lng          : scope.lng,
                    neighborhood : '',
                    move_in_date : scope.inputMoveInDate.getTime(),
                    move_out_date: move_out_date,
                    bedroom      : parseInt(scope.selectedBedroom),
                    bathroom     : parseInt(scope.selectedBathroom),
                    image_ids    : imageIds,
                    description  : scope.description,
                };
                if ($routeParams.rentalId == undefined) {
                    $http({ method: 'POST',
                        url: config.serverUrl + '/api/private/rental',
                        data: data
                    }).then(function(response) {
                        if (response.status == 201) {
                            gcstoken = response.data;
                            scope.uploadImages(gcstoken, scope.images, function() { $route.reload(); });
                        } else {
                            console.log('Rental stays unchanged');
                        }
                    });
                } else {
                    $http({ method: 'PUT',
                        url: config.serverUrl + '/api/private/rental/' + data.id,
                        data: data
                    }).then(function(response) {
                        if (response.status == 200) {
                            gcstoken = response.data;
                            scope.uploadImages(gcstoken, scope.images, function() {
                                scope.deleteImages(gcstoken, scope.deletedImages, function() { $route.reload(); });
                            });
                        } else if (response.status == 202) {
                            console.log('Rental stays unchanged');
                        }
                    });
                }
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

var toBase64 = function (index, url, callback, outputFormat) {
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
        callback(index, dataURL);
        canvas = null;
    };
    img.src = url;
}
var parseDate = function (unix_time) {
    var _date = new Date(parseInt(unix_time));
    return (_date.getMonth() + 1) + "/" + _date.getDate() + "/" + _date.getFullYear();
};

var ip2int = function(ip) {
    return ip.split('.').reduce(function(ipInt, octet) { return (ipInt<<8) + parseInt(octet, 10)}, 0) >>> 0;
}