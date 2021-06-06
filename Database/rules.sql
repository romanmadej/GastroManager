create rule no_order_del as on delete to orders
    do instead nothing;

create rule no_order_details_del as on delete to order_details
    do instead nothing;

create rule prevent_uncancelling as on update to orders where old.status = 'cancelled' and new.status != 'cancelled'
    do instead nothing;