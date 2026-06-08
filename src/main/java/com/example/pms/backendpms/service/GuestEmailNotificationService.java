package com.example.pms.backendpms.service;

import com.example.pms.backendpms.model.Reservation;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class GuestEmailNotificationService {

  private static final Logger log = LoggerFactory.getLogger(GuestEmailNotificationService.class);

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final boolean emailEnabled;
  private final boolean sendCheckIn;
  private final boolean sendCheckOut;
  private final String fromAddress;

  public GuestEmailNotificationService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${pms.notifications.email.enabled:false}") boolean emailEnabled,
      @Value("${pms.notifications.email.send-check-in:true}") boolean sendCheckIn,
      @Value("${pms.notifications.email.send-check-out:true}") boolean sendCheckOut,
      @Value("${pms.notifications.email.from:no-reply@hotelpms.local}") String fromAddress
  ) {
    this.mailSenderProvider = mailSenderProvider;
    this.emailEnabled = emailEnabled;
    this.sendCheckIn = sendCheckIn;
    this.sendCheckOut = sendCheckOut;
    this.fromAddress = fromAddress;
  }

  public void sendCheckInNotification(Reservation reservation) {
    if (!sendCheckIn) {
      return;
    }

    sendReservationEmail(
        reservation,
        "Check-In Confirmed • " + reservation.getProperty().getName(),
        """
        Dear %s,

        Your check-in has been completed successfully at %s.

        Reservation Number: %s
        Check-In Date: %s
        Planned Check-Out Date: %s
        Room Amount: %s
        Balance Amount: %s

        If you need anything during your stay, please contact the front desk.

        Regards,
        %s
        """.formatted(
            reservation.getPrimaryGuest().getFullName(),
            reservation.getProperty().getName(),
            reservation.getReservationNumber(),
            reservation.getCheckInDate(),
            reservation.getCheckOutDate(),
            formatCurrency(reservation.getRoomAmount()),
            formatCurrency(reservation.getBalanceAmount()),
            reservation.getProperty().getName()
        )
    );
  }

  public void sendCheckOutNotification(Reservation reservation) {
    if (!sendCheckOut) {
      return;
    }

    sendReservationEmail(
        reservation,
        "Check-Out Completed • " + reservation.getProperty().getName(),
        """
        Dear %s,

        Your check-out has been completed successfully at %s.

        Reservation Number: %s
        Check-In Date: %s
        Check-Out Date: %s
        Total Amount: %s
        Paid Amount: %s
        Balance Amount: %s

        Thank you for staying with us. We look forward to hosting you again.

        Regards,
        %s
        """.formatted(
            reservation.getPrimaryGuest().getFullName(),
            reservation.getProperty().getName(),
            reservation.getReservationNumber(),
            reservation.getCheckInDate(),
            reservation.getCheckOutDate(),
            formatCurrency(reservation.getTotalAmount()),
            formatCurrency(reservation.getTotalAmount().subtract(reservation.getBalanceAmount())),
            formatCurrency(reservation.getBalanceAmount()),
            reservation.getProperty().getName()
        )
    );
  }

  private void sendReservationEmail(Reservation reservation, String subject, String body) {
    String guestEmail = reservation.getPrimaryGuest() != null ? reservation.getPrimaryGuest().getEmail() : null;
    if (guestEmail == null || guestEmail.isBlank()) {
      return;
    }

    if (!emailEnabled) {
      log.info("Email notifications disabled. Skipping {} email for reservation {}", subject, reservation.getReservationNumber());
      return;
    }

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.warn("Email notifications enabled but no JavaMailSender is configured. Skipping reservation {}", reservation.getReservationNumber());
      return;
    }

    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(guestEmail);
      message.setSubject(subject);
      message.setText(body);
      mailSender.send(message);
    } catch (Exception exception) {
      log.warn("Failed to send guest email for reservation {}: {}", reservation.getReservationNumber(), exception.getMessage());
    }
  }

  private String formatCurrency(BigDecimal amount) {
    BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
    return "INR " + safeAmount.setScale(2, java.math.RoundingMode.HALF_UP);
  }
}
