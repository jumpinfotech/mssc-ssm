package guru.springframework.msscssm.repository;

import guru.springframework.msscssm.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by jt on 2019-07-23.
 */
// H2 in-memory database is on our class path
public interface PaymentRepository extends JpaRepository<Payment, Long> { 
}
