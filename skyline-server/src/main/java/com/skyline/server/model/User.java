package com.skyline.server.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.impl.ClusterSerializable;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class User implements ClusterSerializable {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private String id;
    private String email;
    private String wechatId;
    private Status status;
    private List<Rental> rentals;
    private Date lastUpdatedTimestamp;
    private Date createdTimestamp;

    public User() {
        this.rentals = new ArrayList<>();
    }

    public User(String id) {
        this.id = id;
        this.createdTimestamp = new Timestamp(System.currentTimeMillis());
        this.lastUpdatedTimestamp = createdTimestamp;
        this.status = Status.ACTIVE;
        this.rentals = new ArrayList<>();
    }

    public String getId() {
        return this.id;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getWechatId() {
        return wechatId;
    }

    public User setWechatId(String wechatId) {
        this.wechatId = wechatId;
        return this;
    }

    public List<Rental> getRentals() {
        return this.rentals;
    }

    public User setRentals(List<Rental> rentals) {
        this.rentals = rentals;
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

    public User setLastUpdatedTimestamp(Date lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        return this;
    }

    public User setLastUpdatedTimestamp(String lastUpdatedTimestamp) throws ParseException {
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

    public User setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        return this;
    }

    public User setCreatedTimestamp(String createdTimestamp) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.createdTimestamp = format.parse(createdTimestamp);
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public User setStatus(Status status) {
        this.status = status;
        return this;
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

    @Override
    public void writeToBuffer(Buffer buff) {
        buff.appendLong(this.createdTimestamp.getTime());
        buff.appendLong(this.lastUpdatedTimestamp.getTime());
        byte[] bytes;
        bytes = this.id.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.email.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.wechatId.getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        bytes = this.status.toString().getBytes(UTF8);
        buff.appendInt(bytes.length).appendBytes(bytes);
        buff.appendInt(this.rentals.size());
        for (Rental rental : rentals) {
            rental.writeToBuffer(buff);
        }
    }

    @Override
    public int readFromBuffer(int pos, Buffer buff) {
        this.createdTimestamp = new Date(buff.getLong(pos));
        pos += 8;
        this.lastUpdatedTimestamp = new Date(buff.getLong(pos));
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
        this.email = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.wechatId = new String(bytes, UTF8);
        len = buff.getInt(pos);
        pos += 4;
        bytes = buff.getBytes(pos, pos + len);
        pos += len;
        this.status = Status.valueOf(new String(bytes, UTF8));
        int num = buff.getInt(pos);
        pos += 4;
        for (int i = 0; i < num; i++) {
            Rental rental = new Rental();
            pos = rental.readFromBuffer(pos, buff);
            this.rentals.add(rental);
        }
        return pos;
    }
}
