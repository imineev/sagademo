package io.helidon.examples.saga.booking;


import io.helidon.messaging.MessagingClient;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.Message;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

public class BookingAutoCompensationInDB extends BookingCommon {
    private MessagingClient messagingClient;
    private String sagaid;
    private String sagaState;
    private String eventticketsstate;
    private String hotelstate;
    private String flightstate;
    private static final String BOOKINGREQUESTED = "BOOKINGREQUESTED",
            BOOKINGFAIL = "BOOKINGFAIL", BOOKINGSUCCESS = "BOOKINGSUCCESS",
            SAGACOMPLETEREQUESTED = "SAGACOMPLETEREQUESTED",
            SAGACOMPLETEFAIL = "SAGACOMPLETEFAIL", SAGACOMPLETESUCCESS = "SAGACOMPLETESUCCESS",
            SAGACOMPENSATEREQUESTED = "SAGACOMPENSATEREQUESTED",
            SAGACOMPENSATEFAIL = "SAGACOMPENSATEFAIL", SAGACOMPENSATESUCCESS = "SAGACOMPENSATESUCCESS";



    public BookingAutoCompensationInDB() {
        messagingClient = MessagingClient.build(createConfig());
//        setupMessaging();
    }
    private Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap<>();
            String connectorName = "aq";
            private void createValues() {
                values.put("mp.messaging.connector."+connectorName+".classname",
                        "io.helidon.messaging.jms.connector.JMSConnector");
                createValuesForChannel("eventticketing", "mp.messaging.incoming.");
                createValuesForChannel("eventticketing", "mp.messaging.outgoing.");
                createValuesForChannel("hotel", "mp.messaging.incoming.");
                createValuesForChannel("hotel", "mp.messaging.outgoing.");
                createValuesForChannel("flight", "mp.messaging.incoming.");
                createValuesForChannel("flight", "mp.messaging.outgoing.");
            }

            private void createValuesForChannel(String channelname, String incomingoroutgoingprefix) {
                values.put(incomingoroutgoingprefix + channelname + ".connector", connectorName);
                values.put(incomingoroutgoingprefix + channelname + ".url", BookingService.url);
                values.put(incomingoroutgoingprefix + channelname + ".user", BookingService.user);
                values.put(incomingoroutgoingprefix + channelname + ".password", BookingService.password);
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

    public Message receiveBookingRequest (
            Message message, Connection jdbcConnectionFromAQSession, Session jmsSession) throws JMSException {
        // receive message ...
        TextMessage textMessage = (TextMessage)message.unwrap(TextMessage.class);
        String sagaId = textMessage.getStringProperty("sagaid");
        String action = textMessage.getStringProperty("action");
        System.out.println("BookingAutoCompensationInDB.receiveMessageDoDBWorkSendResponse " +
                "sagaId:" + sagaId + " action:" + action);

        // do DB work
        final String returnValue = bookOperation(jdbcConnectionFromAQSession, sagaId, action);
        System.out.println("BookingAutoCompensationInDB.receiveMessageDoDBWorkSendResponse " +
                "db work complete for sagaId:" + sagaId + " action:" + action + " returnValue:" + returnValue);

        // send message
        return null; // createReplyMessage(jmsSession, sagaId, action, returnValue);
    }


    private String bookOperation(Connection jdbcConnectionFromAQSession, String sagaid, String action) {
        try {
            //adjust inventory
            jdbcConnectionFromAQSession.createStatement().execute(
                    "update table '"+tablename+"' set inventory=0" );
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    // test fail case explicitily
    private String bookFail(Connection jdbcConnectionFromAQSession, String sagaid, String action) {
        return "fail";
    }

}
