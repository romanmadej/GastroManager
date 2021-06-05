--triggers
drop trigger update_ingredients_on_order_cancel on orders;
drop trigger overlap on special_dates;

--trigger functions
drop function update_ingredients_on_order_cancel();
drop function overlap();

--rules
drop rule no_order_del on orders;
drop rule no_order_details_del on order_details;
drop rule prevent_uncancelling on orders;

--views
drop view restaurants_open_status;
drop view customer_stats;
drop view customer_stats_monthly;

--procedures
drop function modify_stock(restaurantid integer, orderid integer, add boolean);
drop function add_order(orderid integer, customerid integer, restaurantid integer, ordereddate timestamp, statuss varchar, isdelivery boolean, dishesarr integer[], quantityarr integer[]);
drop function leq(a date, b date);
drop function is_open(restaurantid integer);
drop function dish_price(dishid integer, atdate timestamp);
drop function dish_discounted_price(dishid integer, atdate timestamp);
drop function customer_discounted_price(customerid integer, total numeric, atdate timestamp);
drop function order_total(orderid integer);
drop function order_total_dish_discounted(orderid integer);
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
