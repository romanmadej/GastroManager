--compare dates irrespective of year
create or replace function leq(a date, b date) returns boolean
as
$$
DECLARE
    na date := a + make_interval(years := 2000 - extract(year from a)::int);
    nb date := b + make_interval(years := 2000 - extract(year from b)::int);
BEGIN
    return na <= nb;
END;
$$ LANGUAGE plpgsql;

--check if date "a" is inbetween dates "b" and "c" irrespective of year
create or replace function inbetween(a date, b date, c date) returns boolean
as
$$
BEGIN
    return leq(b,a) and leq(a,c);
END;
$$ LANGUAGE plpgsql;


--works under assumption that there is a record for every day of the week in table 'opening_hours'

create or replace function is_open(restaurantId int) returns boolean
as
$$
DECLARE
    r record;
BEGIN
    set timezone = 'gmt -2';
    --remove non-cyclic data of no use
    delete from special_dates where is_cyclic = false and leq(date_to, (current_date - interval '1 day')::date);

    select opening_time, closing_time
    into r
    from special_dates
    where restaurant_id = restaurantId
      and inbetween(current_date,date_from,date_to)
    order by is_cyclic;

    if r is null then
        select opening_time, closing_time
        into r
        from special_dates
        where restaurant_id is null
          and inbetween(current_date,date_from,date_to)
        order by is_cyclic;
    end if;

    if r is null then
        select opening_time, closing_time
        into r
        from opening_hours
        where restaurant_id = restaurantId
          and extract('day' from current_date) = day - 1;
    end if;
    return r.opening_time is not null and r.closing_time is not null and
           current_time between r.opening_time and r.closing_time;
END;
$$ LANGUAGE plpgsql;
