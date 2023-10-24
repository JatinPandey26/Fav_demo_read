package com.kipu_fav.read_module.Service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kipu_fav.read_module.Entity.RedisResponse;
import com.kipu_fav.read_module.Entity.Schedule;
import com.kipu_fav.read_module.Entity.Schedule_request;
import com.kipu_fav.read_module.Entity.UserQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.kipu_fav.read_module.Service.Starter_Generator;

@org.springframework.stereotype.Service
@Slf4j
public class Service {

    public static final String HASH_KEY = "KIPU_SCHEDULES";

    @Autowired
    private RedisTemplate template;

    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ZSetOperations zSetOperations;

    public void starter() throws ParseException {
        ResponseEntity<Schedule_request[]> responseEntity = restTemplate.getForEntity("http://localhost:8090/schedule/all",Schedule_request[].class);
        Schedule_request[] scheduleRequestsFromWriteDB = responseEntity.getBody();
//        log.info(Arrays.toString(scheduleRequestsFromWriteDB));
        assert scheduleRequestsFromWriteDB != null;
        CreateAndInjectStarterDataToRedis(scheduleRequestsFromWriteDB);
    }

    private void CreateAndInjectStarterDataToRedis(Schedule_request[] scheduleRequestsFromWriteDB) throws ParseException {
        List<Schedule> scheduleList = new ArrayList<>();

        for (Schedule_request scheduleRequest : scheduleRequestsFromWriteDB){
            List<Schedule> scheduleEntries = Starter_Generator.generateEntries(scheduleRequest.getResource_name(),scheduleRequest.getLocation_name(),scheduleRequest.getStart_date().toString(),scheduleRequest.getEnd_date().toString(),scheduleRequest.getStart_time().toString(),scheduleRequest.getEnd_time().toString(),scheduleRequest.getOff_days(),scheduleRequest.getOff_times());
            for (Schedule schedule : scheduleEntries) {
//                log.info(schedule.toString());
                zSetOperations.add(HASH_KEY, schedule, calculateScore(schedule));
            }
        }
    }


    private double calculateScore(Schedule schedule) {
        String dateString = schedule.getDate(); // Date in "yyyy-MM-dd" format
        String resourceName = schedule.getResource_name();
        String locationName = schedule.getLocation_name();

        // Convert the date to a Unix timestamp (milliseconds)
        long dateTimestamp;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateTimestamp = dateFormat.parse(dateString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            dateTimestamp = 0; // Handle parsing error
        }

        // Create a combined score that incorporates date, resource name, and location name
        double score = dateTimestamp + resourceName.hashCode() + locationName.hashCode();

        return score;
    }

    public List<Schedule> getAllEntries(){
        Set<Schedule> schedules = zSetOperations.range(HASH_KEY,0,-1);
        assert schedules != null;
        return new ArrayList<>(schedules) ;
    }

     public List<RedisResponse> querySlots(UserQuery query) throws IOException {
         String luaScript = new String(Files.readAllBytes(Path.of("C:\\Users\\jatin\\Desktop\\FAV\\read_module\\src\\main\\java\\com\\kipu_fav\\read_module\\scripts\\filter_entries.lua")));

         RedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);

         String[] keys = {HASH_KEY};

         String queryJson = objectMapper.writeValueAsString(query);

//         Arrays.asList(query.getResources(),query.getLocations(),query.getStart_date(),query.getEnd_date(),query.getStart_time(),query.getEnd_time(),query.getDuration_in_mins(),query.getTop())
         String schedulesString = Objects.requireNonNull(template.execute(script, Collections.singletonList(HASH_KEY),query)).toString();
         log.info(schedulesString);
         if(schedulesString.equals("{}")) return null;
         List<RedisResponse> schedules = objectMapper.readValue(schedulesString, new TypeReference<List<RedisResponse>>() {});

         log.info(schedules.toString());
         return schedules;
    }

}
