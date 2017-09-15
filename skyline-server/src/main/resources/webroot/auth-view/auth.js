angular.module('skyline-auth', ['ngRoute'])

.controller('authController', function($rootScope, $scope, $http, $window, config) {
    $scope.login = function() {
        data = {email: $scope.loginEmail, password: $scope.loginPassword}
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/login',
                data: data})
        .then(function successCallback(response) {
            $window.location.href = '#/post';
        }, function errorCallback(response) {
            $scope.loginAlertInfo = 'Unable to login. Please confirm account and password.';
            $scope.loginAlertShow = true;
        });
    };
    $scope.sign_up = function() {
        var hashids = new Hashids("SKYLINE_USER");
        var userId = $scope.signUpEmail.split("@")[0].replace(/[^A-Za-z0-9]/g, '') + '-' + hashids.encode(Date.now());
        data = {id: userId, name: $scope.signUpName, email: $scope.signUpEmail}
        console.log(data);
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/signup',
                data: data})
        .then(function successCallback(response) {
            $scope.signUpSuccessInfo = "You'll receive an email shortly, please check it and continue.";
            $scope.signUpSuccessShow = true;
            $scope.signUpFailureShow = false;
        }, function errorCallback(response) {
            $scope.signUpSuccessShow = false;
            if (response.status == 400) {
                $scope.signUpFailureInfo = 'This email has been registered.';
                $scope.signUpFailureShow = true;
            } else {
                $scope.signUpFailureInfo = 'Internal errors, please try again later.';
                $scope.signUpFailureShow = true;
            }
        });
    };
    $scope.submit_account = function() {

    };
    $scope.set_password = function() {

    };
})
