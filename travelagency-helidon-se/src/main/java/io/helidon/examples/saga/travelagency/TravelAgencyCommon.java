package io.helidon.examples.saga.travelagency;


import io.helidon.messaging.IncomingMessagingService;
import io.helidon.messaging.MessageWithConnectionAndSession;
import io.helidon.messaging.MessagingClient;
import oracle.jdbc.pool.OracleDataSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.jms.Message;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

abstract class TravelAgencyCommon {
    MessagingClient messagingClient;
    String sagaid;
    static String sagaState = null;
    String eventticketsstate = "unknown";
    String hotelstate = "unknown";
    String flightstate = "unknown";
    static String sagaId;
    Connection connection;
    Properties configProps;
    String bookingstate;

    TravelAgencyCommon(String sagaid) throws Exception  {
        setConfig();
        if (!TravelAgencyService.IS_AUTO_COMPENSATING_DB) setTravelAgencyConnection();
        messagingClient = MessagingClient.build(createConfig());
        this.sagaid = sagaid;
        setupIncomingMessaging();
    }


    public Properties setConfig() throws Exception {
        configProps = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("microprofile-config.properties");
        configProps.load(inputStream);
        return configProps;
    }

    //todo this would fail without sysprops currently
    private void setTravelAgencyConnection() throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(TravelAgencyService.url);
        dataSource.setUser(TravelAgencyService.user);
        dataSource.setPassword(TravelAgencyService.password);
        connection = dataSource.getConnection();
    }

    private Config createConfig() throws Exception {
        return new Config() {
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                return (T) configProps.get(propertyName);
            }
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                return Optional.empty();
            }
            public Iterable<String> getPropertyNames() {
                return configProps.stringPropertyNames();
            }
            public Iterable<ConfigSource> getConfigSources() {
                return null;
            }
        };
    }


    // Above is common config and channel setup. Below is trip booking/saga logic...


    void setupIncomingMessaging() {
        IncomingMessagingService incomingMessagingService =
                (message, connection, session) -> {
                    MessageWithConnectionAndSession messageWithConnectionAndSession =
                            (MessageWithConnectionAndSession) message.unwrap(Message.class);
                    try { // does function receive_response(saga_id, recipient, sender, timeout) return JSON;
                        Message jmsMessage =
                                (javax.jms.Message)messageWithConnectionAndSession.getMessage(javax.jms.Session.class);
                        String sagaid = jmsMessage.getStringProperty("sagaid");
                        String service = jmsMessage.getStringProperty("service");
                        String action = jmsMessage.getStringProperty("action");
                        System.out.println("AQ IncomingMessagingService.onProcessing " +
                                "sagaid:" + sagaid + "message:" + message +
                                " bookingService:" + service + " action/reply:" + action);
                        updateDataInReactionToMessage(
                                ((java.sql.Connection)connection.unwrap(java.sql.Connection.class)), service, action);
                        switch (service) {
                            case TravelAgencyService.EVENTTICKETS:
                                eventticketsstate = action;
                                break;
                            case TravelAgencyService.HOTEL:
                                hotelstate = action;
                                break;
                            case TravelAgencyService.FLIGHT:
                                flightstate = action;
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.EVENTTICKETS,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.HOTEL,
                null, true);
        messagingClient.incoming(incomingMessagingService, TravelAgencyService.FLIGHT,
                null, true);

    }

    abstract String processTripBookingRequest();

    abstract boolean beginSaga() throws SQLException;

    abstract void sendMessageToBookingService(String bookingService, String action);

    abstract void updateDataInReactionToMessage(Connection connection, String service, String action) throws SQLException;

    boolean allParticipantsReplySuccessfully(String successstate, String failstate, int secondsToWait) {
        long timeMillis = System.currentTimeMillis();
        while (System.currentTimeMillis() - timeMillis < secondsToWait * 1000) {
            try {
                System.out.println("Participant reply status... " +
                        " eventticketsstate:" + eventticketsstate +
                        " hotelstate:" + hotelstate +
                        " flightstate:" + flightstate);
                    if(successstate.equals(eventticketsstate)
                            && successstate.equals(hotelstate)
                            && successstate.equals(flightstate)) {
                        return true;
                    } else if(failstate.equals(eventticketsstate)
                            || failstate.equals(hotelstate)
                            || failstate.equals(flightstate)) {
                        return false;
                    }
                Thread.sleep(1 * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }



}
