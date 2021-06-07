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
       sum(order_total_discounted(order_id)),
       sum(order_total(order_id)),
       sum(order_total(order_id) - order_total_discounted(order_id))
from customers
         join orders using (customer_id)
where status = 'completed'
  and ordered_date > (now() - interval '30 days')::date
group by customer_id;

--all time customer stats
create or replace view customer_stats(customer_id, revenue, full_value, discounts)
as
select customer_id,
       sum(order_total_discounted(order_id)),
       sum(order_total(order_id)),
       sum(order_total(order_id) - order_total_discounted(order_id))
from customers
         join orders using (customer_id)
where status = 'completed'
group by customer_id;



create or replace view menu_positions (restaurant_id, dish_id, is_available, diet, allergens, price) as
select r.restaurant_id,
       dish_id,
       every(di.quantity <= coalesce(s.quantity, 0)),
       min(i.diet),
       array_remove(array_agg(distinct a.name), null),
       dish_price(dish_id)
from restaurants r
         cross join dish_ingredients di
         left join stock s on (r.restaurant_id, di.ingredient_id) = (s.restaurant_id, s.ingredient_id)
         join ingredients i on di.ingredient_id = i.ingredient_id
         left join ingredients_allergens ia on i.ingredient_id = ia.ingredient_id
         left join allergens a on ia.allergen_id = a.allergen_id
group by r.restaurant_id, dish_id;
