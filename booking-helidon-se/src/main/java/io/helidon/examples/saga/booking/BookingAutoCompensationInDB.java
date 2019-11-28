package io.helidon.examples.saga.booking;

import io.helidon.messaging.MessagingClient;
import java.sql.Connection;
import java.sql.SQLException;

public class BookingAutoCompensationInDB extends BookingCommon {

    private MessagingClient messagingClient;

    public BookingAutoCompensationInDB() throws Exception {
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
        }
        return BookingService.UNKNOWN;
    }

    void updateDataInReactionToMessage(io.helidon.messaging.Connection connection, String sagastate) throws SQLException {
        java.sql.Connection jdbcConnection =
                (java.sql.Connection) connection.unwrap(java.sql.Connection.class);
        switch (sagastate) { // only booking requests are applicable/necessary in in-db auto-compensation case
            case BookingService.BOOKINGSUCCESS:
                jdbcConnection.createStatement().execute(
                        "update " + BookingService.serviceName + " set inventorycount = 0");
                break;
        }
    }

}
