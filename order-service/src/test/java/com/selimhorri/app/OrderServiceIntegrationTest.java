package com.selimhorri.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:order_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.config.import=optional:file:./",
        "SPRING_CONFIG_IMPORT=optional:file:./",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "server.servlet.context-path="
})
class OrderServiceIntegrationTest {

    private static final LocalDateTime EXISTING_ORDER_DATE = LocalDateTime.of(2024, 3, 10, 9, 0, 0);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    private Integer existingId;

    @BeforeEach
    void setUp() {
        this.orderRepository.deleteAll();
        Order order = Order.builder()
                .orderDate(EXISTING_ORDER_DATE)
                .orderDesc("Initial order")
                .orderFee(12.5)
                .build();
        Order saved = this.orderRepository.save(order);
        this.existingId = saved.getOrderId();
    }

    @Test
    void findAllShouldReturnPersistedOrders() {
        List<OrderDto> orders = this.orderService.findAll();
        assertThat(orders).isNotEmpty();
        OrderDto dto = orders.get(0);
        assertThat(dto.getOrderDate()).isEqualTo(EXISTING_ORDER_DATE);
        assertThat(dto.getOrderDesc()).isEqualTo("Initial order");
        assertThat(dto.getOrderFee()).isEqualTo(12.5);
    }

    @Test
    void findByIdShouldReturnOrderWhenPresent() {
        OrderDto dto = this.orderService.findById(this.existingId);
        assertThat(dto).isNotNull();
        assertThat(dto.getOrderId()).isEqualTo(this.existingId);
        assertThat(dto.getOrderDate()).isEqualTo(EXISTING_ORDER_DATE);
    }

    @Test
    void saveShouldPersistOrder() {
        OrderDto payload = OrderDto.builder()
                .orderDate(EXISTING_ORDER_DATE.plusDays(1))
                .orderDesc("New order")
                .orderFee(5.0)
                .build();

        OrderDto saved = this.orderService.save(payload);

        Optional<Order> persisted = this.orderRepository.findById(saved.getOrderId());
        assertThat(persisted).isPresent();
        assertThat(saved.getOrderDesc()).isEqualTo("New order");
    }

    @Test
    void deleteByIdShouldRemoveOrder() {
        this.orderService.deleteById(this.existingId);
        assertThat(this.orderRepository.findAll()).isEmpty();
    }

    @Test
    void findByIdShouldThrowWhenNotPresent() {
        assertThrows(OrderNotFoundException.class, () -> this.orderService.findById(99999));
    }

}
