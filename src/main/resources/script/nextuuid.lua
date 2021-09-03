local function get_next_id()
    local key = KEYS[1]
    local delta = tonumber(ARGV[1])
    local initial = tonumber(ARGV[2])
    local ttl = tonumber(ARGV[3])

    local seq = redis.call("get", key)
    if (initial and initial~= nil and initial>0) then
        if (not seq or seq==nil or (tonumber(seq)<initial)) then
            redis.call("set", key, initial)
        end
    end

    local id = redis.call("incrby", key, delta)
    if (ttl and ttl~= nil and ttl>0 and (not seq or seq==nil)) then
        redis.call("expire", key, ttl)
    end
    return id
end
return get_next_id()