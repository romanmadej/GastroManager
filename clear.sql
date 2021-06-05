--triggers
drop trigger overlap on special_dates;

--trigger functions
drop function overlap();

--views
drop view restaurants_open_status;

--procedures
drop function leq(a date, b date);
drop function is_open(restaurantid integer);
drop function inbetween(a date, b date, c date);


-- foreign keys
alter table ingredients_allergens
    drop constraint allergens_ingredients_allergens;

alter table customer_details
    drop constraint customer_details_customers;

alter table customer_discounts
    drop constraint customer_discounts_customers;

alter table orders
    drop constraint customers_orders;

alter table dish_discounts
    drop constraint discounts_dish_discounts;

alter table customer_discounts
    drop constraint discounts_customer_discounts;

alter table dish_ingredients
    drop constraint dish_details_ingredients;

alter table dish_discounts
    drop constraint dish_discounts_dishes;

alter table dishes
    drop constraint dishes_categories;

alter table dish_ingredients
    drop constraint dishes_dish_details;

alter table order_details
    drop constraint dishes_order_details;

alter table ingredients_allergens
    drop constraint ingriedients_allergents_ingredients;

alter table order_details
    drop constraint orders_order_details;

alter table orders
    drop constraint orders_restaurants;

alter table price_history
    drop constraint price_history_dishes;

alter table stock
    drop constraint restaurants_stock;

alter table opening_hours
    drop constraint restrictions_restaurants;

alter table special_dates
    drop constraint special_dates_restaurants;

alter table stock
    drop constraint stock_ingredients;

-- tables
drop table allergens;

drop table categories;

drop table customer_details;

drop table customer_discounts;

drop table customers;

drop table discounts;

drop table dish_discounts;

drop table dish_ingredients;

drop table dishes;

drop table ingredients;

drop table ingredients_allergens;

drop table opening_hours;

drop table order_details;

drop table orders;

drop table price_history;

drop table restaurants;

drop table special_dates;

drop table stock;

-- types
drop type diet_type;
