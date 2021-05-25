package guru.springframework.msscssm.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by jt on 2019-07-23.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity //standard Java API
public class Payment {

    @Id
    @GeneratedValue
    private Long id;

    // tells Hibernate to create a varchar (the enumeration name), 
    // JT believes without this you'll get the enumeration as a number
    @Enumerated(EnumType.STRING)
    private PaymentState state;

    private BigDecimal amount;
}
