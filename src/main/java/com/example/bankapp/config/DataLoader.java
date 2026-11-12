package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }
        customerRepository.save(new Customer(null, "100001", "Kamal Perera", "kamal@example.com", new BigDecimal("50000.00")));
        customerRepository.save(new Customer(null, "100002", "Nimali Silva", "nimali@example.com", new BigDecimal("25000.00")));
        customerRepository.save(new Customer(null, "100003", "Saman Kumara", "saman@example.com", new BigDecimal("10000.00")));
    }
}