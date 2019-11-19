package io.helidon.examples.saga.travelagency;


import io.helidon.messaging.MessagingClient;
import oracle.jdbc.pool.OracleDataSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

abstract class TravelAgencyCommon {
    protected static final String eventtickets = "eventtickets";
    protected static final String hotel = "hotel";
    protected static final String flight = "flight";
    protected Connection connection = null;
    protected MessagingClient messagingClient;
    protected String sagaid;
    protected String eventticketsstate;
    protected String hotelstate;
    protected String flightstate;
    private org.eclipse.microprofile.config.Config config;
    public static String sagaState = null;
    static String sagaId;
    private OracleDataSource dataSource;

    TravelAgencyCommon(String sagaid) throws Exception  {
        setTravelAgencyConnection();
        messagingClient = MessagingClient.build(createConfig());
        this.sagaid = sagaid;
    }

    void setTravelAgencyConnection() throws SQLException {
        dataSource = new OracleDataSource();
        dataSource.setURL(TravelAgencyService.url);
        dataSource.setUser(TravelAgencyService.user);
        dataSource.setPassword(TravelAgencyService.password);
        connection = dataSource.getConnection();
    }

    protected Config createConfig() {
        return new Config() {
            HashMap<String, String> values = new HashMap<>();
            String connectorName = "aq";
            private void createValues() {
                values.put("mp.messaging.connector."+connectorName+".classname",
                        "io.helidon.messaging.jms.connector.JMSConnector");
                createValuesForChannel(eventtickets, "mp.messaging.incoming.");
                createValuesForChannel(eventtickets, "mp.messaging.outgoing.");
                createValuesForChannel(hotel, "mp.messaging.incoming.");
                createValuesForChannel(hotel, "mp.messaging.outgoing.");
                createValuesForChannel(flight, "mp.messaging.incoming.");
                createValuesForChannel(flight, "mp.messaging.outgoing.");
            }

            private void createValuesForChannel(String channelname, String incomingoroutgoingprefix) {
                values.put(incomingoroutgoingprefix + channelname + ".connector", connectorName);
                values.put(incomingoroutgoingprefix + channelname + ".url", TravelAgencyService.url);
                values.put(incomingoroutgoingprefix + channelname + ".user", TravelAgencyService.user);
                values.put(incomingoroutgoingprefix + channelname + ".password", TravelAgencyService.password);
                values.put(incomingoroutgoingprefix + channelname + ".queue", channelname + "queue");
                if (incomingoroutgoingprefix.equals("mp.messaging.incoming."))
                    values.put(incomingoroutgoingprefix + channelname + ".selector",
                            "action = '" + TravelAgencyService.BOOKINGSUCCESS + "' OR " +
                            "action = '" + TravelAgencyService.BOOKINGFAIL + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPLETESUCCESS + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPLETEFAIL + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPENSATESUCCESS + "' OR " +
                            "action = '" + TravelAgencyService.SAGACOMPENSATEFAIL + "' "
                            );
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

    abstract String processTripBookingRequest();


}
