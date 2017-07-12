angular.module('skyline-post', ['ngRoute', 'ngMap'])

.controller('imageUploadController', function ($scope) {
})

.directive("fileread", ['$http', '$q', 'NgMap', '$window', 'config', function ($http, $q, NgMap, $window, config) {
    return {
        restrict: 'E',
        templateUrl: 'post-view/image.html',
        scope: { images: '=', files: '='},
        link: function (scope, element, attributes) {
            element.bind('change', function (changeEvent) {
                var hashids = new Hashids("SKYLINE_IMAGE")
                var reader = new FileReader();
                reader.onload = function(loadEvent) {
                    base64 = loadEvent.target.result;
                    imageType = getImageType(base64);
                    imageId = scope.id + '-' + hashids.encode(Date.now()) + '.' + imageType;

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
            var posterHashids = new Hashids("SKYLINE_POSTER");
            scope.posterId = 'jtan';
            scope.id = posterHashids.encode(hashCode(scope.posterId), Date.now());
            console.log(scope.id);
            scope.remove = function(index) {
                scope.images.splice(index, 1);
            }
            scope.getAccessToken = function(callback) {
                $http({ method: 'GET',
                        url: config.serverUrl + '/gcstoken',
                })
                .then(function(response) {
                    console.log(response.data)
                    callback(response.data);
                });
            }
            scope.upload = function(oauth2) {
                if (scope.images.length != scope.files.length) {
                    throw 'Internal error';
                }
                var promises = [];
                for (i = 0; i < scope.files.length; i++) {
                    var p = $http({
                        // url:'https://storage.googleapis.com/skylinerental-static-dev/test.jpg',
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
                    /*
                    results.forEach(function(data,status,headers,config){
                        console.log(data, status, headers, config);
                    })*/
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
                    id           : scope.id,
                    posterId     : scope.posterId,
                    price        : 1100,
                    quantifier   : 'MONTH',
                    rentalType   : 'BEDROOM',
                    address      : scope.address,
                    lat          : scope.lat,
                    lng          : scope.lng,
                    neighborhood : 'Newport',
                    startDate    : '2017-07-01',
                    endDate      : '2017-07-31',
                    imageIds     : imageIds,
                    description  : scope.description,
                };
                console.log(data);
                $http({ method: 'POST',
                        url: config.serverUrl + '/rental',
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
                console.log('lat:' + scope.place.geometry.location.lat());
                console.log('lng:' + scope.place.geometry.location.lng());
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
