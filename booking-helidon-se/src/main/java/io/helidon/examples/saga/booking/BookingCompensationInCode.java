package io.helidon.examples.saga.booking;

import java.sql.Connection;
import java.sql.SQLException;

public class BookingCompensationInCode extends BookingCommon {

    public BookingCompensationInCode() throws Exception {
        super();
    }

    String processIncomingMessage(io.helidon.messaging.Connection connection, String action, boolean isFailTest) throws SQLException {
        switch (action) {
            case BookingService.BOOKINGREQUESTED:
                if (isFailTest) {
                    return BookingService.BOOKINGFAIL;
                }
                updateDataInReactionToMessage(connection, BookingService.BOOKINGSUCCESS);
                return BookingService.BOOKINGSUCCESS;
            case BookingService.SAGACOMPLETEREQUESTED:
                if (isFailTest) {
                    return BookingService.SAGACOMPLETEFAIL;
                }
                updateDataInReactionToMessage(connection, BookingService.SAGACOMPLETESUCCESS);
                return BookingService.SAGACOMPLETESUCCESS;
            case BookingService.SAGACOMPENSATEREQUESTED:
                if (isFailTest) {
                    return BookingService.SAGACOMPENSATEFAIL;
                }
                updateDataInReactionToMessage(connection, BookingService.SAGACOMPLETESUCCESS);
                return BookingService.SAGACOMPENSATESUCCESS;
        }
        return BookingService.UNKNOWN;
    }

    void updateDataInReactionToMessage(io.helidon.messaging.Connection connection, String sagastate) throws SQLException {
        // concurrent access of inventory - escrow functionality makes this extra journal table, etc. logic unnecessary...
        java.sql.Connection jdbcConnection =
                (java.sql.Connection) connection.unwrap(java.sql.Connection.class);
        switch (sagastate) {
            case BookingService.BOOKINGSUCCESS:
                jdbcConnection.createStatement().execute(
                        "update " + BookingService.serviceName + " set inventorycount = 0");
                break;
            case BookingService.SAGACOMPLETESUCCESS:
                jdbcConnection.createStatement().execute(
                        "delete " + BookingService.serviceName + BookingService.JOURNAL);
                break;
            case BookingService.SAGACOMPENSATESUCCESS:
                jdbcConnection.createStatement().execute(
                        "update " + BookingService.serviceName + " set inventorycount = 1");
                break;
        }
        jdbcConnection.createStatement().execute(
                "update " + BookingService.serviceName + BookingService.JOURNAL +
                        "set sagastate = '" + sagastate + "', originalinventorycount = 1");
    }

}