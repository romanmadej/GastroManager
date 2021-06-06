--adds/subtracts from stock all ingredients needed to complete order
create or replace function modify_stock(restaurantId int, orderId int, add boolean) returns void
as
$$
declare
    r record;
BEGIN
    for r in select ing.ingredient_id, od.quantity * di.quantity as quantity
             from order_details od
                      join dishes d using (dish_id)
                      join dish_ingredients di using (dish_id)
                      join ingredients ing using (ingredient_id)
             where orderId = od.order_id
        loop
            if add = true then
                update stock
                set quantity = quantity + r.quantity
                where restaurant_id = restaurantId
                  and ingredient_id = r.ingredient_id;
            else
                update stock
                set quantity = quantity - r.quantity
                where restaurant_id = restaurantId
                  and ingredient_id = r.ingredient_id;
            end if;
        end loop;
END;
$$ LANGUAGE plpgsql;

--orders should be added with this procedure to update stock levels
create or replace function add_order(orderId int, customerId int, restaurantId int, orderedDate timestamp,
                                     statuss varchar, isDelivery boolean, dishesArr int[],
                                     quantityArr int[]) returns void
as
$$
DECLARE
    i int;
BEGIN
    insert into orders values (orderId, customerId, restaurantId, orderedDate, statuss, isDelivery);
    for i in 1..array_length(dishesArr, 1)
        loop
            insert into order_details values (orderId, dishesArr[i], quantityArr[i]);
        end loop;
    perform modify_stock(restaurantId, orderId, false);
END;
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




