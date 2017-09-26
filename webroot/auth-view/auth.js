angular.module('skyline-auth', ['ngRoute'])

.controller('authController', function($rootScope, $scope, $http, $window, config) {
    $scope.login = function() {
        if (!validateEmail($scope.loginEmail)) {
            $scope.loginAlertInfo = 'Email is not valid. Please confirm.';
            $scope.loginAlertShow = true;
            return;
        }
        if ($scope.loginPassword == undefined || $scope.loginPassword == "") {
            $scope.loginAlertInfo = 'Password should not be empty!';
            $scope.loginAlertShow = true;
            return;
        }
        data = {email: $scope.loginEmail, password: $scope.loginPassword}
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/user/login',
                data: data})
        .then(function successCallback(response) {
            $window.location.href = '#/dashboard';
        }, function errorCallback(response) {
            $scope.loginAlertInfo = 'Unable to login. Please confirm account and password.';
            $scope.loginAlertShow = true;
        });
    };
    $scope.sign_up = function() {
        if (!validateEmail($scope.signUpEmail)) {
            $scope.signUpFailureInfo = 'Email is not valid. Please confirm.';
            $scope.signUpFailureShow = true;
            return;
        }
        if ($scope.signUpName == undefined || $scope.signUpName == "") {
            $scope.signUpFailureInfo = 'User nickname should not be empty!';
            $scope.signUpFailureShow = true;
            return;
        }
        $http.get("https://ipinfo.io").then(function(response) {
            ipinfo = response.data;
            var hashids = new Hashids("SKYLINE_USER");
            userId = 'U' + hashids.encode(ip2int(ipinfo.ip), Date.now());
            console.log(userId);
            data = {id: userId, name: $scope.signUpName, email: $scope.signUpEmail, phone:$scope.signUpPhone, wechat: $scope.signUpWechat}
            $http({ method: 'POST',
                    url: config.serverUrl + '/api/public/user/register',
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
        });
    };
    $scope.submit_account = function() {
        if (!validateEmail($scope.resetEmail)) {
            $scope.resetFailureInfo = 'Email is not valid. Please confirm.';
            $scope.resetFailureShow = true;
            return;
        }
        data = {email: $scope.resetEmail}
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/user/reset',
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
            url: config.serverUrl + '/api/public/user/verify/' + $routeParams.salt})
    .then(function successCallback(response) {
        console.log("Allowed to set password.");
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
        if ($scope.passwordSet == undefined || $scope.passwordConfirm == undefined) {
            $scope.passwdAlertInfo = 'Please fill in both fields';
            $scope.passwdAlertShow = true;
            return;
        }
        if ($scope.passwordSet != $scope.passwordConfirm) {
            $scope.passwdAlertInfo = 'Passwords not matched! Please confirm.';
            $scope.passwdAlertShow = true;
            return;
        }
        if (!validatePassword($scope.passwordSet)) {
            $scope.passwdAlertInfo = 'Please make sure that your password meets the criteria.';
            $scope.passwdAlertShow = true;
            return;
        }
        data = {password1: $scope.passwordSet, password2: $scope.passwordConfirm, type: $routeParams.type, salt:$routeParams.salt};
        $http({ method: 'POST',
                url: config.serverUrl + '/api/public/user/password',
                data: data})
        .then(function successCallback(response) {
            $scope.passwdSuccessInfo = 'Your account has been created, redirecting you to login ...';
            $scope.passwdAlertShow = false;
            $scope.passwdSuccessShow = true;
            setTimeout(function() { $window.location.href = '#/login'; }, 3000);
        }, function errorCallback(response) {
            $scope.passwdAlertInfo = 'Internal errors, please try again later.';
            $scope.passwdAlertShow = true;
        });
    }
})

function validateEmail(email) {
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
}

function validatePassword(password) {
    var re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%\^&\*])(?=.{8,})/
    return re.test(password);
}
