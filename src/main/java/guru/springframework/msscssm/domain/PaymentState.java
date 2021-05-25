package guru.springframework.msscssm.domain;

/**
 * Created by jt on 2019-07-23.
 */
public enum PaymentState {
    
            //  The various states of the state machine
            //  Successful payment flow>new payment (NEW)>pre-authorized (PRE_AUTH)>authorized (AUTH).
            //  ERROR states, Peter I think this is the flow: NEW>PRE_AUTH_ERROR + PRE_AUTH>AUTH_ERROR        
            NEW, PRE_AUTH, PRE_AUTH_ERROR, AUTH, AUTH_ERROR
}
