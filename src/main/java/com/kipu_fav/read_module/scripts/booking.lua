local start_time = (cjson.decode(ARGV[2])).start_time
local end_time = (cjson.decode(ARGV[2])).end_time

print("keys "..KEYS[1])

print("Args1 "..string.sub(ARGV[1],2,36))
print("Args2 "..ARGV[2])
print("Args3 "..ARGV[3])

local entries = redis.call("ZRANGEBYSCORE", KEYS[1], tostring(ARGV[3]), tostring(ARGV[3]))
local entry = cjson.decode(entries[1])
print(entry)

local function removeSlotFromTimeline(timeline)
    local updated_timeline = {}

    for _, time_slot in ipairs(timeline) do
        local slot_start = time_slot[1]
        local slot_end = time_slot[2]

        -- Check if the current slot is entirely before or after the slot to remove
        if slot_end <= start_time or slot_start >= end_time then
            print(time_slot)
            table.insert(updated_timeline, time_slot)
        else
            -- Slot partially overlaps with the slot to remove, split it if needed
            if slot_start < start_time then
            print({slot_start, start_time})
                table.insert(updated_timeline, {slot_start, start_time})
            end

            if slot_end > end_time then
            print({end_time, slot_end})
                table.insert(updated_timeline, {end_time, slot_end})
            end
        end
    end

    return updated_timeline
end

local function copyAndModifyEntry(entry, new_timeline, sorted_set_key, score)

    local modified_entry = entry

    modified_entry["timeline"] = new_timeline

    local modified_entry_json = cjson.encode(modified_entry)
    redis.call("ZREM",sorted_set_key,cjson.encode(entry))
    redis.call("ZADD", sorted_set_key, score, modified_entry_json)
end

local newTimeline = removeSlotFromTimeline(entry.timeline)

for _, time_slot in ipairs(newTimeline) do
    print(time_slot[1], time_slot[2])
end

copyAndModifyEntry(entry,newTimeline,KEYS[1],tostring(ARGV[3]))