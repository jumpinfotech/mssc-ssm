package guru.springframework.msscssm.config;

import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.services.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;
import java.util.Random;

/**
 * Created by jt on 2019-07-23.
 */
@Slf4j
@EnableStateMachineFactory // component generates the state machine
@Configuration
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    //  org.springframework.statemachine.config.StateMachineConfigurerAdapter>the base implementation
    //  in the same directory there's a EnumStateMachineConfigurerAdapter> we're doing enumerations>
    //  you can use either one, they effectively do the same thing. 
    //  There's a builder pattern to generate the state machine factory>out of scope of this course>consult the documentation.  
    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        //  tell the state machine configuration about the state machine
        states.withStates() 
                .initial(PaymentState.NEW) // initial state
                .states(EnumSet.allOf(PaymentState.class)) // get the enumeration list from PaymentState + load all states
                .end(PaymentState.AUTH) // end = state machine's terminal(finished) states, AUTH is happy path
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        //  withExternal() - an external configuration
        //  source - the beginning state PaymentState.NEW>events can cause a state to remain (PaymentState.NEW)>
        //  doesn't necessarily cause a state change, 
        //  (PaymentEvent.PRE_AUTHORIZE)>calls our virtual credit card processor
        //  pre-authorize>has no state change>it remains PaymentState.NEW

        
        // Staying in NEW when I get event PRE_AUTHORIZE - no state transition happened
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZE) 
                //  when the PRE_AUTHORIZE event happens>preAuthAction() is triggered,
                //  guard added on this first transition>could reuse on other transitions,
                //  can add in multiple actions + multiple guards. 
                .action(preAuthAction()).guard(paymentIdGuard())
                .and()
                // Going from NEW to PRE_AUTH when I get event PRE_AUTH_APPROVED - event triggered a state transition
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                .and()
                // Going from NEW to PRE_AUTH_ERROR when I get event PRE_AUTH_DECLINED
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED)
                //preauth to auth
                //state machine is in preauth state, we send an AUTHORIZE event which we handle with an action handler>authAction()
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH).event(PaymentEvent.AUTHORIZE)
                .action(authAction())
                .and()
                // AUTH_APPROVED event will move the state from PRE_AUTH to AUTH
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(PaymentEvent.AUTH_APPROVED)
                .and()
                // AUTH_DECLINED event will move the state from PRE_AUTH to AUTH_ERROR
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH_ERROR).event(PaymentEvent.AUTH_DECLINED);
    }

    //  Springs State Machine supports event listeners>this configures a listener to see the state machine transitions in the logs 
    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<>(){
            // interface>intelliJ>Generate>Override Methods>implement this interface method
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info(String.format("stateChanged(from: %s, to: %s)", from, to));
            }
        };

        // add our listener:-
        config.withConfiguration().listener(adapter);
        //example log output .... stateChanged(from: ObjectState [getIds()=[NEW] ... to: ObjectState [getIds()=[PRE_AUTH] ....
    }



    //  Guards lets you approve an action in the state machine.
    //  Within a guard>could have business logic e.g. payment is greater than $1.
    public Guard<PaymentState, PaymentEvent> paymentIdGuard(){
        //  If PAYMENT_ID_HEADER is null=problem, payment won't be found.
        //  Guard requires our state machine have a PAYMENT_ID_HEADER on it>otherwise action won't take place.
        return context -> {
            return context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER) != null;
        };
    }

    //  simulates an pre-authorisation service> sends a PRE_AUTH_APPROVED or PRE_AUTH_DECLINED event to the state machine 
    public Action<PaymentState, PaymentEvent> preAuthAction(){  
        // can do what you want to react to a state machine event e.g. send a message, do business logic, write to a DB, call a web service. 
        return context -> {
            System.out.println("PreAuth was called!!!");

            if (new Random().nextInt(10) < 8) { // happens 80% of the time
                System.out.println("Pre Auth Approved");
                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED) // event = PRE_AUTH_APPROVED
                    .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                    .build());

            } else {
                System.out.println("Per Auth Declined! No Credit!!!!!!");
                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_DECLINED)  // event = PRE_AUTH_DECLINED
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }
        };
    }

    //  simulates an authorisation service> sends a AUTH_APPROVED or AUTH_DECLINED event to the state machine 
    public Action<PaymentState, PaymentEvent> authAction(){ 
        return context -> {
            System.out.println("Auth was called!!!");

            if (new Random().nextInt(10) < 8) {
                System.out.println("Auth Approved");
                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTH_APPROVED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());

            } else {
                System.out.println("Auth Declined! No Credit!!!!!!");
                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTH_DECLINED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }
        };
    }
}
