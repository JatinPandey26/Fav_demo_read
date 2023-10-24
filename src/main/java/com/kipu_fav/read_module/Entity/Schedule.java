package com.kipu_fav.read_module.Entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("schedule")
public class Schedule implements Serializable {
    @Id
    private String id = UUID.randomUUID().toString();
    private String date;
    private String resource_name;
    private String location_name;
    private long slots_15_mins;
    private long slots_30_mins;
    private long slots_45_mins;
    private long slots_60_mins;
    private List<List<String>> timeline;


    public Schedule(String date, String resource_name, String location_name, long slots_15_mins, long slots_30_mins, long slots_45_mins, long slots_60_mins, List<List<String>> timeline) {
        this.date = date;
        this.resource_name = resource_name;
        this.location_name = location_name;
        this.slots_15_mins = slots_15_mins;
        this.slots_30_mins = slots_30_mins;
        this.slots_45_mins = slots_45_mins;
        this.slots_60_mins = slots_60_mins;
        this.timeline = timeline;
    }
}


