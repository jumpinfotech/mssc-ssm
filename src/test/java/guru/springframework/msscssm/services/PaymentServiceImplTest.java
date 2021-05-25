package guru.springframework.msscssm.services;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest // bring up SpringBoot context
class PaymentServiceImplTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    Payment payment;

    @BeforeEach
    void setUp() {
        // initialise payment
        payment = Payment.builder().amount(new BigDecimal("12.99")).build(); 
    }

    // @Transactional needed otherwise error as we are working outside of a transactional context:-
    // org.hibernate.LazyInitializationException: could not initialize proxy [guru.springframework.msscssm.domain.Payment#1] - no Session
    @Transactional
    @Test
    void preAuth() {
        // save the payment
        Payment savedPayment = paymentService.newPayment(payment); 

        System.out.println("Should be NEW");
        System.out.println(savedPayment.getState());

        // payment has been PRE_AUTHORIZE, we get state machine
        StateMachine<PaymentState, PaymentEvent> sm = paymentService.preAuth(savedPayment.getId()); 

        // get it back out of the database after we call the state machine
        Payment preAuthedPayment = paymentRepository.getOne(savedPayment.getId());

        System.out.println("Should be PRE_AUTH or PRE_AUTH_ERROR"); 
        System.out.println(sm.getState().getId()); // state transitioned from NEW to PRE_AUTH

        // Lombok does a toString()> accesses all object properties> we have some lazily + initially initialized properties
        // >therefore need to make tests transactional 
        System.out.println(preAuthedPayment); // console output=Payment(id=1, state=PRE_AUTH, amount=12.99)

    }


    @Transactional
    @RepeatedTest(10) // JUnit annotation
    void testAuth() {
        // save the payment
        Payment savedPayment = paymentService.newPayment(payment); 

        // get PRE_AUTHORIZE'd state machine
        StateMachine<PaymentState, PaymentEvent> preAuthSM = paymentService.preAuth(savedPayment.getId()); 

        //  if state machine is in PRE_AUTH state then authorize it, 
        //  if PRE_AUTH had failed we obviously can't proceed with authorization
        if (preAuthSM.getState().getId() == PaymentState.PRE_AUTH) { 
            System.out.println("Payment is Pre Authorized");
            //  2nd state machine passing in payment id to authorize that payment
            StateMachine<PaymentState, PaymentEvent> authSM = paymentService.authorizePayment(savedPayment.getId());

            System.out.println("Result of Auth: " + authSM.getState().getId());
        } else {
            System.out.println("Payment failed pre-auth...");
        }
    }
}
