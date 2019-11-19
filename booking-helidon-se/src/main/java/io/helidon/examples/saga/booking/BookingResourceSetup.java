package io.helidon.examples.saga.booking;

import java.sql.Connection;

public class BookingResourceSetup {

    //todo these arent complete
    private void createQueuesAndTables(Connection connection, String serviceName) throws Exception {
        connection.createStatement().execute(
                "create table " + serviceName + " (sagaid varchar(32), state varchar(32), " +
                        "inventorycount number)");
        connection.createStatement().execute(
                "create table " + serviceName + "journal (sagaid varchar(32), state varchar(32), " +
                        "inventorycountchange number)");
    }

}
