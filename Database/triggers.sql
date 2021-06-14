create or replace function deplete_ingredients_on_order_addition() returns trigger
as
$$
declare
    restaurantId int;
BEGIN
    select restaurant_id into restaurantId from orders where order_id = new.order_id;
    perform modify_stock(restaurantId, new.dish_id, new.quantity, false);
    return new;
END
$$ LANGUAGE plpgsql;


create trigger deplete_ingredients_on_order_addition
    after insert
    on order_details
    for each row
execute procedure deplete_ingredients_on_order_addition();

create or replace function update_ingredients_on_order_cancel() returns trigger
as
$$
declare
    r record;
BEGIN
    if (old.status = 'open' or old.status = 'completed') and new.status = 'cancelled' then
        for r in (select dish_id, quantity
                  from orders
                           join order_details using (order_id)
                  where order_id = old.order_id)
            loop
                perform modify_stock(old.restaurant_id, r.dish_id, r.quantity, true);
            end loop;
--         perform modify_stock(new.restaurant_id, new.order_id, true);
    end if;
    return new;
END
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
    select count(*)
    into cnt
    from special_dates
    where restaurant_id = new.restaurant_id
      and is_cyclic = new.is_cyclic
      and (inbetween(new.date_from, date_from, date_to)
        or inbetween(new.date_to, date_from, date_to));
    if cnt != 0 then
        return null;
    end if;
    return new;
END
$$ LANGUAGE plpgsql;

create trigger overlap
    before insert
    on special_dates
    for each row
execute procedure overlap();


--when inserting a restaurant a record for every ingredient(possibly with quantity 0) must be inserted into its stock
--and default opening hours for all days of the week must be inserted into opening_hours. All has to be done in a single transaction.
create or replace function restaurants_insert() returns trigger
as
$$
declare
    cntIngredients int;
    cntStock       int;
    cntDays        int;
BEGIN
    select count(*) into cntIngredients from ingredients;
    select count(*) into cntStock from stock where restaurant_id = new.restaurant_id;
    select count(*) into cntDays from opening_hours where restaurant_id = new.restaurant_id;
    if cntIngredients != cntStock then
        raise exception 'Every ingredient should have a corresponding record in stock INSERTED IN THE SAME TRANSACTION as restaurant. Restaurant_id: % lacks % ingredient records in stock.',new.restaurant_id,cntIngredients - cntStock;
    end if;
    if cntDays != 7 then
        raise exception 'When creating a restaurant default opening hours for all days of the week must be inserted IN THE SAME TRANSACTION into "opening_hours". Condition failed for restaurant_id: %',new.restaurant_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger restaurants_insert
    after insert
    on restaurants
    initially deferred
    for each row
execute procedure restaurants_insert();

--when deleting from stock according ingredient from table "ingredients" or restaurant from table "restaurants" should be deleted in the same transaction
create or replace function stock_delete() returns trigger
as
$$
declare
    ingredientDeleted boolean;
    restaurantDeleted boolean;
BEGIN
    select count(*) = 0 into ingredientDeleted from ingredients where ingredient_id = old.ingredient_id;
    select count(*) = 0 into restaurantDeleted from restaurants where restaurant_id = old.restaurant_id;
    if not restaurantDeleted and not ingredientDeleted then
        raise exception 'When deleting from stock according ingredient from table "ingredients" or restaurant should be deleted in the same transaction. Condition failed for  restaurant_id : %, ingredient_id %',old.restaurant_id,old.ingredient_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger stock_delete
    after delete
    on stock
    initially deferred
    for each row
execute procedure stock_delete();

--when creating a new ingredient entries corresponding to this ingredient should be inserted into every restaurant's stock in the same transaction
create or replace function ingredient_insert() returns trigger
as
$$
declare
    cntStocks      int;
    cntRestaurants int;
BEGIN
    select count(*) into cntStocks from stock where ingredient_id = new.ingredient_id;
    select count(*) into cntRestaurants from restaurants;
    if cntRestaurants != cntStocks then
        raise exception 'when creating a new ingredient entries corresponding to this ingredient should be inserted into every restaurant''s stock IN THE SAME TRANSACTION. Condition failed for ingredient_id: %',new.ingredient_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger ingredient_insert
    after insert
    on ingredients
    initially deferred
    for each row
execute procedure ingredient_insert();

--when creating a new dish corresponding records have to be inserted into price_history and dish_ingredients in the same transaction
create or replace function dish_insert() returns trigger
as
$$
declare
    priceInserted      boolean;
    ingredientInserted boolean;
BEGIN
    select count(*) != 0 into priceInserted from price_history where dish_id = new.dish_id;
    select count(*) != 0 into ingredientInserted from dish_ingredients where dish_id = new.dish_id;
    if not priceInserted or not ingredientInserted then
        raise exception 'when creating a new dish corresponding records have to be inserted into price_history and dish_ingredients IN THE SAME TRANSACTION. Condition failed for dish_id: %',new.dish_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger dish_insert
    after insert
    on dishes
    initially deferred
    for each row
execute procedure dish_insert();

--when price_history record is to be removed according dish has to be removed from "dishes" in the same transaction
create or replace function price_history_delete() returns trigger
as
$$
declare
    dishDeleted boolean;
BEGIN
    select count(*) = 0 into dishDeleted from dishes where dishes.dish_id = old.dish_id;
    if not dishDeleted then
        raise exception 'When price_history record is to be removed according dish has to be removed from "dishes" IN THE SAME TRANSACTION. Condition failed for dish_id: %',old.dish_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger price_history_delete
    after delete
    on price_history
    initially deferred
    for each row
execute procedure price_history_delete();

--when opening_hours record is to be removed corresponding restaurant has to be removed in the same transaction
create or replace function opening_hours_delete() returns trigger
as
$$
declare
    restaurantDeleted boolean;
BEGIN
    select count(*) = 0 into restaurantDeleted from restaurants where restaurant_id = old.restaurant_id;
    if not restaurantDeleted then
        raise exception 'When opening_hours record is to be removed corresponding restaurant has to be removed in the same transaction. Condition failed for restaurant_id %',old.restaurant_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger opening_hours_delete
    after delete
    on opening_hours
    initially deferred
    for each row
execute procedure opening_hours_delete();


--when last dish ingredient is to be deleted corresponding dish has to be deleted in the same transaction
create or replace function dish_ingredient_delete() returns trigger
as
$$
declare
    ingredientsEmpty boolean;
    dishDeleted      boolean;

BEGIN
    select count(*) = 0 into ingredientsEmpty from dish_ingredients where dish_id = old.dish_id;
    select count(*) = 0 into dishDeleted from dishes where dish_id = old.dish_id;
    if ingredientsEmpty and not dishDeleted then
        raise exception 'When last dish ingredient is to be deleted corresponding dish has to be deleted in the same transaction. Condition failed for dish_id: % , ingredient_id %',old.dish_id,old.ingredient_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger dish_ingredient_delete
    after delete
    on dish_ingredients
    initially deferred
    for each row
execute procedure dish_ingredient_delete();

create or replace function make_order() returns trigger
as
$$
BEGIN
    if new.customer_id != 0 and not is_open(new.restaurant_id) then
        raise exception 'Closed restaurants don''t accept orders';
    end if;
    return new;
END
$$ LANGUAGE plpgsql;

create constraint trigger make_order
    after insert
    on orders
    initially deferred
    for each row
execute procedure make_order();

--customer must have customer_details record to place a delivery order
create or replace function delivery_requires_address() returns trigger
as
$$
BEGIN
    if new.is_delivery and (new.customer_id = 0 or not exists(select * from customer_details where customer_id = new.customer_id)) then
        raise exception ' Delivery is not possible! Customer % doesn''t have an address.',new.customer_id;
    end if;
    return new;
END
$$ LANGUAGE plpgsql;

create trigger delivery_requires_address
    before insert
    on orders
    for each row
execute procedure delivery_requires_address();

--Address cannot be modified when there''s an open order
create or replace function address_change() returns trigger
as
$$
BEGIN
    if exists(select * from orders where customer_id = old.customer_id and status = 'open' and is_delivery) then
        raise exception 'Address cannot be modified when there''s an open delivery order';
    end if;
    return new;
END
$$ LANGUAGE plpgsql;

create trigger address_change_upd
    before update
    on customer_details
    for each row
execute procedure address_change();

create trigger address_change_del
    before delete
    on customer_details
    for each row
execute procedure address_change();