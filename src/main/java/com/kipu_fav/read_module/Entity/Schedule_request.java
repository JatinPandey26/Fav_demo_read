package com.kipu_fav.read_module.Entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class Schedule_request {
    private String resource_name;
    private String location_name;
    private LocalDate start_date;
    private LocalDate end_date;
    private LocalTime start_time;
    private LocalTime end_time;
    private List<String> off_days;
    private List<List<String>> off_times;
}
