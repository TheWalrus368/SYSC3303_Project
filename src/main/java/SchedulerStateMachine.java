public class SchedulerStateMachine {
    /**
     *Interface representing a state in the SchedulerStateMachine
     **/
    interface SchedulerState {
        /**
         * Handles the logic of the current state and determines the next state.
         *
         * @param context The SchedulerStateMachine context.
         */
        void handle(SchedulerStateMachine context);
    }
}
