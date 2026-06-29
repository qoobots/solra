package com.solra.auth.domain.service;

import com.solra.auth.domain.model.*;
import com.solra.auth.domain.repository.LoginSessionRepository;
import com.solra.auth.domain.repository.UserAccountRepository;
import com.solra.common.exception.SolraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthDomainService covering AUTH-001 and AUTH-004.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthDomainService")
class AuthDomainServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private LoginSessionRepository loginSessionRepository;

    @Mock
    private AuthDomainService.PasswordEncoder passwordEncoder;

    private AuthDomainService authDomainService;

    private static final String TEST_PHONE = "13800138000";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "SecurePass123";
    private static final String TEST_PASSWORD_HASH = "$2a$10$hashedPasswordValue";
    private static final String TEST_DISPLAY_NAME = "Test User";
    private static final String TEST_USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        authDomainService = new AuthDomainService(
                userAccountRepository,
                loginSessionRepository,
                passwordEncoder
        );
        lenient().when(passwordEncoder.encode(anyString())).thenReturn(TEST_PASSWORD_HASH);
    }

    // ============================================================
    // AUTH-001: Phone Registration
    // ============================================================
    @Nested
    @DisplayName("registerByPhone")
    class RegisterByPhoneTests {

        @Test
        @DisplayName("should register user with valid phone")
        void shouldRegisterWithValidPhone() {
            when(userAccountRepository.existsByPhone(TEST_PHONE)).thenReturn(false);
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserAccount result = authDomainService.registerByPhone(TEST_PHONE, TEST_PASSWORD, TEST_DISPLAY_NAME);

            assertThat(result).isNotNull();
            verify(passwordEncoder).encode(TEST_PASSWORD);
            verify(userAccountRepository).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("should throw exception for invalid phone format")
        void shouldThrowForInvalidPhone() {
            assertThatThrownBy(() ->
                    authDomainService.registerByPhone("12345", TEST_PASSWORD, TEST_DISPLAY_NAME))
                    .isInstanceOf(SolraException.InvalidArgumentException.class)
                    .hasMessageContaining("phone");
        }

        @Test
        @DisplayName("should throw exception when phone already registered")
        void shouldThrowWhenPhoneAlreadyRegistered() {
            when(userAccountRepository.existsByPhone(TEST_PHONE)).thenReturn(true);

            assertThatThrownBy(() ->
                    authDomainService.registerByPhone(TEST_PHONE, TEST_PASSWORD, TEST_DISPLAY_NAME))
                    .isInstanceOf(SolraException.AlreadyExistsException.class)
                    .hasMessageContaining("Phone already registered");
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePassword() {
            when(userAccountRepository.existsByPhone(TEST_PHONE)).thenReturn(false);
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            authDomainService.registerByPhone(TEST_PHONE, TEST_PASSWORD, TEST_DISPLAY_NAME);

            verify(passwordEncoder).encode(TEST_PASSWORD);
        }
    }

    // ============================================================
    // AUTH-001: Username Registration
    // ============================================================
    @Nested
    @DisplayName("registerByUsername")
    class RegisterByUsernameTests {

        @Test
        @DisplayName("should register user with valid username")
        void shouldRegisterWithValidUsername() {
            when(userAccountRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserAccount result = authDomainService.registerByUsername(TEST_USERNAME, TEST_PASSWORD, TEST_DISPLAY_NAME);

            assertThat(result).isNotNull();
            verify(userAccountRepository).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("should throw for username shorter than 3 characters")
        void shouldThrowForShortUsername() {
            assertThatThrownBy(() ->
                    authDomainService.registerByUsername("ab", TEST_PASSWORD, TEST_DISPLAY_NAME))
                    .isInstanceOf(SolraException.InvalidArgumentException.class)
                    .hasMessageContaining("3-32");
        }

        @Test
        @DisplayName("should throw for username longer than 32 characters")
        void shouldThrowForLongUsername() {
            String longName = "a".repeat(33);
            assertThatThrownBy(() ->
                    authDomainService.registerByUsername(longName, TEST_PASSWORD, TEST_DISPLAY_NAME))
                    .isInstanceOf(SolraException.InvalidArgumentException.class)
                    .hasMessageContaining("3-32");
        }

        @Test
        @DisplayName("should throw when username already taken")
        void shouldThrowWhenUsernameTaken() {
            when(userAccountRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

            assertThatThrownBy(() ->
                    authDomainService.registerByUsername(TEST_USERNAME, TEST_PASSWORD, TEST_DISPLAY_NAME))
                    .isInstanceOf(SolraException.AlreadyExistsException.class);
        }
    }

    // ============================================================
    // AUTH-001: Phone Login
    // ============================================================
    @Nested
    @DisplayName("loginByPhone")
    class LoginByPhoneTests {

        @Test
        @DisplayName("should login successfully with correct credentials")
        void shouldLoginWithCorrectCredentials() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);

            UserAccount result = authDomainService.loginByPhone(TEST_PHONE, TEST_PASSWORD);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw when phone not registered")
        void shouldThrowWhenPhoneNotRegistered() {
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authDomainService.loginByPhone(TEST_PHONE, TEST_PASSWORD))
                    .isInstanceOf(SolraException.UnauthorizedException.class)
                    .hasMessageContaining("not registered");
        }

        @Test
        @DisplayName("should throw for wrong password")
        void shouldThrowForWrongPassword() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches("WrongPassword", TEST_PASSWORD_HASH)).thenReturn(false);

            assertThatThrownBy(() ->
                    authDomainService.loginByPhone(TEST_PHONE, "WrongPassword"))
                    .isInstanceOf(SolraException.UnauthorizedException.class)
                    .hasMessageContaining("Invalid password");
        }

        @Test
        @DisplayName("should throw when account is suspended")
        void shouldThrowWhenAccountSuspended() {
            UserAccount account = createSuspendedAccount();
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(account));

            assertThatThrownBy(() ->
                    authDomainService.loginByPhone(TEST_PHONE, TEST_PASSWORD))
                    .isInstanceOf(SolraException.UnauthorizedException.class);
        }
    }

    // ============================================================
    // AUTH-001: Credential Login (auto-detect)
    // ============================================================
    @Nested
    @DisplayName("loginByCredential")
    class LoginByCredentialTests {

        @Test
        @DisplayName("should auto-detect phone credential")
        void shouldAutoDetectPhone() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);

            UserAccount result = authDomainService.loginByCredential(TEST_PHONE, TEST_PASSWORD);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should auto-detect username credential")
        void shouldAutoDetectUsername() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);

            UserAccount result = authDomainService.loginByCredential(TEST_USERNAME, TEST_PASSWORD);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should auto-detect email credential")
        void shouldAutoDetectEmail() {
            String email = "test@example.com";
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_PASSWORD_HASH)).thenReturn(true);

            UserAccount result = authDomainService.loginByCredential(email, TEST_PASSWORD);

            assertThat(result).isNotNull();
        }
    }

    // ============================================================
    // AUTH-001: Verification Code Login
    // ============================================================
    @Nested
    @DisplayName("loginByPhoneCode")
    class LoginByPhoneCodeTests {

        @Test
        @DisplayName("should login existing user with correct verification code")
        void shouldLoginExistingUser() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(account));

            UserAccount result = authDomainService.loginByPhoneCode(TEST_PHONE, "123456", "123456");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should auto-register new user with verification code")
        void shouldAutoRegisterNewUser() {
            when(userAccountRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.empty());
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserAccount result = authDomainService.loginByPhoneCode(TEST_PHONE, "123456", "123456");

            assertThat(result).isNotNull();
            verify(userAccountRepository).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("should throw for wrong verification code")
        void shouldThrowForWrongCode() {
            assertThatThrownBy(() ->
                    authDomainService.loginByPhoneCode(TEST_PHONE, "123456", "654321"))
                    .isInstanceOf(SolraException.UnauthorizedException.class)
                    .hasMessageContaining("verification code");
        }
    }

    // ============================================================
    // AUTH-004: Real-Name Verification
    // ============================================================
    @Nested
    @DisplayName("real-name verification (AUTH-004)")
    class RealNameVerificationTests {

        @Test
        @DisplayName("should submit real-name verification")
        void shouldSubmitRealNameVerification() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RealNameInfo info = RealNameInfo.create("张三", "110101199001011234");
            UserAccount result = authDomainService.submitRealNameVerification(TEST_USER_ID, info);

            assertThat(result).isNotNull();
            verify(userAccountRepository).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("should throw when real-name already verified")
        void shouldThrowWhenAlreadyVerified() {
            UserAccount account = createActiveAccount();
            RealNameInfo verifiedInfo = RealNameInfo.create("张三", "110101199001011234");
            verifiedInfo.markVerified();
            account.submitRealNameVerification(verifiedInfo);
            account.approveRealNameVerification();

            when(userAccountRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(account));

            RealNameInfo newInfo = RealNameInfo.create("李四", "110101199001011235");
            assertThatThrownBy(() ->
                    authDomainService.submitRealNameVerification(TEST_USER_ID, newInfo))
                    .isInstanceOf(SolraException.AlreadyExistsException.class);
        }

        @Test
        @DisplayName("should approve real-name verification")
        void shouldApproveRealNameVerification() {
            UserAccount account = createActiveAccount();
            RealNameInfo info = RealNameInfo.create("张三", "110101199001011234");
            account.submitRealNameVerification(info);

            when(userAccountRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserAccount result = authDomainService.approveRealNameVerification(TEST_USER_ID);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should detect unverified minor protection status")
        void shouldDetectUnverified() {
            UserAccount account = createActiveAccount();
            when(userAccountRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(account));

            AuthDomainService.MinorProtectionStatus status =
                    authDomainService.checkMinorProtection(TEST_USER_ID);

            assertThat(status).isEqualTo(AuthDomainService.MinorProtectionStatus.UNVERIFIED);
        }

        @Test
        @DisplayName("should detect adult protection status")
        void shouldDetectAdult() {
            UserAccount account = createActiveAccount();
            RealNameInfo adultInfo = RealNameInfo.create("张三", "110101199001011234"); // ~34 years old
            adultInfo.markVerified();
            account.submitRealNameVerification(adultInfo);
            account.approveRealNameVerification();

            when(userAccountRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(account));

            AuthDomainService.MinorProtectionStatus status =
                    authDomainService.checkMinorProtection(TEST_USER_ID);

            assertThat(status).isEqualTo(AuthDomainService.MinorProtectionStatus.ADULT);
        }
    }

    // ============================================================
    // Helper methods
    // ============================================================
    private UserAccount createActiveAccount() {
        UserAccount account = UserAccount.registerByPhone(TEST_PHONE, TEST_PASSWORD_HASH, TEST_DISPLAY_NAME);
        account.setId(TEST_USER_ID);
        return account;
    }

    private UserAccount createSuspendedAccount() {
        UserAccount account = UserAccount.registerByPhone(TEST_PHONE, TEST_PASSWORD_HASH, TEST_DISPLAY_NAME);
        account.setId(TEST_USER_ID);
        account.suspend();
        return account;
    }
}
