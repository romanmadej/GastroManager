create or replace function overlap() returns trigger
as
$$
declare
    cnt int;
BEGIN
    select count(*) into cnt from special_dates
    where restaurant_id = new.restaurant_id
    and is_cyclic = new.is_cyclic
    and (inbetween(new.date_from, date_from, date_to)
    or inbetween(new.date_to,date_from,date_to));
    if cnt!=0 then
        return null;
    end if;
    return new;
END;
$$ LANGUAGE plpgsql;

create trigger overlap
    before insert
    on special_dates
    for each row
execute procedure overlap();


