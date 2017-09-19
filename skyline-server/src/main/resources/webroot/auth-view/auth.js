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
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/signup',
                data: data})
        .then(function successCallback(response) {
            $scope.signUpSuccessInfo = "You'll receive an email to set password shortly, please check it and continue.";
            $scope.signUpSuccessShow = true;
            $scope.signUpFailureShow = false;
            $scope.signUpDisable = true;
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
        data = {email: $scope.resetEmail}
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/reset',
                data: data})
        .then(function successCallback(response) {
            $scope.resetSuccessInfo = "You'll receive an email to set password shortly, please check it and continue.";
            $scope.resetSuccessShow = true;
            $scope.resetFailureShow = false;
            $scope.resetDisable = true;
        }, function errorCallback(response) {
            $scope.resetSuccessShow = false;
            if (response.status == 404) {
                $scope.resetFailureInfo = 'This email has not been registered yet.';
                $scope.resetFailureShow = true;
            } else {
                $scope.resetFailureInfo = 'Internal errors, please try again later.';
                $scope.resetFailureShow = true;
            }
        });
    };
})

.controller('passwordController', function($rootScope, $scope, $http, $window, $routeParams, config) {
    console.log($routeParams);
    $http({ method: 'GET',
            url: config.serverUrl + '/api/public/verify/' + $routeParams.salt})
    .then(function successCallback(response) {
        console.log("Success");
    }, function errorCallback(response) {
        if (response.status == 404) {
            $scope.passwdAlertInfo = 'Your request has expired.';
            $scope.passwdAlertShow = true;
            $scope.passwdDisabled = true;
        } else {
            $scope.passwdAlertInfo = 'Internal errors, please try again later.';
            $scope.passwdAlertShow = true;
            $scope.passwdDisabled = true;
        }
    });
    $scope.set_password = function() {
        data = {password1: $scope.passwordSet, password2: $scope.passwordConfirm, type: $routeParams.type, salt:$routeParams.salt};
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/password',
                data: data})
        .then(function successCallback(response) {
            console.log("Success");
        }, function errorCallback(response) {
            $scope.passwdAlertInfo = 'Internal errors, please try again later.';
            $scope.passwdAlertShow = true;
        });
    }
})
