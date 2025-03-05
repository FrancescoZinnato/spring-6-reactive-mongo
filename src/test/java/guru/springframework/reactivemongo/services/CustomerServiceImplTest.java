package guru.springframework.reactivemongo.services;

import guru.springframework.reactivemongo.domain.Customer;
import guru.springframework.reactivemongo.mappers.CustomerMapper;
import guru.springframework.reactivemongo.model.CustomerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

@SpringBootTest
class CustomerServiceImplTest {

    @Autowired
    CustomerService customerService;

    @Autowired
    CustomerMapper customerMapper;

    CustomerDTO customerDTO;

    @BeforeEach
    void setUp() {
        customerDTO = customerMapper.customerToCustomerDTO(getTestCustomer());
    }

    @Test
    void testSaveCustomer() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        Mono<CustomerDTO> savedMono = customerService.saveCustomerMono(Mono.just(customerDTO));

        savedMono.subscribe(savedDTO -> {
                System.out.println("Customer Saved with ID: " + savedDTO.getId());
                atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);
    }

    public static Customer getTestCustomer() {
        return Customer.builder()
                .customerName("John Thompson")
                .build();
    }

}