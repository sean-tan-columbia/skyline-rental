var client = angular.module("skyline-client", ['ngRoute'])

.controller('rentalListController', function($scope, $http) {
    $http.get("http://localhost:8080/discover")
    .then(function(r1) {
        rentalIds = r1.data
        $scope.rentals = []
        for (i = 0; i < rentalIds.length; i++) {
            $http.get("http://localhost:8080/rental/" + rentalIds[i])
            .then(function(r2) {
                rentalObj = r2.data
                rentalObj.imageIds = rentalObj.imageIds.substring(1, rentalObj.imageIds.length-1).split(", ")
                rentalObj.price = Math.floor(rentalObj.price)
                rentalObj.quantifier = rentalObj.quantifier.toLowerCase()
                $scope.rentals.push(rentalObj)
                console.log(rentalObj)
            })
        }
    })
})
