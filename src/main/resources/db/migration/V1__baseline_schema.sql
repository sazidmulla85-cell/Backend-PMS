CREATE TABLE IF NOT EXISTS organization (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NOT NULL,
  billing_address VARCHAR(255) NULL,
  billing_email VARCHAR(255) NULL,
  billing_phone VARCHAR(255) NULL,
  gst_number VARCHAR(255) NULL,
  legal_name VARCHAR(255) NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_organization_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS platform_plan (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NOT NULL,
  base_monthly_amount DOUBLE NOT NULL,
  included_rooms INT NOT NULL,
  per_room_amount DOUBLE NOT NULL,
  description VARCHAR(500) NULL,
  module_codes_csv VARCHAR(1000) NULL,
  code VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform_plan_code (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NOT NULL,
  last_login_at TIMESTAMP(6) NULL,
  organization_id BIGINT NOT NULL,
  property_id BIGINT NULL,
  email VARCHAR(255) NULL,
  full_name VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(255) NOT NULL,
  role ENUM('FRONT_DESK','HOTEL_OWNER','PROPERTY_MANAGER','SUPER_ADMIN') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_account_email (email),
  UNIQUE KEY uk_user_account_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS property (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NOT NULL,
  auto_renew BOOLEAN NULL,
  monthly_subscription_amount DOUBLE NULL,
  renewal_date DATE NULL,
  subscribed_room_count INT NULL,
  subscription_start_date DATE NULL,
  organization_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  commercial_notes VARCHAR(2000) NULL,
  support_notes VARCHAR(2000) NULL,
  account_manager VARCHAR(255) NULL,
  city VARCHAR(255) NULL,
  code VARCHAR(255) NOT NULL,
  country VARCHAR(255) NULL,
  currency_code VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  module_entitlements_csv VARCHAR(255) NULL,
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(255) NULL,
  state VARCHAR(255) NULL,
  subscription_plan VARCHAR(255) NULL,
  timezone VARCHAR(255) NULL,
  crm_stage ENUM('ACTIVE_CUSTOMER','CHURNED','DEMO','LEAD','TRIAL') NULL,
  subscription_status ENUM('ACTIVE','DUE_SOON','OVERDUE','SUSPENDED','TRIAL') NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_code (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  actor_user_id BIGINT NULL,
  property_id BIGINT NULL,
  description VARCHAR(1000) NOT NULL,
  entity_id VARCHAR(255) NOT NULL,
  entity_type VARCHAR(255) NOT NULL,
  action ENUM('CREATE','DELETE','LOGIN','UPDATE','VIEW') NOT NULL,
  module ENUM('ADMIN','AUDIT_LOGS','AUTH','DASHBOARD','RESERVATIONS','ROOMS','STAY_VIEW') NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS company (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NULL,
  property_id BIGINT NOT NULL,
  billing_address VARCHAR(255) NULL,
  city VARCHAR(255) NULL,
  contact_person VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  gst_vat VARCHAR(255) NULL,
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(255) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS guest (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  date_of_birth DATE NULL,
  vip BOOLEAN NOT NULL,
  property_id BIGINT NOT NULL,
  address VARCHAR(255) NULL,
  city VARCHAR(255) NULL,
  country VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  full_name VARCHAR(255) NULL,
  gender VARCHAR(255) NULL,
  id_number VARCHAR(255) NULL,
  id_type VARCHAR(255) NULL,
  notes VARCHAR(255) NULL,
  phone VARCHAR(255) NULL,
  postal_code VARCHAR(255) NULL,
  state VARCHAR(255) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS password_reset_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  used_at TIMESTAMP(6) NULL,
  user_account_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_token_hash (token_hash)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  adults INT NULL,
  balance_amount DECIMAL(38,2) NULL,
  booking_date DATE NULL,
  check_in_date DATE NULL,
  check_out_date DATE NULL,
  children INT NULL,
  complimentary BOOLEAN NULL,
  discount_amount DECIMAL(38,2) NULL,
  group_booking BOOLEAN NULL,
  nights INT NULL,
  room_amount DECIMAL(38,2) NULL,
  rooms_count INT NULL,
  tax_amount DECIMAL(38,2) NULL,
  total_amount DECIMAL(38,2) NULL,
  booked_by_user_id BIGINT NULL,
  company_id BIGINT NULL,
  primary_guest_id BIGINT NOT NULL,
  property_id BIGINT NOT NULL,
  group_code VARCHAR(255) NULL,
  meal_plan VARCHAR(255) NULL,
  rate_plan VARCHAR(255) NULL,
  reservation_number VARCHAR(255) NOT NULL,
  special_requests VARCHAR(255) NULL,
  bill_to_type ENUM('COMPANY','GUEST') NULL,
  payment_method ENUM('ADVANCE','BANK_TRANSFER','BILL_TO_COMPANY','CARD','CASH','POSTPAID','UPI') NULL,
  payment_status ENUM('PAID','PARTIAL','PENDING','REFUNDED') NULL,
  source ENUM('CORPORATE','ONLINE','OTA','PMS','WALK_IN') NOT NULL,
  status ENUM('CANCELLED','CHECKED_IN','CHECKED_OUT','CONFIRMED','NO_SHOW') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_number (reservation_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS guest_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  file_size BIGINT NULL,
  guest_id BIGINT NOT NULL,
  property_id BIGINT NOT NULL,
  reservation_id BIGINT NOT NULL,
  uploaded_at TIMESTAMP(6) NULL,
  uploaded_by_user_id BIGINT NULL,
  storage_path VARCHAR(1000) NOT NULL,
  content_type VARCHAR(255) NOT NULL,
  document_type VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  stored_file_name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  amount DECIMAL(38,2) NULL,
  payment_date TIMESTAMP(6) NULL,
  received_by_user_id BIGINT NULL,
  reservation_id BIGINT NOT NULL,
  notes VARCHAR(255) NULL,
  reference_number VARCHAR(255) NULL,
  payment_method ENUM('ADVANCE','BANK_TRANSFER','BILL_TO_COMPANY','CARD','CASH','POSTPAID','UPI') NULL,
  status ENUM('PAID','PARTIAL','PENDING','REFUNDED') NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS platform_invoice (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  billing_month DATE NOT NULL,
  due_date DATE NOT NULL,
  paid_amount DOUBLE NOT NULL,
  total_amount DOUBLE NOT NULL,
  property_id BIGINT NOT NULL,
  notes VARCHAR(1000) NULL,
  invoice_number VARCHAR(255) NOT NULL,
  plan_name VARCHAR(255) NOT NULL,
  status ENUM('CANCELLED','DRAFT','OVERDUE','PAID','PARTIAL','PENDING') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform_invoice_number (invoice_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS platform_invoice_payment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  amount DOUBLE NOT NULL,
  invoice_id BIGINT NOT NULL,
  received_at TIMESTAMP(6) NOT NULL,
  payment_method VARCHAR(255) NOT NULL,
  reference_number VARCHAR(255) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS property_communication_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  property_id BIGINT NOT NULL,
  message VARCHAR(2000) NOT NULL,
  actor_name VARCHAR(255) NULL,
  subject VARCHAR(255) NOT NULL,
  channel ENUM('CALL','EMAIL','NOTE','SMS') NOT NULL,
  status ENUM('FAILED','PENDING','SENT') NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservation_charge (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  amount DECIMAL(38,2) NULL,
  charge_date TIMESTAMP(6) NULL,
  created_by_user_id BIGINT NULL,
  reservation_id BIGINT NOT NULL,
  description VARCHAR(255) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS room_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NOT NULL,
  base_occupancy INT NULL,
  base_rate DOUBLE NULL,
  max_occupancy INT NULL,
  property_id BIGINT NOT NULL,
  code VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS room (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  active BOOLEAN NOT NULL,
  property_id BIGINT NOT NULL,
  room_type_id BIGINT NOT NULL,
  floor_name VARCHAR(255) NULL,
  room_number VARCHAR(255) NOT NULL,
  housekeeping_status ENUM('CLEAN','DIRTY','INSPECTED') NOT NULL,
  status ENUM('AVAILABLE','MAINTENANCE','OCCUPIED','OUT_OF_ORDER') NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservation_room (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NULL,
  updated_at TIMESTAMP(6) NULL,
  assigned_from DATE NULL,
  assigned_to DATE NULL,
  nightly_rate DECIMAL(38,2) NULL,
  reservation_id BIGINT NOT NULL,
  room_id BIGINT NULL,
  room_type_id BIGINT NOT NULL,
  status ENUM('ASSIGNED','CANCELLED','CHECKED_IN','CHECKED_OUT') NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

ALTER TABLE audit_log
  ADD CONSTRAINT fk_audit_log_actor_user FOREIGN KEY (actor_user_id) REFERENCES user_account(id),
  ADD CONSTRAINT fk_audit_log_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE company
  ADD CONSTRAINT fk_company_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE guest
  ADD CONSTRAINT fk_guest_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE guest_document
  ADD CONSTRAINT fk_guest_document_guest FOREIGN KEY (guest_id) REFERENCES guest(id),
  ADD CONSTRAINT fk_guest_document_property FOREIGN KEY (property_id) REFERENCES property(id),
  ADD CONSTRAINT fk_guest_document_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id),
  ADD CONSTRAINT fk_guest_document_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES user_account(id);

ALTER TABLE password_reset_token
  ADD CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_account_id) REFERENCES user_account(id);

ALTER TABLE payment
  ADD CONSTRAINT fk_payment_received_by FOREIGN KEY (received_by_user_id) REFERENCES user_account(id),
  ADD CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id);

ALTER TABLE platform_invoice
  ADD CONSTRAINT fk_platform_invoice_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE platform_invoice_payment
  ADD CONSTRAINT fk_platform_invoice_payment_invoice FOREIGN KEY (invoice_id) REFERENCES platform_invoice(id);

ALTER TABLE property
  ADD CONSTRAINT fk_property_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
  ADD CONSTRAINT fk_property_owner_user FOREIGN KEY (owner_user_id) REFERENCES user_account(id);

ALTER TABLE property_communication_log
  ADD CONSTRAINT fk_property_communication_log_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE reservation
  ADD CONSTRAINT fk_reservation_booked_by FOREIGN KEY (booked_by_user_id) REFERENCES user_account(id),
  ADD CONSTRAINT fk_reservation_company FOREIGN KEY (company_id) REFERENCES company(id),
  ADD CONSTRAINT fk_reservation_primary_guest FOREIGN KEY (primary_guest_id) REFERENCES guest(id),
  ADD CONSTRAINT fk_reservation_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE reservation_charge
  ADD CONSTRAINT fk_reservation_charge_created_by FOREIGN KEY (created_by_user_id) REFERENCES user_account(id),
  ADD CONSTRAINT fk_reservation_charge_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id);

ALTER TABLE reservation_room
  ADD CONSTRAINT fk_reservation_room_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id),
  ADD CONSTRAINT fk_reservation_room_room FOREIGN KEY (room_id) REFERENCES room(id),
  ADD CONSTRAINT fk_reservation_room_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id);

ALTER TABLE room
  ADD CONSTRAINT fk_room_property FOREIGN KEY (property_id) REFERENCES property(id),
  ADD CONSTRAINT fk_room_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id);

ALTER TABLE room_type
  ADD CONSTRAINT fk_room_type_property FOREIGN KEY (property_id) REFERENCES property(id);

ALTER TABLE user_account
  ADD CONSTRAINT fk_user_account_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
  ADD CONSTRAINT fk_user_account_property FOREIGN KEY (property_id) REFERENCES property(id);
