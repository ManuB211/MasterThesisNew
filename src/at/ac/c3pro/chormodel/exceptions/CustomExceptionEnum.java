package at.ac.c3pro.chormodel.exceptions;

/**
 * Lists all custom exceptions. Used to count occurences of Exceptions to create retry mechanism
 */
public enum CustomExceptionEnum {
    PRIVATE_MODEL_DISCONNECTED,
    NO_TRACES_TO_END_FOUND,
    TWO_HOW_RECEIVE_ONE_PARTICIPANT,
    NOT_EASY_SOUND;
}
