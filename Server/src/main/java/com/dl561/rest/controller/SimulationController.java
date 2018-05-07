package com.dl561.rest.controller;

import com.dl561.rest.domain.dto.*;
import com.dl561.rest.service.ISimulationService;
import com.dl561.simulation.Simulation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", allowCredentials = "true", allowedHeaders = "*")
@RestController
@ComponentScan
public class SimulationController {

    private final ISimulationService simulationService;
    private static final String JSON_RESULT = "application/json";

    @Autowired
    public SimulationController(ISimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @RequestMapping(value = "/simulation", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<List<SimulationDto>> getAllSimulations() {
        return new ResponseEntity<>(simulationService.getAllSimulations(), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/{simulation_id}", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<SimulationDto> getSimulationById(@PathVariable("simulation_id") Integer simulationId) {
        return new ResponseEntity<>(simulationService.getSimulation(simulationId), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation", method = RequestMethod.PUT, produces = JSON_RESULT)
    public ResponseEntity<SimulationDto> createSimulation(@RequestBody Simulation simulation) {
        return new ResponseEntity<>(simulationService.createSimulation(simulation), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/options", method = RequestMethod.PUT, produces = JSON_RESULT)
    public ResponseEntity createSimulationByOptions(@RequestBody NewSimulationOptionsDto newSimulationOptionsDto) {
        return new ResponseEntity<>(simulationService.createSimulation(newSimulationOptionsDto), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/{simulation_id}/start", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<SimulationDto> startSimulation(@PathVariable("simulation_id") Integer simulationId) {
        return new ResponseEntity<>(simulationService.startSimulation(simulationId), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/{simulation_id}/stop", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<SimulationDto> stopSimulation(@PathVariable("simulation_id") Integer simulationId) {
        return new ResponseEntity<>(simulationService.stopSimulation(simulationId), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/{simulation_id}/vehicles", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<List<VehicleDto>> getAllVehicles(@PathVariable("simulation_id") Integer simulationId) {
        return new ResponseEntity<>(simulationService.getAllVehicles(simulationId), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/{simulation_id}/vehicle/{vehicle_id}", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable("simulation_id") Integer simulationId, @PathVariable("vehicle_id") Integer vehicleId) {
        return new ResponseEntity<>(simulationService.getVehicleById(simulationId, vehicleId), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/{simulation_id}/vehicle/{vehicle_id}", method = RequestMethod.POST, produces = JSON_RESULT)
    public ResponseEntity<VehicleDto> updateVehicleById(@PathVariable("simulation_id") Integer simulationId, @PathVariable("vehicle_id") Integer vehicleId, @RequestBody VehicleUpdateDto vehicleUpdateDto) {
        return new ResponseEntity<>(simulationService.updateVehicle(simulationId, vehicleId, vehicleUpdateDto), HttpStatus.OK);
    }

    @RequestMapping(value = "/example", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<SimulationDto> exampleData() {
        return new ResponseEntity<>(simulationService.exampleSimulationData(), HttpStatus.OK);
    }

    @RequestMapping(value = "/simulation/search", method = RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity<List<SimulationSearchResponseDto>> searchForSimulations() {
        System.out.println("Searching for simulations");
        return new ResponseEntity<>(simulationService.searchForSimulations(), HttpStatus.OK);
    }

    @RequestMapping(value="/wipe", method=RequestMethod.GET, produces = JSON_RESULT)
    public ResponseEntity wipe(){
        System.out.println("Resetting");
        return new ResponseEntity(simulationService.wipe(), HttpStatus.OK);
    }
}
