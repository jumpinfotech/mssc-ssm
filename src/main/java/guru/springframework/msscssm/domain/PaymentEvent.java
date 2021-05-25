package guru.springframework.msscssm.domain;

/**
 * Created by jt on 2019-07-23.
 */
public enum PaymentEvent {
    
    //  Events we send to the state machine, is this right Peter????:-
    //  PRE_AUTHORIZE>call the credit card processor to pre authorize a transaction
    //  APPROVED>PRE_AUTH_APPROVED    
    //  DECLINED>PRE_AUTH_DECLINED
    //  AUTHORIZE>authorize the payment, take it from the PRE_AUTH state
    //  APPROVED>AUTH_APPROVED
    //  DECLINED>AUTH_DECLINED
    PRE_AUTHORIZE, PRE_AUTH_APPROVED, PRE_AUTH_DECLINED, AUTHORIZE, AUTH_APPROVED, AUTH_DECLINED
}
