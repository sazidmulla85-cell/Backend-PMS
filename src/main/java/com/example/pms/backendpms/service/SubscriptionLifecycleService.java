package com.example.pms.backendpms.service;

import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.SubscriptionStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionLifecycleService {

  public boolean refreshStatus(Property property) {
    boolean changed = false;
    LocalDate today = LocalDate.now();

    if (property.getSubscriptionStartDate() == null) {
      property.setSubscriptionStartDate(today);
      changed = true;
    }

    if (property.getRenewalDate() == null) {
      property.setRenewalDate(property.getSubscriptionStartDate().plusMonths(1));
      changed = true;
    }

    if (property.getSubscriptionStatus() == null) {
      property.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
      changed = true;
    }

    SubscriptionStatus currentStatus = property.getSubscriptionStatus();
    if (currentStatus == SubscriptionStatus.SUSPENDED) {
      return changed;
    }

    if (currentStatus == SubscriptionStatus.TRIAL && !property.getRenewalDate().isBefore(today)) {
      return changed;
    }

    SubscriptionStatus computedStatus;
    if (property.getRenewalDate().isBefore(today)) {
      computedStatus = SubscriptionStatus.OVERDUE;
    } else if (!property.getRenewalDate().isAfter(today.plusDays(7))) {
      computedStatus = SubscriptionStatus.DUE_SOON;
    } else {
      computedStatus = SubscriptionStatus.ACTIVE;
    }

    if (computedStatus != property.getSubscriptionStatus()) {
      property.setSubscriptionStatus(computedStatus);
      changed = true;
    }

    return changed;
  }
}
