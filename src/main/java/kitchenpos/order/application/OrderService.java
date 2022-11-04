package kitchenpos.order.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import kitchenpos.menu.dao.MenuDao;
import kitchenpos.order.dao.OrderDao;
import kitchenpos.order.dao.OrderLineItemDao;
import kitchenpos.table.dao.OrderTableDao;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.order.dto.OrderLineItemRequest;
import kitchenpos.order.dto.OrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final MenuDao menuDao;
    private final OrderDao orderDao;
    private final OrderLineItemDao orderLineItemDao;
    private final OrderTableDao orderTableDao;

    public OrderService(
            final MenuDao menuDao,
            final OrderDao orderDao,
            final OrderLineItemDao orderLineItemDao,
            final OrderTableDao orderTableDao
    ) {
        this.menuDao = menuDao;
        this.orderDao = orderDao;
        this.orderLineItemDao = orderLineItemDao;
        this.orderTableDao = orderTableDao;
    }

    @Transactional
    public Order create(final OrderRequest orderRequest) {
        final Order order = toOrder(orderRequest);

        validateOrderLineItems(order);

        final OrderTable orderTable = orderTableDao.findById(order.getOrderTableId())
                .orElseThrow(IllegalArgumentException::new);

        final Order savedOrder = saveOrder(order, orderTable);

        saveOrderLineItems(savedOrder, order.getOrderLineItems());

        return savedOrder;
    }

    private Order toOrder(final OrderRequest orderRequest) {
        return new Order(orderRequest.getOrderTableId(),
                OrderStatus.from(orderRequest.getOrderStatus()),
                toOrderLineItems(orderRequest.getOrderLineItemRequests()));
    }

    private List<OrderLineItem> toOrderLineItems(final List<OrderLineItemRequest> orderLineItemRequests) {
        if (orderLineItemRequests == null) {
            throw new IllegalArgumentException();
        }
        return orderLineItemRequests.stream()
                .map(it -> new OrderLineItem(it.getSeq(),
                        it.getMenuId(),
                        it.getMenuPrice(),
                        it.getMenuName(),
                        it.getQuantity())).collect(Collectors.toList());
    }

    private void validateOrderLineItems(final Order order) {
        final List<Long> menuIds = order.getOrderMenuIds();
        order.validateOrderLineItemsSize(menuDao.countByIdIn(menuIds));
    }

    private Order saveOrder(final Order order, final OrderTable orderTable) {
        final Order newOrder = new Order(orderTable.getId(), OrderStatus.COOKING,
                LocalDateTime.now(), order.getOrderLineItems());
        return orderDao.save(newOrder);
    }

    private void saveOrderLineItems(final Order savedOrder, final List<OrderLineItem> orderLineItems) {
        final Long orderId = savedOrder.getId();
        final List<OrderLineItem> savedOrderLineItems = new ArrayList<>();
        for (final OrderLineItem orderLineItem : orderLineItems) {
            orderLineItem.addOrderId(orderId);
            savedOrderLineItems.add(orderLineItemDao.save(orderLineItem));
        }
        savedOrder.changeOrderLineItems(savedOrderLineItems);
    }

    public List<Order> list() {
        final List<Order> orders = orderDao.findAll();

        for (final Order order : orders) {
            order.changeOrderLineItems(orderLineItemDao.findAllByOrderId(order.getId()));
        }

        return orders;
    }

    @Transactional
    public Order changeOrderStatus(final Long orderId, final Order order) {
        final Order savedOrder = orderDao.findById(orderId)
                .orElseThrow(IllegalArgumentException::new);

        savedOrder.validateOrderStatus();
        savedOrder.changeOrderStatus(order.getOrderStatus());
        orderDao.save(savedOrder);

        savedOrder.changeOrderLineItems(orderLineItemDao.findAllByOrderId(orderId));

        return savedOrder;
    }
}
