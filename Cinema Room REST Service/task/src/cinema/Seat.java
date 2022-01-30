package cinema;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Seat {
    final int row;
    final int column;
    final int price;
    @JsonIgnore
    private transient boolean available = true;

    public Seat(int row, int column) {
        this.row = row;
        this.column = column;
        this.price = row <= 4 ? 10 : 8;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public int getPrice() {
        return this.price;
    }

    public int getRow() {
        return this.row;
    }

    public int getColumn() {
        return this.column;
    }

    @Override
    public String toString() {
        return "Seat{" +
                "row=" + row +
                ", column=" + column +
                ", price=" + price +
                ", available=" + available +
                '}';
    }
}
