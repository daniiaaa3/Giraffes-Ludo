
package Model;

/**
 * Observer interface for educational question results.
 */
@FunctionalInterface
public interface QuestionObserver {

    /**
     * Called when a player answers a question.
     */
    void onQuestionAnswered(QuestionResultEvent event);
}
