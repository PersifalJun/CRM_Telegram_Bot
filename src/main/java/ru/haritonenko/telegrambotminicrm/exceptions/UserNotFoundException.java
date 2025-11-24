package ru.haritonenko.telegrambotminicrm.exceptions;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
