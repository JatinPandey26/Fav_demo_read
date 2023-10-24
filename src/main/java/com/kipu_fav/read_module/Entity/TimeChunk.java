package com.kipu_fav.read_module.Entity;

import java.util.Date;

public class TimeChunk{
    public Date start_time;
    public Date end_time;

    public TimeChunk(Date start_time, Date end_time) {
        this.start_time = start_time;
        this.end_time = end_time;
    }
}