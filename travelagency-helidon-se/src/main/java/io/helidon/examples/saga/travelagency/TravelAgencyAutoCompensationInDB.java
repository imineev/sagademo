package io.helidon.examples.saga.travelagency;

public class TravelAgencyAutoCompensationInDB extends TravelAgencyCommon {

    OracleConnection oracleConnection = new OracleConnection();

    public TravelAgencyAutoCompensationInDB(String sagaid) throws Exception {
        super(sagaid);
    }

    void initMessagingMethods() {
    }

    String processTripBookingRequest() {
        String bookingstate = "none";
        try {
            beginSaga();
            bookEventTickets();
            bookFlight();
            bookHotel();
            commitSaga();
            bookingstate = "success";
        } catch (Exception exception) {
            abortSaga(); // Automatic compensation by the system
            bookingstate = "fail" ;
        } finally {
            return bookingstate;
        }
    }

    private void beginSaga() {
        sagaId = oracleConnection.beginSaga();
    }

    private void bookEventTickets() {

    }

    private void bookFlight() {

    }

    private void bookHotel() {

    }

    private void commitSaga() {

    }

    private void abortSaga() {
        oracleConnection.abortSaga();
    }



}
