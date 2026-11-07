package com.seby.jobleaf.domain.port.in.auth;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;

import java.util.Locale;

import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * Inbound port for logging in with email and password.
 */
public interface LoginUseCase {

    /**
     * @throws InvalidCredentialsException when email or password is wrong
     */
    LoginResult login(LoginCommand command);

    record LoginCommand(String email, String password) {
        private static final String RESOURCE = "Login";

        public LoginCommand {
            email = normalizeNullableText(email);
            if (email == null) {
                throw DomainValidationException.requiredField("email", RESOURCE);
            }
            email = email.toLowerCase(Locale.ROOT);
            if (password == null || password.isEmpty()) {
                throw DomainValidationException.requiredField("password", RESOURCE);
            }
        }

        /**
         * Keeps the password out of logs and exception messages.
         */
        @Override
        public String toString() {
            return "LoginCommand[email=" + email + ", password=***]";
        }
    }

    record LoginResult(JobLeafUser user, AuthToken token) {
    }
}
