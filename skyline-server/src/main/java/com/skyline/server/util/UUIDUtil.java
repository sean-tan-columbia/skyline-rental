package com.skyline.server.util;

import org.hashids.Hashids;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by jtan on 6/3/17.
 */
public class UUIDUtil {

    public static String getUUID(long ... params) {
        String salt = "SKYLINE";
        Hashids hashids = new Hashids(salt, 6);
        return hashids.encode(params);
    }

}
