angular.module('skyline-login', ['ngRoute'])

.controller('loginController', function($rootScope, $scope, $http, $window, config) {
    $scope.login = function() {
        data = {username: $scope.username, password: $scope.password}
        console.log(data);
        $http({ method: 'POST',
                url: config.serverUrl + '/login',
                data: data})
        .then(function successCallback(response) {
            $window.location.href = '/';
        }, function errorCallback(response) {
            console.log("Fail");
        });
    }
})