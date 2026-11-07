package com.seby.jobleaf.domain.port.in.auth;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * Inbound port for creating an account. A successful registration also logs the user in.
 * <p>
 * The role is not chosen by the user: every new account is a {@code ROLE_JOB_SEEKER}; only an admin can change it.
 */
public interface RegisterUseCase {

    /**
     * @return the new user and an access token, like a login
     * @throws com.seby.jobleaf.domain.exceptions.auth.UserAlreadyExistsException when the email or mobile number is
     *                                                                            already used
     * @throws DomainValidationException                                          when the password is known from
     *                                                                            data breaches
     */
    LoginResult register(RegisterCommand command);

    record RegisterCommand(String name, String email, String mobileNumber, String password) {
        private static final String RESOURCE = "Registration";
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_PASSWORD_LENGTH = 72;
        private static final Pattern MOBILE_NUMBER = Pattern.compile("^\\+[1-9]\\d{6,14}$");

        public RegisterCommand {
            name = requireText(name, "name");
            email = requireText(email, "email").toLowerCase(Locale.ROOT);
            mobileNumber = requireText(mobileNumber, "mobileNumber");
            if (!MOBILE_NUMBER.matcher(mobileNumber).matches()) {
                throw DomainValidationException.invalidField("mobileNumber", "Mobile number must be in international format, e.g. +40712345678");
            }
            if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
                throw DomainValidationException.invalidField("password", "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters");
            }
        }

        private static String requireText(String value, String field) {
            String normalized = normalizeNullableText(value);
            if (normalized == null) {
                throw DomainValidationException.requiredField(field, RESOURCE);
            }
            return normalized;
        }

        /**
         * Keeps the password out of logs and exception messages.
         */
        @Override
        public String toString() {
            return "RegisterCommand[name=" + name + ", email=" + email + ", mobileNumber=" + mobileNumber
                    + ", password=***]";
        }
    }
}
