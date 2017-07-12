package com.skyline.server.model;

import com.skyline.server.util.UUIDUtil;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


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
    };

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

    public enum RentalType {
        MASTER_BEDROOM,
        BEDROOM,
        LIVING_ROOM,
        APARTMENT
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public enum Quantifier {
        DAY,
        MONTH
    }

}
