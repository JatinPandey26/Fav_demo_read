package com.kipu_fav.read_module.Entity;

import lombok.Data;

import java.util.List;

@Data
public class RedisResponse {
    String id;
    String resource_name;
    String location_name;
    String date;
    List<List<String>> timeSlots;
}
