package tcc;

import java.time.Duration;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import tcc.flight.FlightReservationDoc;
import tcc.hotel.HotelReservationDoc;

/**
 * Simple non-transactional client. Can be used to populate the booking services
 * with some requests.
 */
public class TestClient {
	public static void main(String[] args) throws InterruptedException {
		int duration = 100;
		for(int i = 1; i <= 100; i++) {
			Thread.sleep(Duration.ofMillis(100).toMillis());
			System.out.println("Request " + i + "/" + duration);
			try {
				Client client = ClientBuilder.newClient();
				WebTarget target = client.target(TestServer.BASE_URI);

				GregorianCalendar tomorrow = new GregorianCalendar();
				tomorrow.setTime(new Date());
				tomorrow.add(GregorianCalendar.DAY_OF_YEAR, 1);

				// book flight
				WebTarget webTargetFlight = target.path("flight");

				FlightReservationDoc docFlight = new FlightReservationDoc();
				docFlight.setName("Christian");
				docFlight.setFrom("Karlsruhe");
				docFlight.setTo("Berlin");
				docFlight.setAirline("airberlin");
				docFlight.setDate(tomorrow.getTimeInMillis());

				Response responseFlight = webTargetFlight.request().accept(MediaType.APPLICATION_XML)
						.post(Entity.xml(docFlight));
				int flightStatus = responseFlight.getStatus();
				if(flightStatus != 200) {
					System.out.println("Flight reservation failed with HTTP error code: " + responseFlight.getStatus());
				}
				FlightReservationDoc outputFlight = responseFlight.readEntity(FlightReservationDoc.class);
				System.out.println("Flight before Confirmation: " + outputFlight);

				// book hotel
				WebTarget webTargetHotel = target.path("hotel");
				HotelReservationDoc docHotel = new HotelReservationDoc();
				docHotel.setName("Christian");
				docHotel.setHotel("Interconti");
				docHotel.setDate(tomorrow.getTimeInMillis());

				Response responseHotel = webTargetHotel.request().accept(MediaType.APPLICATION_XML)
						.post(Entity.xml(docHotel));
				int hotelStatus = responseHotel.getStatus();
				if(hotelStatus != 200) {
					System.out.println("Hotel reservation failed with HTTP error code: " + responseHotel.getStatus());
				}

				HotelReservationDoc outputHotel = responseHotel.readEntity(HotelReservationDoc.class);
				System.out.println("Hotel before Confirmation: " + outputHotel);

				// Task Implementation
				// put: confirmation successful?
				boolean confirmed = outputFlight.getConfirmed() && outputHotel.getConfirmed();
				// post: reservation expired?
				boolean expired = outputFlight.getExpires() == 0 && outputHotel.getExpires() == 0;
				if(flightStatus == 200 && hotelStatus == 200) {
					while(!confirmed && !expired) {
						// Confirmation
						Response responseConfirmFlight =
								client.target(outputFlight.getUrl()).request().accept(MediaType.TEXT_PLAIN).put(Entity.xml(new FlightReservationDoc()));
						flightStatus = responseConfirmFlight.getStatus();
						Response responseConfirmHotel =
								client.target(outputHotel.getUrl()).request().accept(MediaType.TEXT_PLAIN).put(Entity.xml(new HotelReservationDoc()));
						hotelStatus = responseConfirmHotel.getStatus();
						// Update Success
						confirmed = flightStatus == 200 && hotelStatus == 200;
						if(!confirmed) {
							// Update expired
							expired = outputFlight.getExpires() == 0 && outputHotel.getExpires() == 0;
							System.out.println("Confirmation of flight or hotel failed, initiate retry, reservation expired? " + expired);
						}
						// Get
						// Flight
						Response responseGetFlight =
								client.target(outputFlight.getUrl()).request().accept(MediaType.APPLICATION_XML).get();
						outputFlight = responseGetFlight.readEntity(FlightReservationDoc.class);
						// Hotel
						Response responseGetHotel =
								client.target(outputHotel.getUrl()).request().accept(MediaType.APPLICATION_XML).get();
						outputHotel = responseGetHotel.readEntity(HotelReservationDoc.class);
						System.out.println("Flight after Confirmation: " + outputFlight);
						System.out.println("Hotel after Confirmation: " + outputHotel);
					}
				}
				if(expired || !confirmed) {
					// Cancellation
					// Flight
					if(flightStatus == 200) {
						Response responseCancelFlight =
								client.target(outputFlight.getUrl()).request().accept(MediaType.TEXT_PLAIN).delete();
						int cancelFlightStatus = responseCancelFlight.getStatus();
						if(cancelFlightStatus != 200) {
							System.err.println("Canceling flight failed with HTTP error code:" + cancelFlightStatus);
						} else {
							System.out.println("Canceling flight was successful with HTTP code: " + cancelFlightStatus);
						}
					}
					if(hotelStatus == 200) {
						// Hotel
						Response responseCancelHotel = client.target(outputHotel.getUrl()).request().accept(MediaType.TEXT_PLAIN).delete();
						int cancelHotelStatus = responseCancelHotel.getStatus();
						if(cancelHotelStatus != 200) {
							System.err.println("Canceling hotel failed with HTTP error code:" + cancelHotelStatus);
						} else {
							System.out.println("Canceling hotel was successful with HTTP code: " + cancelHotelStatus);
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
