package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.constants.ParkingType;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        double duration = (outHour / 1000 / 3600) - (inHour / 1000 / 3600);

        ParkingType parkingType = ticket.getParkingSpot().getParkingType();
        double ratePerHour;
        double priceDiscount;

        if (parkingType != ParkingType.CAR && parkingType != ParkingType.BIKE ) {
        	throw new NullPointerException("Unkown Parking Type");
        }

        if ( duration < 0.5) {
        	ticket.setPrice(0);
        } else {
        	ratePerHour = (parkingType == ParkingType.CAR) ? Fare.CAR_RATE_PER_HOUR : Fare.BIKE_RATE_PER_HOUR;
        	priceDiscount = (discount) ? 0.95 : 1;
        	ticket.setPrice(duration * ratePerHour * priceDiscount);
        }
    }

    public void calculateFare(Ticket ticket) {
    	FareCalculatorService fareCalculatorService = new FareCalculatorService();
    	fareCalculatorService.calculateFare(ticket, false);
    }
}