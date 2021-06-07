create rule no_delete_orders as on delete to orders
    do instead nothing;

create rule no_delete_order_details as on delete to order_details
    do instead nothing;

create rule prevent_order_uncancelling as on update to orders where old.status = 'cancelled' and new.status != 'cancelled'
    do instead nothing;