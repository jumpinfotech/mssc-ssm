package guru.springframework.msscssm.services;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by jt on 2019-08-10.
 */
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {
    public static final String PAYMENT_ID_HEADER = "payment_id";

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;
    private final PaymentStateChangeInterceptor paymentStateChangeInterceptor;

    @Override
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW); // set state as NEW
        return paymentRepository.save(payment);
    }

    //  @Transactional > transaction but that transactional boundaries is within the action of the method call for Spring Data JPA??? Not sure what he means exactly.
    //  Using @Transactional sets the transactional context>we will be able to work with hibernate + won't get that a lazy initialization error. 
    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        // restore state machine from the database
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId); 
        //  send PRE_AUTHORIZE event into state machine>which triggers a state change transition
        sendEvent(paymentId, sm, PaymentEvent.PRE_AUTHORIZE);
        // return back state machine
        return sm; 
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTHORIZE);

        return sm;
    }

    @Deprecated // not needed - JT made a mistake, state machine actions will progress things to AUTH_APPROVED OR AUTH_DECLINED
    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTH_DECLINED);

        return sm;
    }

    //  sends event to state machine
    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent event){
        //  enriching org.springframework.messaging.Message with paymentId 
        Message msg = MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_ID_HEADER, paymentId)
                .build();
        //  I send to the state machine the enum event + paymentID
        sm.sendEvent(msg);
    }

    //  Use case>running some microservices> persisting state machine's state to the database> reset/rehydrate it to the proper state from the object in the database. 
    //  Separate processes are running over time e.g. could be minutes / hours between the pre-authorization + payment authorization.
    //  Considered expensive process to restore the state machine, sometimes with complex processing you might not persist a state machine to a database.
    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId){
        // get payment from database
        Payment payment = paymentRepository.getOne(paymentId); 

        // get state machine instance
        StateMachine<PaymentState, PaymentEvent> sm = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));

        sm.stop();

        sm.getStateMachineAccessor()
                // with doWithAllRegions we get a StateMachineAccessor
                .doWithAllRegions(sma -> { 
                    // interceptor gets payment out of DB + sets it to the event state + saves in DB
                    sma.addStateMachineInterceptor(paymentStateChangeInterceptor); 
                    // state machine is stopped, set it to the state of the payment that we got from the database
                    sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(), null, null, null));
                });

        // restart state machine
        sm.start(); 

        return sm;
    }
}
