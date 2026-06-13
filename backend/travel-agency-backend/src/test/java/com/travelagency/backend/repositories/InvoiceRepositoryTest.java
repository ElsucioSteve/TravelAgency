package com.travelagency.backend.repositories;

import com.travelagency.backend.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    private PaymentEntity payment;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        travelPackageRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setEmail("test@test.com");
        user.setFullName("Test User");
        user.setKeycloakId("kc-test");
        user.setUserRole("CLIENT");
        user.setAccountStatus("ACTIVE");
        user = userRepository.save(user);

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setName("Test Pkg");
        pkg.setDestination("Chile");
        pkg.setStartDate(LocalDate.now().plusDays(10));
        pkg.setEndDate(LocalDate.now().plusDays(15));
        pkg.setBasePrice(new BigDecimal("500000"));
        pkg.setTotalSlots(10);
        pkg.setAvailableSlots(10);
        pkg.setTravelType("NATIONAL");
        pkg.setSeason("HIGH");
        pkg.setCategory("ADVENTURE");
        pkg.setPackageStatus("AVAILABLE");
        pkg.setIsVisibleWeb(true);
        pkg = travelPackageRepository.save(pkg);

        BookingEntity booking = new BookingEntity();
        booking.setUser(user);
        booking.setTravelPackage(pkg);
        booking.setBookingDate(LocalDateTime.now());
        booking.setPassengerCount(1);
        booking.setGrossAmount(new BigDecimal("500000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("500000"));
        booking.setBookingStatus("CONFIRMED");
        booking.setBookingCode("BK-INV-001");
        booking = bookingRepository.save(booking);

        payment = new PaymentEntity();
        payment.setBooking(booking);
        payment.setAmountPaid(new BigDecimal("500000"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setPaymentStatus("APPROVED");
        payment.setInternalTransactionId("TXN-INV-001");
        payment = paymentRepository.save(payment);
    }

    private InvoiceEntity createInvoice(String folio, String type) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setPayment(payment);
        invoice.setFolioNumber(folio);
        invoice.setInvoiceType(type);
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setSnapshotDetails("Sample invoice details");
        return invoice;
    }

    @Test
    @DisplayName("save() should persist an invoice")
        // Guarda una factura/boleta y verifica el ID
    void save_ShouldPersist() {
        InvoiceEntity saved = invoiceRepository.save(createInvoice("B-2026-00001", "RECEIPT"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFolioNumber()).isEqualTo("B-2026-00001");
    }

    @Test
    @DisplayName("findByFolioNumber() should find by folio")
        // Busca una factura por su numero de folio unico
    void findByFolioNumber_ShouldFind() {
        invoiceRepository.save(createInvoice("F-2026-00005", "INVOICE"));

        Optional<InvoiceEntity> found = invoiceRepository.findByFolioNumber("F-2026-00005");

        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceType()).isEqualTo("INVOICE");
    }

    @Test
    @DisplayName("findByPaymentId() should find by payment ID")
        // Busca la factura asociada a un pago
    void findByPaymentId_ShouldFind() {
        invoiceRepository.save(createInvoice("B-2026-00002", "RECEIPT"));

        Optional<InvoiceEntity> found = invoiceRepository.findByPaymentId(payment.getId());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("countByInvoiceType() should count invoices by type")
        // El servicio usa countByInvoiceType() para generar folios secuenciales
    void countByInvoiceType_ShouldReturnCount() {
        invoiceRepository.save(createInvoice("B-2026-00001", "RECEIPT"));

        long receipts = invoiceRepository.countByInvoiceType("RECEIPT");
        long invoices = invoiceRepository.countByInvoiceType("INVOICE");

        assertThat(receipts).isEqualTo(1);
        assertThat(invoices).isEqualTo(0);
    }

    @Test
    @DisplayName("findByFolioNumber() should return empty when not exists")
        // Verifica que devuelve vacio si el folio no existe
    void findByFolioNumber_WhenNotExists_ShouldReturnEmpty() {
        Optional<InvoiceEntity> found = invoiceRepository.findByFolioNumber("INEXISTENT");

        assertThat(found).isEmpty();
    }
}