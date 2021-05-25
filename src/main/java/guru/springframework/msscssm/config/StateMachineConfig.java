package guru.springframework.msscssm.config;

import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
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

/**
 * Created by jt on 2019-07-23.
 */
@Slf4j
@RequiredArgsConstructor // added
@EnableStateMachineFactory
@Configuration
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    // 3 original methods added as Spring Beans.
    // We have Action's, we are not autowiring by Type,
    // the property name e.g. preAuthAction matches the class name PreAuthAction.java
    // spring can autowire this property>advanced spring configuration,
    // if I renamed it to something other than the class name then I'd have to specify
    // a constructor and give it some type of Qualifier so spring would know which
    // instance I want, by default spring autowires by type + if multiple types exist
    // spring will try to match the property name to the bean name 
    // e.g. preAuthAction = PreAuthAction.java + authAction = AuthAction.java
    private final Action<PaymentState, PaymentEvent> preAuthAction;
    private final Action<PaymentState, PaymentEvent> authAction;
    private final Guard<PaymentState, PaymentEvent> paymentIdGuard;
    
    // notification actions
    private final Action<PaymentState, PaymentEvent> preAuthApprovedAction;
    private final Action<PaymentState, PaymentEvent> preAuthDeclinedAction;
    private final Action<PaymentState, PaymentEvent> authApprovedAction;
    private final Action<PaymentState, PaymentEvent> authDeclinedAction;

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(PaymentState.NEW)
                .states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH)
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZE)
                    .action(preAuthAction).guard(paymentIdGuard)
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                    .action(preAuthApprovedAction) // action added
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED)
                    .action(preAuthDeclinedAction) // action added
                //preauth to auth
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH).event(PaymentEvent.AUTHORIZE)
                    .action(authAction)
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(PaymentEvent.AUTH_APPROVED)
                    .action(authApprovedAction) // action added
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH_ERROR).event(PaymentEvent.AUTH_DECLINED)
                    .action(authDeclinedAction); // action added
                // authDeclinedAction may need to do lots of work, 
                // the authDeclinedAction is in 1 self contained class>helps to stop this class becoming huge
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<>(){
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info(String.format("stateChanged(from: %s, to: %s)", from, to));
            }
        };

        config.withConfiguration()
                .listener(adapter);
    }


//  Assignment to implement notification actions, here we commented out the original methods we used for the actions, 
//  as we'll implement these as beans.
//  Gives a realistic example how a state machine would be used in a microservice environment,
//  were the state machine is transitioning, our business logic is componentized.
    
// implemented now as a class msscssm\config\guards\PaymentIdGuard.java
//    public Guard<PaymentState, PaymentEvent> paymentIdGuard(){
//        return context -> {
//            return context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER) != null;
//        };
//    }

// implemented now as a class msscssm\config\actions\PreAuthAction.java    
//    public Action<PaymentState, PaymentEvent> preAuthAction(){
//        return context -> {
//            System.out.println("PreAuth was called!!!");
//
//            if (new Random().nextInt(10) < 8) {
//                System.out.println("Pre Auth Approved");
//                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED)
//                    .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
//                    .build());
//
//            } else {
//                System.out.println("Per Auth Declined! No Credit!!!!!!");
//                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_DECLINED)
//                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
//                        .build());
//            }
//        };
//    }

// implemented now as a class msscssm\config\actions\AuthAction.java    
//    public Action<PaymentState, PaymentEvent> authAction(){
//        return context -> {
//            System.out.println("Auth was called!!!");
//
//            if (new Random().nextInt(10) < 8) {
//                System.out.println("Auth Approved");
//                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTH_APPROVED)
//                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
//                        .build());
//
//            } else {
//                System.out.println("Auth Declined! No Credit!!!!!!");
//                context.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTH_DECLINED)
//                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
//                        .build());
//            }
//        };
//    }
}
