package io.helidon.examples.saga.booking;

import io.helidon.messaging.MessagingClient;
import java.sql.Connection;
import java.sql.SQLException;

public class BookingAutoCompensationInDB extends BookingCommon {

    private MessagingClient messagingClient;

    public BookingAutoCompensationInDB() throws Exception {
        super();
    }

    String processIncomingMessage(Connection connection, String action, boolean isFailTest) throws SQLException {
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

    void updateDataInReactionToMessage(Connection connection, String sagastate) throws SQLException {
        // only booking requests are applicable/necessary in in-db auto-compensation case
        switch (sagastate) {
            case BookingService.BOOKINGSUCCESS:
                connection.createStatement().execute(
                        "update " + BookingService.serviceName + " set inventorycount = 0");
                break;
        }
    }

}
