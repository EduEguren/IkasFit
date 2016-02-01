
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
Parse.Cloud.define("hello", function(request, response) {
  response.success("Hello world!");
});



var PasosDia = Parse.Object.extend("PasosDia");

Parse.Cloud.beforeSave("PasosDia", function(request, response) {
    var query = new Parse.Query(PasosDia);
    query.equalTo("StartDate", request.object.get("StartDate"));
	query.equalTo("Usuario", request.object.get("Usuario"));
    query.first().then(function(existingObject) {
      if (existingObject) {
		response.error("Existing object");
      } else {
        response.success();
      }
    }, function (error) {
      response.error("Error performing checks or saves.");
    });
});