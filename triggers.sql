create or replace function update_ingredients_on_order_cancel() returns trigger
as
$$
declare
BEGIN
    if (old.status = 'open' or old.status = 'completed') and new.status = 'cancelled' then
        perform modify_stock(new.restaurant_id, new.order_id, true);
    end if;
    return new;
END;
$$ LANGUAGE plpgsql;


create trigger update_ingredients_on_order_cancel
    after update
    on orders
    for each row
execute procedure update_ingredients_on_order_cancel();

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


