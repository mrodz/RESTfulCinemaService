package cinema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Theater {
    @JsonProperty("total_rows")
    int totalRows = 9;
    @JsonProperty("total_columns")
    int totalColumns = 9;
    @JsonProperty("available_seats")
    List<Seat> availableSeats = Collections.synchronizedList(new ArrayList<>());

    {
        for (int i = 1; i <= totalRows; i++) {
            for (int j = 1; j <= totalColumns; j++) {
                availableSeats.add(new Seat(i, j));
            }
        }
    }

    public Seat getSeatAt(int row, int column) {
        return availableSeats.get((row - 1) * totalRows + (column - 1));
    }

    public List<Seat> getAvailableSeats() {
        return availableSeats.stream().filter(Seat::isAvailable).collect(Collectors.toList());
    }
}
