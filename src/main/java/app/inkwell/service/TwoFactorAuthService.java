package app.inkwell.service;

import app.inkwell.model.User;
import app.inkwell.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TwoFactorAuthService {

  private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthService.class);
  private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
  private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
  private final TimeProvider timeProvider = new SystemTimeProvider();
  private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
  private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

  @Autowired
  private UserRepository userRepository;

  /**
   * Generates a new secret for TOTP
   * @return the generated secret
   */
  public String generateSecret() {
      return secretGenerator.generate();
  }

  /**
   * Generates a QR code for the given user and secret
   * @param user the user
   * @param secret the TOTP secret
   * @return a data URI for the QR code image
   */
  public String generateQrCodeImageUri(User user, String secret) {
      QrData data = new QrData.Builder()
              .label(user.getEmail())
              .secret(secret)
              .issuer("Inkwell")
              .algorithm(HashingAlgorithm.SHA1)
              .digits(6)
              .period(30)
              .build();

      try {
          byte[] imageData = qrGenerator.generate(data);
          return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
      } catch (QrGenerationException e) {
          logger.error("Error generating QR code", e);
          return null;
      }
  }

  /**
   * Verifies a TOTP code against a secret
   * @param code the code to verify
   * @param secret the secret (may include device tokens after a colon)
   * @return true if the code is valid, false otherwise
   */
  public boolean verifyCode(String code, String secret) {
      // The secret might contain device tokens after a colon, extract just the TOTP secret
      String totpSecret = secret.contains(":") ? secret.split(":")[0] : secret;
      return codeVerifier.isValidCode(totpSecret, code);
  }

  /**
   * Enables 2FA for a user
   * @param user the user
   * @param secret the TOTP secret
   * @param code the verification code
   * @return true if 2FA was enabled successfully, false otherwise
   */
  public boolean enableTwoFactorAuth(User user, String secret, String code) {
      if (verifyCode(code, secret)) {
          user.setMfaSecret(secret);
          user.setUsingMfa(true);
          userRepository.save(user);
          return true;
      }
      return false;
  }

  /**
   * Disables 2FA for a user
   * @param user the user
   */
  public void disableTwoFactorAuth(User user) {
      user.setMfaSecret(null);
      user.setUsingMfa(false);
      userRepository.save(user);
  }

  /**
   * Adds a remember-device token for a user
   * @param user the user
   * @return the generated device token
   */
  public String addRememberDeviceToken(User user) {
      String deviceToken = UUID.randomUUID().toString();
      String mfaSecret = user.getMfaSecret();
      
      // If the secret already has tokens, add the new one
      if (mfaSecret.contains(":")) {
          String[] parts = mfaSecret.split(":");
          String totpSecret = parts[0];
          List<String> tokens = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length)));
          // Limit to 5 remembered devices
          if (tokens.size() >= 5) {
              tokens.remove(0); // Remove the oldest token
          }
          tokens.add(deviceToken);
          
          // Rebuild the MFA secret with tokens
          StringBuilder newSecret = new StringBuilder(totpSecret);
          for (String token : tokens) {
              newSecret.append(":").append(token);
          }
          user.setMfaSecret(newSecret.toString());
      } else {
          // First token for this user
          user.setMfaSecret(mfaSecret + ":" + deviceToken);
      }
      
      userRepository.save(user);
      return deviceToken;
  }

  /**
   * Checks if a device token is valid for a user
   * @param user the user
   * @param deviceToken the device token to check
   * @return true if the token is valid
   */
  public boolean isValidDeviceToken(User user, String deviceToken) {
    if (deviceToken == null || deviceToken.trim().isEmpty()) {
        logger.debug("Device token is null or empty");
        return false;
    }
    
    if (!user.isUsingMfa() || user.getMfaSecret() == null || !user.getMfaSecret().contains(":")) {
        logger.debug("User doesn't have 2FA enabled or no device tokens stored. User: {}, Using MFA: {}, MFA Secret: {}", 
                 user.getUsername(), user.isUsingMfa(), user.getMfaSecret() != null ? "exists" : "null");
        return false;
    }
    
    String[] parts = user.getMfaSecret().split(":");
    logger.debug("Checking device token {} against {} stored tokens", deviceToken, parts.length - 1);
    
    for (int i = 1; i < parts.length; i++) {
        if (parts[i].equals(deviceToken)) {
            logger.info("Found matching device token for user: {}", user.getUsername());
            return true;
        }
    }
    
    logger.debug("No matching device token found for user: {}", user.getUsername());
    return false;
  }
}