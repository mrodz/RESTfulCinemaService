package cinema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RestController
public class EndpointSeats {
    static final transient Theater theater = new Theater();
    static final transient ConcurrentHashMap<UUID, Seat> uuidToSeatMapping = new ConcurrentHashMap<>();
    static final transient AtomicInteger revenue = new AtomicInteger(0);
    static final transient AtomicInteger availableSeats = new AtomicInteger(theater.totalColumns * theater.totalRows);
    static final transient AtomicInteger purchasedTickets = new AtomicInteger(0);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SeatPurchaseRequest {
        int row;
        int column;
    }

    @Data
    @NoArgsConstructor
    static class SeatPurchaseResponse {
        String token;
        Seat ticket;

        public SeatPurchaseResponse(String token, Seat ticket) {
            this.token = token;
            this.ticket = ticket;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class SeatReturnRequest {
        UUID token;
    }

    @Data
    @NoArgsConstructor
    static class SeatReturnResponse {
        @JsonProperty("returned_ticket")
        Seat returnedSeat;

        public SeatReturnResponse(Seat returnedSeat) {
            this.returnedSeat = returnedSeat;
        }
    }

    @Data
    @NoArgsConstructor
    static class StatsResponse {
        @JsonProperty("current_income")
        AtomicInteger income = revenue;
        @JsonProperty("number_of_available_seats")
        AtomicInteger availableSeats = EndpointSeats.availableSeats;
        @JsonProperty("number_of_purchased_tickets")
        AtomicInteger purchasedTickets = EndpointSeats.purchasedTickets;
    }

    @SuppressWarnings("rawtypes")
    public static final Map[] ERROR_MESSAGES = {
            Map.of("error", "The number of a row or a column is out of bounds!"),
            Map.of("error", "The ticket has been already purchased!"),
            Map.of("error", "Wrong token!"),
            Map.of("error", "The password is wrong!")
    };

    @PostMapping("/stats")
    public ResponseEntity<?> viewStats(@RequestParam(required = false) String password) {

        if (password != null) {
            Optional<StatsResponse> statsResponse = login(password, "super_secret", () -> {});
            if (statsResponse.isPresent()) {
                return new ResponseEntity<>(statsResponse, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ERROR_MESSAGES[3], HttpStatus.UNAUTHORIZED);
            }
        } else {
            return new ResponseEntity<>(ERROR_MESSAGES[3], HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnPurchase(@RequestBody SeatReturnRequest seatReturnRequest) {
        Optional<Seat> requestedSeat = getSeatFromUUI(seatReturnRequest.token, seat -> {
            seat.setAvailable(true);
            uuidToSeatMapping.remove(seatReturnRequest.token);
            availableSeats.incrementAndGet();
            purchasedTickets.decrementAndGet();
            revenue.set(revenue.get() - seat.price);
        });

        if (requestedSeat.isPresent()) {
            return new ResponseEntity<>(new SeatReturnResponse(requestedSeat.get()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(ERROR_MESSAGES[2], HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseSeat(@RequestBody SeatPurchaseRequest purchaseRequest) {
        if (!(inBounds(purchaseRequest.column, theater.totalColumns) && inBounds(purchaseRequest.row, theater.totalRows))) {
            return new ResponseEntity<>(ERROR_MESSAGES[0], HttpStatus.BAD_REQUEST);
        }
        Seat attempted = theater.getSeatAt(purchaseRequest.row, purchaseRequest.column);
        if (!attempted.isAvailable()) {
            return new ResponseEntity<>(ERROR_MESSAGES[1], HttpStatus.BAD_REQUEST);
        }

        SeatPurchaseResponse response = purchaseSeat(attempted);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public Optional<StatsResponse> login(String password, String secret, Runnable callBack) {
        if (password.equals(secret)) {
            callBack.run();
            return Optional.of(new StatsResponse());
        } else {
            return Optional.empty();
        }
    }

    public SeatPurchaseResponse purchaseSeat(Seat seat) {
        if (!seat.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Seat is not available");
        } else {
            revenue.set(revenue.get() + seat.price);
            purchasedTickets.incrementAndGet();
            availableSeats.decrementAndGet();

            seat.setAvailable(false);
            UUID purchaseUUID = UUID.randomUUID();
            uuidToSeatMapping.put(purchaseUUID, seat);

            return new SeatPurchaseResponse(purchaseUUID.toString(), seat);
        }
    }

    public Optional<Seat> getSeatFromUUI(UUID uuid, Consumer<Seat> callBack) {
        if (uuidToSeatMapping.containsKey(uuid)) {
            Seat requestedSeat = uuidToSeatMapping.get(uuid);
            callBack.accept(requestedSeat);
            return Optional.of(requestedSeat);
        } else {
            return Optional.empty();
        }
    }

    public boolean inBounds(int val, int max) {
        return val > 0 && val <= max;
    }

    @GetMapping("/seats")
    public Theater getTheater() {
        return theater;
    }
}
