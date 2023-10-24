package com.kipu_fav.read_module.Entity;

import lombok.Data;

import java.util.List;

@Data
public class UserQuery {
    List<String> resources;
    List<String> locations;
    String start_date;
    String end_date;
    String start_time;
    String end_time;
    int duration_in_mins;
    int top;
}
