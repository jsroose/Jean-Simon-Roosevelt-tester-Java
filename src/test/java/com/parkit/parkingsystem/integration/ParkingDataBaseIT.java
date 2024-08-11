package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.model.ParkingSpot;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Timestamp;
import java.lang.Thread;
import com.parkit.parkingsystem.constants.Fare;
import java.text.DecimalFormat;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @Mock
    private static ParkingDataBaseIT parkingDataBaseIT;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        ticketDAO = new TicketDAO();
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
    	when(inputReaderUtil.readSelection()).thenReturn(1);
    	lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability

        DataBaseConfig dataBaseConfig = new DataBaseConfig();
        Logger logger = LogManager.getLogger("ParkingDataBaseIT");
        Connection con = null;
        int nbTicket=0;

        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.CHECK_TICKET_PARKING);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1,"ABCDEF");
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
            	nbTicket = rs.getInt(1);
                System.out.println("rs.getInt(1): " + rs.getInt(1));
            }

            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }

        System.out.println("testParkingACar nbTicket: " + nbTicket);
        assertEquals(1, nbTicket);

    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        try {
        	  Thread.sleep(1000);
        	} catch (InterruptedException e) {
        	  Thread.currentThread().interrupt();
        	}

        parkingService.processExitingVehicle();
        //TODO: check that the fare generated and out time are populated correctly in the database

        Date outTime = new Date();
        Date outTimeTest = new Date();
        double fare =0;
        DataBaseConfig dataBaseConfig = new DataBaseConfig();
        Logger logger = LogManager.getLogger("ParkingDataBaseIT");
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1,"ABCDEF");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
            	outTime = rs.getTimestamp(5);
            	fare = rs.getDouble(3);
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }

        fare = (double) Math.round(fare * 100) / 100;
        double carRate = (double) Math.round(Fare.CAR_RATE_PER_HOUR * 0.95 * 100) / 100;

        assertEquals(carRate, fare);
        assertEquals(outTime.getHours(), outTimeTest.getHours());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    	parkingService.processIncomingVehicle();

        try {
        	  Thread.sleep(1000);
        	} catch (InterruptedException e) {
        	  Thread.currentThread().interrupt();
        	}

        parkingService.processExitingVehicle();

    	parkingService.processIncomingVehicle();

        try {
        	  Thread.sleep(1000);
        	} catch (InterruptedException e) {
        	  Thread.currentThread().interrupt();
        	}

        parkingService.processExitingVehicle();

        double fare =0;
        DataBaseConfig dataBaseConfig = new DataBaseConfig();
        Logger logger = LogManager.getLogger("ParkingDataBaseIT");
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1,"ABCDEF");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
            	fare = rs.getDouble(3);
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }
        fare = (double) Math.round(fare * 100) / 100;
        double carRate = (double) Math.round(Fare.CAR_RATE_PER_HOUR * 0.95 * 100) / 100;

        assertEquals(carRate, fare);
    }
}
