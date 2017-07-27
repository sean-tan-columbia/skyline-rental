package com.skyline.server.model;

import java.sql.Timestamp;
import java.util.*;


/**
 * Created by jtan on 6/3/17.
 */
public class Rental {

    private final String id;
    private final Date createdTimestamp;
    private final String posterId;
    private RentalType rentalType;
    private String address;
    private double latitude;
    private double longitude;
    private String neighborhood;
    private double price;
    private Quantifier quantifier;
    private List<String> imageIds;
    private Date startDate;
    private Date endDate;
    private String description;
    private Status status;
    private Date lastUpdatedTimestamp;
    private Bedroom bedroom;
    private Bathroom bathroom;

    public Rental(String id, String posterId) {
        this.id = id;
        this.posterId = posterId;
        this.createdTimestamp = new Timestamp(System.currentTimeMillis());
        this.lastUpdatedTimestamp = this.createdTimestamp;
        this.status = Status.ACTIVE;
        this.imageIds = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public Rental setPrice(double price) {
        this.price = price;
        return this;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    public Rental setQuantifier(Quantifier quantifier) {
        this.quantifier = quantifier;
        return this;
    }

    public List<String> getImageIds() {
        return this.imageIds;
    }

    public Rental addImageIds(String imageId) {
        this.imageIds.add(imageId);
        return this;
    }

    public Rental setImageIds(List<String> imageIds) {
        imageIds.stream().forEach(i -> this.imageIds.add(i));
        return this;
    }

    public RentalType getRentalType() {
        return rentalType;
    }

    public Rental setRentalType(RentalType rentalType) {
        this.rentalType = rentalType;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Rental setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public Rental setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Rental setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Rental setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Rental setDescription(String description) {
        this.description = description;
        return this;
    }

    public Date getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public Rental setLastUpdatedTimestamp(Date lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        return this;
    }

    public Date getCreatedTimestamp() {
        /*
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(this.createdTimestamp);
        */
        return this.createdTimestamp;
    }

    public String getPosterId() {
        return posterId;
    }

    public Status getStatus() {
        return status;
    }

    public Rental setStatus(Status status) {
        this.status = status;
        return this;
    }

    public double getLatitude() {
        return latitude;
    }

    public Rental setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public Rental setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public Bedroom getBedroom() {
        return bedroom;
    }

    public Rental setBedroom(Bedroom bedroom) {
        this.bedroom = bedroom;
        return this;
    }

    public Bathroom getBathroom() {
        return bathroom;
    }

    public Rental setBathroom(Bathroom bathroom) {
        this.bathroom = bathroom;
        return this;
    }

    public enum RentalType {
        MASTER_BEDROOM(0), BEDROOM(1), LIVING_ROOM(2), APARTMENT(3);

        private int val;

        RentalType(int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }
    }

    public enum Status {
        ACTIVE(0), INACTIVE(1);

        private int val;

        Status(int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }
    }

    public enum Quantifier {
        MONTH(0), DAY(1);

        private int val;

        Quantifier(int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }
    }

    public enum Bathroom {
        SHARED(0), ONE(1), TWO(2), THREE(3);

        private int val;

        Bathroom(int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }
    }

    public enum Bedroom {
        STUDIO(0), ONE(1), TWO(2), THREE(3);

        private int val;

        Bedroom(int val) {
            this.val = val;
        }

        public int getVal() {
            return this.val;
        }
    }

}
