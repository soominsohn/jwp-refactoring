package kitchenpos.domain;

import static kitchenpos.application.ServiceTestFixture.ORDER_LINE_ITEMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void throw_exception_when_orderLineItems_empty() {
        assertThatThrownBy(() -> new Order(1L, OrderStatus.MEAL.name(), new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateOrderLineItemsSize_fail_when_different_size() {
        final Order order = new Order(1L, OrderStatus.MEAL.name(), ORDER_LINE_ITEMS);

        assertThatThrownBy(() -> order.validateOrderLineItemsSize(4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateOrderStatus_fail_when_orderStatus_is_COMPLETION() {
        final Order order = new Order(1L, OrderStatus.COMPLETION.name(), ORDER_LINE_ITEMS);

        assertThatThrownBy(order::validateOrderStatus)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeOrderStatus() {
        final Order order = new Order(1L, OrderStatus.MEAL.name(), ORDER_LINE_ITEMS);
        final Order newOrder = new Order(1L, OrderStatus.COOKING.name(), ORDER_LINE_ITEMS);

        order.changeOrderStatus(newOrder);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COOKING.name());
    }
}