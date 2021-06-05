--opening hours ranked from highest to lowest priority
-- 1.special_dates restaurant specific, non-cyclic
-- 2.special_dates restaurant specific, cyclic
-- 3.special_dates global(restaurant_id=null), non-cyclic
-- 4.special_dates global(restaurant_id=null), cyclic
-- 5.opening_hours
create or replace view restaurants_open_status(restaurant_id, is_open)
as
select restaurant_id, is_open(restaurant_id)
from restaurants;

create or replace view customer_stats_monthly(customer_id, revenue, full_value, discounts)
as
select customer_id,
       sum(least(order_total_dish_discounted(order_id),
                 customer_Discounted_Price(customer_id, order_total(order_id), ordered_date))),
       sum(order_total(order_id)),
       sum(order_total(order_id)) - sum(least(order_total_dish_discounted(order_id),
                                              customer_discounted_price(customer_id, order_total(order_id),
                                                                        ordered_date)))
from customers
         join orders using (customer_id)
where status = 'completed'
  and ordered_date > (now() - interval '30 days')::date
group by customer_id;

--all time customer stats
create or replace view customer_stats(customer_id, revenue, full_value, discounts)
as
select customer_id,
       sum(least(order_total_dish_discounted(order_id),
                 customer_Discounted_Price(customer_id, order_total(order_id), ordered_date))),
       sum(order_total(order_id)),
       sum(order_total(order_id)) - sum(least(order_total_dish_discounted(order_id),
                                              customer_discounted_price(customer_id, order_total(order_id),
                                                                        ordered_date)))
from customers
         join orders using (customer_id)
where status = 'completed'
group by customer_id;



