var hostIP;

function setHostIP(hostIp) {
	hostIP = hostIp;
}

var HttpClient = function () {
	this.get = function (url, callBackFunction) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4 && xhr.status == 200)
				callBackFunction(xhr.responseText);
		}
		xhr.open("GET", url, true);
		xhr.send();
	}
	this.put = function (url, callBackFunction, data) {
		var json = JSON.stringify(data);
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4 && xhr.status == 200)
				callBackFunction(xhr.responseText);
		}
		xhr.open("PUT", url, true);
		xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8');
		xhr.send(json);
	}
	this.post = function (url, callBackFunction, data) {
		var json = JSON.stringify(data);
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4 && xhr.status == 200)
				callBackFunction(xhr.responseText);
		}
		xhr.open("POST", url, true);
		xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8');
		xhr.send(json);
	}
}

function doResetSimulation(callBackFunction) {
	var simulationDataDto = new Object();
	var client = new HttpClient();
	client.put('http://' + hostIP + ':8080/simulation', callBackFunction, simulationDataDto);
}

function doStartSimulation(callBackFunction, simulationId) {
	var client = new HttpClient();
	client.get('http://' + hostIP + ':8080/simulation/' + simulationId + '/start', callBackFunction);
}

function doStopSimulation(callBackFunction) {
	var client = new HttpClient();
	client.get('http://' + hostIP + ':8080/simulation/' + simulationId + '/stop', callBackFunction);
}

function doNewSimulation(callBackFunction) {
	var simulationDataDto = new Object();
	var client = new HttpClient();
	client.put('http://' + hostIP + ':8080/simulation', callBackFunction, simulationDataDto);
}

function doNewSimulationByOptions(callBackFunction, name, trackNumber, vehiclesToCreate, numberOfLaps) {
	var newSimulationOptionsDto = new Object();
	newSimulationOptionsDto.name = name;
	newSimulationOptionsDto.trackNumber = trackNumber;
	newSimulationOptionsDto.vehiclesToCreate = vehiclesToCreate;
	newSimulationOptionsDto.numberOfLaps = numberOfLaps;
	var client = new HttpClient();
	client.put('http://' + hostIP + ':8080/simulation/options', callBackFunction, newSimulationOptionsDto);
}

function doFetchExample(callBackFunction) {
	var client = new HttpClient();
	client.get('http://' + hostIP + ':8080/example/', callBackFunction);
}

function doFetch(callBackFunction, simulationId) {
	var client = new HttpClient();
	client.get('http://' + hostIP + ':8080/simulation/' + simulationId, callBackFunction);
}

function doUpdateVehicle(vehicleUpdateDto, simulationId, vehicleId, callBackFunction) {
	var client = new HttpClient();
	client.post('http://' + hostIP + ':8080/simulation/' + simulationId + '/vehicle/' + vehicleId, callBackFunction, vehicleUpdateDto);
}

function doSearchSimulationIds(callBackFunction){
	var client = new HttpClient();
	client.get('http://' + hostIP + ':8080/simulation/search', callBackFunction);
}
