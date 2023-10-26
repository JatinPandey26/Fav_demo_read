package com.kipu_fav.read_module.Service;
import com.kipu_fav.read_module.Entity.Schedule;
import com.kipu_fav.read_module.Entity.TimeChunk;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
@Slf4j
public  class Starter_Generator {

    static class TimeSlotIndex
    {
        long slots_15_mins;
        long slots_30_mins;
        long slots_45_mins;
        long slots_60_mins;

        public TimeSlotIndex(long slots_15_mins, long slots_30_mins, long slots_45_mins, long slots_60_mins) {
            this.slots_15_mins = slots_15_mins;
            this.slots_30_mins = slots_30_mins;
            this.slots_45_mins = slots_45_mins;
            this.slots_60_mins = slots_60_mins;
        }
    }

    static  class  IndexPair{
        int chunk_start_index;
        int chunk_end_index;

        public IndexPair(int chunk_start_index, int chunk_end_index) {
            this.chunk_start_index = chunk_start_index;
            this.chunk_end_index = chunk_end_index;
        }
    }


    public static List<Schedule> generateEntries(String resource_name, String location_name, String start_date, String end_date, String start_time, String end_time, List<String> off_days, List<List<String>> off_times) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = sdf.parse(start_date);
        Date endDate = sdf.parse(end_date);
//        log.info(startDate.toString() );
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        List<Schedule> entries = new ArrayList<>();

        while (!calendar.getTime().after(endDate)) {
            String currentDate = sdf.format(calendar.getTime());

            if (!off_days.contains(getDayOfWeek(currentDate))) {
//                String timeline = generateTimeline(start_time, end_time,off_times);
                List<List<String>> timeline = generateTimeLineChunks(start_time,end_time,off_times);
//                TimeSlotIndex timeSlotIndex = getTimeIntervalIndexing(timeline);
                TimeSlotIndex timeSlotIndex = getTimeIntervalIndexingUsingChunks(timeline);
//                log.info(String.valueOf(timeSlotIndex.slots_15_mins));
                entries.add(new Schedule(currentDate, resource_name, location_name,timeSlotIndex.slots_15_mins,timeSlotIndex.slots_30_mins,timeSlotIndex.slots_45_mins,timeSlotIndex.slots_60_mins, timeline));

            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return entries;
    }

    private static String getDayOfWeek(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("EEEE");
        Date d = sdf.parse(date);
        return sdf2.format(d);
    }

    private static boolean isCurrentTimeIsOffTime(Date current_time , List<List<String>> off_times) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date currentTime = null;
        try {
            currentTime = sdf.parse(sdf.format(current_time));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        for (List<String> offTime : off_times) {
            String offTimeStart = offTime.get(0);
            String offTimeEnd = offTime.get(1);

            try {
                Date offStartTime = sdf.parse(offTimeStart);
                Date offEndTime = sdf.parse(offTimeEnd);

                assert currentTime != null;
                if(currentTime.after(offStartTime) && currentTime.before(offEndTime)) return true;

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private static String generateTimeline(String start_time, String end_time,List<List<String>> offTimes) {
        StringBuilder timeline = new StringBuilder();
        int totalMinutes = 24 * 60; // 24 hours in minutes
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        try {
            Date startTime = sdf.parse(start_time);
            Date endTime = sdf.parse(end_time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse("00:00"));

            for (int minute = 0; minute < totalMinutes; minute+=30) {

                if (isCurrentTimeIsOffTime(calendar.getTime(),offTimes) || calendar.getTime().before(startTime) || calendar.getTime().after(endTime)) {
                    timeline.append("0");
                } else {
                    timeline.append("1");
                }

                calendar.add(Calendar.MINUTE, 1);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timeline.toString();
    }
    private static TimeSlotIndex  getTimeIntervalIndexingUsingChunks(List<List<String>> timeLine) throws ParseException {

        long slots_15_mins = 0;
        long slots_30_mins = 0;
        long slots_45_mins = 0;
        long slots_60_mins = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        for (List<String> timeChunk : timeLine){
            Date chunk_start_time = sdf.parse(timeChunk.get(0));
            Date chunk_end_time = sdf.parse(timeChunk.get(1));
            long duration = chunk_end_time.getTime() - chunk_start_time.getTime();
            long hh = duration / (3600 * 1000);
            long mm = (duration - hh * 3600 * 1000) / (60 * 1000);
            long total_minutes = hh*60 + mm;
            slots_60_mins += total_minutes/60;
            slots_45_mins += total_minutes/45;
            slots_30_mins += total_minutes/30;
            slots_15_mins += total_minutes/15;
        }

//        log.info(indexPairs.toString());
        return new TimeSlotIndex(slots_15_mins,slots_30_mins,slots_45_mins,slots_60_mins);
    }


//    private static TimeSlotIndex  getTimeIntervalIndexing(String timeLine){
//
//        int slots_15_mins = 0;
//        int slots_30_mins = 0;
//        int slots_45_mins = 0;
//        int slots_60_mins = 0;
//
//        List<IndexPair> indexPairs = findChunkIndices(timeLine);
//
//        for (IndexPair indexPair : indexPairs){
//            int duration = indexPair.chunk_end_index - indexPair.chunk_start_index;
//            slots_60_mins += duration/60;
//            slots_45_mins += duration/45;
//            slots_30_mins += duration/30;
//            slots_15_mins += duration/15;
//        }
//
////        log.info(indexPairs.toString());
//        return new TimeSlotIndex(slots_15_mins,slots_30_mins,slots_45_mins,slots_60_mins);
//    }

    private static List<List<String>> generateTimeLineChunks(String start_time, String end_time, List<List<String>> offTimes){
        List<List<String>> timeChunks = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        try {
            Date startTime = sdf.parse(start_time);
            Date endTime = sdf.parse(end_time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            for (List<String> offTimeChunk : offTimes){
                Date off_time_start = sdf.parse(offTimeChunk.get(0));
                Date off_time_end = sdf.parse(offTimeChunk.get(1    ));

                Date chunkStartTime = calendar.getTime();
                calendar.setTime(off_time_start);
                calendar.add(Calendar.MINUTE,-1);
                Date chunkEndTime = calendar.getTime();
                List<String> entry = new ArrayList<>();
                entry.add(sdf.format(chunkStartTime));
                entry.add(sdf.format(chunkEndTime));
                calendar.setTime(off_time_end);
                calendar.add(Calendar.MINUTE,1);
                timeChunks.add(entry);
            }

            Date chunkStartTime = calendar.getTime();
            calendar.setTime(endTime);
            Date chunkEndTime = calendar.getTime();
            List<String> entry = new ArrayList<>();
            entry.add(sdf.format(chunkStartTime));
            entry.add(sdf.format(chunkEndTime));
            timeChunks.add(entry);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timeChunks;
    }

    private static List<IndexPair> findChunkIndices(String timeLine) {
        List<IndexPair> indexPairs = new ArrayList<>();

        int index = 0;
        int chunkStart = -1;
        while (index < timeLine.length()) {
            if (timeLine.charAt(index) == '0') {
                if (chunkStart == -1) {
                    chunkStart = index;
                }
            } else {
                if (chunkStart != -1) {

                    chunkStart = -1;
                }
            }
            index++;
        }

        if (chunkStart != -1) {
            indexPairs.add(new IndexPair(chunkStart, index -1));
        }
        return indexPairs;
    }

}
