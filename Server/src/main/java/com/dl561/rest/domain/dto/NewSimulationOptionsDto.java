package com.dl561.rest.domain.dto;

import java.util.List;

public class NewSimulationOptionsDto {
    private String name;
    private int trackNumber;
    private int numberOfLaps;
    private List<VehicleCreationDto> vehiclesToCreate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public List<VehicleCreationDto> getVehiclesToCreate() {
        return vehiclesToCreate;
    }

    public void setVehiclesToCreate(List<VehicleCreationDto> vehiclesToCreate) {
        this.vehiclesToCreate = vehiclesToCreate;
    }

    public int getNumberOfLaps() {
        return numberOfLaps;
    }

    public void setNumberOfLaps(int numberOfLaps) {
        this.numberOfLaps = numberOfLaps;
    }
}
