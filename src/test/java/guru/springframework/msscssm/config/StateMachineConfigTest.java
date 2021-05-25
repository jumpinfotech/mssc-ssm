package guru.springframework.msscssm.config;

import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import java.util.UUID;

@SpringBootTest // use SpringBoot context 
class StateMachineConfigTest {

    @Autowired // tell SpringBoot to inject that
    StateMachineFactory<PaymentState, PaymentEvent> factory; // intelliJ isn't picking up the context for the StateMachine, factory is underlined

    @Test // intelliJ>Generate>Test Method
    void testNewStateMachine() {
        //  id=randomUUID maybe optional
        StateMachine<PaymentState, PaymentEvent> sm = factory.getStateMachine(UUID.randomUUID()); 

        sm.start();//  the state machine "is running", you can set some things, but when you start it that may trigger action.????

        //  state initialises as NEW
        System.out.println(sm.getState().toString());  

        sm.sendEvent(PaymentEvent.PRE_AUTHORIZE);

        //  state shouldn't transition, stays NEW
        System.out.println(sm.getState().toString()); 

        sm.sendEvent(PaymentEvent.PRE_AUTH_APPROVED);

        //  state should transition to PRE_AUTH, console output> ObjectState [getIds()=[PRE_AUTH],...
        System.out.println(sm.getState().toString()); 

        sm.sendEvent(PaymentEvent.PRE_AUTH_DECLINED);

        //  state stays PRE_AUTH > as we haven't defined a PRE_AUTH_DECLINED event to transition a PRE_AUTH state, event is ignored>we get no error
        System.out.println(sm.getState().toString()); 

    }
}
