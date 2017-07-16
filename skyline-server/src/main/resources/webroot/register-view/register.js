angular.module('skyline-register', ['ngRoute'])

.controller('registerController', function($rootScope, $scope, $http, $window, config) {
    $scope.register = function() {
        data = {username: $scope.username, password: $scope.password, email: $scope.email}
        console.log(data);
        $http({ method: 'POST',
                             url: config.serverUrl + '/api/public/register',
                             data: data})
                     .then(function successCallback(response) {
                         $window.location.href = '/';
                     }, function errorCallback(response) {
                         console.log("Fail");
                     });
    }
})