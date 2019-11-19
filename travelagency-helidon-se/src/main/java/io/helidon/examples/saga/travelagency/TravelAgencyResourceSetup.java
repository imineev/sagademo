package io.helidon.examples.saga.travelagency;

import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessagingClient;
import oracle.AQ.AQQueueTable;
import oracle.AQ.AQQueueTableProperty;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsDestinationProperty;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.jms.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

public class TravelAgencyResourceSetup {
    private OracleDataSource dataSource;
    Connection connection;


    TravelAgencyResourceSetup() throws SQLException {
        dataSource = new OracleDataSource();
        dataSource.setURL(TravelAgencyService.url);
        dataSource.setUser(TravelAgencyService.user);
        dataSource.setPassword(TravelAgencyService.password);
        connection = dataSource.getConnection();
    }

    void createAll() throws Exception {
        createTravelAgencyTables();
        createBookingTables();
        createQueues();
    }

    private void createBookingTables() throws Exception {
        createBookingTablesForService( "eventtickets");
        createBookingTablesForService( "hotel");
        createBookingTablesForService( "flight");
    }

    private void createBookingTablesForService(String tablename) throws SQLException {
        connection.createStatement().execute(
                "create table " + tablename + " (inventoryid varchar(32),  inventorycount INTEGER)");
        connection.createStatement().execute(
                "create table " + tablename + "journal (sagaid varchar(32), sagastate varchar(32), " +
                        "inventoryid varchar(32), originalinventorycount INTEGER)");
    }

    private void createTravelAgencyTables() throws Exception {
        connection.createStatement().execute(
                "create table travelagency (sagaid varchar(32), sagastate varchar(32), " +
                        "eventticketsstate varchar(32), hotelstate varchar(32), flightstate varchar(32))");
    }

    private void createQueues() throws Exception {
        createQueue( "aquser1", "eventticketsqueue", "text");
        createQueue( "aquser1", "hotelqueue", "text");
        createQueue( "aquser1", "flightqueue", "text");
    }

    private Object createQueue(String queueOwner, String queueName, String msgType) throws Exception {
        System.out.println("create queue queueName:" + queueName);
        QueueConnectionFactory q_cf = AQjmsFactory.getQueueConnectionFactory(dataSource);
        QueueConnection q_conn = q_cf.createQueueConnection();
        Session session = q_conn.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
        AQQueueTable q_table = null;
        AQQueueTableProperty qt_prop =
                new AQQueueTableProperty(
                        msgType.equals("map") ? "SYS.AQ$_JMS_MAP_MESSAGE":"SYS.AQ$_JMS_TEXT_MESSAGE" )  ;
        q_table = ((AQjmsSession) session).createQueueTable(queueOwner, queueName, qt_prop);
        Queue queue = ((AQjmsSession) session).createQueue(q_table, queueName, new AQjmsDestinationProperty());
        System.out.println("create queue successful for queue:" + queue.toString());
        return "createOrderQueue successful for queue:" + queue.toString();
    }



    //cleanAll...
    MessagingClient messagingClient;

    void cleanAll() throws Exception {
        messagingClient = MessagingClient.build(createConfig());
        cleanQueues();
        cleanTables();
        System.out.println("wait 30 seconds for queue messages");
        Thread.sleep(1000 * 30);
    }

    private void cleanTables() throws SQLException {
        cleanTable( "eventtickets");
        cleanTable( "hotel");
        cleanTable( "flight");
        System.out.println("TravelAgencyResourceSetup.cleanTable tablename = [travelagency]...");
        connection.createStatement().execute( "delete  travelagency"  );
    }

    private void cleanTable(String tablename) throws SQLException {
        System.out.println("TravelAgencyResourceSetup.cleanTable tablename = [" + tablename + "]...");
        connection.createStatement().execute(
                "delete  " + tablename );
        connection.createStatement().execute(
                "delete  " + tablename + "journal" );
    }

    void cleanQueues() {
        IncomingMessagingService incomingMessagingService =
                new IncomingMessagingService() {
                    @Override
                    public void onIncoming(org.eclipse.microprofile.reactive.messaging.Message message, Connection connection, Session session) {
                        System.out.println("no-op cleaning queue, message = [" + message.getPayload() + "]");
                    }
                };
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.eventtickets,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.hotel,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.flight,
                null, true);
    }

    private Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap<>();
            String connectorName = "aq";
            private void createValues() {
                values.put("mp.messaging.connector."+connectorName+".classname",
                        "io.helidon.messaging.jms.connector.JMSConnector");
                createValuesForChannel(TravelAgencyService.eventtickets, "mp.messaging.incoming.");
                createValuesForChannel(TravelAgencyService.hotel, "mp.messaging.incoming.");
                createValuesForChannel(TravelAgencyService.flight, "mp.messaging.incoming.");
            }

            private void createValuesForChannel(String channelname, String incomingoroutgoingprefix) {
                values.put(incomingoroutgoingprefix + channelname + ".connector", connectorName);
                values.put(incomingoroutgoingprefix + channelname + ".url", TravelAgencyService.url);
                values.put(incomingoroutgoingprefix + channelname + ".user", TravelAgencyService.user);
                values.put(incomingoroutgoingprefix + channelname + ".password", TravelAgencyService.password);
                values.put(incomingoroutgoingprefix + channelname + ".queue", channelname + "queue");

            }

            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                createValues();
                return (T) values.get(propertyName);
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                createValues();
                return Optional.empty();
            }

            @Override
            public Iterable<String> getPropertyNames() {
                createValues();
                return values.keySet();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };

    }


}
