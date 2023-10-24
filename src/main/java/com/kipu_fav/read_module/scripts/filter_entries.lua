
print("Debug Message: Script started")
local function dateSubstring(dateString, startIndex, endIndex)
    if startIndex < 1 or endIndex < startIndex or endIndex > #dateString then
        return nil
    end
    return string.sub(dateString, startIndex, endIndex)
end



-- Get the list of entries from the sorted set
local entries = redis.call('ZRANGE', KEYS[1], 0, -1)

local userQuery = cjson.decode(ARGV[1])

local result = {}
local resourcesList = {}
local locationList = {}
local start_date = userQuery.start_date
local end_date = userQuery.end_date
local start_time = userQuery.start_time
local end_time = userQuery.end_time
local duration_in_mins = userQuery.duration_in_mins
local top = userQuery.top

local count = 0

local function isInList(value, list)
    for _, listItem in ipairs(list) do
        if listItem == value then
            return true
        end
    end
    return false
end

local function checkSlotForADuration(entry,duration)
    if(duration == 15) then return entry.slots_15_mins
    elseif(duration == 30) then return entry.slots_30_mins
    elseif(duration == 45) then return entry.slots_45_mins
    elseif(duration == 60) then return entry.slots_60_mins
    else return 0;
    end
end



-- Access "resources" as an array
if userQuery.resources and type(userQuery.resources) == "table" then
    for i, resource in ipairs(userQuery.resources) do
        table.insert(resourcesList,resource) -- Print each resource in the "resources" array
    end
end

-- Access "locations" as an array
if userQuery.locations and type(userQuery.locations) == "table" then
    for i, location in ipairs(userQuery.locations) do
        table.insert(locationList,location) -- Print each resource in the "resources" array
    end
end

local function timeToMinutes(timeStr)
    local hours, minutes = timeStr:match("(%d+):(%d+)")
    return tonumber(hours) * 60 + tonumber(minutes)
end

local function getSlotsCountBtwTimeRange(entryData)
    local count = 0
    for i,timeRange in ipairs(entryData.timeline) do
        count = math.floor((timeToMinutes(timeRange[2]) - timeToMinutes(timeRange[1]))/duration_in_mins)
    end
     return count
end

-- for i, resource in ipairs(resourcesList) do
--         print(resource)-- Print each resource in the "resources" array
--     end
-- for i, location in ipairs(locationList) do
--         print(location)-- Print each resource in the "resources" array
--     end
--
-- print(start_date)
-- print(end_date)
-- print(start_time)
-- print(end_time)
-- print(duration_in_mins)
-- print(top)

local function getSlotsForDuration(slots, start_time_query, end_time_query, duration)
    local startTime = timeToMinutes(start_time_query)
    local endTime = timeToMinutes(end_time_query)

    while startTime + duration <= endTime and startTime >= timeToMinutes(start_time) and startTime + duration <= timeToMinutes(end_time) do
        local slotStart = string.format("%02d:%02d", math.floor(startTime / 60), startTime % 60)
        local slotEnd = string.format("%02d:%02d", math.floor((startTime + duration) / 60), (startTime + duration) % 60)
        count = count + 1;
        print(count)
        table.insert(slots, {slotStart, slotEnd})
        startTime = startTime + duration
    end
end


local function getSlots(slots, entryData, duration_in_mins)
    for i, timeRange in ipairs(entryData.timeline) do

        getSlotsForDuration(slots, timeRange[1], timeRange[2], duration_in_mins,count)
    end
end

local function formatEntryAsJSON(entry)
    local formattedEntry = "{"
    for key, value in pairs(entry) do
        formattedEntry = formattedEntry .. '"' .. key .. '": '
        if type(value) == "table" then
            local formattedValue = "{" .. table.concat(value, ", ") .. "}"
            formattedEntry = formattedEntry .. formattedValue
        else
            formattedEntry = formattedEntry .. '"' .. value .. '"'
        end

        if next(entry, key) then
            formattedEntry = formattedEntry .. ", "
        end
    end
    formattedEntry = formattedEntry .. "}"
    return formattedEntry
end





for _, entry in ipairs(entries) do
    -- Break the loop if the count exceeds 50
    if count > top then
        break
    end

    -- Parse the JSON data
    local entryData = cjson.decode(entry)
    local slotsPossible = checkSlotForADuration(entryData,duration_in_mins);
    local slotsCountBtwTimeRange = getSlotsCountBtwTimeRange(entryData)
    local date =  entryData.date
    if date and date >= string.sub(start_date, 1, 11)
    and date <= string.sub(end_date, 1, 11)
    and isInList(entryData.resource_name,resourcesList)
    and isInList(entryData.location_name,locationList)
    and slotsPossible > 0
    and slotsCountBtwTimeRange > 0
    then

        local slots = {}
        getSlots(slots,entryData,duration_in_mins)
        local entryWithSlots = {
            id = entryData.id,
            date = entryData.date,
            resource_name = entryData.resource_name,
            location_name = entryData.location_name,
            timeSlots = slots
        }
        print(type(slots))
        table.insert(result, cjson.encode(entryWithSlots))
    end
end

-- Return the result as a JSON-encoded string

return cjson.encode(result)
