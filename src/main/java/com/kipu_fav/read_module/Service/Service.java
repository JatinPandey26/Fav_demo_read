package com.kipu_fav.read_module.Service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kipu_fav.read_module.Entity.*;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
    private final String BOOKING_KAFKA_TOPIC = "booking-kafka-topic";


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
                zSetOperations.add(HASH_KEY, schedule, calculateScore(schedule.getDate(),schedule.getResource_name(),schedule.getLocation_name()));
            }
        }
    }


    private double calculateScore(String dateString , String resourceName , String locationName) {

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
         String luaScript = new String(Files.readAllBytes(Path.of("C:\\Users\\hp\\Desktop\\Fav_kipu\\Fav_demo_read\\src\\main\\java\\com\\kipu_fav\\read_module\\scripts\\filter_entries.lua")));

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
    @KafkaListener(topics = BOOKING_KAFKA_TOPIC,groupId = "booking-kafka-consumer-group",properties = {"spring.json.value.default.type=com.kipu_fav.read_module.Entity.Booking"})
    public void bookingSync(Booking booking) throws IOException {
        String luaScript = new String(Files.readAllBytes(Path.of("C:\\Users\\hp\\Desktop\\Fav_kipu\\Fav_demo_read\\src\\main\\java\\com\\kipu_fav\\read_module\\scripts\\booking.lua")));

        RedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);

        String[] keys = {HASH_KEY};
        log.info(booking.toString());

        double score = calculateScore(booking.getDate(),booking.getResource_name(),booking.getLocation_name());
        BigInteger scoreInteger = new BigDecimal(String.valueOf(score)).toBigInteger();
        Object result =  template.execute(script, Collections.singletonList(HASH_KEY),booking.getId_redis(),booking,scoreInteger);
        assert result != null;
//        log.info(result.toString());
    }


}
