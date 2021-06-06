--adds/subtracts ingredients required to cook a given dish from given's restaurants stock
create or replace function modify_stock(restaurantId int, dishId int, dishQuantity int, add boolean) returns void
as
$$
declare
    r record;
    currentQuantity int;
BEGIN
    for r in select ingredient_id, dishQuantity * di.quantity as quantity
                      from dishes d
                      join dish_ingredients di using (dish_id)
             where dish_id = dishId
        loop
            if add = true then
                update stock
                set quantity = quantity + r.quantity
                where restaurant_id = restaurantId
                  and ingredient_id = r.ingredient_id;
            else
                select quantity into currentQuantity from stock
                where restaurant_id = restaurantId
                  and ingredient_id = r.ingredient_id;
                if currentQuantity is null then
                    raise exception 'Illegal State. no ingredient_id: % is lacking from restaurant_id: %',r.ingredient_id,restaurantId;
                end if;
                if currentQuantity - r.quantity < 0 then
                    raise exception 'Illegal Order Details insertion. Restaurant has too few of ingredient_id: % to cook dish_id: %',r.ingredient_id,dishId;
                end if;

                update stock
                set quantity = quantity - r.quantity
                where restaurant_id = restaurantId
                  and ingredient_id = r.ingredient_id;
            end if;
        end loop;
END
$$ LANGUAGE plpgsql;

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

--returns price without discounts, if atDate not supplied returns current price
create or replace function dish_price(dishId int, atDate timestamp default null) returns numeric(10, 2)
as
$$
DECLARE
    price numeric(10, 2);
BEGIN
    if atDate is null then
        set timezone = 'gmt -2';
        atDate = now();
    end if;

    select past.value
    into price
    from (select value
          from dishes
                   join price_history using (dish_id)
          where date <= atDate
        order by date desc) as past(value);
    if price is null then
        raise exception 'Dish with id % is cancelled',dishId;
    end if;
    return price;

END;
$$ LANGUAGE plpgsql;

--returns price of dish after applying highest customer/dish discount
create or replace function dish_discounted_price(dishId int, customerId int, atDate timestamp default null) returns numeric(10, 2)
as
$$
DECLARE
    dishDiscount numeric(2);
    customerDiscount numeric(2);
BEGIN
    if atDate is null then
        set timezone = 'gmt -2';
        atDate := now();
    end if;

    select discount
    into dishDiscount
    from dish_discounts
             join discounts using (discount_id)
    where dish_id = dishId
      and atDate::date between date_from and date_to
    order by discount desc;

    if dishDiscount is null then
        dishDiscount := 0;
    end if;

    select discount
    into customerDiscount
    from discounts
             join customer_discounts using (discount_id)
    where customer_id = customerId
      and atDate between date_from and date_to
    order by discount desc;

    if customerDiscount is null then
        customerDiscount := 0;
    end if;

    return dish_price(dishId, atDate) * (1.0 - greatest(customerDiscount,dishDiscount) / 100.0);
END;
$$ LANGUAGE plpgsql;

--returns total price of order without discounts
create or replace function order_total(orderId int) returns numeric(10, 2)
as
$$
DECLARE
    total     numeric(10, 2);
    orderDate timestamp;
BEGIN
    select ordered_date into orderDate from orders where order_id = orderId;
    select sum(dish_price(dish_id, orderDate) * quantity)
    into total
    from orders
             join order_details od using (order_id)
    where order_id = orderId;
    return total;
END;
$$ LANGUAGE plpgsql;

--returns value of order after applying highest of customer/dish discounts available to each dish
create or replace function order_total_discounted(orderId int) returns numeric(10, 2)
as
$$
DECLARE
    total     numeric(10, 2);
    orderDate timestamp;
    customerId int;
BEGIN
    select ordered_date into orderDate from orders where order_id = orderId;
    select customer_id into customerId from orders where order_id=orderId;
    select sum(dish_discounted_price(dish_id,customerId, orderDate) * quantity)
    into total
    from orders
             join order_details od using (order_id)
    where order_id = orderId;

    return total;
END;
$$ LANGUAGE plpgsql;




