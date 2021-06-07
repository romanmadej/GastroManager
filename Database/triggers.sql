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


--when inserting a restaurant a record for every ingredient(possibly with quantity 0) must be inserted into its stock in the same transaction
create or replace function stock_cross_join_ingredients_insert() returns trigger
as
$$
declare
    cntIngredients int;
    cntStock int;
BEGIN
    select count(*) into cntIngredients from ingredients;
    select count(*) into cntStock from stock where restaurant_id=new.restaurant_id;
    if cntIngredients != cntStock then
        raise exception 'Every ingredient should have a corresponding record in stock INSERTED IN THE SAME TRANSACTION as restaurant. Restaurant_id: % lacks % ingredient records in stock.',new.restaurant_id,cntIngredients-cntStock;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger stock_cross_join_ingredients_insert after insert on restaurants
    initially deferred for each row execute procedure stock_cross_join_ingredients_insert();


--when deleting from stock according ingredient from table "ingredients" should be deleted in the same transaction
create or replace function stock_delete() returns trigger
as
$$
declare
    ingredientDeleted boolean;
BEGIN
    select count(*)=0 into ingredientDeleted from ingredients where ingredient_id=old.ingredient_id;
    if not ingredientDeleted then
        raise exception 'when deleting from stock according ingredient from table "ingredients" should be DELETED IN THE SAME TRANSACTION. Condition failed for ingredient_id %',old.ingredient_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger stock_delete after delete on stock
    initially deferred for each row execute procedure stock_delete();

--when creating a new ingredient entries corresponding to this ingredient should be inserted into every restaurant's stock in the same transaction
create or replace function ingredient_insert() returns trigger
as
$$
declare
    cntStocks int;
    cntRestaurants int;
BEGIN
    select count(*) into cntStocks from stock where ingredient_id=new.ingredient_id;
    select count(*) into cntRestaurants from restaurants;
    if cntRestaurants != cntStocks then
        raise exception 'when creating a new ingredient entries corresponding to this ingredient should be inserted into every restaurant''s stock in the same transaction. Condition failed for ingredient_id: %',new.ingredient_id;
    end if;
    return null;
END
$$ LANGUAGE plpgsql;

create constraint trigger ingredient_insert after insert on ingredients
    initially deferred for each row execute procedure ingredient_insert();


