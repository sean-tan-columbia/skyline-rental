angular.module('skyline-dashboard', ['ngRoute', 'ngMap'])

.factory('dataSharedService', function($rootScope, $http, config) {
    var sharedService = {};
    sharedService.httpGetRental = function(rentalId) {
        $http.get(config.serverUrl + "/api/public/rental/" + rentalId)
        .then(function(response) {
            $rootScope.$broadcast('dashRentalGet', response.data);
        });
    };
    sharedService.clearRental = function() {
        $rootScope.$broadcast('dashRentalClear');
    };
    sharedService.addRental = function(data) {
        $rootScope.$broadcast('dashRentalAdd', data);
    }
    sharedService.editRental = function(data) {
        $rootScope.$broadcast('dashRentalEdit', data);
    }
    sharedService.alertFailure = function(msg) {
        $rootScope.$broadcast('dashRentalFail', msg);
    }
    return sharedService;
})

.controller('userDashboardController', function ($scope, $route, $http, config, $q, $window, userInfo, dataSharedService) {
    $scope.rentals = [];
    var user = userInfo.data;
    console.log(user);
    $scope.id = user.id;
    $scope.name = user.name;
    $scope.phone = user.phone
    $scope.email = user.email;
    $scope.wechat = user.wechatId;
    var originalUserName = user.name;
    var originalPhone = user.phone;
    var originalWechatId = user.wechatId;
    for (i = 0; i < user.rentals.length; i++) {
        rentalObj = user.rentals[i];
        address_parts = rentalObj.address.split(",")
        displayAddress = address_parts[0] + "," + address_parts[1];
        displayCreatedTimestamp = parseDate(rentalObj.createdTimestamp);
        $scope.rentals.push({'id':rentalObj.id,'displayAddress':displayAddress,'displayCreatedTimestamp':displayCreatedTimestamp});
    }
    $scope.deleteRental = function(rentalIndex) {
        rentalId = $scope.rentals[rentalIndex].id;
        console.log($scope.rentals[rentalIndex].displayAddress);
        $http.delete(config.serverUrl + "/api/private/rental/" + rentalId)
        .then(function(r1) {
            if (r1.status == 200) {
                dataSharedService.clearRental();
                $scope.alertSuccessInfo = 'Rental at ' + $scope.rentals[rentalIndex].displayAddress +' has been deleted.';
                $scope.alertSuccessShow = true;

                var targetIndex = -1;
                for (i = 0; i < $scope.rentals.length; i++) {
                    if ($scope.rentals[i].id == rentalId) {
                        targetIndex = i;
                        break;
                    }
                }
                if (targetIndex != -1) {
                    $scope.rentals.splice(targetIndex, 1);
                }
                $window.scrollTo(0, 0);
            } else {
                $scope.alertFailureInfo = 'There was an error deleting rental at ' + $scope.rentals[rentalIndex].displayAddress;
                $scope.alertFailureShow = true;
                $window.scrollTo(0, 0);
            }
        });
    };
    $scope.editRental = function(rentalIndex) {
        rentalId = $scope.rentals[rentalIndex].id;
        dataSharedService.httpGetRental(rentalId);
    };
    $scope.clearRental = function() {
        dataSharedService.clearRental();
    };
    $scope.$on('dashRentalAdd', function(event, data) {
        address_parts = data.address.split(",");
        displayAddress = address_parts[0] + "," + address_parts[1];
        displayCreatedTimestamp = parseDate((new Date()).getTime());
        $scope.rentals.unshift({'id':data.id,'displayAddress':displayAddress,'displayCreatedTimestamp':displayCreatedTimestamp});
        $scope.alertSuccessInfo = 'New rental at ' + data.address +' has been created.';
        $scope.alertSuccessShow = true;
        $window.scrollTo(0, 0);
    });
    $scope.$on('dashRentalEdit', function(event, data) {
        address_parts = data.address.split(",");
        displayAddress = address_parts[0] + "," + address_parts[1];
        displayCreatedTimestamp = parseDate((new Date()).getTime());
        var targetIndex = -1;
        for (i = 0; i < $scope.rentals.length; i++) {
            if ($scope.rentals[i].id == data.id) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex != -1) {
            $scope.rentals.splice(targetIndex, 1);
        }
        $scope.rentals.unshift({'id':data.id,'displayAddress':displayAddress,'displayCreatedTimestamp':displayCreatedTimestamp});
        $scope.alertSuccessInfo = 'Rental at ' + data.address +' has been updated.';
        $scope.alertSuccessShow = true;
        $window.scrollTo(0, 0);
    });
    $scope.$on('dashRentalFail', function(event, msg) {
        $scope.alertFailureInfo = msg;
        $scope.alertFailureShow = true;
        $window.scrollTo(0, 0);
    });
    $scope.updateUser = function() {
        if ($scope.name == undefined || $scope.name == "") {
            $scope.alertFailureInfo = "User name cannot be null!";
            $scope.alertFailureShow = true;
            $window.scrollTo(0, 0);
            return;
        }
        if ($scope.wechat == undefined) {
            $scope.wechat = "";
        }
        if ($scope.phone == undefined) {
            $scope.phone = "";
        }
        if ($scope.name == originalUserName && $scope.wechat == originalWechatId && $scope.phone == originalPhone) {
            return;
        }
        data = {id:$scope.id, name:$scope.name, email:$scope.email, phone:$scope.phone, wechat:$scope.wechat};
        console.log(data);
        $http({ method: 'PUT',
                url: config.serverUrl + '/api/private/user',
                data: data})
        .then(function successCallback(response) {
            $scope.alertSuccessInfo = 'User info has been updated.';
            $scope.alertSuccessShow = true;
            $window.scrollTo(0, 0);
        }, function errorCallback(response) {
            $scope.alertFailureInfo = "Failed to update user info. Please retry later.";
            $scope.alertFailureShow = true;
            $window.scrollTo(0, 0);
        });
    }
})

.directive("imagereader", ['$http', '$q', 'NgMap', '$window', 'config', '$routeParams', '$route', 'dataSharedService',
                            function ($http, $q, NgMap, $window, config, $routeParams, $route, dataSharedService) {
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
            scope.init = function() {
                scope.mode = 'new';
                var posterHashids = new Hashids("SKYLINE_POSTER");
                $http.get("https://ipinfo.io")
                .then(function(response) {
                    ipinfo = response.data;
                    scope.rentalId = 'R' + posterHashids.encode(ip2int(ipinfo.ip), Date.now());
                    console.log(scope.rentalId);
                });
                scope.existedImages = new Set();
                scope.deletedImages = [];
                scope.rental = undefined;
                scope.inputPrice = undefined;
                scope.address = undefined;
                scope.lat = undefined;
                scope.lng = undefined;
                scope.inputMoveInDate = undefined;
                scope.inputMoveOutDate = undefined;
                scope.description = undefined;
                scope.selectedBedroom = undefined;
                scope.selectedBathroom = undefined;
                scope.selectedQuantifier = "0";
                scope.images = [];
            }
            scope.$on('dashRentalClear', function() {
                scope.init();
            });
            scope.$on('dashRentalGet', function(event, data) {
                scope.mode = 'edit';
                scope.existedImages = new Set();
                scope.deletedImages = [];
                rentalObj = data;
                scope.rental = rentalObj;
                scope.rentalId = rentalObj.id;
                scope.inputPrice = parseFloat(rentalObj.price);
                scope.address = rentalObj.address;
                scope.lat = parseFloat(rentalObj.latitude);
                scope.lng = parseFloat(rentalObj.longitude);

                // scope.map.setCenter({'lat':scope.lat, 'lng':scope.lng});
                // scope.markerShown = true;

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
                var imageIds = rentalObj.imageIds;
                scope.images = [];
                for (i = 0; i < imageIds.length; i++) {
                    scope.images.push({});
                }
                for (i = 0; i < imageIds.length; i++) {
                    scope.existedImages.add(imageIds[i]);
                    imageUrl = config.googleCloudStorageBaseUrl + '/' + config.googleCloudStorageBucket + '/' + imageIds[i].trim();
                    toBase64(i, imageUrl, function(imageIndex, base64){
                        scope.$apply(function() {
                            scope.images[imageIndex] = {'id': imageIds[imageIndex].trim(), 'base64': base64, 'type': 'jpeg'};
                        });
                    });
                }
            });
            scope.removeImage = function(index) {
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
                if (scope.mode == 'new') {
                    $http({ method: 'POST',
                        url: config.serverUrl + '/api/private/rental',
                        data: data
                    }).then(function(response) {
                        if (response.status == 201) {
                            gcstoken = response.data;
                            scope.uploadImages(gcstoken, scope.images, function() {
                                dataSharedService.addRental(data);
                            });
                        } else {
                            dataSharedService.alertFailure('There was an error creating the rental at ' + data.address);
                        }
                    });
                } else if (scope.mode == 'edit') {
                    $http({ method: 'PUT',
                        url: config.serverUrl + '/api/private/rental/' + data.id,
                        data: data
                    }).then(function(response) {
                        if (response.status == 200) {
                            gcstoken = response.data;
                            scope.uploadImages(gcstoken, scope.images, function() {
                                scope.deleteImages(gcstoken, scope.deletedImages, function(){
                                    dataSharedService.editRental(data);
                                });
                            });
                        } else if (response.status == 202) {
                            dataSharedService.alertFailure('There was an error updating the rental at ' + data.address);
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
                scope.markerShown = true;
            }

            scope.init();
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