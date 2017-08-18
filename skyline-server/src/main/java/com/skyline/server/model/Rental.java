package com.skyline.server.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.impl.ClusterSerializable;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by jtan on 6/3/17.
 */
public class Rental implements ClusterSerializable {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private String id;
    private Date createdTimestamp;
    private String posterId;
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

    public Rental() {
        this.imageIds = new ArrayList<>();
    }

    public Rental(String id) {
        this.id = id;
        this.imageIds = new ArrayList<>();
        this.createdTimestamp = new Timestamp(System.currentTimeMillis());
        this.lastUpdatedTimestamp = createdTimestamp;
        this.status = Status.ACTIVE;
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

    public String formatStartDate() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(this.startDate);
    }

    public Rental setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Rental setStartDate(String startDate) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        this.startDate = format.parse(startDate);
        return this;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String formatEndDate() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(this.endDate);
    }

    public Rental setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    public Rental setEndDate(String endDate) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        this.endDate = format.parse(endDate);
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

    public String formatLastUpdatedTimestamp() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(this.lastUpdatedTimestamp);
    }

    public Rental setLastUpdatedTimestamp(Date lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        return this;
    }

    public Rental setLastUpdatedTimestamp(String lastUpdatedTimestamp) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.lastUpdatedTimestamp = format.parse(lastUpdatedTimestamp);
        return this;
    }

    public Date getCreatedTimestamp() {
        return this.createdTimestamp;
    }

    public String formatCreatedTimestamp() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(this.createdTimestamp);
    }

    public Rental setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        return this;
    }

    public Rental setCreatedTimestamp(String createdTimestamp) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.createdTimestamp = format.parse(createdTimestamp);
        return this;
    }

    public Rental setPosterId(String posterId) {
        this.posterId = posterId;
        return this;
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

    @Override
    public void writeToBuffer(Buffer buff) {
        buff.appendLong(this.createdTimestamp.getTime());
        buff.appendLong(this.lastUpdatedTimestamp.getTime());
        buff.appendLong(this.startDate.getTime());
        buff.appendLong(this.endDate.getTime());
        buff.appendDouble(this.latitude);
        buff.appendDouble(this.longitude);
        buff.appendDouble(this.price);
        byte[] bytes;
        bytes = this.id.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.posterId.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.rentalType.toString().getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.address.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.neighborhood.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.quantifier.toString().getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.bedroom.toString().getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.bathroom.toString().getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.description.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.status.toString().getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        buff.appendInt(this.imageIds.size());
        for (String imageId : imageIds) {
            bytes = imageId.getBytes(UTF8);
            buff.appendInt(bytes.length).appendBytes(bytes);
        }
    }

    @Override
    public int readFromBuffer(int pos, Buffer buff) {
        this.createdTimestamp = new Date(buff.getLong(pos));
        pos += 8;
        this.lastUpdatedTimestamp = new Date(buff.getLong(pos));
        pos += 8;
        this.startDate = new Date(buff.getLong(pos));
        pos += 8;
        this.endDate = new Date(buff.getLong(pos));
        pos += 8;
        this.latitude = buff.getDouble(pos);
        pos += 8;
        this.longitude = buff.getDouble(pos);
        pos += 8;
        this.price = buff.getDouble(pos);
        pos += 8;
        int len;
        byte[] bytes;
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.id = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.posterId = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.rentalType = RentalType.valueOf(new String(bytes, UTF8));
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.address = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.neighborhood = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.quantifier = Quantifier.valueOf(new String(bytes, UTF8));
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.bedroom = Bedroom.valueOf(new String(bytes, UTF8));
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.bathroom = Bathroom.valueOf(new String(bytes, UTF8));
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.description = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.status = Status.valueOf(new String(bytes, UTF8));
        int num = buff.getInt(pos);
        pos += 4;
        for (int i = 0; i < num; i++) {
            len = buff.getInt(pos);
            pos += 4;
            bytes = buff.getBytes(pos, pos + len);
            pos += len;
            this.imageIds.add(new String(bytes, UTF8));
        }
        return pos;
    }

}
