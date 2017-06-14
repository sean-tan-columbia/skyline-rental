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
    private final String imagePath;
    private RentalType rentalType;
    private String address;
    private String neighborhood;
    private double price;
    private Quantifier quantifier;
    private List<String> imageIds;
    private Date startDate;
    private Date endDate;
    private String description;
    private Status status;
    private Date lastUpdatedTimestamp;

    public Rental(String posterId, String imagePath) {
        this.posterId = posterId;
        this.createdTimestamp = new Timestamp(System.currentTimeMillis());
        this.lastUpdatedTimestamp = this.createdTimestamp;
        this.status = Status.ACTIVE;
        this.imagePath = imagePath;
        this.imageIds = new ArrayList<>();
        this.id = UUIDUtil.getUUID((long)this.posterId.hashCode(), createdTimestamp.getTime());
    }

    public String getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    public void setQuantifier(Quantifier quantifier) {
        this.quantifier = quantifier;
    }

    public List<String> getImageIds() {
        return this.imageIds.stream()
                .map(img -> this.imagePath + "/" + this.id + "/" + img)
                .collect(Collectors.toList());
    };

    public void addImageIds(String imageId) {
        this.imageIds.add(imageId);
    }

    public RentalType getRentalType() {
        return rentalType;
    }

    public void setRentalType(RentalType rentalType) {
        this.rentalType = rentalType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(Date lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
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

    public void setStatus(Status status) {
        this.status = status;
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
