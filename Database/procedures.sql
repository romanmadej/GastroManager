--adds/subtracts ingredients required to cook a given dish from given's restaurants stock
create or replace function modify_stock(restaurantId int, dishId int, dishQuantity int, add boolean) returns void
as
$$
declare
    r               record;
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
                select quantity
                into currentQuantity
                from stock
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
    return leq(b, a) and leq(a, c);
END
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

    select special_date_id, opening_time, closing_time
    into r
    from special_dates
    where restaurant_id = restaurantId
      and inbetween(current_date, date_from, date_to)
    order by is_cyclic;


    if r is null then
        select special_date_id, opening_time, closing_time
        into r
        from special_dates
        where restaurant_id is null
          and inbetween(current_date, date_from, date_to)
        order by is_cyclic;
    end if;

    if r is null then
        select restaurant_id, opening_time, closing_time
        into r
        from opening_hours
        where restaurant_id = restaurantId
          and extract(isodow from current_date) = day;
    end if;

    return r.opening_time is not null and r.closing_time is not null and
           current_time between r.opening_time and r.closing_time;
END
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
            and dish_id = dishId
          order by date desc) as past(value);
    if price is null then
        raise exception 'Dish with id % is cancelled',dishId;
    end if;
    return price;

END
$$ LANGUAGE plpgsql;

--returns price of dish after applying highest customer/dish discount
create or replace function dish_discounted_price(dishId int, customerId int, atDate timestamp default null) returns numeric(10, 2)
as
$$
DECLARE
    dishDiscount     numeric(2);
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

    return dish_price(dishId, atDate) * (1.0 - greatest(customerDiscount, dishDiscount) / 100.0);
END
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
END
$$ LANGUAGE plpgsql;

--returns value of order after applying highest of customer/dish discounts available to each dish
create or replace function order_total_discounted(orderId int) returns numeric(10, 2)
as
$$
DECLARE
    total      numeric(10, 2);
    orderDate  timestamp;
    customerId int;
BEGIN
    select ordered_date into orderDate from orders where order_id = orderId;
    select customer_id into customerId from orders where order_id = orderId;
    select sum(dish_discounted_price(dish_id, customerId, orderDate) * quantity)
    into total
    from orders
             join order_details od using (order_id)
    where order_id = orderId;

    return total;
END
$$ LANGUAGE plpgsql;



create or replace function can_delete_restaurant(restaurantId int) returns boolean
as
$$
DECLARE
    noOrders boolean;
BEGIN
    select count(*) = 0 into noOrders from orders where restaurant_id = restaurantId;
    return noOrders;
END
$$ LANGUAGE plpgsql;

--returns:
--0 deletion failed
--1 if restaurant was removed
--2 there's no such restaurant
create or replace function delete_restaurant(restaurantId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from restaurants where restaurant_id = restaurantId;
    if not exists then
        return 2;
    end if;
    if not can_delete_restaurant(restaurantId) then
        return 0;
    end if;
    delete from opening_hours where restaurant_id = restaurantId;
    delete from stock where restaurant_id = restaurantId;
    delete from restaurants where restaurant_id = restaurantId;
    return 1;
END
$$ LANGUAGE plpgsql;


create or replace function can_delete_ingredient(ingredientId int) returns boolean
as
$$
DECLARE
    noDishes boolean;
BEGIN
    select count(*) = 0 into noDishes from dish_ingredients where ingredient_id = ingredientId;
    return noDishes;
END
$$ LANGUAGE plpgsql;

create or replace function delete_ingredient(ingredientId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from ingredients where ingredient_id = ingredientId;
    if not exists then
        return 2;
    end if;
    if not can_delete_ingredient(ingredientId) then
        return 0;
    end if;
    delete from ingredients_allergens where ingredient_id = ingredientId;
    delete from stock where ingredient_id = ingredientId;
    delete from ingredients where ingredient_id = ingredientId;
    return 1;
END
$$ LANGUAGE plpgsql;



create or replace function can_delete_dish(dishId int) returns boolean
as
$$
DECLARE
    noOrders boolean;
BEGIN
    select count(*) = 0 into noOrders from order_details where dish_id = dishId;
    return noOrders;
END
$$ LANGUAGE plpgsql;

create or replace function delete_dish(dishId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from dishes where dish_id = dishId;
    if not exists then
        return 2;
    end if;
    if not can_delete_dish(dishId) then
        return 0;
    end if;
    delete from price_history where dish_id = dishId;
    delete from dish_ingredients where dish_id = dishId;
    delete from dish_discounts where dish_id = dishId;
    delete from dishes where dish_id = dishId;
    return 1;
END
$$ LANGUAGE plpgsql;

create or replace function can_delete_dish_ingredient(dishId int) returns boolean
as
$$
DECLARE
    cnt int;
BEGIN
    select count(*) into cnt from dish_ingredients where dish_id = dishId;
    return cnt > 1;
END
$$ LANGUAGE plpgsql;

create or replace function delete_dish_ingredient(dishId int, ingredientId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from dish_ingredients where dish_id = dishId and ingredient_id = ingredientId;
    if not exists then
        return 2;
    end if;
    if not can_delete_dish_ingredient(dishId) then
        return 0;
    end if;
    delete from dish_ingredients where dish_id = dishId and ingredient_id = ingredientId;
    return 1;
END
$$ LANGUAGE plpgsql;


--returns true if discount hasn't been used in any order
create or replace function can_delete_discount(discountId int) returns boolean
as
$$
DECLARE
    discountFrom       date;
    discountTo         date;
    o                  record;
    od                 record;
    dishDiscount       int;
    customerDiscount   int;
    dishDiscountId     int;
    customerDiscountId int;

BEGIN
    select date_from into discountFrom from discounts where discount_id = discountId;
    select date_to into discountTo from discounts where discount_id = discountId;

    for o in select order_id, customer_id, ordered_date
             from orders
             where ordered_date between discountFrom and discountTo
        loop
            for od in select dish_id from order_details where order_id = o.order_id
                loop
                    select discount, discount_id
                    into dishDiscount,dishDiscountId
                    from discounts
                             join dish_discounts using (discount_id)
                    where o.ordered_date between date_from and date_to
                    order by discount desc;
                    select discount, discount_id
                    into customerDiscount,customerDiscountId
                    from discounts
                             join customer_discounts using (discount_id)
                    where customer_id = o.customer_id
                      and o.ordered_date between date_from and date_to
                    order by discount desc;
                    if dishDiscountId is null and customerDiscount is not null then
                        if customerDiscountId = discountId then
                            return false;
                        end if;
                    end if;

                    if dishDiscountId is not null and customerDiscount is null then
                        if dishDiscountId = discountId then
                            return false;
                        end if;
                    end if;

                    if dishDiscount > customerDiscount and dishDiscountId = discountId then
                        return false;
                    end if;

                    if customerDiscount > dishDiscount and customerDiscountId = discountId then
                        return false;
                    end if;

                    if customerDiscount = dishDiscount and customerDiscount = discountId or
                       dishDiscountId = discountId then
                        return false;
                    end if;
                end loop;
        end loop;
    return true;
END
$$ LANGUAGE plpgsql;

create or replace function delete_discount(discountId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from discounts where discount_id = discountId;
    if not exists then
        return 2;
    end if;
    if not can_delete_discount(discountId) then
        return 0;
    end if;
    delete from dish_discounts where discount_id = discountId;
    delete from customer_discounts where discount_id = discountId;
    delete from discounts where discount_id = discountId;
    return 1;
END
$$ LANGUAGE plpgsql;


create or replace function delete_special_date(specialdateId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from special_dates where special_date_id = specialDateId;
    if not exists then
        return 2;
    end if;
    delete from special_dates where special_date_id = specialDateId;
    return 1;
END
$$ LANGUAGE plpgsql;


create or replace function can_delete_category(categoryId int) returns boolean
as
$$
DECLARE
    noDishes boolean;
BEGIN
    select count(*) = 0 into noDishes from dishes where category_id = categoryId;
    return noDishes;
END
$$ LANGUAGE plpgsql;

create or replace function delete_category(categoryId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from categories where category_id = categoryId;
    if not exists then
        return 2;
    end if;
    if not can_delete_category(categoryId) then
        return 0;
    end if;
    delete from categories where category_id = categoryId;
    return 1;
END
$$ LANGUAGE plpgsql;


create or replace function delete_customer(customerId int) returns int
as
$$
declare
    exists boolean;
BEGIN
    select count(*) > 0 into exists from customer_details where customer_id = customerId;
    if not exists then
        return 2;
    end if;
    delete from customer_details where customer_id = customerId;
    return 1;
END
$$ LANGUAGE plpgsql;




