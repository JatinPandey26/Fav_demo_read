package com.kipu_fav.read_module.Entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class Booking {
    private String id_redis;
    private String resource_name;
    private String location_name;
    private String date;
    private String start_time;
    private String end_time;
    private long duration;
}

