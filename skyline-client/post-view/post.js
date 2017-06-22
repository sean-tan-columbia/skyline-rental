angular.module('skyline-post', ['ngRoute'])

.controller('imageUploadController', function ($scope) {
    $scope.posterId = 'jtan';
})

.directive("fileread", ['$http', '$q', function ($http, $q) {
    return {
        restrict: 'E',
        templateUrl: 'post-view/image.html',
        scope: { images: '=', files: '='},
        link: function (scope, element, attributes) {
            element.bind('change', function (changeEvent) {
                var hashids = new Hashids("SKYLINE_IMAGE")
                var reader_show = new FileReader();
                reader_show.onload = function(loadEvent) {
                    scope.$apply(function() {
                        base64 = loadEvent.target.result
                        imageId = scope.id + '-' + hashids.encode(Date.now()) + '.' + getImageType(base64)
                        scope.images.push({'id': imageId, 'base64': base64, 'type': getImageType(base64)});
                    });
                }
                var reader_post = new FileReader();
                reader_post.onload = function(loadEvent) {
                    scope.files.push(new Uint8Array(reader_post.result))
                }
                if (changeEvent.target.files != null) {
                    // document.getElementById('...').files[0] == changeEvent.target.files[0]
                    reader_show.readAsDataURL(changeEvent.target.files[0]);
                    reader_post.readAsArrayBuffer(changeEvent.target.files[0]);
                }
            });
            var posterHashids = new Hashids("SKYLINE_POSTER");
            scope.posterId = 'jtan';
            scope.id = posterHashids.encode(hashCode(scope.posterId), Date.now());
            console.log(scope.id);
            scope.remove = function(index) {
                scope.images.splice(index, 1);
            }
            scope.send = function() {
                // Upload file to Google Cloud Storage
                var oauth2 = 'ya29.GlxxBHhM4EPjKyVwxFMvPdtPRBs3g_ru5-p0CfZ8N5C8I9lMdfGbkhTOH6dIYRfhVplDBmh-0OoWUEKIN1pRlCpXMDU19uj40w8tkk9jSmeocU_2Uyf1Xb-Bvkwx9w';
                if (scope.images.length != scope.files.length) {
                    throw 'Internal error';
                }
                var promises = [];
                baseURL = 'https://storage.googleapis.com';
                bucket = 'skylinerental-static-dev';
                for (i = 0; i < scope.files.length; i++) {
                    var p = $http({
                        // url:'https://storage.googleapis.com/skylinerental-static-dev/test.jpg',
                        url: baseURL + '/' + bucket + '/' + scope.images[i].id,
                        method: 'PUT',
                        headers: {'Authorization': 'Bearer ' + oauth2,
                                  'Content-Type' : 'image/' + scope.images[i].type /*undefined*/ },
                        data: scope.files[i],
                        transformRequest: []
                    });
                    promises.push(p);
                }
                $q.all(promises).then(function(results){
                    results.forEach(function(data,status,headers,config){
                        console.log(data, status, headers, config);
                    })
                    scope.postRental();
                })
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
                    neighborhood : 'Newport',
                    startDate    : '2017-07-01',
                    endDate      : '2017-07-31',
                    imageIds     : imageIds,
                    description  : scope.description,
                };
                $http({ method: 'POST',
                        url: 'http://localhost:8080/rental',
                        data: data
                })
                .then(function(response) {
                    console.log('Post Success!')
                });
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
}
