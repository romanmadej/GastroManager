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


